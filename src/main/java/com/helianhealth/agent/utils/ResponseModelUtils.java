package com.helianhealth.agent.utils;

import com.helianhealth.agent.common.ResultData;
import com.helianhealth.agent.common.ResultStatusCode;

public class ResponseModelUtils {

    public static <T> ResultData<T> success() {
        return new ResultData<>(ResultStatusCode.OK, ResultStatusCode.OK.getMsg(), null);
    }

    public static <T> ResultData<T> render(T rsp) {
        return new ResultData<>(ResultStatusCode.OK, ResultStatusCode.OK.getMsg(), rsp);
    }

    public static <T> ResultData<T> error(String msg) {
        return new ResultData<>(ResultStatusCode.ERROR, msg, null);
    }
}
