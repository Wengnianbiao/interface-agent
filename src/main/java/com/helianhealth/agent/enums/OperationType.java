package com.helianhealth.agent.enums;

import lombok.Getter;

/**
 * 数据库操作枚举
 */
@Getter
public enum OperationType {

    EQUAL("="),
    NOT_EQUAL("!="),
    GREATER_THAN(">"),
    LESS_THAN("<"),
    GREATER_EQUAL(">="),
    LESS_EQUAL("<="),
    LIKE("LIKE"),
    IN("IN"),
    NOT_IN("NOT IN"),
    IS_NULL("IS NULL"),
    IS_NOT_NULL("IS NOT NULL");

    private final String operator;

    OperationType(String operator) {
        this.operator = operator;
    }
}
