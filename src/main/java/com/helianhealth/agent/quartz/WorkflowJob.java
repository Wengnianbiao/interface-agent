package com.helianhealth.agent.quartz;

import com.helianhealth.agent.model.domain.InterfaceWorkflowDO;
import com.helianhealth.agent.service.InterfaceWorkflowService;
import com.helianhealth.agent.schedule.WorkFlowEngineScheduler;
import com.helianhealth.agent.utils.JsonUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

import java.util.Map;

/**
 * Job增强抽象类，拓展
 */
@Slf4j
@DisallowConcurrentExecution
@AllArgsConstructor
public class WorkflowJob implements Job {

    private final InterfaceWorkflowService workflowService;
    private final WorkFlowEngineScheduler workFlowEngineScheduler;

    @Override
    public void execute(JobExecutionContext context) {
        doService(context);
    }

    private void doService(JobExecutionContext context) {
        Integer workflowId = (Integer) context.getJobDetail().getJobDataMap().get("workflowId");
        String params = (String) context.getJobDetail().getJobDataMap().get("params");
        Map<String, Object> businessData = JsonUtils.toMap(params);
        log.info("workflowId: {}, params: {}", workflowId, params);
        InterfaceWorkflowDO workflow = workflowService.selectByFlowId(workflowId);
        log.info("WorkflowJob execute and start schedule workflow!!");
        workFlowEngineScheduler.schedule(workflow.getFirstFlowNodes(), businessData);
    }
}
