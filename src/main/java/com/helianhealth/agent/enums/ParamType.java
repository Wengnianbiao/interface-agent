package com.helianhealth.agent.enums;

import lombok.Getter;

/**
 * 参数类型
 */
@Getter
public enum ParamType {

    /**
     * 字符串
     */
    STRING,

    /**
     * 数字INT
     */
    INTEGER,

    /**
     * 数字LONG
     */
    LONG,

    /**
     * 布尔
     */
    BOOLEAN,

    /**
     * 对象
     */
    OBJECT,

    /**
     * 数组
     */
    ARRAY,

    /**
     * 接口入参就是数组
     */
    PURE_ARRAY,

    /**
     * 无
     */
    NONE;
}
