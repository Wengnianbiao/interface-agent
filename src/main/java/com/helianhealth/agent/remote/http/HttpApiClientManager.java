package com.helianhealth.agent.remote.http;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class HttpApiClientManager {


    private final PoolingHttpClientConnectionManager connectionManager;
    private final CloseableHttpClient httpClient;
    private static final Logger logger = LoggerFactory.getLogger(HttpApiClientManager.class);

    // 默认连接超时时间，单位毫秒
    private static final int DEFAULT_CONNECTION_TIMEOUT = 20000;
    // 默认请求超时时间，单位毫秒
    private static final int DEFAULT_REQUEST_TIMEOUT = 20000;
    // 从连接池获取连接的超时时间，单位毫秒
    private static final int DEFAULT_CONNECTION_REQUEST_TIMEOUT = 10000;

    // 私有构造函数，防止外部实例化
    HttpApiClientManager() {
        // 创建连接池管理器
        connectionManager = new PoolingHttpClientConnectionManager();

        // 设置连接池参数
        connectionManager.setMaxTotal(200); // 最大连接数
        connectionManager.setDefaultMaxPerRoute(20); // 每个路由默认最大连接数

        // 创建请求配置
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(DEFAULT_CONNECTION_TIMEOUT))        // 连接超时时间
                .setResponseTimeout(Timeout.ofMilliseconds(DEFAULT_REQUEST_TIMEOUT))        // 读取超时时间
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(DEFAULT_CONNECTION_REQUEST_TIMEOUT)) // 从连接池获取连接的超时时间
                .build();

        // 创建HttpClient实例
        httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .build();
    }

    /**
     * 执行HTTP POST请求
     * @param url 请求URL
     * @param requestBody 请求体
     * @return 响应内容
     * @throws IOException IO异常
     */
    public String executePost(String url, String requestBody) throws IOException, ParseException {
        logger.info("执行 POST 请求，URL: [{}]，请求体: [{}]", url, requestBody);
        HttpPost httpPost = new HttpPost(url);
        if (requestBody != null && !requestBody.isEmpty()) {
            httpPost.setEntity(new StringEntity(requestBody, ContentType.APPLICATION_JSON.withCharset(StandardCharsets.UTF_8)));
        }
        try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
            String responseEntity = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            logger.info("POST 请求响应: [{}]", responseEntity);
            return responseEntity;
        } catch (IOException | ParseException e) {
            logger.error("执行 POST 请求失败，URL: [{}]，异常: [{}]", url, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 执行HTTP GET请求
     * @param url 请求URL
     * @return 响应内容
     * @throws IOException IO异常
     */
    public String executeGet(String url) throws IOException, ParseException {
        logger.info("执行 GET 请求，URL: [{}]", url);
        HttpGet httpGet = new HttpGet(url);
        try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
            int statusCode = response.getCode();
            logger.info("GET 请求响应状态码: [{}]", statusCode);
            return EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
        } catch (IOException | ParseException e) {
            logger.error("执行 GET 请求失败，URL: [{}]，异常: [{}]", url, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 获取连接池状态信息
     * @return 连接池状态字符串
     */
    public String getPoolStats() {
        return connectionManager.getTotalStats().toString();
    }

    /**
     * 关闭连接池和相关资源
     */
    public void close() {
        try {
            httpClient.close();
            connectionManager.close();
        } catch (Exception e) {
            logger.error("Error closing HTTP client resources", e);
        }
    }
}