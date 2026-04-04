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
 * 支持 MySQL
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
        config.addPackage("org.dragon.permission.entity");
        config.setName("mysql");
        config.setDefaultServer(true);
        return DatabaseFactory.create(config);
    }
}