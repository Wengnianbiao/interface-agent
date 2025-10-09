package com.helianhealth.agent.remote;

import com.helianhealth.agent.enums.MappingSource;
import com.helianhealth.agent.model.domain.InterfaceWorkflowNodeDO;
import com.helianhealth.agent.model.domain.NodeParamConfigDO;
import com.helianhealth.agent.model.dto.ParamTreeNode;
import com.helianhealth.agent.utils.JsonUtils;
import com.helianhealth.agent.utils.ParamNodeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class ProxyConvertHelper {

    public Map<String, Object> convertResponse(InterfaceWorkflowNodeDO flowNode, List<ParamTreeNode> paramTree) {
        // 获取节点的元信息
        String metaInfo = flowNode.getMetaInfo();
        if (StringUtils.isEmpty(metaInfo)) {
            // 如果没有元信息默认使用JSON
            return convertToJsonFormatMap(paramTree);
        }

        try {
            Map<String, Object> metaJson = JsonUtils.fromJsonStringToObjectMap(metaInfo);
            // 获取响应类型，默认为JSON
            String responseType = (String) metaJson.get("responseType");
            if (StringUtils.isEmpty(responseType)) {
                responseType = "json";
            }

            // 根据响应类型进行不同处理
            if ("xml".equalsIgnoreCase(responseType)) {
                return ParamNodeUtils.convertToXmlFormatMap(paramTree);
            } else {
                return convertToJsonFormatMap(paramTree);
            }
        } catch (Exception e) {
            log.error("解析节点元信息失败，nodeId: {}, metaInfo: {}", flowNode.getNodeId(), metaInfo, e);
            return ParamNodeUtils.convertToJsonFormatMap(paramTree);
        }
    }

    private Map<String, Object> convertToJsonFormatMap(List<ParamTreeNode> paramTree) {
        return ParamNodeUtils.convertToJsonFormatMap(paramTree);
    }

    @SuppressWarnings("unchecked")
    public Object convertSourceValue(NodeParamConfigDO config, Map<String, Object> businessData, Map<String, Object> rootBusinessData) {
        MappingSource mappingSource = config.getMappingSource();
        Map<String, Object> tagertMap = mappingSource == MappingSource.INPUT ? rootBusinessData : businessData;
        String sourceParamKey = config.getSourceParamKey();
        // 如果源参数key为空，则说明不需要进行转化
        if (StringUtils.isEmpty(sourceParamKey)) {
            return businessData;
        }
        String[] keys = sourceParamKey.split("\\.");
        Object current = tagertMap;

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

