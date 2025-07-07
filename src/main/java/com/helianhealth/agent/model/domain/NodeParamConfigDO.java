package com.helianhealth.agent.model.domain;

import com.helianhealth.agent.enums.MappingSource;
import com.helianhealth.agent.enums.MappingType;
import com.helianhealth.agent.enums.OperationType;
import com.helianhealth.agent.enums.ParamType;
import com.helianhealth.agent.enums.ProcessType;
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
 * 工作流节点参数前置处理配置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "node_param_config")
public class NodeParamConfigDO {

    /**
     * 参数id
     */
    @Id
    @Column(name = "config_id", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer configId;

    /**
     * 处理类型：前置PRE_PROCESS 或 后置POST_PROCESS
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "process_type", nullable = false)
    private ProcessType processType;

    /**
     * 所属工作流节点
     */
    @Column(name = "node_id")
    private Integer nodeId;

    /**
     * 父节点ID（顶级节点为null）
     */
    @Column(name = "parent_id")
    private Integer parentId;

    /**
     * 参数键名:这个其实是jarvis的参数名称
     */
    @Column(name = "source_param_key")
    private String sourceParamKey;

    /**
     * 节点类型：object、array、string、integer等
     */
    @Column(name = "source_param_type")
    @Enumerated(EnumType.STRING)
    private ParamType sourceParamType;

    /**
     * 参数释义
     */
    @Column(name = "param_desc")
    private String paramDesc;

    /**
     *三方接口所需要的参数名称
     */
    @Column(name = "target_param_key")
    private String targetParamKey;

    /**
     * 三方接口所需要的参数类型
     */
    @Column(name = "target_param_type")
    @Enumerated(EnumType.STRING)
    private ParamType targetParamType;

    /**
     * 三方接口所需要的参数类型
     */
    @Column(name = "operation_type")
    @Enumerated(EnumType.STRING)
    private OperationType operationType;

    /**
     * 排序号（控制参数顺序）
     */
    @Column(name = "sort")
    private Integer sort;

    /**
     * 映射规则枚举
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "mapping_type")
    private MappingType mappingType;

    /**
     * 映射来源
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "mapping_source")
    private MappingSource mappingSource;

    /**
     * 映射规则
     * 根据映射类型进行解析
     * 譬如如果固定值，这里的就是个固定值
     * 如果是表达式，这里就是表达式
     * 如果是名称映射，这里就是名称映射后的字段值
     */
    @Column(name = "mapping_rule")
    private String mappingRule;
}
