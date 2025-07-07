package com.helianhealth.agent.model.domain;

import com.helianhealth.agent.enums.NodeType;
import com.helianhealth.agent.enums.ScheduleParamSourceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * 接口规则引擎工作流节点
 * 每个工作流节点对应的是三方接口的调用
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "interface_workflow_node")
public class InterfaceWorkflowNodeDO {

    @Id
    @Column(name = "node_id", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer nodeId;

    /**
     * 节点描述
     */
    @Column(name = "node_name")
    private String nodeName;

    /**
     * 工作流id
     */
    @Column(name = "flow_id")
    private Integer flowId;

    /**
     * 参数过滤表达式
     */
    @Column(name = "param_filter_expr")
    private String paramFilterExpr;

    /**
     * 节点厂商类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "node_type")
    private NodeType nodeType;

    /**
     * api实例元数据,以key-value封装的Json数据
     * e.g. 模式为Http下则包含请求的url、请求头、请求方式等
     * e.g. 模式为Database下则包含数据库连接信息、数据库表名、数据库字段名等
     */
    @Column(name = "meta_info")
    private String metaInfo;

    /**
     * 调度规则表达式
     */
    @Column(name = "schedule_expr")
    private String scheduleExpr;

    /**
     * 调度参数来源类型
     */
    @Column(name = "schedule_param_source_type")
    @Enumerated(EnumType.STRING)
    private ScheduleParamSourceType scheduleParamSourceType;
}
