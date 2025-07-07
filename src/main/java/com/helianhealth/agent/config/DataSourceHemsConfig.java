package com.helianhealth.agent.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class DataSourceHemsConfig {

    @Bean(name = "hemsJdbcTemplate")
    public JdbcTemplate hemsJdbcTemplate(@Qualifier("hemsDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean(name = "hemsDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.hems")
    public DataSource hemsDataSource() {
        return DataSourceBuilder.create().build();
    }
}
