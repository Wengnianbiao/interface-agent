package com.helianhealth.agent.remote;

import com.helianhealth.agent.enums.NodeType;
import com.helianhealth.agent.model.domain.InterfaceWorkflowNodeDO;
import com.helianhealth.agent.model.dto.ParamTreeNode;
import com.helianhealth.agent.utils.JsonUtils;
import com.helianhealth.agent.utils.ParamNodeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
@Slf4j
public class ResponseConvertHelper {

    public Map<String, Object> convertResponse(InterfaceWorkflowNodeDO flowNode, List<ParamTreeNode> paramTree) {
        // 获取节点的元信息
        String metaInfo = flowNode.getMetaInfo();
        if (StringUtils.isEmpty(metaInfo)) {
            // 如果没有元信息默认使用JSON
            return convertToJsonFormatMap(flowNode, paramTree);
        }

        try {
            Map<String, String> metaJson = JsonUtils.fromJsonStringToMap(metaInfo);
            // 获取响应类型，默认为json
            String responseType = metaJson.get("responseType");
            if (StringUtils.isEmpty(responseType)) {
                responseType = "json";
            }

            // 根据响应类型进行不同处理
            if ("xml".equalsIgnoreCase(responseType)) {
                return ParamNodeUtils.convertToXmlFormatMap(paramTree);
            } else {
                return convertToJsonFormatMap(flowNode, paramTree);
            }
        } catch (Exception e) {
            log.error("解析节点元信息失败，nodeId: {}, metaInfo: {}", flowNode.getNodeId(), metaInfo, e);
            return ParamNodeUtils.convertNodesToMap(paramTree);
        }
    }

    private Map<String, Object> convertToJsonFormatMap(InterfaceWorkflowNodeDO flowNode, List<ParamTreeNode> paramTree) {
        NodeType nodeType = flowNode.getNodeType();
        if (Objects.requireNonNull(nodeType) == NodeType.DATABASE) {
            return ParamNodeUtils.convertToJsonFormatMap(paramTree);
        } else if (nodeType == NodeType.WEBSERVICE) {
            return ParamNodeUtils.convertToJsonFormatMap(paramTree);
        } else {
            return ParamNodeUtils.convertNodesToMap(paramTree);
        }
    }
}

