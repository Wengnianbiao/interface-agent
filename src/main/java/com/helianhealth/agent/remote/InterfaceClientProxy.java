package com.helianhealth.agent.remote;

import com.helianhealth.agent.model.domain.InterfaceWorkflowNodeDO;

import java.util.Map;

/**
 * 接口调用客户端代理
 */
public interface InterfaceClientProxy {

    /**
     * 三方接口调用
     * @param flowNode 工作流节点
     * @param businessData 业务数据
     * @return 调用结果
     */
    Map<String, Object> remoteInvoke(InterfaceWorkflowNodeDO flowNode, Map<String, Object> businessData);
}
