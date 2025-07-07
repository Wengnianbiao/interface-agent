package com.helianhealth.agent.quartz.model.entity;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Data
@Entity
@Table(name = "workflow_task")
public class WorkflowTaskDO {

    @ApiModelProperty(value ="主键id")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ApiModelProperty(value ="任务工作流名称")
    @Column(name = "workflow_id")
    private Integer workflowId;

    @ApiModelProperty(value ="任务描述")
    @Column(name = "task_desc")
    private String taskDesc;

    @ApiModelProperty(value ="cron表达式")
    @Column(name = "cron_expression")
    private String cronExpression;

    @ApiModelProperty(value ="启动状态 0:关闭 1:启动")
    @Column(name = "open_status")
    private Integer openStatus;

    @ApiModelProperty(value ="工作流调度入参")
    @Column(name = "params")
    private String params;

    @ApiModelProperty(value ="最近一次执行时间")
    @Column(name = "latest_execute_time")
    private String latestExecuteTime;
}
