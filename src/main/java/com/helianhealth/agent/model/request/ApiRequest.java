package com.helianhealth.agent.model.request;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * api请求参数
 * @param <T>请求参数
 */
@Data
@AllArgsConstructor
public class ApiRequest<T> {

    private String apiUri;

    private T data;
}
