package com.helianhealth.agent.controller.request.workflownode;

import lombok.Data;

/**
 * 创建工作流节点请求参数
 */
@Data
public class WorkflowNodeCreateReq {

    /**
     * 节点名称
     */
    private String nodeName;

    /**
     * 工作流ID
     */
    private Integer flowId;

    /**
     * 节点类型（HTTP/WEBSERVICE/DATABASE/MOCK）
     */
    private String nodeType;

    /**
     * 参数过滤表达式
     */
    private String paramFilterExpr;

    /**
     * API实例元数据（JSON格式），例如：
     * - HTTP: 包含url、headers、method等
     * - Database: 包含连接信息、表名、字段等
     */
    private String metaInfo;

    /**
     * 调度规则表达式（如 cron 表达式）
     */
    private String scheduleExpr;

    /**
     * 调度参数来源类型
     */
    private String scheduleParamSourceType;
}
