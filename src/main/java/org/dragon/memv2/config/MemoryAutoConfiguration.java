package org.dragon.memv2.config;

import org.dragon.memv2.app.DefaultMemoryFacade;
import org.dragon.memv2.app.DefaultCharacterMemoryService;
import org.dragon.memv2.app.DefaultWorkspaceMemoryService;
import org.dragon.memv2.app.DefaultSessionMemoryService;
import org.dragon.memv2.app.DefaultMemoryRecallService;
import org.dragon.memv2.app.DefaultMemoryExtractionService;
import org.dragon.memv2.app.DefaultMemoryRanker;
import org.dragon.memv2.app.DefaultMemoryDedupPolicy;
import org.dragon.memv2.core.MemoryFacade;
import org.dragon.memv2.core.CharacterMemoryService;
import org.dragon.memv2.core.WorkspaceMemoryService;
import org.dragon.memv2.core.SessionMemoryService;
import org.dragon.memv2.core.MemoryRecallService;
import org.dragon.memv2.core.MemoryExtractionService;
import org.dragon.memv2.core.MemoryRanker;
import org.dragon.memv2.core.MemoryDedupPolicy;
import org.dragon.memv2.storage.fs.FileCharacterMemoryRepository;
import org.dragon.memv2.storage.fs.FileWorkspaceMemoryRepository;
import org.dragon.memv2.storage.fs.FileSessionMemoryRepository;
import org.dragon.memv2.storage.MemoryPathResolver;
import org.dragon.memv2.storage.MemoryMarkdownParser;
import org.dragon.memv2.storage.MemoryIndexParser;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 记忆系统自动配置类
 * 负责Spring Boot环境下MemV2模块的Bean自动装配
 *
 * @author wyj
 * @version 1.0
 */
@Configuration
@EnableConfigurationProperties(MemoryProperties.class)
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
    public FileCharacterMemoryRepository fileCharacterMemoryRepository(
            MemoryPathResolver memoryPathResolver,
            MemoryMarkdownParser memoryMarkdownParser,
            MemoryIndexParser memoryIndexParser) {
        return new FileCharacterMemoryRepository(memoryPathResolver, memoryMarkdownParser, memoryIndexParser);
    }

    @Bean
    public FileWorkspaceMemoryRepository fileWorkspaceMemoryRepository(
            MemoryPathResolver memoryPathResolver,
            MemoryMarkdownParser memoryMarkdownParser,
            MemoryIndexParser memoryIndexParser) {
        return new FileWorkspaceMemoryRepository(memoryPathResolver, memoryMarkdownParser, memoryIndexParser);
    }

    @Bean
    public FileSessionMemoryRepository fileSessionMemoryRepository(
            MemoryPathResolver memoryPathResolver,
            MemoryMarkdownParser memoryMarkdownParser) {
        return new FileSessionMemoryRepository(memoryPathResolver, memoryMarkdownParser);
    }

    @Bean
    public DefaultCharacterMemoryService defaultCharacterMemoryService(
            FileCharacterMemoryRepository fileCharacterMemoryRepository) {
        return new DefaultCharacterMemoryService(fileCharacterMemoryRepository);
    }

    @Bean
    public DefaultWorkspaceMemoryService defaultWorkspaceMemoryService(
            FileWorkspaceMemoryRepository fileWorkspaceMemoryRepository) {
        return new DefaultWorkspaceMemoryService(fileWorkspaceMemoryRepository);
    }

    @Bean
    public DefaultSessionMemoryService defaultSessionMemoryService(
            FileSessionMemoryRepository fileSessionMemoryRepository,
            DefaultMemoryExtractionService defaultMemoryExtractionService) {
        return new DefaultSessionMemoryService(fileSessionMemoryRepository, defaultMemoryExtractionService);
    }

    @Bean
    public DefaultMemoryRecallService defaultMemoryRecallService(
            FileCharacterMemoryRepository fileCharacterMemoryRepository,
            FileWorkspaceMemoryRepository fileWorkspaceMemoryRepository) {
        return new DefaultMemoryRecallService(fileCharacterMemoryRepository, fileWorkspaceMemoryRepository);
    }

    @Bean
    public DefaultMemoryExtractionService defaultMemoryExtractionService(
            FileCharacterMemoryRepository fileCharacterMemoryRepository,
            FileWorkspaceMemoryRepository fileWorkspaceMemoryRepository) {
        return new DefaultMemoryExtractionService(fileCharacterMemoryRepository, fileWorkspaceMemoryRepository);
    }

    @Bean
    public DefaultMemoryRanker defaultMemoryRanker() {
        return new DefaultMemoryRanker();
    }

    @Bean
    public DefaultMemoryDedupPolicy defaultMemoryDedupPolicy() {
        return new DefaultMemoryDedupPolicy();
    }

    @Bean
    public DefaultMemoryFacade defaultMemoryFacade(
            DefaultCharacterMemoryService defaultCharacterMemoryService,
            DefaultWorkspaceMemoryService defaultWorkspaceMemoryService,
            DefaultSessionMemoryService defaultSessionMemoryService,
            DefaultMemoryRecallService defaultMemoryRecallService) {
        return new DefaultMemoryFacade(
                defaultCharacterMemoryService,
                defaultWorkspaceMemoryService,
                defaultSessionMemoryService,
                defaultMemoryRecallService
        );
    }
}
