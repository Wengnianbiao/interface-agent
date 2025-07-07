package com.helianhealth.agent.model.domain;

import com.helianhealth.agent.config.ListIntegerTypeHandler;
import com.helianhealth.agent.enums.WorkflowContentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.ibatis.type.JdbcType;
import tk.mybatis.mapper.annotation.ColumnType;

import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.List;

/**
 * 接口工作流
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "interface_workflow")
public class InterfaceWorkflowDO {

    @Id
    @Column(name = "flow_id", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer flowId;

    /**
     * 工作流描述
     */
    @Column(name = "flow_name")
    private String flowName;

    /**
     * uri,上游(jarvis)调用接口的路由和flow是一对一的关系
     */
    @Column(name = "interface_uri")
    private String interfaceUri;

    /**
     * 入参格式:e.g.xml、json
     */
    @Column(name = "content_type")
    @Enumerated(EnumType.STRING)
    private WorkflowContentType contentType;

    /**
     * content元数据信息
     */
    @Column(name = "content_meta_info")
    private String contentMetaInfo;

    /**
     * 工作流首节点:每个工作流必定会有一个(or多个)起始节点
     */
    @Column(name = "first_flow_nodes")
    @ColumnType(typeHandler = ListIntegerTypeHandler.class, jdbcType = JdbcType.VARCHAR)
    private List<Integer> firstFlowNodes;

    /**
     * 1-启用，0-禁用
     */
    @Column(name = "status")
    private Integer status;
}
