package com.helianhealth.agent.utils;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.helianhealth.agent.exception.JsonUtilsException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String toJsonString(Object object) {
        try {
            return JSONObject.toJSONString(object);
        } catch (Exception e) {
            throw JsonUtilsException.JSON_PROCESS_ERROR.toException();
        }
    }

    public static Map<String, String> fromJsonStringToMap(String jsonString) {
        try {
            return objectMapper.readValue(jsonString, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Error converting JSON to list", e);
        }
    }

    /**
     * 将不同类型的对象转换为 Map<String, Object>
     * 支持 JSONObject、String、Map 等类型
     * @param obj 输入对象
     * @return 转换后的 Map
     */
    public static Map<String, Object> toMap(Object obj) {
        if (obj == null) {
            return null;
        }

        try {
            // 如果已经是 Map，直接返回
            if (obj instanceof Map) {
                return (Map<String, Object>) obj;
            }

            // 如果是 JSONObject，直接转换，避免序列化/反序列化开销
            if (obj instanceof JSONObject) {
                JSONObject jsonObject = (JSONObject) obj;
                Map<String, Object> map = new HashMap<>();
                for (String key : jsonObject.keySet()) {
                    map.put(key, jsonObject.get(key));
                }
                return map;
            }

            // 如果是字符串，解析为 Map
            if (obj instanceof String) {
                String str = (String) obj;
                if (str.trim().isEmpty()) {
                    return null;
                }
                return objectMapper.readValue(str, new TypeReference<Map<String, Object>>() {});
            }

            // 其他类型，先序列化再反序列化（向后兼容）
            String jsonStr = objectMapper.writeValueAsString(obj);
            return objectMapper.readValue(jsonStr, new TypeReference<Map<String, Object>>() {});

        } catch (Exception e) {
            throw new RuntimeException("Error converting object to map: " + e.getMessage(), e);
        }
    }

    /**
     * 深度复制 Map<String, Object>（支持嵌套结构：内部 Map、List 等）
     * 避免修改副本时影响原始 Map，适用于需要隔离数据的场景（如流程处理、多线程操作）
     * @param originalMap 原始 Map（可为 null，返回 null）
     * @return 深度复制后的新 Map
     */
    public static Map<String, Object> deepCopyMap(Map<String, Object> originalMap) {
        if (originalMap == null) {
            return null;
        }

        try {
            // 序列化：将原始 Map 转为 JSON 字符串（保留所有嵌套结构）
            String jsonStr = objectMapper.writeValueAsString(originalMap);
            // 反序列化：将 JSON 字符串转为新的 Map（重新创建所有嵌套对象，实现深度隔离）
            return objectMapper.readValue(jsonStr, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Error deep copying map: " + e.getMessage(), e);
        }
    }

    /**
     * 将 XML 元素（org.w3c.dom.Element）递归转换为 Map，处理同名标签为数组
     */
    public static Map<String, Object> xmlElementToMap(Element element) {
        Map<String, Object> map = new HashMap<>();
        NodeList childNodes = element.getChildNodes();

        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if (node instanceof Element) {
                Element child = (Element) node;
                String tagName = child.getTagName();
                Object existingValue = map.get(tagName);

                Object childValue;
                if (child.getChildNodes().getLength() == 1 && child.getFirstChild() instanceof Text) {
                    // 叶子节点，取文本值
                    childValue = child.getTextContent().trim();
                } else {
                    // 非叶子节点，递归处理
                    childValue = xmlElementToMap(child);
                }

                if (existingValue == null) {
                    map.put(tagName, childValue);
                } else if (existingValue instanceof JSONArray) {
                    ((JSONArray) existingValue).add(childValue);
                } else {
                    JSONArray array = new JSONArray();
                    array.add(existingValue);
                    array.add(childValue);
                    map.put(tagName, array);
                }
            }
        }
        return map;
    }
}

