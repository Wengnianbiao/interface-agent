package com.helianhealth.agent.remote.http;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
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
        // 兼容调用方的请求体是纯数组的场景,e.g. [1,2,3] or [{"name":"John"},{"name":"Mike"}]
        if (params.size() == 1 && params.get(0).getParamType() == ParamType.PURE_ARRAY) {
            return JsonUtils.toJsonString(processArrayNodeType(params.get(0)));
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
            case ARRAY:
                return processArrayNodeType(node);

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

    private Object processArrayNodeType(ParamTreeNode node) {
        JSONArray jsonArray = new JSONArray();

        if (node.getChildren() != null && !node.getChildren().isEmpty()) {
            // 遍历所有虚拟节点
            for (ParamTreeNode virtualNode : node.getChildren()) {
                // 虚拟节点的子节点才是真正的元素
                if (virtualNode.getChildren() != null && !virtualNode.getChildren().isEmpty()) {
                    List<ParamTreeNode> actualNodes = virtualNode.getChildren();

                    if (actualNodes.size() == 1) {
                        // 只有一个子节点说明是基础数据类型的节点，直接提取值加入数组
                        ParamTreeNode actualNode = actualNodes.get(0);
                        Object value = extractValueFromNode(actualNode);
                        jsonArray.add(value);
                    } else {
                        // 多个子节点，封装成对象后加入数组
                        JSONObject obj = new JSONObject();
                        for (ParamTreeNode actualNode : actualNodes) {
                            if (actualNode.getParamKey() != null) {
                                Object value = extractValueFromNode(actualNode);
                                obj.put(actualNode.getParamKey(), value);
                            }
                        }
                        jsonArray.add(obj);
                    }
                }
            }
        }

        return jsonArray;
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

