package com.helianhealth.agent.config;

import com.alibaba.druid.spring.boot.autoconfigure.DruidDataSourceBuilder;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import tk.mybatis.mapper.autoconfigure.ConfigurationCustomizer;
import tk.mybatis.spring.annotation.MapperScan;

import javax.sql.DataSource;

/**
 * 体软数据库
 */
@Configuration
@MapperScan(basePackages = {"com.helianhealth.agent.mapper.hems"}, sqlSessionFactoryRef = "hemsSqlSessionFactory",sqlSessionTemplateRef = "hemsSqlSessionTemplate")
public class DataSourceHemsConfig {

    @Bean(name = "hemsDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.hems")
    public DataSource dataSource() {
        return DruidDataSourceBuilder.create().build();
    }

    @Bean(name = "hemsSqlSessionFactory")
    public SqlSessionFactory sqlSessionFactory(@Qualifier("hemsDataSource") DataSource dataSource) throws Exception {
        SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
        bean.setDataSource(dataSource);
        bean.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath*:mapper/agent/**/*.xml"));
        // 开启驼峰映射
        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
        configuration.setMapUnderscoreToCamelCase(true);

        return bean.getObject();
    }

    @Bean(name = "hemsSqlSessionTemplate")
    public SqlSessionTemplate sqlSessionTemplate(@Qualifier("hemsSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }

    @Bean(name = "hemsTransactionManager")
    public DataSourceTransactionManager transactionManager(@Qualifier("hemsDataSource") DataSource ds){
        return new DataSourceTransactionManager(ds);
    }

    @Bean
    public ConfigurationCustomizer mybatisConfigurationCustomizer() {
        return configuration -> configuration.getTypeHandlerRegistry().register(ListIntegerTypeHandler.class);
    }
}
