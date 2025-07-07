package com.helianhealth.agent.log.aspect;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.helianhealth.agent.model.domain.InterfaceWorkflowNodeDO;
import com.helianhealth.agent.service.InterfaceInvokeLogService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Aspect
@Component
@Slf4j
public class FlowNodeLogAspect {

    @Autowired
    private InterfaceInvokeLogService interfaceInvokeLogService;

    @Autowired
    private ObjectMapper objectMapper;

    private static final ThreadLocal<LogContext> logContext = new ThreadLocal<>();
    /**
     * 环绕整个executeFlowNode流程，在最后阶段统一保存日志
     */
    /**
     * 环绕AbstractClientProxy的remoteInvoke方法，收集完整的调用信息
     */
    @Around("execution(* com.helianhealth.agent.remote.AbstractClientProxy.remoteInvoke(..))")
    public Object aroundRemoteInvoke(ProceedingJoinPoint joinPoint) throws Throwable {

        // 获取方法参数
        long startTime = System.currentTimeMillis();
        Object[] args = joinPoint.getArgs();
        InterfaceWorkflowNodeDO flowNode = (InterfaceWorkflowNodeDO) args[0];
        Map<String, Object> businessData = (Map<String, Object>) args[1];

        LogContext context = new LogContext();
        context.setNodeId(flowNode.getNodeId());
        context.setBusinessData(convertToJson(businessData));
        logContext.set(context);

        try {
            log.info("开始执行工作流节点: [{}]", flowNode.getNodeName());

            // 执行原方法
            Object result = joinPoint.proceed();

            // 记录调用结果
            context.setParamAfterInvoke(convertToJson(result));

            return result;
        } catch (Exception e) {
            log.error("工作流节点执行异常", e);
            context.setErrorMsg(e.getMessage());
            throw e;
        } finally {
            try {
                saveAllCollectedLogs(context);
                log.info("工作流节点: [{}] 执行结束, 耗时: [{}]ms", flowNode.getNodeName(), System.currentTimeMillis() - startTime);
            } catch (Exception e) {
                log.error("日志保存失败", e);
            } finally {
                // 清除ThreadLocal数据，防止内存泄漏
                logContext.remove();
            }
        }
    }

    /**
     * 收集参数预处理结果
     */
    @AfterReturning(
            pointcut = "execution(* com.helianhealth.agent.remote.ParamResolver.resolveParamNodes(..))",
            returning = "result")
    public void collectParamPreProcessResult(Object result) {
        try {
            LogContext context = logContext.get();
            context.setParamBeforeInvoke(convertToJson(result));
        } catch (Exception e) {
            log.error("收集参数预处理结果失败", e);
        }
    }

    /**
     * 环绕doInvoke方法，记录响应结果和计算其执行耗时
     */
    @Around("execution(public java.util.Map com.helianhealth.agent.remote.AbstractClientProxy+.doInvoke(..))")
    public Object aroundDoInvoke(ProceedingJoinPoint joinPoint) throws Throwable {
        LogContext context = logContext.get();
        if (context == null) {
            // 如果上下文不存在，直接执行方法
            return joinPoint.proceed();
        }

        // 记录doInvoke方法开始时间
        long doInvokeStartTime = System.currentTimeMillis();
        try {
            // 执行doInvoke方法
            Object result = joinPoint.proceed();
            // 记录响应结果
            context.setRemoteInvokeResponse(convertToJson(result));
            return result;
        } finally {
            // 计算doInvoke方法执行耗时
            long doInvokeTime = System.currentTimeMillis() - doInvokeStartTime;
            context.setInvokeTime(Long.toString(doInvokeTime));
        }
    }


    /**
     * 统一保存所有收集到的日志数据
     */
    private void saveAllCollectedLogs(LogContext context) {
        try {
            interfaceInvokeLogService.saveInvokeLog(
                    context.getNodeId(),
                    context.getBusinessData(),
                    context.getParamBeforeInvoke(),
                    context.getRemoteInvokeResponse(),
                    context.getParamAfterInvoke(),
                    context.getInvokeTime()
            );
        } catch (Exception e) {
            log.error("保存接口调用日志失败", e);
        }
    }

    /**
     * 对象转JSON字符串工具方法
     */
    private String convertToJson(Object obj) {
        if (obj == null) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("对象转JSON失败", e);
            return "{}";
        }
    }

    /**
     * 日志上下文封装类
     */
    @Data
    private static class LogContext {
        private Integer nodeId;
        private String businessData = "{}";
        private String paramBeforeInvoke = "{}";
        private String remoteInvokeResponse = "{}";
        private String paramAfterInvoke = "{}";
        private String invokeTime = "0";
        private String errorMsg;
    }
}

