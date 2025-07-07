package com.helianhealth.agent.quartz;

import lombok.AllArgsConstructor;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerFactory;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@AllArgsConstructor
public class QuartzJobHelper {
    private static final String TRIGGER_GROUP_NAME = "DEFAULT_GROUP_NAME";


    public void saveJobCron(String jobName, Class<? extends Job> jobClass, String time, Map<String, Object> params) throws Exception {
        saveJobCron(jobName, jobName, jobName, TRIGGER_GROUP_NAME, jobClass, time, params);
    }

    private void saveJobCron(String jobName, String jobGroupName, String triggerName,
                             String triggerGroupName, Class<? extends Job> jobClass,
                             String time, Map<String, Object> params)
            throws Exception {
        SchedulerFactory schedulerFactory = new StdSchedulerFactory();
        Scheduler scheduler = schedulerFactory.getScheduler();

        TriggerKey triggerKey = TriggerKey.triggerKey(jobName, triggerGroupName);
        CronTrigger trigger = (CronTrigger) scheduler.getTrigger(triggerKey);
        //不存在，创建一个
        if (null == trigger) {
            JobDetail jobDetail = JobBuilder.newJob(jobClass).withIdentity(jobName, jobGroupName).requestRecovery().build();
            // 上下文data
            JobDataMap dataMap = jobDetail.getJobDataMap();
            convertMapToJobDataMap(params, dataMap);
            //表达式调度构建器
            CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule(time).withMisfireHandlingInstructionDoNothing();
            //按新的cronExpression表达式构建一个新的trigger
            trigger = TriggerBuilder.newTrigger().withIdentity(triggerName, triggerGroupName).withSchedule(scheduleBuilder).build();
            scheduler.scheduleJob(jobDetail, trigger);
            //启动
            scheduler.start();
        } else {
            // Trigger已存在，那么更新相应的定时设置
            //表达式调度构建器
            CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule(time).withMisfireHandlingInstructionDoNothing();
            //按新的cronExpression表达式重新构建trigger
            trigger = trigger.getTriggerBuilder().withIdentity(triggerKey)
                    .withSchedule(scheduleBuilder).build();
            //按新的trigger重新设置job执行
            scheduler.rescheduleJob(triggerKey, trigger);
            if (!scheduler.isStarted()){
                scheduler.start();
            }
        }
    }

    private void convertMapToJobDataMap(Map<String, Object> params, JobDataMap dataMap) {
        if(params != null){
            for (String key : params.keySet()) {
                Object value = params.get(key);
                dataMap.put(key, value);
            }
        }
    }

}
