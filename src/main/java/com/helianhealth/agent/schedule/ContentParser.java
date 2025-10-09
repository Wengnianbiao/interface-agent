package com.helianhealth.agent.schedule;

import com.helianhealth.agent.enums.WorkflowContentType;
import com.helianhealth.agent.model.domain.InterfaceWorkflowDO;
import com.helianhealth.agent.utils.JsonUtils;
import com.helianhealth.agent.utils.XmlUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class ContentParser {

    public Map<String, Object> parseRequest(InterfaceWorkflowDO workflow, String request) {
        if (request == null || request.trim().isEmpty()) {
            return new HashMap<>();
        }

        WorkflowContentType contentType = workflow.getContentType();
        try {
            if (WorkflowContentType.XML.equals(contentType)) {
                // 解析SOAP XML为Map
                return parseSoapXmlToMap(request, workflow.getContentMetaInfo());
            } else {
                // 默认解析JSON为Map
                return parseJsonToMap(request);
            }
        } catch (Exception e) {
            log.error("解析请求内容失败，类型: {}, 错误: {}", contentType, e.getMessage(), e);
        }

        return new HashMap<>();
    }

    /**
     * 解析JSON字符串为Map
     */
    private Map<String, Object> parseJsonToMap(String json) {
        return JsonUtils.fromJsonStringToObjectMap(json);
    }

    /**
     * 解析SOAP XML字符串为Map
     * 处理SOAP信封，提取CDATA中的实际业务数据
     */
    private Map<String, Object> parseSoapXmlToMap(String xml, String contentMetaInfo) {
        return XmlUtils.parseRequestXml(xml, contentMetaInfo);
    }

    public String responseBuilder(InterfaceWorkflowDO workflow, Map<String, Object> response) {
        WorkflowContentType contentType = workflow.getContentType();
        if (WorkflowContentType.JSON.equals(contentType)) {
            return JsonUtils.toJsonString(response);
        } else if (WorkflowContentType.XML.equals(contentType)) {
            return XmlUtils.buildResponseXml(response, workflow.getContentMetaInfo());
        } else {
            return null;
        }
    }
}
