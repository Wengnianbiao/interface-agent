package com.helianhealth.agent.remote.database;

import com.alibaba.druid.pool.DruidDataSource;
import com.helianhealth.agent.utils.JsonUtils;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * 数据库连接管理器
 */
@Component
public class DatabaseApiClientManager {

    // 连接池缓存（key=数据库URL，value=连接池）
    private final Map<String, DruidDataSource> dataSourceCache = new HashMap<>();

    /**
     * 根据元信息获取数据库连接
     */
    public Connection getConnection(String metaInfo) throws SQLException {
        // 解析 metaInfo 为数据库配置
        Map<String, String> metaMap = parseMetaInfo(metaInfo);

        // 获取或创建连接池
        DruidDataSource dataSource = getOrCreateDataSource(metaMap);

        // 从连接池获取连接
        return dataSource.getConnection();
    }

    /**
     * 解析元信息为Map
     */
    private Map<String, String> parseMetaInfo(String metaInfo) {
        return JsonUtils.fromJsonStringToMap(metaInfo);
    }

    /**
     * 获取或创建数据库连接池（线程安全）
     */
    private synchronized DruidDataSource getOrCreateDataSource(Map<String, String> metaMap) {
        String jdbcUrl = metaMap.get("url");
        String username = metaMap.get("username");
        String password = metaMap.get("password");
        String driverClassName = metaMap.get("driverClassName");

        return dataSourceCache.computeIfAbsent(jdbcUrl, url -> {
            DruidDataSource dataSource = new DruidDataSource();
            dataSource.setUrl(url);
            dataSource.setUsername(username);
            dataSource.setPassword(password);
            dataSource.setDriverClassName(driverClassName);

            // Druid 连接池配置（可根据实际情况调整）
            dataSource.setInitialSize(5);         // 初始化连接数
            dataSource.setMaxActive(20);          // 最大连接数
            dataSource.setMinIdle(5);             // 最小空闲连接数
            dataSource.setMaxWait(60000);         // 最大等待时间（毫秒）
            dataSource.setTimeBetweenEvictionRunsMillis(60000);
            dataSource.setMinEvictableIdleTimeMillis(300000);
            dataSource.setValidationQuery("SELECT 1");
            dataSource.setTestWhileIdle(true);
            dataSource.setTestOnBorrow(false);
            dataSource.setTestOnReturn(false);
            dataSource.setPoolPreparedStatements(true);
            dataSource.setMaxPoolPreparedStatementPerConnectionSize(20);

            return dataSource;
        });
    }

    /**
     * 关闭连接池（应用关闭时调用）
     */
    public void closeAllDataSources() {
        dataSourceCache.values().forEach(DruidDataSource::close);
        dataSourceCache.clear();
    }
}
