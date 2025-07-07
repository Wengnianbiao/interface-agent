package com.helianhealth.agent.model.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Date;

/**
 * 接口调用日志记录实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "interface_invoke_log")
public class InterfaceInvokeLogDO {

    /**
     * 日志ID
     */
    @Id
    @Column(name = "log_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long logId;

    /**
     * 工作流节点ID
     */
    @Column(name = "node_id")
    private Integer nodeId;

    /**
     * 业务数据
     */
    @Column(name = "business_data")
    private String businessData;

    /**
     * 调用前参数
     */
    @Column(name = "param_before_invoke")
    private String paramBeforeInvoke;

    /**
     * 远程调用响应
     */
    @Column(name = "remote_invoke_response")
    private String remoteInvokeResponse;

    /**
     * 调用后参数
     */
    @Column(name = "param_after_invoke")
    private String paramAfterInvoke;

    /**
     * 调用耗时
     */
    @Column(name = "invoke_time")
    private String invokeTime;

    /**
     * 创建时间
     */
    @Column(name = "create_time")
    private Date createTime;
}
