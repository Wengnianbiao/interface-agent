package com.helianhealth.agent.exception;

/**
 * 数据库模式调用异常
 */
public enum DatabaseRemoteBusinessException implements BusinessCode, BusinessExceptionSupplier {

    DATABASE_TEMPLATE_NOT_FOUND("DATABASE-001", "SQL模式下务必有模板配置！"),
    DATABASE_OPERATION_NOT_FOUND("DATABASE-002", "SQL模式下务必有操作类型!"),

    DATABASE_PARAM_TYPE_ERROR("DATABASE-003", "SQL模式下节点类型错误!"),
    DATABASE_PLACE_HOLDER_ERROR("DATABASE-004", "SQL模板中存在未匹配的参数占位符!")

    ;

    private final String code;
    private final String defaultMessage;

    DatabaseRemoteBusinessException(String code, String defaultMessage) {
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
