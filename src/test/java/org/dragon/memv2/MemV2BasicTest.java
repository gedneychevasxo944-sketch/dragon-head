package org.dragon.memv2;

import org.dragon.memv2.app.DefaultMemoryExtractionService;
import org.dragon.memv2.app.DefaultMemoryRoutingPolicy;
import org.dragon.memv2.app.DefaultMemoryValidationPolicy;
import org.dragon.memv2.app.DefaultMemoryDedupPolicy;
import org.dragon.memv2.config.MemoryAutoConfiguration;
import org.dragon.memv2.storage.fs.FileCharacterMemoryRepository;
import org.dragon.memv2.storage.fs.FileWorkspaceMemoryRepository;
import org.dragon.memv2.storage.MemoryPathResolver;
import org.dragon.memv2.storage.MemoryMarkdownParser;
import org.dragon.memv2.storage.MemoryIndexParser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * MemV2 模块基本功能测试
 *
 * @author binarytom
 * @version 1.0
 */
@SpringBootTest(classes = MemoryAutoConfiguration.class)
public class MemV2BasicTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    public void testApplicationContextLoads() {
        assertNotNull(applicationContext, "ApplicationContext should not be null");
    }

    @Test
    public void testMemoryPathResolverBean() {
        MemoryPathResolver pathResolver = applicationContext.getBean(MemoryPathResolver.class);
        assertNotNull(pathResolver, "MemoryPathResolver bean should be available");
    }

    @Test
    public void testMemoryMarkdownParserBean() {
        MemoryMarkdownParser markdownParser = applicationContext.getBean(MemoryMarkdownParser.class);
        assertNotNull(markdownParser, "MemoryMarkdownParser bean should be available");
    }

    @Test
    public void testMemoryIndexParserBean() {
        MemoryIndexParser indexParser = applicationContext.getBean(MemoryIndexParser.class);
        assertNotNull(indexParser, "MemoryIndexParser bean should be available");
    }

    @Test
    public void testFileCharacterMemoryRepositoryBean() {
        FileCharacterMemoryRepository characterRepository = applicationContext.getBean(FileCharacterMemoryRepository.class);
        assertNotNull(characterRepository, "FileCharacterMemoryRepository bean should be available");
    }

    @Test
    public void testFileWorkspaceMemoryRepositoryBean() {
        FileWorkspaceMemoryRepository workspaceRepository = applicationContext.getBean(FileWorkspaceMemoryRepository.class);
        assertNotNull(workspaceRepository, "FileWorkspaceMemoryRepository bean should be available");
    }

    @Test
    public void testDefaultMemoryExtractionServiceBean() {
        DefaultMemoryExtractionService extractionService = applicationContext.getBean(DefaultMemoryExtractionService.class);
        assertNotNull(extractionService, "DefaultMemoryExtractionService bean should be available");
    }

    @Test
    public void testDefaultMemoryRoutingPolicyBean() {
        DefaultMemoryRoutingPolicy routingPolicy = applicationContext.getBean(DefaultMemoryRoutingPolicy.class);
        assertNotNull(routingPolicy, "DefaultMemoryRoutingPolicy bean should be available");
    }

    @Test
    public void testDefaultMemoryValidationPolicyBean() {
        DefaultMemoryValidationPolicy validationPolicy = applicationContext.getBean(DefaultMemoryValidationPolicy.class);
        assertNotNull(validationPolicy, "DefaultMemoryValidationPolicy bean should be available");
    }

    @Test
    public void testDefaultMemoryDedupPolicyBean() {
        DefaultMemoryDedupPolicy dedupPolicy = applicationContext.getBean(DefaultMemoryDedupPolicy.class);
        assertNotNull(dedupPolicy, "DefaultMemoryDedupPolicy bean should be available");
    }
}
