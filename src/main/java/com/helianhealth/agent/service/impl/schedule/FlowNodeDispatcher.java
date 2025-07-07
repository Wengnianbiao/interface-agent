package com.helianhealth.agent.service.impl.schedule;

import com.helianhealth.agent.common.ResultData;
import com.helianhealth.agent.controller.request.JarvisRequest;
import com.helianhealth.agent.enums.WorkflowContentType;
import com.helianhealth.agent.exception.InstanceBusinessException;
import com.helianhealth.agent.model.domain.InterfaceWorkflowDO;
import com.helianhealth.agent.service.InterfaceWorkflowService;
import com.helianhealth.agent.utils.JsonUtils;
import com.helianhealth.agent.utils.ResponseModelUtils;
import com.helianhealth.agent.utils.XmlUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 工作流节点分发器
 * Dispatcher负责去调度工作流节节点之间流转
 */
@Component
@Slf4j
@AllArgsConstructor
public class FlowNodeDispatcher {

    private final InterfaceWorkflowService workflowService;
    private final WorkFlowEngineScheduler workFlowEngineScheduler;

    /**
     * 适配jarvis的参数解析调度
     * @param request 请求参数
     * @return 响应
     */
    public ResultData<Object> jarvisDispatch(JarvisRequest request) {
        try {
            Optional<InterfaceWorkflowDO> workflowOptional = workflowService.findByInterfaceUri(request.getBusinessMethod());
            if (workflowOptional.isPresent()) {
                // 兼容jarvis的API调用入参,理论上jarvis应当统一入参格式
                Map<String, Object> businessData = request.getBusinessMethod().equals("GetItemResult") ?
                        JsonUtils.toMap(request) : JsonUtils.toMap(request.getData());

                Map<String, Object> scheduleRsp = workFlowEngineScheduler.schedule(workflowOptional.get().getFirstFlowNodes(),
                        businessData);
                return processJarvisRsp(scheduleRsp);
            } else {
                log.error(InstanceBusinessException.METHOD_NOT_SUPPORT.getMessage());
                throw InstanceBusinessException.METHOD_NOT_SUPPORT.toException();
            }
        } catch (Exception e) {
            log.error("dispatch request error:", e);
            return ResultData.fail();
        }
    }

    public String dispatch(String request, String interfaceUri) {
        try {
            // 1、根据调用URI获取对应的工作流
            Optional<InterfaceWorkflowDO> workflowOptional = workflowService.findByInterfaceUri(interfaceUri);
            if (workflowOptional.isPresent()) {
                InterfaceWorkflowDO workflow = workflowOptional.get();
                // 2、参数预处理,根据入参格式转化
                Map<String, Object> businessData = contentParser(workflow, request);

                // 3、调度工作流统一入参格式为Map<String,Object>
                Map<String, Object> scheduleRsp = workFlowEngineScheduler.schedule(workflow.getFirstFlowNodes(),
                        businessData);

                // 4、根据工作流内容类型构建响应
                return buildResponse(workflow, scheduleRsp);
            } else {
                log.error(InstanceBusinessException.METHOD_NOT_SUPPORT.getMessage());
                throw InstanceBusinessException.METHOD_NOT_SUPPORT.toException();
            }
        } catch (Exception e) {
            log.error("dispatch request error:", e);
            return null;
        }
    }

    private ResultData<Object> processJarvisRsp(Map<String, Object> flowNodeResponse) {
        Object code = flowNodeResponse.get("code");
        String message = (String) flowNodeResponse.get("message");
        Object rsp = flowNodeResponse.get("rsp");

        if ("200".equals(code)) {
            return ResponseModelUtils.render(rsp);
        } else {
            return ResponseModelUtils.error(message);
        }
    }

    /**
     * 根据内容类型解析请求为Map结构
     * @param workflow 工作流
     * @param request 请求内容字符串
     * @return 解析后的Map
     */
    private Map<String, Object> contentParser(InterfaceWorkflowDO workflow, String request) {
        if (request == null || request.trim().isEmpty()) {
            return new HashMap<>();
        }

        WorkflowContentType contentType = workflow.getContentType();
        try {
            if (WorkflowContentType.JSON.equals(contentType)) {
                // 解析JSON为Map
                return parseJsonToMap(request);
            } else if (WorkflowContentType.XML.equals(contentType)) {
                // 解析SOAP XML为Map
                return parseSoapXmlToMap(request, workflow.getContentMetaInfo());
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
        return JsonUtils.toMap(json);
    }

    /**
     * 解析SOAP XML字符串为Map
     * 处理SOAP信封，提取CDATA中的实际业务数据
     */
    private Map<String, Object> parseSoapXmlToMap(String xml, String metaInfo) {
        return XmlUtils.parseRequestXml(xml, metaInfo);
    }

    private String buildResponse(InterfaceWorkflowDO workflow, Map<String, Object> scheduleRsp) {
        WorkflowContentType contentType = workflow.getContentType();
        if (WorkflowContentType.JSON.equals(contentType)) {
            return JsonUtils.toJsonString(scheduleRsp);
        } else if (WorkflowContentType.XML.equals(contentType)) {
            return XmlUtils.buildResponseXml(scheduleRsp, workflow.getContentMetaInfo());
        } else {
            return null;
        }
    }
}
