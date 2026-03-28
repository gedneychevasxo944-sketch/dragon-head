package org.dragon.datasource.config;

import io.ebean.Database;
import io.ebean.DatabaseFactory;
import io.ebean.config.DatabaseConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Ebean Database 配置
 * 支持 MySQL 和 H2 多个数据源
 */
@Configuration
public class EbeanDatabaseConfig {

    /**
     * MySQL Ebean Database - 用于 ChatMessage 存储
     */
    @Bean("mysqlEbeanDatabase")
    public Database mysqlEbeanDatabase(@Qualifier("mysqlDataSource") DataSource mysqlDataSource) {
        DatabaseConfig config = new DatabaseConfig();
        config.setDataSource(mysqlDataSource);
        config.addPackage("org.dragon.datasource.entity");
        config.addPackage("org.dragon.user.entity");
        config.setName("mysql");
        config.setDefaultServer(false);
        return DatabaseFactory.create(config);
    }

    /**
     * H2 Ebean Database - 默认数据源
     */
    @Bean("h2EbeanDatabase")
    public Database h2EbeanDatabase(@Qualifier("h2DataSource") DataSource h2DataSource) {
        DatabaseConfig config = new DatabaseConfig();
        config.setDataSource(h2DataSource);
        config.addPackage("org.dragon.datasource.entity");
        config.setName("h2");
        config.setDefaultServer(true);
        return DatabaseFactory.create(config);
    }
}