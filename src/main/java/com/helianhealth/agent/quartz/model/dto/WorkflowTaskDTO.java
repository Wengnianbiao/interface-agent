package com.helianhealth.agent.quartz.model.dto;

import lombok.Data;

@Data
public class WorkflowTaskDTO {
    // 主键id
    private Integer id;

    // 任务bean名称
    private Integer workflowId;

    // 任务描述
    private String taskDesc;

    // cron表达式
    private String cronExpression;

    // 启动状态 0:关闭 1:启动
    private Integer openStatus;

    // 参数
    private String params;

    // 最近一次执行时间
    private String latestExecuteTime;
}
