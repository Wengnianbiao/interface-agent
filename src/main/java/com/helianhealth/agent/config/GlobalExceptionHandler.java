package com.helianhealth.agent.config;

import com.helianhealth.agent.common.ResultData;
import com.helianhealth.agent.exception.BaseBusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    // 处理业务异常（BaseBusinessException 及其子类）
    @ExceptionHandler(BaseBusinessException.class)
    public ResponseEntity<ResultData<?>> handleBusinessException(BaseBusinessException ex) {
        ResultData<?> resultData = ResultData.builder()
                .code(ex.getBusinessCode().getCode())
                .message(ex.getMessage())
                .rsp(null) // 业务异常通常数据返回 null，也可按需处理
                .build();
        return new ResponseEntity<>(resultData, HttpStatus.BAD_REQUEST);
    }

    // 处理系统异常（其他未捕获的异常）
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResultData<?>> handleSystemException() {
        ResultData<?> resultData = ResultData.builder()
                .code(String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value())) // 系统异常返回 500
                .message("Internal Server Error")
                .rsp(null)
                .build();

        return new ResponseEntity<>(resultData, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}