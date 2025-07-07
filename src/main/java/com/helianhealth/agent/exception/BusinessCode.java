package com.helianhealth.agent.exception;

/**
 * 业务错误码接口，定义错误码和错误信息的标准格式
 */
public interface BusinessCode {

    /**
     * 获取错误码
     */
    String getCode();

    /**
     * 获取错误信息
     */
    String getMessage();
}
