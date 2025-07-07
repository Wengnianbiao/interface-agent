package com.helianhealth.agent.common;


import lombok.Getter;

@Getter
public enum ResultStatusCode {

    OK("200", "OK"),
    ERROR("-1", "未知异常");


    private final String code;

    private final String msg;

    ResultStatusCode(String code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
