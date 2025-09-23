package com.helianhealth.agent.remote;

import com.helianhealth.agent.model.domain.NodeParamConfigDO;
import com.helianhealth.agent.model.dto.ParamTreeNode;

import java.util.List;
import java.util.Map;

/**
 * 参数节点处理器接口
 */
public interface ParamNodeHandler {

    void processObjectNodeType(NodeParamConfigDO config,
                               List<NodeParamConfigDO> allNodes,
                               Map<String, Object> businessData,
                               Map<String, Object> rootBusinessData,
                               ParamTreeNode node);

    void processArrayNodeType(NodeParamConfigDO config,
                              List<NodeParamConfigDO> allNodes,
                              Map<String, Object> businessData,
                              Map<String, Object> rootBusinessData,
                              ParamTreeNode node);
}
