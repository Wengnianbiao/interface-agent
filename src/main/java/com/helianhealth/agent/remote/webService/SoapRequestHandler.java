package com.helianhealth.agent.remote.webService;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.helianhealth.agent.model.domain.InterfaceWorkflowNodeDO;
import com.helianhealth.agent.model.dto.ParamTreeNode;
import com.helianhealth.agent.remote.ParamResolver;
import com.helianhealth.agent.utils.ParamNodeUtils;
import com.helianhealth.agent.utils.XmlUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SOAP消息生成器
 * 负责所有SOAP协议相关的处理：元数据解析、消息构建、请求发送和响应处理
 */
@Slf4j
@Component
public class SoapRequestHandler implements ParamResolver {

    /**
     * 根据metaInfo配置构建完整的SOAP XML消息
     *
     * @param flowNode 工作流节点，包含metaInfo配置
     * @param paramNode 参数节点列表
     * @return 完整的SOAP XML字符串
     */
    public String resolveParamNodes(InterfaceWorkflowNodeDO flowNode, List<ParamTreeNode> paramNode) {
        try {
            // 解析metaInfo配置
            JSONObject metaInfo = parseMetaInfo(flowNode.getMetaInfo());

            // 将参数节点转换为XML字符串
            String paramXml = ParamNodeUtils.paramNodeDTO2Xml(paramNode.get(0));

            // 根据配置构建SOAP消息
            return buildSoapFromConfig(metaInfo, paramXml);
        } catch (Exception e) {
            log.error("构建SOAP消息失败", e);
            throw new RuntimeException("构建SOAP消息失败: " + e.getMessage());
        }
    }

