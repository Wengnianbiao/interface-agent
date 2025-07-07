package com.helianhealth.agent.exception;

/**
 * 业务异常提供者接口，用于将业务错误码转换为具体的异常实例
 */
public interface BusinessExceptionSupplier {
    /**
     * 创建默认消息的业务异常
     */
    BaseBusinessException toException();

    /**
     * 创建带自定义消息的业务异常
     */
    BaseBusinessException toException(String message);
}
