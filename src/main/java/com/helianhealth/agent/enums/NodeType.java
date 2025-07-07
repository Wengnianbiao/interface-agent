package com.helianhealth.agent.enums;

import lombok.Getter;

/**
 * 三方接口调用方式枚举
 */
@Getter
public enum NodeType {
    /**
     * 数据库,存储过程&视图
     */
    DATABASE,

    /**
     * webservice,入参通常为xml格式
     */
    WEBSERVICE,

    /**
     * http,入参通常为json
     */
    HTTP,

    /**
     * Mock,mock模式
     */
    MOCK,

    /**
     * 无三方接口调用
     */
    NONE
}
