package com.helianhealth.agent.exception;

/**
 * JSON工具异常
 */
public enum JsonUtilsException implements BusinessCode, BusinessExceptionSupplier {

    JSON_PROCESS_ERROR("JSON-001", "JSON处理异常"),
    JSON_PARSE_ERROR("JSON-002", "JSON解析失败"),
    JSON_SERIALIZE_ERROR("JSON-003", "JSON序列化失败"),
    DEEP_COPY_ERROR("JSON-004", "对象深拷贝失败");

    private final String code;
    private final String defaultMessage;

    JsonUtilsException(String code, String defaultMessage) {
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
