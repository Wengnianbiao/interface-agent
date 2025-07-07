package com.helianhealth.agent.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.helianhealth.agent.common.PageList;
import com.helianhealth.agent.controller.request.workflownode.WorkflowNodeCreateReq;
import com.helianhealth.agent.controller.workflow.reponse.NodeParamConfigExportResponse;
import com.helianhealth.agent.enums.NodeType;
import com.helianhealth.agent.enums.ScheduleParamSourceType;
import com.helianhealth.agent.mapper.agent.InterfaceFlowNodeMapper;
import com.helianhealth.agent.model.domain.InterfaceWorkflowNodeDO;
import com.helianhealth.agent.model.domain.NodeParamConfigDO;
import com.helianhealth.agent.remote.InterfaceClientProxy;
import com.helianhealth.agent.service.InterfaceWorkflowNodeService;
import com.helianhealth.agent.service.NodeParamConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class InterfaceWorkflowNodeServiceImpl implements InterfaceWorkflowNodeService {

    private final InterfaceFlowNodeMapper flowNodeMapper;
    private final InterfaceClientProxy interfaceClientProxy;
    private final NodeParamConfigService nodeParamConfigService;

    @Autowired
    public InterfaceWorkflowNodeServiceImpl(
            InterfaceFlowNodeMapper flowNodeMapper,
            @Qualifier("interfaceClientProxyDelegate") InterfaceClientProxy interfaceClientProxy,
            NodeParamConfigService nodeParamConfigService) {
        this.flowNodeMapper = flowNodeMapper;
        this.interfaceClientProxy = interfaceClientProxy;
        this.nodeParamConfigService = nodeParamConfigService;
    }


    @Override
    public InterfaceWorkflowNodeDO selectByNodeId(Integer flowId) {
        return flowNodeMapper.selectByNodeId(flowId);
    }

    @Override
    public PageList<InterfaceWorkflowNodeDO> getAllNodes(Integer flowId, int pageNum, int pageSize) {
        try {
            PageHelper.startPage(pageNum, pageSize);
            // 将查询结果转换为 PageInfo 对象
            List<InterfaceWorkflowNodeDO> flowNodes = flowNodeMapper.selectAllNodes(flowId);
            PageInfo<InterfaceWorkflowNodeDO> pageInfo = new PageInfo<>(flowNodes);

            // 构建 PageList 对象
            PageList<InterfaceWorkflowNodeDO> pageList = new PageList<>();
            pageList.setRows(flowNodes);
            pageList.setTotal(pageInfo.getTotal());
            pageList.setPageNum(pageInfo.getPageNum());
            pageList.setPageSize(pageInfo.getPageSize());
            return pageList;
        } catch (Exception e) {
            log.error("获取工作流节点列表失败", e);
            throw new RuntimeException("获取工作流节点列表失败: " + e.getMessage());
        }
    }

    @Override
    public List<InterfaceWorkflowNodeDO> getAllNodesWithoutPaged() {
        return flowNodeMapper.selectAllNodes(null);
    }

    @Override
    public Map<String, Object> executeFlowNode(InterfaceWorkflowNodeDO flowNode, Map<String, Object> businessData) {
        // 节点处理就是一个远程调用
        return interfaceClientProxy.remoteInvoke(flowNode, businessData);
    }

    @Override
    public int save(WorkflowNodeCreateReq nodeCreateReq) {
        InterfaceWorkflowNodeDO build = InterfaceWorkflowNodeDO.builder()
                .nodeName(nodeCreateReq.getNodeName())
                .flowId(nodeCreateReq.getFlowId())
                .paramFilterExpr(nodeCreateReq.getParamFilterExpr())
                .nodeType(NodeType.valueOf(nodeCreateReq.getNodeType()))
                .metaInfo(nodeCreateReq.getMetaInfo())
                .scheduleExpr(nodeCreateReq.getScheduleExpr())
                .scheduleParamSourceType(ScheduleParamSourceType.valueOf(nodeCreateReq.getScheduleParamSourceType())).build();

        return flowNodeMapper.createNode(build);
    }

    @Override
    public int update(InterfaceWorkflowNodeDO flowNodeDO) {
        return flowNodeMapper.updateByPrimaryKeySelective(flowNodeDO);
    }

    @Override
    public void deleteByNodeId(Integer nodeId) {
        try {
            // 验证参数
            if (nodeId == null) {
                throw new IllegalArgumentException("参数不能为空，nodeId 必须存在");
            }
            // 执行删除操作
            flowNodeMapper.deleteByPrimaryKey(nodeId);
        } catch (Exception e) {
            log.error("删除节点失败", e);
            throw new RuntimeException("删除节点失败: " + e.getMessage());
        }
    }

    @Override
    public NodeParamConfigExportResponse exportNodeParamConfig(Integer nodeId) {
        try {
            // 1. 查询节点的所有参数配置（包括所有处理类型）
            List<NodeParamConfigDO> paramConfigs = nodeParamConfigService.findAllNodeParamConfigsUnpaged(nodeId, null);

            // 2. 构建导出响应对象
            NodeParamConfigExportResponse response = new NodeParamConfigExportResponse();

            // 3. 设置节点信息
            NodeParamConfigExportResponse.NodeInfo nodeInfo = new NodeParamConfigExportResponse.NodeInfo();
            InterfaceWorkflowNodeDO interfaceWorkflowNodeDO = flowNodeMapper.selectByNodeId(nodeId);
            nodeInfo.setNodeName(interfaceWorkflowNodeDO.getNodeName());
            nodeInfo.setNodeType(interfaceWorkflowNodeDO.getNodeType().name());
            // 注意：如果需要完整的节点信息（如节点名称等），需要从其他服务或表中获取
            response.setNodeInfo(nodeInfo);

            // 4. 构建树形结构
            List<NodeParamConfigExportResponse.NodeParamConfigExportDTO> rootParams =
                    buildParamTree(paramConfigs, null);

            response.setParamsConfig(rootParams);

            return response;
        } catch (Exception e) {
            log.error("导出节点参数配置失败，nodeId={}", nodeId, e);
            throw new RuntimeException("导出节点参数配置失败: " + e.getMessage());
        }
    }

    /**
     * 构建参数配置的树形结构
     * @param paramConfigs 所有参数配置
     * @param parentId 父节点ID，null表示根节点
     * @return 树形结构的参数配置列表
     */
    private List<NodeParamConfigExportResponse.NodeParamConfigExportDTO> buildParamTree(
            List<NodeParamConfigDO> paramConfigs, Integer parentId) {

        return paramConfigs.stream()
                .filter(config -> (parentId == null && config.getParentId() == null) ||
                        (parentId != null && parentId.equals(config.getParentId())))
                .map(config -> {
                    NodeParamConfigExportResponse.NodeParamConfigExportDTO dto = convertToExportDTO(config);
                    // 递归构建子节点
                    List<NodeParamConfigExportResponse.NodeParamConfigExportDTO> childParams =
                            buildParamTree(paramConfigs, config.getConfigId());
                    dto.setChildParam(childParams.isEmpty() ? null : childParams);
                    return dto;
                })
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 将NodeParamConfigDO转换为NodeParamConfigExportDTO（不包含层级关系）
     * @param config NodeParamConfigDO对象
     * @return NodeParamConfigExportDTO对象
     */
    private NodeParamConfigExportResponse.NodeParamConfigExportDTO convertToExportDTO(NodeParamConfigDO config) {
        NodeParamConfigExportResponse.NodeParamConfigExportDTO dto =
                new NodeParamConfigExportResponse.NodeParamConfigExportDTO();

        dto.setProcessType(config.getProcessType());
        dto.setSourceParamKey(config.getSourceParamKey());
        dto.setSourceParamType(config.getSourceParamType());
        dto.setParamDesc(config.getParamDesc());
        dto.setTargetParamKey(config.getTargetParamKey());
        dto.setTargetParamType(config.getTargetParamType());
        dto.setSort(config.getSort());
        dto.setMappingType(config.getMappingType());
        dto.setMappingSource(config.getMappingSource());
        dto.setMappingRule(config.getMappingRule());

        return dto;
    }
}
