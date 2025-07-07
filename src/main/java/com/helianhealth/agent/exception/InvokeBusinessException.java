package com.helianhealth.agent.exception;

/**
 * 远程方法调用异常
 */
public enum InvokeBusinessException implements BusinessCode, BusinessExceptionSupplier {

    DATABASE_EXECUTE_ERROR("INVOKE-001", "Database execute error!");

    private final String code;
    private final String defaultMessage;

    InvokeBusinessException(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    @Override
    public String getCode() {
        return this.code;
    }

    @Override
    public String getMessage() {
        return this.defaultMessage;
    }

    @Override
    public BaseBusinessException toException() {
        return new BaseBusinessException(this);
    }

    @Override
    public BaseBusinessException toException(String message) {
        return new BaseBusinessException(message, this);
    }
}
