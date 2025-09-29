package com.helianhealth.agent.schedule;

import com.helianhealth.agent.common.ResultData;
import com.helianhealth.agent.controller.request.JarvisRequest;
import com.helianhealth.agent.exception.WorkflowBusinessException;
import com.helianhealth.agent.model.domain.InterfaceWorkflowDO;
import com.helianhealth.agent.service.InterfaceWorkflowService;
import com.helianhealth.agent.utils.JsonUtils;
import com.helianhealth.agent.utils.ResponseRenderUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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
    private final ContentParser contentParser;

    /**
     * 适配jarvis的参数解析调度
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
                log.error(WorkflowBusinessException.METHOD_NOT_SUPPORT.getMessage());
                // 对于没有实现jarvis的接口,直接返回成功
                return ResultData.ok();
            }
        } catch (Exception e) {
            log.error("dispatch request error:", e);
            return ResultData.fail();
        }
    }

    private ResultData<Object> processJarvisRsp(Map<String, Object> flowNodeResponse) {
        Object code = flowNodeResponse.get("code");
        String message = (String) flowNodeResponse.get("message");
        Object rsp = flowNodeResponse.get("rsp");

        if ("200".equals(code)) {
            return ResponseRenderUtils.render(rsp);
        } else {
            return ResponseRenderUtils.error(message);
        }
    }

    /**
     * open api 调度
     */
    public String dispatch(String request, String interfaceUri) {
        try {
            // 1、根据调用URI获取对应的工作流
            Optional<InterfaceWorkflowDO> workflowOptional = workflowService.findByInterfaceUri(interfaceUri);
            if (workflowOptional.isPresent()) {
                InterfaceWorkflowDO workflow = workflowOptional.get();
                // 2、参数预处理,根据contentType转化为Map
                Map<String, Object> businessData = contentParser.parseRequest(workflow, request);

                // 3、调度工作流
                Map<String, Object> scheduleRsp = workFlowEngineScheduler.schedule(workflow.getFirstFlowNodes(),
                        businessData);

                // 4、根据工作流contentType构建响应
                return contentParser.responseBuilder(workflow, scheduleRsp);
            } else {
                log.error(WorkflowBusinessException.METHOD_NOT_SUPPORT.getMessage());
                throw WorkflowBusinessException.METHOD_NOT_SUPPORT.toException();
            }
        } catch (Exception e) {
            log.error("dispatch request error:", e);
            return null;
        }
    }
}
