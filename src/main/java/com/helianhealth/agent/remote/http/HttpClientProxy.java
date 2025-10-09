package com.helianhealth.agent.remote.http;

import com.helianhealth.agent.enums.NodeType;
import com.helianhealth.agent.enums.ParamType;
import com.helianhealth.agent.model.domain.InterfaceWorkflowNodeDO;
import com.helianhealth.agent.model.domain.NodeParamConfigDO;
import com.helianhealth.agent.model.dto.ParamTreeNode;
import com.helianhealth.agent.remote.AbstractClientProxy;
import com.helianhealth.agent.remote.ProxyConvertHelper;
import com.helianhealth.agent.utils.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class HttpClientProxy extends AbstractClientProxy {

    @Autowired
    private HttpRequestHandler httpRequestHandler;

    @Autowired
    private ProxyConvertHelper proxyConvertHelper;

    @Override
    @SuppressWarnings("unchecked")
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
        Object sourceValue = proxyConvertHelper.convertSourceValue(config, businessData, rootBusinessData);

        if (config.getSourceParamType() == ParamType.OBJECT && sourceValue != null) {
            // 源参数是Object，包装成大小为1的数组
            List<ParamTreeNode> arrayChildren = buildParamTree(allNodes,
                    config.getConfigId(),
                    JsonUtils.toMap(sourceValue),
                    rootBusinessData);
            node.setChildren(arrayChildren);
        } else if (config.getSourceParamType() == ParamType.ARRAY ||
                config.getSourceParamType() == ParamType.PURE_ARRAY &&
                        sourceValue != null) {
            processArrayNodeTypeWhenSourceTypeIsArray(config, allNodes, sourceValue, rootBusinessData, node);
        } else {
            // 其他情况创建空数组
            node.setChildren(new ArrayList<>());
        }
    }

    private void processArrayNodeTypeWhenSourceTypeIsArray(NodeParamConfigDO config,
                                                           List<NodeParamConfigDO> allNodes,
                                                           Object sourceValue,
                                                           Map<String, Object> rootBusinessData,
                                                           ParamTreeNode node) {
        // 目标参数是数组,原参数可能是数组也可能是单个对象
        List<ParamTreeNode> allArrayChildren = new ArrayList<>();
        if (sourceValue instanceof List) {
            List<?> sourceList = (List<?>) sourceValue;
            Object o = sourceList.get(0);
            // 数组可以是基础数据类型和对象
            if (o instanceof Map) {
                for (Object item : sourceList) {
                    List<ParamTreeNode> nestedArrayChildren = buildParamTree(allNodes,
                            config.getConfigId(),
                            JsonUtils.toMap(item),
                            rootBusinessData);
                    allArrayChildren.addAll(nestedArrayChildren);
                }
            } else {
                // 对于基础数据类型item是一个值,将目标映射key作为Map的key,item作为Map的value
                for (Object item : sourceList) {
                    Map<String, Object> itemMap = new HashMap<>();
                    itemMap.put(config.getTargetParamKey(), item);
                    List<ParamTreeNode> nestedArrayChildren = buildParamTree(allNodes,
                            config.getConfigId(),
                            JsonUtils.toMap(itemMap),
                            rootBusinessData);
                    allArrayChildren.addAll(nestedArrayChildren);
                }
            }
        } else {
            // 其他情况下就是数组为一维的单个基础数据类型
            Map<String, Object> itemMap = new HashMap<>();
            itemMap.put(config.getTargetParamKey(), sourceValue);
            List<ParamTreeNode> nestedArrayChildren = buildParamTree(allNodes,
                    config.getConfigId(),
                    JsonUtils.toMap(itemMap),
                    rootBusinessData);
            allArrayChildren.addAll(nestedArrayChildren);
        }

        node.setChildren(allArrayChildren);
    }
}