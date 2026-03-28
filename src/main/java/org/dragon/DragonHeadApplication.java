package org.dragon;

import org.dragon.config.config.ConfigAutoConfiguration;
import org.dragon.datasource.config.DataSourceConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Import;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
@Import({ConfigAutoConfiguration.class, DataSourceConfig.class})
public class DragonHeadApplication {

    public static void main(String[] args) {
        SpringApplication.run(DragonHeadApplication.class, args);
    }

}
