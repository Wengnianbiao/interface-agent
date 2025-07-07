package com.helianhealth.agent.remote.http;

import com.helianhealth.agent.enums.ParamType;
import com.helianhealth.agent.model.domain.InterfaceWorkflowNodeDO;
import com.helianhealth.agent.model.dto.ParamTreeNode;
import com.helianhealth.agent.remote.ParamResolver;
import com.helianhealth.agent.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.core5.http.ParseException;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HTTP参数解析委派类
 * 负责将ParamNodeDTO结构解析为HTTP调用所需的参数格式
 */
@Slf4j
@Component
public class HttpRequestHandler implements ParamResolver {

    private final HttpApiClientManager httpApiClientManager;

    public HttpRequestHandler() {
        this.httpApiClientManager = new HttpApiClientManager();
    }

    /**
     * http参数解析
     */
    public String resolveParamNodes(InterfaceWorkflowNodeDO flowNode, List<ParamTreeNode> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }
        // 兼容调用方的请求体是数组的情况
        if (params.size() == 1 && params.get(0).getParamType() == ParamType.PURE_ARRAY) {
            Object paramValue = params.get(0).getParamValue();
            // 安全地处理不同类型的参数值
            if (paramValue instanceof String) {
                return (String) paramValue;
            } else {
                // 如果不是String类型，转换为JSON字符串
                return JsonUtils.toJsonString(paramValue);
            }
        }
        Map<String, Object> queryParams = new HashMap<>();

        params.forEach(param -> queryParams.put(param.getParamKey(), extractValueFromNode(param)));

        return JsonUtils.toJsonString(queryParams);
    }

    private Object extractValueFromNode(ParamTreeNode node) {
        if (node == null) {
            return null;
        }

        switch (node.getParamType()) {
            case STRING:
            case INTEGER:
            case LONG:
            case BOOLEAN:
                return node.getParamValue();
            // 这里的数组处理直接用封装好的JSON数据,这样保证嵌套结构不丢失
            // 但是其实还需要处理一种如果数组只有一个元素的情况
            case ARRAY:
                return node.getParamValue();

            case OBJECT:
                if (node.getChildren() != null && !node.getChildren().isEmpty()) {
                    Map<String, Object> objectMap = new HashMap<>();
                    for (ParamTreeNode child : node.getChildren()) {
                        if (child.getParamKey() != null) {
                            objectMap.put(child.getParamKey(), extractValueFromNode(child));
                        }
                    }
                    return objectMap;
                } else {
                    return node.getParamValue() != null ?
                            JsonUtils.toJsonString(node.getParamValue()) : null;
                }
            default:
                return node.getParamValue() != null ? node.getParamValue().toString() : null;
        }
    }

    public String executeHttpRequest(String url, String method, Map<String, String> headers, String requestBody) throws IOException, ParseException {
        HttpHeaders httpHeaders = new HttpHeaders();
        headers.forEach(httpHeaders::set);
        if (httpHeaders.getContentType() == null) {
            httpHeaders.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        }

        switch (method.toUpperCase()) {
            case "GET":
                // GET 请求拼接查询参数到 URL
                return httpApiClientManager.executeGet(url);
            case "POST":
                // 设置 POST 请求体
                return httpApiClientManager.executePost(url, requestBody);
            default:
                throw new IllegalArgumentException("不支持的HTTP方法: " + method);
        }
    }
}

