package com.helianhealth.agent.utils;

import com.alibaba.fastjson2.JSONArray;
import com.helianhealth.agent.enums.ParamType;
import com.helianhealth.agent.model.dto.ParamTreeNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ParamNodeUtils {

    /**
     * 将ParamNodeDTO列表转换为Map<String, Object>
     * 递归处理子节点，支持嵌套对象结构
     */
    public static Map<String, Object> convertToJsonFormatMap(List<ParamTreeNode> nodeList) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (nodeList == null || nodeList.isEmpty()) {
            return result;
        }

        for (ParamTreeNode node : nodeList) {
            String key = node.getParamKey();
            boolean isArray = node.getParamType() == ParamType.ARRAY;

            // 处理数组类型：将所有子节点放入JSONArray
            if (isArray) {
                JSONArray jsonArray = new JSONArray();
                // 遍历所有子节点，转换后添加到JSONArray
                if (node.getChildren() != null && !node.getChildren().isEmpty()) {
                    for (ParamTreeNode child : node.getChildren()) {
                        // 转换子节点为Map或值
                        Object childValue = child.getChildren() != null && !child.getChildren().isEmpty()
                                ? convertToJsonFormatMap(child.getChildren())
                                : child.getParamValue();
                        jsonArray.add(childValue);
                    }
                }
                // 将JSONArray放入结果Map
                result.put(key, jsonArray);
            } else {
                // 非数组类型：如果有子节点则递归转换，否则直接使用节点的value
                Object value = node.getChildren() != null && !node.getChildren().isEmpty()
                        ? convertToJsonFormatMap(node.getChildren())
                        : node.getParamValue();
                result.put(key, value);
            }
        }
        return result;
    }

    /**
     * 将ParamNodeDTO转换为XML字符串
     * @param node 根节点
     * @return XML字符串
     */
    public static String paramNodeDTO2Xml(ParamTreeNode node) {
        if (node == null) {
            return "";
        }

        StringBuilder xml = new StringBuilder();
        paramNodeDTO2Xml(node, xml, 0);
        return xml.toString();
    }

    /**
     * 递归构建XML
     * @param node 当前节点
     * @param xml StringBuilder对象
     * @param indent 缩进级别
     */
    private static void paramNodeDTO2Xml(ParamTreeNode node, StringBuilder xml, int indent) {
        if (node == null) {
            return;
        }

        String tagName = node.getParamKey();
        ParamType paramType = node.getParamType();

        // 添加缩进
        addIndent(xml, indent);

        // 开始标签
        xml.append("<").append(tagName);

        // 添加属性
        List<ParamTreeNode> attributeNodes = node.getAttributeNodes();
        if (attributeNodes != null && !attributeNodes.isEmpty()) {
            for (ParamTreeNode attrNode : attributeNodes) {
                if (attrNode.getParamType() == ParamType.STRING && attrNode.getParamValue() != null) {
                    xml.append(" ").append(attrNode.getParamKey()).append("=\"")
                            .append(attrNode.getParamValue().toString()).append("\"");
                }
            }
        }

        // 判断是否为自闭合标签
        boolean isSelfClosing = isSelfClosingTag(node);

        if (isSelfClosing) {
            // 自闭合标签
            xml.append("/>\n");
        } else if (paramType == ParamType.OBJECT) {
            // 普通标签
            xml.append(">");

            List<ParamTreeNode> children = node.getChildren();
            if (children != null && !children.isEmpty()) {
                xml.append("\n");
                for (ParamTreeNode child : children) {
                    paramNodeDTO2Xml(child, xml, indent + 1);
                }
                addIndent(xml, indent);
            }

            xml.append("</").append(tagName).append(">\n");
        }  else if (paramType == ParamType.ARRAY) {
            // 数组类型：父标签在最外面，子节点使用自己的key和value
            xml.append(">");

            List<ParamTreeNode> children = node.getChildren();
            if (children != null && !children.isEmpty()) {
                xml.append("\n");
                for (ParamTreeNode child : children) {
                    paramNodeDTO2Xml(child, xml, indent + 1);
                }
                addIndent(xml, indent);
            }

            xml.append("</").append(tagName).append(">\n");
        } else {
            // 有值的标签
            if (node.getParamValue() != null) {
                xml.append(">").append(node.getParamValue().toString())
                        .append("</").append(tagName).append(">\n");
            } else {
                xml.append("/>\n");
            }
        }
    }

    /**
     * 判断是否为自闭合标签
     * @param node 节点
     * @return 是否为自闭合标签
     */
    private static boolean isSelfClosingTag(ParamTreeNode node) {
        // 如果没有子节点且没有值，则为自闭合标签
        return (node.getChildren() == null || node.getChildren().isEmpty())
                && node.getParamValue() == null
                && node.getParamType() != ParamType.STRING;
    }

    /**
     * 添加缩进
     * @param xml StringBuilder对象
     * @param indent 缩进级别
     */
    private static void addIndent(StringBuilder xml, int indent) {
        for (int i = 0; i < indent * 4; i++) {
            xml.append(" ");
        }
    }

    public static Map<String, Object> convertToXmlFormatMap(List<ParamTreeNode> nodeList) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (nodeList == null || nodeList.isEmpty()) {
            return result;
        }

        for (ParamTreeNode node : nodeList) {
            String key = node.getParamKey();
            // 处理值：如果有子节点则递归转换，否则直接使用节点的value
            Object value = node.getChildren() != null && !node.getChildren().isEmpty()
                    ? convertToXmlFormatMap(node.getChildren())
                    : node.getParamValue();

            result.put(key, value);
        }
        return result;
    }
}
