package com.helianhealth.agent.service;

import com.helianhealth.agent.common.PageList;
import com.helianhealth.agent.model.domain.InterfaceWorkflowDO;

import java.util.List;
import java.util.Optional;

public interface InterfaceWorkflowService {

    Optional<InterfaceWorkflowDO> findByInterfaceUri(String interfaceUri);

    int save(InterfaceWorkflowDO interfaceInstanceDO);

    PageList<InterfaceWorkflowDO> getAllWorkflows(int pageNum, int pageSize);

    List<InterfaceWorkflowDO> getAllWorkflowsWithoutPaged();

    int update(InterfaceWorkflowDO interfaceInstanceDO);

    void deleteByFlowId(Integer flowId);

    InterfaceWorkflowDO selectByFlowId(Integer flowId);
}
