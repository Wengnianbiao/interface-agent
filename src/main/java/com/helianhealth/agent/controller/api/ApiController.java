package com.helianhealth.agent.controller.api;

import com.helianhealth.agent.common.ResultData;
import com.helianhealth.agent.controller.request.JarvisRequest;
import com.helianhealth.agent.schedule.FlowNodeDispatcher;
import com.helianhealth.agent.utils.ResponseModelUtils;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@Slf4j
@AllArgsConstructor
public class ApiController {

    private final FlowNodeDispatcher dispatcherManager;

    @ApiOperation(value = "健康检查")
    @GetMapping("/HLOpenApi/Hjk")
    public ResultData<Object> healthCheck() {
        return ResponseModelUtils.success();
    }

    @PostMapping("/HLOpenApi/Hjk")
    public ResultData<Object> invokeJarvisApi(@RequestBody JarvisRequest request) {
        MDC.put("method", request.getBusinessMethod() == null ? "" : request.getBusinessMethod());
        try {
            return dispatcherManager.jarvisDispatch(request);
        } finally {
            MDC.clear();
        }
    }

    @RequestMapping(
            value = "/agent-open-api/**",
            method = RequestMethod.POST,
            consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE,
                    MediaType.TEXT_XML_VALUE, MediaType.TEXT_PLAIN_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE}
    )
    @ResponseBody
    public String invokeOpenApi(@RequestBody String request, HttpServletRequest httpRequest) {
        String requestURI = httpRequest.getRequestURI();
        MDC.put("invokeUri", requestURI == null ? "" : requestURI);
        try {
            return dispatcherManager.dispatch(request, requestURI);
        } finally {
            MDC.clear();
        }
    }
}
