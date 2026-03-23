package org.dragon.datasource;

import io.ebean.Database;
import io.ebean.DatabaseFactory;
import io.ebean.config.DatabaseConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Ebean Database 配置
 * 配置 Ebean 使用 MySQL 数据源
 */
@Configuration
public class EbeanDatabaseConfig {

    @Bean
    public Database ebeanDatabase(@Qualifier("mysqlDataSource") DataSource mysqlDataSource) {
        DatabaseConfig config = new DatabaseConfig();
        config.setDataSource(mysqlDataSource);
        config.addPackage("org.dragon.workspace.chat");
        config.setDefaultServer(true);
        return DatabaseFactory.create(config);
    }
}