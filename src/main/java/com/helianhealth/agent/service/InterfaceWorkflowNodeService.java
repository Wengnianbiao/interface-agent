package com.helianhealth.agent.service;

import com.helianhealth.agent.common.PageList;
import com.helianhealth.agent.controller.request.workflownode.WorkflowNodeCreateReq;
import com.helianhealth.agent.controller.workflow.reponse.NodeParamConfigExportResponse;
import com.helianhealth.agent.model.domain.InterfaceWorkflowNodeDO;

import java.util.List;
import java.util.Map;

public interface InterfaceWorkflowNodeService {

    InterfaceWorkflowNodeDO selectByNodeId(Integer nodeId);

    PageList<InterfaceWorkflowNodeDO> getAllNodes(Integer flowId, int pageNum, int pageSize);

    List<InterfaceWorkflowNodeDO> getAllNodesWithoutPaged();

    /**
     * 执行工作流节点,对于工作流节点不care上游是jarvis还是啥，只管调用所以用Map是最合适的
     * @param flowNode 工作流节点
     * @param businessData 业务数据,统一入参为Map高度抽象
     * @return 统一出参为Map高度抽象
     */
    Map<String, Object> executeFlowNode(InterfaceWorkflowNodeDO flowNode, Map<String, Object> businessData);

    int save(WorkflowNodeCreateReq nodeCreateReq);

    int update(InterfaceWorkflowNodeDO flowNodeDO);

    void deleteByNodeId(Integer nodeId);

    NodeParamConfigExportResponse exportNodeParamConfig(Integer nodeId);

}
