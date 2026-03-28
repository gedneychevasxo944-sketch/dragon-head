package org.dragon;

import lombok.extern.slf4j.Slf4j;
import org.dragon.datasource.config.DataSourceConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Import;

@Slf4j
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
@Import({DataSourceConfig.class})
public class DragonHeadApplication {

    public static void main(String[] args) {
        SpringApplication.run(DragonHeadApplication.class, args);
        log.info("龙头启动成功！！");
    }

}
