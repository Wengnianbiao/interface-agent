package com.helianhealth.agent.exception;

import lombok.Getter;

/**
 * 业务异常基类，所有业务相关异常应继承此类
 */
@Getter
public class BaseBusinessException extends RuntimeException {
    private final BusinessCode businessCode;

    public BaseBusinessException(BusinessCode businessCode) {
        super(businessCode.getMessage());
        this.businessCode = businessCode;
    }

    public BaseBusinessException(String message, BusinessCode businessCode) {
        super(message);
        this.businessCode = businessCode;
    }

    public String getErrorCode() {
        return businessCode.getCode();
    }

    @Override
    public String getMessage() {
        return String.format("[%s] %s", businessCode.getCode(), super.getMessage());
    }
}
