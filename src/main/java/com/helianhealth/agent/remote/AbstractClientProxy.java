package com.helianhealth.agent.remote;

import com.helianhealth.agent.model.domain.InterfaceWorkflowNodeDO;
import com.helianhealth.agent.model.domain.NodeParamConfigDO;
import com.helianhealth.agent.model.dto.ParamTreeNode;
import com.helianhealth.agent.remote.helper.ResponseConvertHelper;
import com.helianhealth.agent.remote.resolver.ValueResolveService;
import com.helianhealth.agent.service.NodeParamConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
public abstract class AbstractClientProxy implements InterfaceClientProxy, ParamNodeHandler {

    @Autowired
    private NodeParamConfigService nodeParamConfigService;

    @Autowired
    private ValueResolveService valueResolveService;

    @Autowired
    private ResponseConvertHelper responseConvertHelper;

    @Override
    public Map<String, Object> remoteInvoke(InterfaceWorkflowNodeDO flowNode, Map<String, Object> businessData) {
        // 1. 参数前处理
        List<ParamTreeNode> paramBeforeInvoke = preProcess(flowNode, businessData);

        // 2. 执行协议的调用,由子类实现
        AbstractClientProxy proxy = (AbstractClientProxy) AopContext.currentProxy();
        Map<String, Object> remoteInvokeResponse = proxy.doInvoke(flowNode, paramBeforeInvoke);

        // 3. 参数后处理
        return postProcess(flowNode, remoteInvokeResponse, businessData);
    }

    /**
     * 参数前置处理转化成参数树
     * 子类可以根据需要重写此方法
     * @param flowNode 工作流节点
     * @param businessData 业务数据
     * @return 处理后的参数
     */
    protected List<ParamTreeNode> preProcess(InterfaceWorkflowNodeDO flowNode, Map<String, Object> businessData) {
        // 获取的是前置参数配置
        List<NodeParamConfigDO> nodeConfigs = nodeParamConfigService.selectPreProcessConfigByNodeId(flowNode.getNodeId());
        if (CollectionUtils.isEmpty(nodeConfigs)) {
            return new ArrayList<>();
        }
        return buildParamTree(nodeConfigs, null, businessData, businessData);
    }

    /**
     * 参数后置处理
     * @param flowNode 工作流节点
     * @param response 调用响应
     * @param businessData 原始业务数据
     * @return 处理后的结果
     */
    public Map<String, Object> postProcess(InterfaceWorkflowNodeDO flowNode, Map<String, Object> response, Map<String, Object> businessData) {
        List<NodeParamConfigDO> nodeConfigs = nodeParamConfigService.selectPostProcessConfigByNodeId(flowNode.getNodeId());
        if (CollectionUtils.isEmpty(nodeConfigs)) {
            return new HashMap<>();
        }

        List<ParamTreeNode> rootNodes = buildParamTree(nodeConfigs, null, response, businessData);

        return responseConvertHelper.convertResponse(flowNode, rootNodes);
    }

    /**
     * 执行具体的远程调用逻辑，由子类实现
     * @param flowNode 工作流节点
     * @param params 处理后的参数
     * @return 调用结果
     */
    public abstract Map<String, Object> doInvoke(InterfaceWorkflowNodeDO flowNode,
                                                    List<ParamTreeNode> params);

    protected List<ParamTreeNode> buildParamTree(List<NodeParamConfigDO> configs,
                                                 Integer parentId,
                                                 Map<String, Object> businessData,
                                                 Map<String, Object> sourceBusinessData) {
        return doBuildParamTree(configs, parentId, businessData, sourceBusinessData);
    }

    private List<ParamTreeNode> doBuildParamTree(List<NodeParamConfigDO> configs,
                                                 Integer parentId,
                                                 Map<String, Object> businessData,
                                                 Map<String, Object> sourceBusinessData) {
        List<ParamTreeNode> children = new ArrayList<>();

        try {
            // 1、从root节点开始构建参数树,筛选当前父节点的子节点
            List<NodeParamConfigDO> currentNodes = configs.stream()
                    .filter(node -> Objects.equals(node.getParentId(), parentId))
                    .sorted(Comparator.comparingInt(NodeParamConfigDO::getSort))
                    .collect(Collectors.toList());

            // 2、组装每个节点的参数值
            for (NodeParamConfigDO config : currentNodes) {
                children.add(processParamNode(config, configs, businessData, sourceBusinessData));
            }
        } catch (Exception e) {
            log.error("convert param error:", e);
        }

        return children;
    }

    private ParamTreeNode processParamNode(NodeParamConfigDO config,
                                           List<NodeParamConfigDO> allNodes,
                                           Map<String, Object> businessData,
                                           Map<String, Object> sourceBusinessData) {
        ParamTreeNode node = ParamTreeNode.builder()
                .paramKey(config.getTargetParamKey())
                .paramType(config.getTargetParamType())
                .operationType(config.getOperationType())
                .build();

        // 不同的节点类型处理方式不同
        switch (config.getTargetParamType()) {
            case OBJECT:
                // 对象类型处理
                processObjectNodeType(config, allNodes, businessData, sourceBusinessData, node);
                break;
            case ARRAY:
            case PURE_ARRAY:
                // 数组类型处理
                processArrayNodeType(config, allNodes, businessData, sourceBusinessData, node);
                break;
            default:
                // 基本类型：从业务数据中取值（根据mappingRule映射）
                node.setParamValue(valueResolveService.resolveValue(config, businessData, sourceBusinessData));
        }

        return node;
    }
}