    /**
     * 发送SOAP请求并获取响应
     */
    public String sendSoapRequest(InterfaceWorkflowNodeDO flowNode, String soapMessage) {
        try {
            // 解析metaInfo获取请求地址
            JSONObject metaInfo = parseMetaInfo(flowNode.getMetaInfo());
            String endpointUrl = metaInfo.getString("url");

            if (StringUtils.isEmpty(endpointUrl)) {
                throw new IllegalArgumentException("metaInfo中未配置url");
            }

            // 创建HTTP连接
            URL url = new URL(endpointUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // 设置请求头
            connection.setRequestMethod(metaInfo.getString("method"));
            connection.setDoOutput(true);

            // 设置自定义请求头
            JSONObject headers = metaInfo.getJSONObject("headers");
            if (headers != null) {
                for (String key : headers.keySet()) {
                    connection.setRequestProperty(key, headers.getString(key));
                }
            }

            // 如果没有设置Content-Type，使用默认值
            if (headers == null || !headers.containsKey("Content-Type")) {
                connection.setRequestProperty("Content-Type", "text/xml;charset=UTF-8");
            }

            // 发送SOAP消息
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = soapMessage.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // 读取响应
            StringBuilder response = getStringBuilder(connection);

            // 处理错误响应
            if (connection.getResponseCode() < 200 || connection.getResponseCode() >= 300) {
                throw new RuntimeException("SOAP请求失败，响应码: " + connection.getResponseCode() +
                        ", 响应信息: " + response);
            }

            return response.toString();
        } catch (Exception e) {
            log.error("发送SOAP请求失败", e);
            throw new RuntimeException("发送SOAP请求失败: " + e.getMessage());
        }
    }

    private static StringBuilder getStringBuilder(HttpURLConnection connection) throws IOException {
        StringBuilder response = new StringBuilder();
        try (InputStream is = connection.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        } catch (Exception e) {
            try (InputStream es = connection.getErrorStream()) {
                if (es != null) {
                    try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(es, StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = errorReader.readLine()) != null) {
                            response.append(line);
                        }
                    }
                }
            }
        }
        return response;
    }

    /**
     * 解析SOAP响应
     */
    public Map<String, Object> parseSoapResponse(InterfaceWorkflowNodeDO flowNode, String response) {
        try {
            return XmlUtils.parseResponseXml(response, flowNode.getMetaInfo());
        } catch (Exception e) {
            log.error("解析SOAP响应失败", e);
            throw new RuntimeException("解析SOAP响应失败: " + e.getMessage());
        }
    }

    /**
     * 解析metaInfo JSON配置
     */
    private JSONObject parseMetaInfo(String metaInfoJson) {
        if (!StringUtils.hasText(metaInfoJson)) {
            return new JSONObject();
        }

        try {
            return JSON.parseObject(metaInfoJson);
        } catch (Exception e) {
            log.warn("解析metaInfo JSON失败，使用空对象: {}", e.getMessage());
            return new JSONObject();
        }
    }

    /**
     * 根据配置构建SOAP消息
     */
    private String buildSoapFromConfig(JSONObject metaInfo, String paramXml) {
        // 构建完整的SOAP信封
        StringBuilder soapMessage = new StringBuilder();

        // 获取envelope配置
        JSONObject envelope = metaInfo.getJSONObject("envelope");
        if (envelope == null) {
            throw new IllegalArgumentException("metaInfo中缺少envelope配置");
        }

        // 开始envelope标签
        soapMessage.append("<soapenv:Envelope");

        // 添加命名空间
        for (String key : envelope.keySet()) {
            soapMessage.append(" ").append(key).append("=\"").append(envelope.getString(key)).append("\"");
        }
        soapMessage.append(">\n");

        // 处理header
        JSONObject headerConfig = metaInfo.getJSONObject("header");
        soapMessage.append("  <soapenv:Header");
        soapMessage.append(">");
        if (headerConfig != null && !headerConfig.isEmpty()) {
            // 如果有header内容，添加进去
            soapMessage.append("\n");
            buildHeaderContent(soapMessage, headerConfig, 4);
            soapMessage.append("  ");
        }
        soapMessage.append("</soapenv:Header>\n");

        // 处理body
        String bodyContent = metaInfo.getString("body");
        if (bodyContent == null) {
            throw new IllegalArgumentException("metaInfo中缺少body配置");
        }

        // 替换占位符${inputParam}为实际参数XML
        bodyContent = bodyContent.replace("${inputParam}", escapeXml(paramXml));

        soapMessage.append("  <soapenv:Body>\n");
        soapMessage.append(bodyContent).append("\n");
        soapMessage.append("  </soapenv:Body>\n");

        // 结束envelope标签
        soapMessage.append("</soapenv:Envelope>");

        return soapMessage.toString();
    }

    /**
     * 构建Header内容
     */
    private void buildHeaderContent(StringBuilder soapMessage, JSONObject headerConfig, int indent) {
        // 遍历Header中的所有元素
        for (String key : headerConfig.keySet()) {
            Object value = headerConfig.get(key);
            if (value instanceof JSONObject) {
                buildElement(soapMessage, (JSONObject) value, indent);
            }
        }
    }

    /**
     * 构建单个元素
     */
    private void buildElement(StringBuilder soapMessage, JSONObject elementConfig, int indent) {
        String elementName = elementConfig.getString("element");
        String elementValue = elementConfig.getString("value");

        if (elementName == null) {
            return;
        }

        // 添加缩进
        addIndent(soapMessage, indent);

        soapMessage.append("<").append(elementName).append(">");

        if (elementValue != null) {
            soapMessage.append(escapeXml(elementValue));
        } else {
            // 处理子元素
            JSONObject children = elementConfig.getJSONObject("children");
            if (children != null && !children.isEmpty()) {
                soapMessage.append("\n");
                for (String childKey : children.keySet()) {
                    Object childValue = children.get(childKey);
                    if (childValue instanceof JSONObject) {
                        buildElement(soapMessage, (JSONObject) childValue, indent + 2);
                    }
                }
                addIndent(soapMessage, indent);
            }
        }

        soapMessage.append("</").append(elementName).append(">\n");
    }

    /**
     * 解析响应XML
     */
    private Map<String, Object> parseResponseXml(String responseXml, JSONObject responseConfig) {
        // 实际项目中应使用XML解析器解析响应
        // 这里简化处理，返回原始响应和一些示例字段
        Map<String, Object> result = new HashMap<>();
        result.put("rawResponse", responseXml);

        // 根据配置提取需要的字段
        String rootElement = responseConfig.getString("rootElement");
        if (rootElement != null) {
            result.put("rootElement", rootElement);
        }

        return result;
    }

    /**
     * XML转义
     */
    private String escapeXml(String value) {
        return value;
    }

    /**
     * 添加缩进
     */
    private void addIndent(StringBuilder sb, int indent) {
        for (int i = 0; i < indent; i++) {
            sb.append(" ");
        }
    }
}
