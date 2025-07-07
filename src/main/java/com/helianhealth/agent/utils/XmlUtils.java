package com.helianhealth.agent.utils;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class XmlUtils {

    private static final Logger log = LoggerFactory.getLogger(XmlUtils.class);

    /**
     * 将XML字符串转换为Map<String, Object>
     *
     * @param xml XML字符串
     * @return 转换后的Map对象
     */
    public static Map<String, Object> xmlToMap(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(xml)));

            return elementToMap(document.getDocumentElement());
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse XML to Map", e);
        }
    }

    /**
     * 解析SOAP响应XML，支持命名空间配置
     *
     * @param responseXml 原始SOAP响应XML
     * @param metaInfo    元信息（包含命名空间等配置）
     * @return 解析后的Map对象
     */
    public static Map<String, Object> parseResponseXml(String responseXml, String metaInfo) {
        try {
            // 解析metaInfo获取配置
            Map<String, Object> metaMap = new HashMap<>();
            if (metaInfo != null && !metaInfo.isEmpty()) {
                metaMap = JSON.parseObject(metaInfo, Map.class);
            }

            // 获取命名空间配置
            String namespaceURI = (String) metaMap.get("responseNamespace");
            String resultElementName = (String) metaMap.getOrDefault("resultElementName", "MessageInResult");
            String responseType = (String) metaMap.getOrDefault("responseType", "xml");

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(responseXml));
            Document doc = builder.parse(is);

            Element resultElement;
            if (namespaceURI != null && !namespaceURI.isEmpty()) {
                // 使用命名空间查找
                resultElement = (Element) doc.getElementsByTagNameNS(namespaceURI, resultElementName).item(0);
            } else {
                // 不使用命名空间查找
                resultElement = (Element) doc.getElementsByTagName(resultElementName).item(0);
            }

            if (resultElement == null) {
                log.error("未找到结果元素: {}", resultElementName);
                throw new IllegalArgumentException("未找到结果元素: " + resultElementName);
            }

            // 获取 CDATA 内容（自动处理）
            String cdataContent = resultElement.getTextContent();

            if (cdataContent == null || cdataContent.trim().isEmpty()) {
                return new HashMap<>();
            }

            // 转化成Map
            return xmlToMap(cdataContent);
        } catch (Exception e) {
            log.error("解析SOAP响应失败", e);
            throw new RuntimeException("解析SOAP响应失败: " + e.getMessage(), e);
        }
    }

    /**
     * 递归将Element转换为Map
     *
     * @param element XML元素
     * @return 转换后的Map对象
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> elementToMap(Element element) {
        Map<String, Object> resultMap = new LinkedHashMap<>();

        // 处理属性
        NamedNodeMap attributes = element.getAttributes();
        if (attributes != null) {
            for (int i = 0; i < attributes.getLength(); i++) {
                Node attribute = attributes.item(i);
                resultMap.put(attribute.getNodeName(), attribute.getNodeValue());
            }
        }

        // 处理子节点
        NodeList childNodes = element.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);

            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element childElement = (Element) node;
                String tagName = childElement.getNodeName();

                // 递归处理子元素
                Map<String, Object> childMap = elementToMap(childElement);

                // 如果该节点已存在，则转换为列表
                if (resultMap.containsKey(tagName)) {
                    Object existingValue = resultMap.get(tagName);
                    if (existingValue instanceof List) {
                        ((List<Map<String, Object>>) existingValue).add(childMap);
                    } else {
                        List<Object> list = new ArrayList<>();
                        list.add(existingValue);
                        list.add(childMap);
                        resultMap.put(tagName, list);
                    }
                } else {
                    resultMap.put(tagName, childMap);
                }
            } else if (node.getNodeType() == Node.TEXT_NODE) {
                String textValue = node.getNodeValue().trim();
                if (!textValue.isEmpty()) {
                    resultMap.put("#text", textValue);
                }
            }
        }

        // 如果只有文本内容，直接返回文本（不嵌套）
        if (resultMap.size() == 1 && resultMap.containsKey("#text")) {
            return Collections.singletonMap("#text", resultMap.get("#text"));
        }

        for (Map.Entry<String, Object> entry : resultMap.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Map && ((Map<?, ?>) value).containsKey("#text")) {
                Map<String, Object> textMap = (Map<String, Object>) value;
                if (textMap.size() == 1) {
                    entry.setValue(textMap.get("#text"));
                }
            }
        }

        return resultMap;
    }

    /**
     * 解析SOAP请求XML，支持命名空间配置
     *
     * @param requestXml 原始SOAP请求XML
     * @param metaInfo   元信息（包含命名空间等配置）
     * @return 解析后的Map对象
     */
    public static Map<String, Object> parseRequestXml(String requestXml, String metaInfo) {
        try {
            // 解析metaInfo获取配置
            Map<String, Object> metaMap = new HashMap<>();
            if (metaInfo != null && !metaInfo.isEmpty()) {
                metaMap = JSON.parseObject(metaInfo, Map.class);
            }

            // 获取命名空间配置
            String namespaceURI = (String) metaMap.get("requestNamespace");
            String requestElementName = (String) metaMap.get("requestElementName");
            String requestType = (String) metaMap.getOrDefault("requestType", "xml");
            // 新增配置项，控制是否使用CDATA解析
            boolean useCdata = (boolean) metaMap.getOrDefault("useCdata", false);

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(requestXml));
            Document doc = builder.parse(is);

            Element requestElement;
            if (useCdata) {
                // 使用CDATA解析（原有的命名空间查找逻辑）
                if (namespaceURI != null && !namespaceURI.isEmpty()) {
                    // 使用命名空间查找
                    requestElement = (Element) doc.getElementsByTagNameNS(namespaceURI, requestElementName).item(0);
                } else {
                    // 不使用命名空间查找
                    requestElement = (Element) doc.getElementsByTagName(requestElementName).item(0);
                }
            } else {
                // 直接解析整个文档，不查找特定元素
                requestElement = doc.getDocumentElement();
            }

            if (requestElement == null) {
                log.error("未找到请求元素: {}", requestElementName);
                throw new IllegalArgumentException("未找到请求元素: " + requestElementName);
            }

            Map<String, Object> result = elementToMap(requestElement);


            if ("json".equalsIgnoreCase(requestType)) {
                String jsonStr = JSON.toJSONString(result);
                return JsonUtils.toMap(jsonStr);
            } else {
                // XML格式直接返回Map
                return result;
            }
        } catch (Exception e) {
            log.error("解析SOAP请求失败", e);
            throw new RuntimeException("解析SOAP请求失败: " + e.getMessage(), e);
        }
    }

    /**
     * 将Map转换为XML字符串，支持自定义根元素名称
     *
     * @param scheduleRsp 需要转换的Map数据
     * @param metaInfo    元信息（包含命名空间等配置）
     * @return 生成的XML字符串
     */
    public static String buildResponseXml(Map<String, Object> scheduleRsp, String metaInfo) {
        try {
            if (scheduleRsp == null || scheduleRsp.isEmpty()) {
                throw new IllegalArgumentException("scheduleRsp cannot be null or empty");
            }

            // 解析metaInfo获取配置
            Map<String, Object> metaMap = new HashMap<>();
            if (metaInfo != null && !metaInfo.isEmpty()) {
                metaMap = JSON.parseObject(metaInfo, Map.class);
            }

            // 获取配置信息
            String namespaceURI = (String) metaMap.get("responseNamespace");
            String resultElementName = (String) metaMap.get("resultElementName");

            // 如果配置中没有指定根元素名称，则使用scheduleRsp的第一个key作为根元素名称
            if (resultElementName == null || resultElementName.isEmpty()) {
                resultElementName = scheduleRsp.keySet().iterator().next();
            }

            // 创建文档
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();

            // 创建根元素
            Element rootElement;
            if (namespaceURI != null && !namespaceURI.isEmpty()) {
                // 使用命名空间创建根元素
                rootElement = doc.createElementNS(namespaceURI, resultElementName);
            } else {
                // 不使用命名空间创建根元素
                rootElement = doc.createElement(resultElementName);
            }
            doc.appendChild(rootElement);

            // 获取根元素对应的数据并转换为XML元素
            Object rootData = scheduleRsp.get(resultElementName);
            if (rootData instanceof Map) {
                mapToElement((Map<String, Object>) rootData, rootElement, doc);
            } else {
                // 即使值为null，也创建标签
                String textContent = rootData == null ? "" : rootData.toString();
                rootElement.appendChild(doc.createTextNode(textContent));
            }

            // 创建XML输出
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();

            // 设置输出属性
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

            // 关键：设置不输出XML声明
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            DOMSource source = new DOMSource(doc);
            transformer.transform(source, result);

            return writer.toString();
        } catch (Exception e) {
            log.error("构建SOAP响应XML失败", e);
            throw new RuntimeException("构建SOAP响应XML失败: " + e.getMessage(), e);
        }
    }

    /**
     * 递归将Map转换为XML元素，确保null值也会生成对应的标签
     *
     * @param map    要转换的Map
     * @param parent 父元素
     * @param doc    文档对象
     */
    private static void mapToElement(Map<String, Object> map, Element parent, Document doc) {
        if (map == null) {
            return;
        }

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            // 处理属性
            if (key.startsWith("#")) {
                String attrValue = value != null ? value.toString() : "";
                parent.setAttribute(key.substring(1), attrValue);
                continue;
            }

            // 创建子元素 - 即使值为null也会创建标签
            Element childElement = doc.createElement(key);

            if (value instanceof Map) {
                // 如果值是Map，则递归处理
                mapToElement((Map<String, Object>) value, childElement, doc);
            } else if (value instanceof List) {
                // 如果值是List，则遍历每个元素
                List<?> list = (List<?>) value;
                for (Object item : list) {
                    if (item instanceof Map) {
                        Element listItem = doc.createElement(key);
                        mapToElement((Map<String, Object>) item, listItem, doc);
                        parent.appendChild(listItem);
                    } else {
                        // 列表项处理
                        Element listItem = doc.createElement(key);
                        String textContent = item != null ? item.toString() : "";
                        listItem.appendChild(doc.createTextNode(textContent));
                        parent.appendChild(listItem);
                    }
                }
                // 列表已通过循环添加，不需要再添加当前childElement
                continue;
            } else {
                // 普通值处理，包括null值
                String textContent = value != null ? value.toString() : "";
                childElement.appendChild(doc.createTextNode(textContent));
            }

            // 添加子元素到父元素
            parent.appendChild(childElement);
        }
    }
}
