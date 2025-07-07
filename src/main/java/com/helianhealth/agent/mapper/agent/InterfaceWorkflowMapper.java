package com.helianhealth.agent.mapper.agent;

import com.helianhealth.agent.model.domain.InterfaceWorkflowDO;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface InterfaceWorkflowMapper extends Mapper<InterfaceWorkflowDO> {

    InterfaceWorkflowDO selectByInterfaceUri(String interfaceUri);

    List<InterfaceWorkflowDO> selectAllWorkflows();

    int createWorkflow(InterfaceWorkflowDO workflow);
}
