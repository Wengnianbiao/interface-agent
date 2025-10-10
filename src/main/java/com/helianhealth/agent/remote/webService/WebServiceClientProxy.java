package com.helianhealth.agent.remote.webService;

import com.helianhealth.agent.enums.ParamType;
import com.helianhealth.agent.model.domain.InterfaceWorkflowNodeDO;
import com.helianhealth.agent.model.domain.NodeParamConfigDO;
import com.helianhealth.agent.model.dto.ParamTreeNode;
import com.helianhealth.agent.remote.AbstractClientProxy;
import com.helianhealth.agent.remote.ProxyConvertHelper;
import com.helianhealth.agent.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class WebServiceClientProxy extends AbstractClientProxy {

    @Autowired
    private SoapRequestHandler soapRequestHandler;

    @Autowired
    private ProxyConvertHelper proxyConvertHelper;

    @Override
    public Map<String, Object> doInvoke(InterfaceWorkflowNodeDO flowNode, List<ParamTreeNode> params) {
        try {
            String soapMessage = soapRequestHandler.resolveParamNodes(flowNode, params);
            String soapResponse = soapRequestHandler.sendSoapRequest(flowNode, soapMessage);
            return soapRequestHandler.parseSoapResponse(flowNode, soapResponse);
        } catch (Exception e) {
            log.error("WebService调用失败", e);
            throw new RuntimeException("WebService调用失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void processObjectNodeType(NodeParamConfigDO config, List<NodeParamConfigDO> allNodes, Map<String, Object> businessData, Map<String, Object> rootBusinessData, ParamTreeNode node) {
        // 如果是对象的情况其实就是映射源定位嵌套结构完成映射
        node.setChildren(buildParamTree(allNodes,
                config.getConfigId(),
                processSourceParamWhenTargetIsObject(config, businessData),
                rootBusinessData));
    }

    private Map<String, Object> processSourceParamWhenTargetIsObject(NodeParamConfigDO config, Map<String, Object> businessData) {
        ParamType sourceParamType = config.getSourceParamType();
        Object sourceValue = businessData.get(config.getSourceParamKey());
        switch(sourceParamType) {
            // soap的入参用的是标签体，若不需要转化说明只是一个标签体,不需要进入递归
            case NONE:
                return businessData;
            // 若为对象直接返回
            case OBJECT:
                return JsonUtils.toMap(sourceValue);
            // 若为数组则需要转换成当前数组的首个索引对应的对象的key-value格式
            case ARRAY:
                if (sourceValue == null) {
                    throw new IllegalArgumentException("Source value for ARRAY type is null: " + config.getSourceParamKey());
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

                return JsonUtils.toMap(list.get(0));
            default:
                return new HashMap<>();
        }
    }

    @Override
    public void processArrayNodeType(NodeParamConfigDO config, List<NodeParamConfigDO> allNodes, Map<String, Object> businessData, Map<String, Object> rootBusinessData, ParamTreeNode node) {
        doProcessArrayNodeType(config, allNodes, businessData, rootBusinessData, node);
    }

    private void doProcessArrayNodeType(NodeParamConfigDO config,
                                      List<NodeParamConfigDO> allNodes,
                                      Map<String, Object> businessData,
                                      Map<String, Object> rootBusinessData,
                                      ParamTreeNode node) {
        Object sourceValue = proxyConvertHelper.convertSourceValue(config, businessData, rootBusinessData);

        if (config.getSourceParamType() == ParamType.OBJECT && sourceValue != null) {
            // 源参数是Object，包装成大小为1的数组
            List<ParamTreeNode> arrayChildren = buildParamTree(allNodes,
                    config.getConfigId(),
                    JsonUtils.toMap(sourceValue),
                    rootBusinessData);
            node.setChildren(arrayChildren);
        } else if (config.getSourceParamType() == ParamType.ARRAY && sourceValue != null) {
            // 目标参数是数组,原参数可能是数组也可能是单个对象
            List<ParamTreeNode> allArrayChildren = new ArrayList<>();
            if (sourceValue instanceof List) {
                List<?> sourceList = (List<?>) sourceValue;
                Object o = sourceList.get(0);
                // 数组可以是基础数据类型或对象
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
        } else {
            // 其他情况创建空数组
            node.setChildren(new ArrayList<>());
        }
    }
}
