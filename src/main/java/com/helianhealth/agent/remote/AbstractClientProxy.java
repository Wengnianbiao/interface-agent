package com.helianhealth.agent.remote;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;
import com.helianhealth.agent.enums.MappingType;
import com.helianhealth.agent.enums.NodeType;
import com.helianhealth.agent.mapper.agent.NodeParamConfigMapper;
import com.helianhealth.agent.model.domain.InterfaceWorkflowNodeDO;
import com.helianhealth.agent.model.domain.NodeParamConfigDO;
import com.helianhealth.agent.model.dto.ParamTreeNode;
import com.helianhealth.agent.utils.ExpressionMapperUtils;
import com.helianhealth.agent.utils.ParamNodeUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopContext;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@AllArgsConstructor
public abstract class AbstractClientProxy implements InterfaceClientProxy, ParamNodeHandler {

    private final NodeParamConfigMapper nodeMapper;

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
        List<NodeParamConfigDO> nodeConfigs = nodeMapper.selectPreProcessConfigByNodeId(flowNode.getNodeId());
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
        List<NodeParamConfigDO> nodeConfigs = nodeMapper.selectPostProcessConfigByNodeId(flowNode.getNodeId());
        if (CollectionUtils.isEmpty(nodeConfigs)) {
            return new HashMap<>();
        }

        List<ParamTreeNode> rootNodes = buildParamTree(nodeConfigs, null, response, businessData);

        return convertParamTreeToMap(flowNode, rootNodes);
    }

    private Map<String, Object> convertParamTreeToMap(InterfaceWorkflowNodeDO flowNode, List<ParamTreeNode> paramTree) {

        // 获取节点的元信息
        String metaInfo = flowNode.getMetaInfo();
        if (StringUtils.isEmpty(metaInfo)) {
            // 如果没有元信息，使用默认转换
            return ParamNodeUtils.convertNodesToMap(paramTree);
        }

        try {
            // 解析metaInfo为JSON对象
            JSONObject metaJson = JSON.parseObject(metaInfo);
            // 获取响应类型，默认为json
            String responseType = metaJson.getString("responseType");
            if (StringUtils.isEmpty(responseType)) {
                responseType = "json";
            }

            // 根据响应类型进行不同处理
            if ("xml".equalsIgnoreCase(responseType)) {
                // XML类型处理：可能需要将参数树转换为XML格式的Map结构
                return ParamNodeUtils.convertToXmlFormatMap(paramTree);
            } else {
                NodeType nodeType = flowNode.getNodeType();
                switch (nodeType) {
                    case DATABASE:
                    case HTTP:
                        // HTTP类型处理：可能需要将参数树转换为HTTP请求参数
                        // 数据库类型处理：可能需要将参数树转换为数据库查询语句
                        return ParamNodeUtils.convertNodesToMap(paramTree);
                    case WEBSERVICE:
                        // WebService类型处理：可能需要将参数树转换为WebService请求参数
                        return ParamNodeUtils.convertToJsonFormatMap(paramTree);
                    default:
                        // 默认处理：将参数树转换为JSON格式的Map结构
                        return ParamNodeUtils.convertNodesToMap(paramTree);
                }
            }
        } catch (JSONException e) {
            // 处理JSON解析异常
            log.error("解析节点元信息失败，nodeId: {}, metaInfo: {}", flowNode.getNodeId(), metaInfo, e);
            // 解析失败时使用默认转换
            return ParamNodeUtils.convertNodesToMap(paramTree);
        }
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
                node.setParamValue(parseValueFromBusinessData(config, businessData, sourceBusinessData));
        }

        return node;
    }

    public Object parseValueFromBusinessData(NodeParamConfigDO config, Map<String, Object> businessData, Map<String, Object> sourceBusinessData) {
        if (config == null || businessData == null) {
            return null;
        }

        Object targetValue = null;
        MappingType mappingType = config.getMappingType();
        String mappingRule = config.getMappingRule();

        switch (mappingType) {
            case CONSTANT:
                targetValue = mappingRule;
                break;
            case NAME:
                // 如果是名称的映射，值的话就直接取即可
                targetValue = businessData.get(config.getSourceParamKey());
                break;
            case EXPRESSION:
                // 使用SpEL表达式解析
                targetValue = ExpressionMapperUtils.parser(mappingRule,
                        config.getSourceParamKey() == null ? businessData : businessData.get(config.getSourceParamKey()),
                        sourceBusinessData);
                break;
            case BEAN_EXPRESSION:
                targetValue = parseExpressionWithBeanAccess(mappingRule,
                        config.getSourceParamKey() == null ? businessData : businessData.get(config.getSourceParamKey()),
                        sourceBusinessData);
                break;
        }

        return targetValue;
    }

    /**
     * 使用SpEL表达式解析并执行Spring Bean方法调用
     * 例如：@interfaceFlowNodeMapper.selectByNodeId(#nodeId)
     * @param expression SpEL表达式
     * @param businessData 业务数据
     * @param sourceBusinessData 源业务数据
     * @return 表达式执行结果
     */
    public Object parseExpressionWithBeanAccess(String expression, Object businessData, Map<String, Object> sourceBusinessData) {
        try {
            // 使用ExpressionMapperUtils的增强版本，支持Bean访问
            return ExpressionMapperUtils.parserWithBeanAccess(expression, businessData, sourceBusinessData);
        } catch (Exception e) {
            log.error("SpEL表达式执行失败，表达式: {}", expression, e);
            return null;
        }
    }
}
