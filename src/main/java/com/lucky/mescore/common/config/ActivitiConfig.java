package com.lucky.mescore.common.config;

import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.impl.cfg.StandaloneProcessEngineConfiguration;
import org.activiti.engine.impl.history.HistoryLevel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Activiti 流程引擎手动配置。
 * <p>
 * 由于项目使用 Spring Boot 3 + Jakarta，而 Activiti 官方的
 * activiti-spring-boot-starter 依赖 Spring Boot 2 (javax)，
 * 此处直接通过 ProcessEngineConfiguration 创建 ProcessEngine Bean，
 * 共享 Spring Boot 管理的 DataSource。
 */
@Configuration
public class ActivitiConfig {

    @Value("${spring.datasource.druid.url}")
    private String jdbcUrl;

    @Value("${spring.datasource.druid.username}")
    private String jdbcUsername;

    @Value("${spring.datasource.druid.password}")
    private String jdbcPassword;

    @Value("${spring.datasource.druid.driver-class-name}")
    private String jdbcDriver;

    @Bean(destroyMethod = "close")
    public ProcessEngine processEngine() {
        // 使用 createStandaloneProcessEngineConfiguration() 创建默认配置
        StandaloneProcessEngineConfiguration config = (StandaloneProcessEngineConfiguration)
                ProcessEngineConfiguration.createStandaloneProcessEngineConfiguration();

        config.setJdbcUrl(jdbcUrl)
                .setJdbcUsername(jdbcUsername)
                .setJdbcPassword(jdbcPassword)
                .setJdbcDriver(jdbcDriver)
                .setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_FALSE)
                .setHistoryLevel(HistoryLevel.AUDIT)
                .setAsyncExecutorActivate(true);

        return config.buildProcessEngine();
    }
}
