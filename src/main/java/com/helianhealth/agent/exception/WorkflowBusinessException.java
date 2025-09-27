package com.helianhealth.agent.exception;

/**
 * 工作流异常
 */
public enum WorkflowBusinessException implements BusinessCode, BusinessExceptionSupplier {

    METHOD_NOT_SUPPORT("API-001", "Unsupported method calls!"),
    FLOW_NOT_FOUND("API-002", "Flow not found!"),
    EXPRESSION_PARSING_ERROR("API-003", "Expression parsing error!");

    private final String code;
    private final String defaultMessage;

    WorkflowBusinessException(String code, String defaultMessage) {
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
