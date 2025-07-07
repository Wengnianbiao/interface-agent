package com.helianhealth.agent.remote.mock;

import com.helianhealth.agent.mapper.agent.NodeParamConfigMapper;
import com.helianhealth.agent.model.domain.InterfaceWorkflowNodeDO;
import com.helianhealth.agent.model.domain.NodeParamConfigDO;
import com.helianhealth.agent.model.dto.ParamTreeNode;
import com.helianhealth.agent.remote.AbstractClientProxy;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class MockApiClientProxy extends AbstractClientProxy {

    public MockApiClientProxy(NodeParamConfigMapper nodeMapper) {
        super(nodeMapper);
    }

    @Override
    public Map<String, Object> doInvoke(InterfaceWorkflowNodeDO flowNode, List<ParamTreeNode> params) {
        // Mock的数据来源也是通过可配置后台进行配置
        // 如果配置了，如何实现到底调用哪一个case呢？
        return new HashMap<>();
    }

    @Override
    public void processObjectNodeType(NodeParamConfigDO config, List<NodeParamConfigDO> allNodes, Map<String, Object> businessData, Map<String, Object> rootBusinessData, ParamTreeNode node) {

    }

    @Override
    public void processArrayNodeType(NodeParamConfigDO config, List<NodeParamConfigDO> allNodes, Map<String, Object> businessData, Map<String, Object> rootBusinessData, ParamTreeNode node) {

    }
}
