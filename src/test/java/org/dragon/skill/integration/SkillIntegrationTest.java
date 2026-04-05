package org.dragon.skill.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dragon.DragonHeadApplication;
import org.dragon.skill.domain.SkillDO;
import org.dragon.skill.dto.*;
import org.dragon.skill.enums.*;
import org.dragon.skill.runtime.*;
import org.dragon.skill.service.*;
import org.dragon.tools.AgentTool;
import org.dragon.tools.builtin.SkillTool;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Skill 模块集成测试
 *
 * <p>测试完整的 Skill 生命周期流程：
 * 1. 注册 → 2. 发布 → 3. 绑定 → 4. 查询 → 5. 执行 → 6. 使用统计
 */
@SpringBootTest(classes = DragonHeadApplication.class)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SkillIntegrationTest {

    // ==================== Service 层依赖 ====================

    @Autowired
    private SkillRegisterService registerService;

    @Autowired
    private SkillLifecycleService lifecycleService;

    @Autowired
    private SkillQueryService queryService;

    @Autowired
    private SkillBindingService bindingService;

    @Autowired
    private SkillUsageService usageService;

    // ==================== Runtime 层依赖 ====================

    @Autowired
    private SkillRegistry skillRegistry;

    @Autowired
    private SkillFilter skillFilter;

    @Autowired
    private SkillExecutor skillExecutor;

    @Autowired
    private SkillPermissionChecker permissionChecker;

    // ==================== Tool ====================

    @Autowired
    private SkillTool skillTool;

    // ==================== 辅助 ====================

    private static String currentSkillId;
    private static Long currentBindingId;
    private static final String TEST_CHARACTER_ID = "test-character-001";
    private static final String TEST_WORKSPACE_ID = "test-workspace-001";
    private static final Long OPERATOR_ID = 1L;
    private static final String OPERATOR_NAME = "test-operator";

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // ==================== 1. 注册测试 ====================

    @Test
    @Order(1)
    @DisplayName("1.1 表单注册新 Skill")
    void testRegisterSkill() {
        // given: 创建 MockMultipartFile 模拟 ZIP 文件上传
        String skillContent = "# Git Commit Skill\n\nGenerate a commit message.";
        MockMultipartFile zipFile = new MockMultipartFile(
                "file",
                "git-commit.zip",
                "application/zip",
                createMockZipContent(skillContent)
        );

        SkillRegisterRequest request = new SkillRegisterRequest();
        request.setName("git-commit-test");
        request.setDisplayName("Git Commit Test");
        request.setDescription("Generate commit message");
        request.setContent(skillContent);
        request.setCategory("development");
        request.setVisibility("public");
        request.setExecutionContext("inline");

        // when
        SkillRegisterResult result = registerService.register(zipFile, request, OPERATOR_ID, OPERATOR_NAME);

        // then
        assertNotNull(result);
        assertNotNull(result.getSkillId());
        assertEquals(1, result.getVersion());
        assertEquals("draft", result.getStatus());

        currentSkillId = result.getSkillId();
        System.out.println("[注册] skillId=" + currentSkillId);
    }

    @Test
    @Order(2)
    @DisplayName("1.2 查询刚注册的 Skill 详情")
    void testQueryNewSkill() {
        // when
        SkillDetailVO detail = queryService.getDetail(currentSkillId, true);

        // then
        assertNotNull(detail);
        assertEquals("git-commit-test", detail.getName());
        assertEquals("Git Commit Test", detail.getDisplayName());
        assertEquals(SkillStatus.DRAFT, detail.getStatus());
        assertEquals(1, detail.getVersion());
        assertTrue(detail.getContent().contains("Git Commit Skill"));
        System.out.println("[查询] 刚注册的 Skill: name=" + detail.getName() + ", status=" + detail.getStatus());
    }

    // ==================== 2. 发布测试 ====================

    @Test
    @Order(3)
    @DisplayName("2.1 发布 Skill: draft → active")
    void testPublishSkill() {
        // when
        lifecycleService.publish(currentSkillId, OPERATOR_ID);

        // then
        SkillDetailVO detail = queryService.getDetail(currentSkillId, false);
        assertEquals(SkillStatus.ACTIVE, detail.getStatus());
        assertNotNull(detail.getPublishedAt());
        System.out.println("[发布] skillId=" + currentSkillId + ", status=ACTIVE");
    }

    @Test
    @Order(4)
    @DisplayName("2.2 查询已发布的 Skill")
    void testQueryPublishedSkill() {
        // when: 使用 pageSearch 查询 active 状态的 skill
        SkillQueryRequest queryRequest = new SkillQueryRequest();
        queryRequest.setStatus(SkillStatus.ACTIVE);
        PageResult<SkillSummaryVO> pageResult = queryService.pageSearch(queryRequest);

        // then
        assertNotNull(pageResult);
        assertTrue(pageResult.getTotal() > 0);
        boolean found = pageResult.getItems().stream()
                .anyMatch(s -> s.getSkillId().equals(currentSkillId));
        assertTrue(found);
        System.out.println("[查询] 已发布 Skill 数量: " + pageResult.getTotal());
    }

    // ==================== 3. 绑定测试 ====================

    @Test
    @Order(5)
    @DisplayName("3.1 绑定 Skill 到 Character")
    void testBindSkillToCharacter() {
        // given
        SkillBindingRequest request = new SkillBindingRequest();
        request.setSkillId(currentSkillId);
        request.setVersionType("latest");

        // when
        SkillBindingResult result = bindingService.bindToCharacter(TEST_CHARACTER_ID, request);

        // then
        assertNotNull(result);
        assertNotNull(result.getBindingId());
        currentBindingId = result.getBindingId();
        assertEquals("latest", result.getVersionType());
        System.out.println("[绑定] bindingId=" + currentBindingId + ", characterId=" + TEST_CHARACTER_ID);
    }

    @Test
    @Order(6)
    @DisplayName("3.2 查询 Character 的绑定列表")
    void testQueryCharacterBindings() {
        // when
        List<SkillBindingVO> bindings = bindingService.listByCharacter(TEST_CHARACTER_ID);

        // then
        assertTrue(bindings.size() > 0);
        boolean found = bindings.stream()
                .anyMatch(b -> b.getSkillId().equals(currentSkillId));
        assertTrue(found);
        System.out.println("[查询] Character " + TEST_CHARACTER_ID + " 绑定数量: " + bindings.size());
    }

    // ==================== 4. 运行时执行测试 ====================

    @Test
    @Order(7)
    @DisplayName("4.1 SkillRegistry 获取 Skill 列表")
    void testGetSkillsFromRegistry() {
        // when
        List<SkillDefinition> skills = skillRegistry.getSkills(TEST_CHARACTER_ID, TEST_WORKSPACE_ID);

        // then
        assertNotNull(skills);
        SkillDefinition skill = skills.stream()
                .filter(s -> s.getName().equals("git-commit-test"))
                .findFirst()
                .orElse(null);
        assertNotNull(skill);
        System.out.println("[Registry] 获取到 " + skills.size() + " 个 Skills");
    }

    @Test
    @Order(8)
    @DisplayName("4.2 SkillFilter 过滤")
    void testSkillFilter() {
        // given
        List<SkillDefinition> allSkills = skillRegistry.getSkills(TEST_CHARACTER_ID, TEST_WORKSPACE_ID);

        // when
        List<SkillDefinition> visibleSkills = skillFilter.filter(allSkills);

        // then
        assertNotNull(visibleSkills);
        assertTrue(visibleSkills.size() <= allSkills.size());
        System.out.println("[Filter] 过滤前: " + allSkills.size() + ", 过滤后: " + visibleSkills.size());
    }

    @Test
    @Order(9)
    @DisplayName("4.3 SkillTool 执行 Skill")
    void testSkillToolExecute() throws Exception {
        // given
        String sessionKey = "test-session-001";

        ObjectNode parameters = objectMapper.createObjectNode();
        parameters.put("skill", "git-commit-test");
        parameters.put("args", "test: test integration");

        AgentTool.ToolContext context = AgentTool.ToolContext.builder()
                .parameters(parameters)
                .sessionKey(sessionKey)
                .characterId(TEST_CHARACTER_ID)
                .workspaceId(TEST_WORKSPACE_ID)
                .cwd("/workspace")
                .build();

        // when
        CompletableFuture<AgentTool.ToolResult> future = skillTool.execute(context);
        AgentTool.ToolResult result = future.get();

        // then
        assertTrue(result.isSuccess(), "Skill should execute successfully");
        assertNotNull(result.getData());
        System.out.println("[SkillTool] 执行成功, output=" + result.getOutput());
    }

    // ==================== 5. 状态流转测试 ====================

    @Test
    @Order(10)
    @DisplayName("5.1 下架 Skill: active → disabled")
    void testDisableSkill() {
        // when
        lifecycleService.disable(currentSkillId, OPERATOR_ID);

        // then
        SkillDetailVO detail = queryService.getDetail(currentSkillId, false);
        assertEquals(SkillStatus.DISABLED, detail.getStatus());
        System.out.println("[下架] skillId=" + currentSkillId + ", status=DISABLED");
    }

    @Test
    @Order(11)
    @DisplayName("5.2 重新发布: disabled → active")
    void testRepublishSkill() {
        // when
        lifecycleService.republish(currentSkillId, OPERATOR_ID);

        // then
        SkillDetailVO detail = queryService.getDetail(currentSkillId, false);
        assertEquals(SkillStatus.ACTIVE, detail.getStatus());
        System.out.println("[重新发布] skillId=" + currentSkillId + ", status=ACTIVE");
    }

    // ==================== 6. 使用统计测试 ====================

    @Test
    @Order(12)
    @DisplayName("6.1 记录 Skill 使用")
    void testRecordSkillUsage() {
        // given
        SkillDefinition skill = skillRegistry.findByName(TEST_CHARACTER_ID, TEST_WORKSPACE_ID, "git-commit-test");
        assertNotNull(skill);

        AgentContext agentContext = AgentContext.builder()
                .characterId(TEST_CHARACTER_ID)
                .workspaceId(TEST_WORKSPACE_ID)
                .agentId("test-agent-001")
                .build();

        // when & then: 记录成功（异步，不抛异常即可）
        assertDoesNotThrow(() -> {
            usageService.recordSuccess(skill, agentContext, "test-session", "test args", 100L);
        });
        System.out.println("[使用统计] 记录成功");
    }

    @Test
    @Order(13)
    @DisplayName("6.2 查询 Skill 使用排行")
    void testQueryUsageRanking() {
        // when
        List<SkillUsageService.SkillRankItem> ranking = usageService.rankGlobal(null, 10);

        // then
        assertNotNull(ranking);
        System.out.println("[使用统计] 全局排行数量: " + ranking.size());
    }

    // ==================== 7. 清理测试 ====================

    @Test
    @Order(14)
    @DisplayName("7.1 解绑 Skill")
    void testUnbindSkill() {
        // when
        bindingService.unbind(currentBindingId);

        // then: 解绑后查询不到
        List<SkillBindingVO> bindings = bindingService.listByCharacter(TEST_CHARACTER_ID);
        boolean stillExists = bindings.stream()
                .anyMatch(b -> b.getSkillId().equals(currentSkillId));
        assertFalse(stillExists);
        System.out.println("[解绑] bindingId=" + currentBindingId);
    }

    @Test
    @Order(15)
    @DisplayName("7.2 删除 Skill: → deleted")
    void testDeleteSkill() {
        // when
        lifecycleService.delete(currentSkillId, OPERATOR_ID);

        // then
        SkillDetailVO detail = queryService.getDetail(currentSkillId, false);
        assertEquals(SkillStatus.DELETED, detail.getStatus());
        System.out.println("[删除] skillId=" + currentSkillId + ", status=DELETED");
    }

    // ==================== 辅助方法 ====================

    /**
     * 创建模拟的 ZIP 文件内容（包含有效的 SKILL.md）
     */
    private byte[] createMockZipContent(String skillContent) {
        try {
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(baos);

            // 创建 SKILL.md 内容（包含 frontmatter）
            String frontmatter = "---\n" +
                    "name: git-commit-test\n" +
                    "displayName: Git Commit Test\n" +
                    "description: Generate commit message\n" +
                    "context: inline\n" +
                    "category: development\n" +
                    "visibility: public\n" +
                    "---\n\n";
            String fullContent = frontmatter + skillContent;

            zos.putNextEntry(new java.util.zip.ZipEntry("SKILL.md"));
            zos.write(fullContent.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            zos.closeEntry();

            zos.finish();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create mock ZIP content", e);
        }
    }
}