package org.dragon.datasource.config;

import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * MySQL 数据源配置
 */
@Configuration
public class DataSourceConfig {

    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${spring.datasource.driver-class-name}")
    private String driverClassName;

    @Bean
    @Primary
    public DataSource mysqlDataSource() {
        DataSource dataSource = DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .url(jdbcUrl)
                .username(username)
                .password(password)
                .driverClassName(driverClassName)
                .build();
        // 启动 flyway 做库表迁移
        // 运行 Flyway 迁移
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .cleanDisabled(false)
                .placeholderReplacement(false)
                .load();
        flyway.clean();
        flyway.migrate();
        return dataSource;
    }
}