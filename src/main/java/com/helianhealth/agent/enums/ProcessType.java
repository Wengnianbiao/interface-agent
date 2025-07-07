package com.helianhealth.agent.enums;

import lombok.Getter;

/**
 * 工作流节点的参数处理类型枚举:前置处理or后置处理
 */
@Getter
public enum ProcessType {

    /**
     * 参数前置处理
     */
    PRE_PROCESS,

    /**
     * 参数后置处理
     */
    POST_PROCESS;
}
