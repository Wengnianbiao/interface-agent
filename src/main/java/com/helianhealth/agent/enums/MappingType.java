package com.helianhealth.agent.enums;

import lombok.Getter;

/**
 * 映射规则类型
 */
@Getter
public enum MappingType {

    /**
     * 固定值
     */
    CONSTANT,

    /**
     * 名称映射
     */
    NAME,

    /**
     * 表达式:通常为SpEL
     */
    EXPRESSION,

    /**
     * Bean表达式映射:主要场景还是查体软表完成字段映射
     */
    BEAN_EXPRESSION,

    /**
     * 直接映射,兼容数组的情况
     */
    DIRECT
}
