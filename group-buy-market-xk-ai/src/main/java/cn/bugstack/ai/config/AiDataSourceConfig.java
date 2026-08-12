package cn.bugstack.ai.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * AI 知识库独立数据源（PostgreSQL，配置前缀 ai.datasource，从 AiProperties 显式构造）
 */
@Configuration
public class AiDataSourceConfig {

    @Bean(name = "aiDataSource")
    public DataSource aiDataSource(AiProperties aiProperties) {
        AiProperties.Datasource ds = aiProperties.getDatasource();
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(ds.getUrl());
        config.setUsername(ds.getUsername());
        config.setPassword(ds.getPassword());
        config.setDriverClassName(ds.getDriverClassName());
        AiProperties.Hikari hikari = ds.getHikari();
        if (hikari.getPoolName() != null) {
            config.setPoolName(hikari.getPoolName());
        }
        if (hikari.getMaximumPoolSize() != null) {
            config.setMaximumPoolSize(hikari.getMaximumPoolSize());
        }
        if (hikari.getMinimumIdle() != null) {
            config.setMinimumIdle(hikari.getMinimumIdle());
        }
        if (hikari.getConnectionTimeout() != null) {
            config.setConnectionTimeout(hikari.getConnectionTimeout());
        }
        return new HikariDataSource(config);
    }

    @Bean(name = "aiJdbcTemplate")
    public JdbcTemplate aiJdbcTemplate(@Qualifier("aiDataSource") DataSource aiDataSource) {
        return new JdbcTemplate(aiDataSource);
    }

}