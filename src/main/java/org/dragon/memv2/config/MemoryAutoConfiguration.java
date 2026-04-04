package org.dragon.memv2.config;

import org.dragon.memv2.storage.MemoryPathResolver;
import org.dragon.memv2.storage.MemoryMarkdownParser;
import org.dragon.memv2.storage.MemoryIndexParser;
import org.dragon.memv2.storage.repo.CharacterMemoryRepository;
import org.dragon.memv2.storage.repo.WorkspaceMemoryRepository;
import org.dragon.memv2.storage.repo.SessionMemoryRepository;
import org.dragon.memv2.storage.fs.FileCharacterMemoryRepository;
import org.dragon.memv2.storage.fs.FileWorkspaceMemoryRepository;
import org.dragon.memv2.storage.fs.FileSessionMemoryRepository;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ComponentScan;

/**
 * 记忆系统自动配置类
 * 负责Spring Boot环境下MemV2模块的Bean自动装配
 *
 * @author binarytom
 * @version 1.0
 */
@Configuration
@EnableConfigurationProperties(MemoryProperties.class)
@ComponentScan(basePackages = "org.dragon.memv2")
public class MemoryAutoConfiguration {
    @Bean
    public MemoryPathResolver memoryPathResolver(MemoryProperties memoryProperties) {
        return new MemoryPathResolver(memoryProperties);
    }

    @Bean
    public MemoryMarkdownParser memoryMarkdownParser() {
        return new MemoryMarkdownParser();
    }

    @Bean
    public MemoryIndexParser memoryIndexParser() {
        return new MemoryIndexParser();
    }


    @Bean
    public SessionMemoryRepository sessionMemoryRepository(
            MemoryPathResolver pathResolver,
            MemoryMarkdownParser markdownParser) {
        return new FileSessionMemoryRepository(pathResolver, markdownParser);
    }
}
