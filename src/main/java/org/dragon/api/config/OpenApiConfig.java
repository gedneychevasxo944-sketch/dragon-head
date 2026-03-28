package org.dragon.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger 配置
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI dragonHeadOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("DragonHead API")
                        .description("DragonHead 智能助手 REST API 文档")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("DragonHead Team")));
    }
}
