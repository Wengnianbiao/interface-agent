package com.helianhealth.agent.remote;

import com.helianhealth.agent.mapper.agent.NodeParamConfigMapper;
import com.helianhealth.agent.model.domain.InterfaceWorkflowNodeDO;
import com.helianhealth.agent.model.domain.NodeParamConfigDO;
import com.helianhealth.agent.model.dto.ParamTreeNode;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class DefaultApiClientProxy extends AbstractClientProxy {

    public DefaultApiClientProxy(NodeParamConfigMapper nodeMapper) {
        super(nodeMapper);
    }

    @Override
    public Map<String, Object> doInvoke(InterfaceWorkflowNodeDO flowNode, List<ParamTreeNode> params) {
        // 没有三方调用的API直接返回参数
        return new HashMap<>();
    }

    @Override
    public void processObjectNodeType(NodeParamConfigDO config, List<NodeParamConfigDO> allNodes, Map<String, Object> businessData, Map<String, Object> rootBusinessData, ParamTreeNode node) {

    }

    @Override
    public void processArrayNodeType(NodeParamConfigDO config, List<NodeParamConfigDO> allNodes, Map<String, Object> businessData, Map<String, Object> rootBusinessData, ParamTreeNode node) {

    }
}
