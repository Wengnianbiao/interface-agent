package com.helianhealth.agent.quartz;

import com.helianhealth.agent.quartz.mapper.WorkflowTaskMapper;
import com.helianhealth.agent.quartz.model.dto.WorkflowTaskDTO;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 定时任务初始化
 * 容器启动完成后执行
 */
@Component
@Slf4j
@AllArgsConstructor
public class QuartzJobInit implements ApplicationListener<ApplicationReadyEvent> {

    private final WorkflowTaskMapper businessTaskMapper;

    private final QuartzJobHelper quartzJobHelper;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        businessTaskInit();
    }

    private void businessTaskInit() {
//        List<WorkflowTaskDTO> businessTasks = businessTaskMapper.selectAllTask();
//        if(CollectionUtils.isEmpty(businessTasks)){
//            return;
//        }
//        // 目前就只支持Cron表达式的任务，后期可增强
//        businessTasks.forEach(businessTask -> {
//            try {
//                // 将工作流节点放入上下文等待调度
//                Map<String, Object> map = new HashMap<>();
//                map.put("workflowId", businessTask.getWorkflowId());
//                map.put("params", businessTask.getParams());
//                quartzJobHelper.saveJobCron(WorkflowJob.class.getSimpleName(), WorkflowJob.class, businessTask.getCronExpression(), map);
//            } catch (Exception e) {
//                log.error("定时任务初始化失败", e);
//            }
//        });
        log.info("定时任务初始化完成");
    }
}
