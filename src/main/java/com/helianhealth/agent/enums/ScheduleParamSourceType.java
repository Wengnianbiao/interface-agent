package com.helianhealth.agent.enums;

import lombok.Getter;

/**
 * 调度节点参数来源类型
 */
@Getter
public enum ScheduleParamSourceType {

    /**
     * 原始入参
     */
    ORIGINAL,

    /**
     * 当前节点出参
     */
    CURRENT_OUTPUT
}
