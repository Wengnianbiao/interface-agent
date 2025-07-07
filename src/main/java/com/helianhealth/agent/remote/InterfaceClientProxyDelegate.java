package com.helianhealth.agent.remote;

import com.helianhealth.agent.enums.NodeType;
import com.helianhealth.agent.model.domain.InterfaceWorkflowNodeDO;
import com.helianhealth.agent.remote.database.DatabaseClientProxy;
import com.helianhealth.agent.remote.http.HttpClientProxy;
import com.helianhealth.agent.remote.mock.MockApiClientProxy;
import com.helianhealth.agent.remote.webService.WebServiceClientProxy;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@AllArgsConstructor
@Component
public class InterfaceClientProxyDelegate implements InterfaceClientProxy {

    private final HttpClientProxy httpClientProxy;

    private final DatabaseClientProxy databaseApiClientProxy;

    private final WebServiceClientProxy webServiceClientProxy;

    private final MockApiClientProxy mockApiClientProxy;

    private final DefaultApiClientProxy defaultApiClientProxy;

    private InterfaceClientProxy getExecuteClientProxy(NodeType type) {
        switch ( type) {
            case DATABASE:
                return databaseApiClientProxy;
            case HTTP:
                return httpClientProxy;
            case WEBSERVICE:
                return webServiceClientProxy;
            case MOCK:
                return mockApiClientProxy;
            default:
                return defaultApiClientProxy;
        }
    }


    @Override
    public Map<String, Object> remoteInvoke(InterfaceWorkflowNodeDO flowNode, Map<String, Object> businessData) {
        // 直接委托给具体的客户端代理，参数处理已经在抽象类中完成
        return getExecuteClientProxy(flowNode.getNodeType())
                .remoteInvoke(flowNode, businessData);
    }
}
