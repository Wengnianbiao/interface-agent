package com.helianhealth.agent.remote;

import com.helianhealth.agent.model.domain.InterfaceWorkflowNodeDO;
import com.helianhealth.agent.model.dto.ParamTreeNode;

import java.util.List;

public interface ParamResolver {

    Object resolveParamNodes(InterfaceWorkflowNodeDO flowNode, List<ParamTreeNode> params);
}
