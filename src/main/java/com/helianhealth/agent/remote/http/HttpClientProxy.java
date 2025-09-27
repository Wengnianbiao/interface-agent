package com.helianhealth.agent.remote.http;

import com.alibaba.fastjson2.JSONArray;
import com.helianhealth.agent.enums.MappingSource;
import com.helianhealth.agent.enums.MappingType;
import com.helianhealth.agent.enums.NodeType;
import com.helianhealth.agent.enums.ParamType;
import com.helianhealth.agent.model.domain.InterfaceWorkflowNodeDO;
import com.helianhealth.agent.model.domain.NodeParamConfigDO;
import com.helianhealth.agent.model.dto.ParamTreeNode;
import com.helianhealth.agent.remote.AbstractClientProxy;
import com.helianhealth.agent.utils.JsonUtils;
import com.helianhealth.agent.utils.ParamNodeUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class HttpClientProxy extends AbstractClientProxy {

    private final HttpRequestHandler httpRequestHandler;

    public HttpClientProxy(HttpRequestHandler httpRequestHandler) {
        this.httpRequestHandler = httpRequestHandler;
    }


    @Override
    public Map<String, Object> doInvoke(InterfaceWorkflowNodeDO flowNode, List<ParamTreeNode> params) {
        try {
            // 1. 检查节点类型是否为HTTP
            if (flowNode.getNodeType() != NodeType.HTTP) {
                throw new IllegalArgumentException("当前节点类型不是HTTP，无法进行HTTP远程调用");
            }

            // 2. 解析元数据（包含HTTP请求信息）
            Map<String, Object> metaInfo = JsonUtils.toMap(flowNode.getMetaInfo());
            if (metaInfo == null || !metaInfo.containsKey("url")) {
                throw new IllegalArgumentException("HTTP请求信息中缺少URL");
            }

            String url = (String) metaInfo.get("url");
            String method = (String) metaInfo.getOrDefault("method", "GET");
            Map<String, String> headers = (Map<String, String>) metaInfo.getOrDefault("headers", new HashMap<>());
            String requestBody = httpRequestHandler.resolveParamNodes(flowNode, params);

            String responseBody = httpRequestHandler.executeHttpRequest(url, method, headers, requestBody);

            return JsonUtils.toMap(responseBody);
        } catch (Exception e) {
            // 处理异常，可根据需要进行重试或降级处理
            throw new RuntimeException("HTTP远程调用失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void processObjectNodeType(NodeParamConfigDO config, List<NodeParamConfigDO> allNodes, Map<String, Object> businessData, Map<String, Object> rootBusinessData, ParamTreeNode node) {
        node.setChildren(buildParamTree(allNodes,
                config.getConfigId(),
                processSourceParamWhenTargetIsObject(config, businessData),
                rootBusinessData));
    }

    private Map<String, Object> processSourceParamWhenTargetIsObject(NodeParamConfigDO config, Map<String, Object> businessData) {
        ParamType sourceParamType = config.getSourceParamType();
        Object sourceValue = businessData.get(config.getSourceParamKey());
        switch(sourceParamType) {
            case NONE:
                return businessData;
            // 若为对象直接返回
            case OBJECT:
                return JsonUtils.toMap(sourceValue);
            // 若为数组则需要转换成当前数组的首个索引对应的对象的key-value格式
            // 默认获取索引为0的元素
            case ARRAY:
                if (sourceValue == null) {
                    return null;
                }
                if (!(sourceValue instanceof List)) {
                    throw new IllegalArgumentException("Expected List for ARRAY type, but got: " + sourceValue.getClass().getSimpleName());
                }

                List<?> list = (List<?>) sourceValue;
                if (list.isEmpty()) {
                    return new HashMap<>();
                }

                Object firstItem = list.get(0);
                if (!(firstItem instanceof Map)) {
                    throw new IllegalArgumentException("First element of array must be a Map, but got: " + firstItem.getClass().getSimpleName());
                }

                // 安全转换并获取第一个元素
                return JsonUtils.toMap(list.get(0));
            default:
                return new HashMap<>();
        }
    }

    @Override
    public void processArrayNodeType(NodeParamConfigDO config, List<NodeParamConfigDO> allNodes, Map<String, Object> businessData, Map<String, Object> rootBusinessData, ParamTreeNode node) {
        doProcessArrayNodeType(config, allNodes, businessData, rootBusinessData, node);
    }

    private void doProcessArrayNodeType(NodeParamConfigDO config, List<NodeParamConfigDO> allNodes, Map<String, Object> businessData, Map<String, Object> rootBusinessData, ParamTreeNode node) {
        // 根据映射源进入迭代
        Object sourceValue = config.getMappingSource() == MappingSource.INPUT ?
                getSourceValue(config.getSourceParamKey(), rootBusinessData) :
                getSourceValue(config.getSourceParamKey(), businessData);

        JSONArray jsonArray = new JSONArray();

        if (config.getSourceParamType() == ParamType.OBJECT && sourceValue != null) {
            // 情况1: 源参数是Object，包装成大小为1的数组
            List<ParamTreeNode> arrayChildren = buildParamTree(allNodes,
                    config.getConfigId(),
                    JsonUtils.toMap(sourceValue),
                    rootBusinessData);
            // 提取单个节点的值
            if (arrayChildren != null && arrayChildren.size() == 1) {
                ParamTreeNode singleNode = arrayChildren.get(0);
                jsonArray.add(singleNode.getParamValue());
            } else {
                jsonArray.add(ParamNodeUtils.convertNodesToMap(arrayChildren));
            }
            node.setParamValue(jsonArray);
        } else if (config.getSourceParamType() == ParamType.ARRAY && sourceValue != null) {
            // 情况2: 源参数是数组，需要处理数组中的每个元素
             if (sourceValue instanceof List){
                List<?> sourceList = (List<?>) sourceValue;
                for (Object item : sourceList) {
                    // 如果数组是一个对象数组配置的时候需要增加一个虚拟节点!
                    List<ParamTreeNode> arrayChildren = buildParamTree(allNodes,
                            config.getConfigId(),
                            JsonUtils.toMap(item),
                            rootBusinessData);
                    // 提取单个节点的值
                    if (arrayChildren != null && arrayChildren.size() == 1) {
                        ParamTreeNode singleNode = arrayChildren.get(0);
                        jsonArray.add(singleNode.getParamValue());
                    } else {
                        jsonArray.add(ParamNodeUtils.convertNodesToMap(arrayChildren));
                    }
                }
            } else if (sourceValue instanceof Map) {
                 List<ParamTreeNode> arrayChildren = buildParamTree(allNodes,
                         config.getConfigId(),
                         JsonUtils.toMap(sourceValue),
                         rootBusinessData);
                 jsonArray.add(ParamNodeUtils.convertNodesToMap(arrayChildren));
            }
            // 数组是嵌套结构只能通过value去保持这种嵌套结构
            node.setParamValue(jsonArray);
        } else if (config.getSourceParamType() == ParamType.PURE_ARRAY && sourceValue != null){ // 纯数组的场景说明入参是一个基础数据类型的数组
            // 如果是直连映射，也就是数组元素是基本数据类型就直接映射
            if (config.getMappingType().equals(MappingType.DIRECT)) {
                node.setParamValue(sourceValue);
            }
            if (sourceValue instanceof List) {
                // 此时的数组一定是基础数据类型的数组
                List<?> sourceList = (List<?>) sourceValue;
                for (Object item : sourceList) {
                    Map<String, Object> itemNode = new HashMap<>();
                    itemNode.put(config.getTargetParamKey(), item);
                    List<ParamTreeNode> arrayChildren = buildParamTree(allNodes,
                            config.getConfigId(),
                            JsonUtils.toMap(itemNode),
                            rootBusinessData);
                    // 提取单个节点的值
                    if (arrayChildren != null && arrayChildren.size() == 1) {
                        ParamTreeNode singleNode = arrayChildren.get(0);
                        jsonArray.add(singleNode.getParamValue());
                    } else {
                        jsonArray.add(ParamNodeUtils.convertNodesToMap(arrayChildren));
                    }
                }
            } else if (sourceValue instanceof String) {
                Map<String, Object> itemNode = new HashMap<>();
                itemNode.put(config.getTargetParamKey(), sourceValue);
                List<ParamTreeNode> arrayChildren = buildParamTree(allNodes,
                        config.getConfigId(),
                        JsonUtils.toMap(itemNode),
                        rootBusinessData);
                jsonArray.add(ParamNodeUtils.convertNodesToMap(arrayChildren));
            }

            node.setParamValue(jsonArray);
        }
    }

    @SuppressWarnings("unchecked")
    private Object getSourceValue(String sourceParamKey, Map<String, Object> businessData) {
        if (businessData == null || StringUtils.isEmpty(sourceParamKey)) {
            return null;
        }
        String[] keys = sourceParamKey.split("\\.");
        Object current = businessData;

        for (String key : keys) {
            if (!(current instanceof Map)) {
                return null; // 非 Map 类型无法继续深入
            }

            Map<String, Object> currentMap = (Map<String, Object>) current;
            current = currentMap.get(key);
            if (current == null) {
                return null;
            }
        }

        return current;
    }
}