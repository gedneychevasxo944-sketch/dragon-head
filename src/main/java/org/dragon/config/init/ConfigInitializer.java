package org.dragon.config.init;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dragon.config.store.ConfigDefinitionStore;
import org.dragon.datasource.entity.ConfigDefinition;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ConfigInitializer 配置项定义初始化器
 *
 * <p>在应用启动时将所有配置项定义写入 config_definitions 表
 *
 * <p>配置项分类：
 * <ul>
 *   <li>GLOBAL: 全局配置（系统级、LLM 凭证等）</li>
 *   <li>STUDIO: 用户级配置</li>
 *   <li>WORKSPACE: 工作空间配置</li>
 *   <li>MEMBER: 成员配置</li>
 *   <li>CHARACTER: Character 资产配置</li>
 *   <li>MEMORY: 记忆配置</li>
 *   <li>OBSERVER: Observer 配置</li>
 *   <li>SKILL: Skill 配置</li>
 *   <li>TOOL: Tool 配置</li>
 * </ul>
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class ConfigInitializer implements CommandLineRunner {

    private final ConfigDefinitionStore configDefinitionStore;

    @Override
    public void run(String... args) {
        log.info("[ConfigInitializer] Starting config definitions initialization...");
        long startTime = System.currentTimeMillis();

        int total = 0;
        int created = 0;

        // GLOBAL 配置项
        total += getGlobalDefinitions().size();
        created += saveDefinitions(getGlobalDefinitions());

        // STUDIO 配置项
        total += getStudioDefinitions().size();
        created += saveDefinitions(getStudioDefinitions());

        // WORKSPACE 配置项
        total += getWorkspaceDefinitions().size();
        created += saveDefinitions(getWorkspaceDefinitions());

        // MEMBER 配置项
        total += getMemberDefinitions().size();
        created += saveDefinitions(getMemberDefinitions());

        // CHARACTER 配置项
        total += getCharacterDefinitions().size();
        created += saveDefinitions(getCharacterDefinitions());

        // MEMORY 配置项
        total += getMemoryDefinitions().size();
        created += saveDefinitions(getMemoryDefinitions());

        // OBSERVER 配置项
        total += getObserverDefinitions().size();
        created += saveDefinitions(getObserverDefinitions());

        // SKILL 配置项
        total += getSkillDefinitions().size();
        created += saveDefinitions(getSkillDefinitions());

        // TOOL 配置项
        total += getToolDefinitions().size();
        created += saveDefinitions(getToolDefinitions());

        // WORKFLOW/BASH/SANDBOX 配置项
        total += getRuntimeDefinitions().size();
        created += saveDefinitions(getRuntimeDefinitions());

        // 外部服务配置项
        total += getExternalServiceDefinitions().size();
        created += saveDefinitions(getExternalServiceDefinitions());

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("[ConfigInitializer] Completed: {} definitions total, {} created ({} existing), {}ms",
                total, created, total - created, elapsed);
    }

    /**
     * 批量保存定义，已存在的跳过
     */
    private int saveDefinitions(List<ConfigDefinition> definitions) {
        int created = 0;
        for (ConfigDefinition def : definitions) {
            if (!configDefinitionStore.exists(def.getScopeType(), def.getConfigKey())) {
                def.setCreatedAt(LocalDateTime.now());
                def.setUpdatedAt(LocalDateTime.now());
                configDefinitionStore.save(def);
                created++;
                log.debug("[ConfigInitializer] Created: {}:{}", def.getScopeType(), def.getConfigKey());
            }
        }
        return created;
    }

    // ==================== GLOBAL 配置项 ====================

    private List<ConfigDefinition> getGlobalDefinitions() {
        List<ConfigDefinition> defs = new ArrayList<>();

        // Platform 配置
        defs.add(makeDef("GLOBAL", "platform.defaultMaxToken", "NUMBER", "默认最大 Token 数", 4096));
        defs.add(makeDef("GLOBAL", "platform.defaultModel", "STRING", "默认模型", "deepseek-chat"));

        // Character 默认配置
        defs.add(makeDef("GLOBAL", "character.defaultAllowedTools", "LIST", "Character 默认允许工具列表", null));
        defs.add(makeDef("GLOBAL", "character.defaultMemoryBackend", "STRING", "默认记忆后端", "memory"));

        // Workspace 默认配置
        defs.add(makeDef("GLOBAL", "workspace.defaultObserverMode", "STRING", "Workspace 默认 Observer 模式", "evaluation"));
        defs.add(makeDef("GLOBAL", "workspace.maxMembersPerWorkspace", "NUMBER", "Workspace 最大成员数", 10));

        // Skill 默认配置
        defs.add(makeDef("GLOBAL", "skill.defaultVisibility", "STRING", "Skill 默认可见性", "private"));

        // Memory 默认配置
        defs.add(makeDef("GLOBAL", "memory.defaultSyncStrategy", "STRING", "默认同步策略", "lazy"));

        // Observer 默认配置
        defs.add(makeDef("GLOBAL", "observer.defaultEvaluationMode", "STRING", "默认评估模式", "auto"));

        // System 配置
        defs.add(makeDef("GLOBAL", "system.maintenanceMode", "BOOLEAN", "系统维护模式", false));
        defs.add(makeDef("GLOBAL", "system.allowUserRegistration", "BOOLEAN", "允许用户注册", true));
        defs.add(makeDef("GLOBAL", "system.defaultLocale", "STRING", "默认语言", "zh-CN"));

        // JWT 配置
        defs.add(makeDef("GLOBAL", "jwt.secret", "STRING", "JWT 签名密钥", null));
        defs.add(makeDef("GLOBAL", "jwt.access-token-validity", "NUMBER", "Access Token 有效期(秒)", 7200));
        defs.add(makeDef("GLOBAL", "jwt.refresh-token-validity", "NUMBER", "Refresh Token 有效期(秒)", 604800));

        // Deepseek LLM 配置
        defs.add(makeDef("GLOBAL", "llm.deepseek.apiKey", "STRING", "DeepSeek API Key", null));
        defs.add(makeDef("GLOBAL", "llm.deepseek.baseUrl", "STRING", "DeepSeek API URL", "https://api.deepseek.com"));
        defs.add(makeDef("GLOBAL", "llm.deepseek.model", "STRING", "DeepSeek 模型", "deepseek-chat"));

        // Minimax LLM 配置
        defs.add(makeDef("GLOBAL", "llm.minimax.apiKey", "STRING", "Minimax API Key", null));
        defs.add(makeDef("GLOBAL", "llm.minimax.baseUrl", "STRING", "Minimax API URL", "https://api.minimax.chat"));
        defs.add(makeDef("GLOBAL", "llm.minimax.model", "STRING", "Minimax 模型", "MiniMax-Text-01"));
        defs.add(makeDef("GLOBAL", "llm.minimax.groupId", "STRING", "Minimax Group ID", null));

        // Kimi LLM 配置
        defs.add(makeDef("GLOBAL", "llm.kimi.apiKey", "STRING", "Kimi API Key", null));
        defs.add(makeDef("GLOBAL", "llm.kimi.baseUrl", "STRING", "Kimi API URL", "https://api.moonshot.cn"));
        defs.add(makeDef("GLOBAL", "llm.kimi.model", "STRING", "Kimi 模型", "moonshot-v1-8k"));
        defs.add(makeDef("GLOBAL", "llm.kimi.embeddingModel", "STRING", "Kimi Embedding 模型", "m3e"));

        return defs;
    }

    // ==================== STUDIO 配置项 ====================

    private List<ConfigDefinition> getStudioDefinitions() {
        List<ConfigDefinition> defs = new ArrayList<>();
        defs.add(makeDef("STUDIO", "studio.defaultCharacterParams", "OBJECT", "Studio 默认 Character 参数", null));
        defs.add(makeDef("STUDIO", "studio.independentRunMode", "BOOLEAN", "Studio 独立运行模式", false));
        defs.add(makeDef("STUDIO", "studio.defaultMemoryScope", "STRING", "Studio 默认记忆范围", "WORKSPACE"));
        defs.add(makeDef("STUDIO", "studio.skillBindingStrategy", "STRING", "Studio Skill 绑定策略", "auto"));
        defs.add(makeDef("STUDIO", "studio.memberInvitePolicy", "STRING", "Studio 成员邀请策略", "approval"));
        return defs;
    }

    // ==================== WORKSPACE 配置项 ====================

    private List<ConfigDefinition> getWorkspaceDefinitions() {
        List<ConfigDefinition> defs = new ArrayList<>();
        defs.add(makeDef("WORKSPACE", "workspace.personality", "OBJECT", "Workspace 个性化配置", null));
        defs.add(makeDef("WORKSPACE", "workspace.defaultObserverConfig", "OBJECT", "Workspace 默认 Observer 配置", null));
        defs.add(makeDef("WORKSPACE", "workspace.autoSyncMemory", "BOOLEAN", "Workspace 自动同步记忆", true));
        defs.add(makeDef("WORKSPACE", "workspace.memberPermissions", "OBJECT", "Workspace 成员权限配置", null));
        defs.add(makeDef("WORKSPACE", "workspace.allowedChannels", "LIST", "Workspace 允许的渠道", null));
        defs.add(makeDef("WORKSPACE", "workspace.featureFlags", "OBJECT", "Workspace 功能开关", null));
        return defs;
    }

    // ==================== MEMBER 配置项 ====================

    private List<ConfigDefinition> getMemberDefinitions() {
        List<ConfigDefinition> defs = new ArrayList<>();
        defs.add(makeDef("MEMBER", "member.role", "STRING", "成员角色", "member"));
        defs.add(makeDef("MEMBER", "member.resourceQuota", "OBJECT", "成员资源配额", null));
        defs.add(makeDef("MEMBER", "member.notificationSettings", "OBJECT", "成员通知设置", null));
        defs.add(makeDef("MEMBER", "member.displayPreferences", "OBJECT", "成员显示偏好", null));
        return defs;
    }

    // ==================== CHARACTER 配置项 ====================

    private List<ConfigDefinition> getCharacterDefinitions() {
        List<ConfigDefinition> defs = new ArrayList<>();
        defs.add(makeDef("CHARACTER", "character.maxToken", "NUMBER", "Character 最大 Token 数", 4096));
        defs.add(makeDef("CHARACTER", "character.temperature", "NUMBER", "Character 温度参数", 0.7));
        defs.add(makeDef("CHARACTER", "character.topP", "NUMBER", "Character TopP 参数", 0.9));
        defs.add(makeDef("CHARACTER", "character.systemPrompt", "STRING", "Character 系统提示词", null));
        defs.add(makeDef("CHARACTER", "character.allowedTools", "LIST", "Character 允许的工具列表", null));
        return defs;
    }

    // ==================== MEMORY 配置项 ====================

    private List<ConfigDefinition> getMemoryDefinitions() {
        List<ConfigDefinition> defs = new ArrayList<>();
        defs.add(makeDef("MEMORY", "memory.backend", "STRING", "记忆后端类型", "memory"));
        defs.add(makeDef("MEMORY", "memory.provider", "STRING", "记忆提供者", "built-in"));
        defs.add(makeDef("MEMORY", "memory.syncStrategy", "STRING", "记忆同步策略", "lazy"));
        defs.add(makeDef("MEMORY", "memory.indexStrategy", "STRING", "记忆索引策略", "auto"));
        defs.add(makeDef("MEMORY", "memory.recallLimit", "NUMBER", "记忆召回限制", 10));
        defs.add(makeDef("MEMORY", "memory.maxMemorySize", "NUMBER", "最大记忆大小", 10000));
        defs.add(makeDef("MEMORY", "memory.retentionPolicy", "STRING", "记忆保留策略", "forever"));
        defs.add(makeDef("MEMORY", "memory.embeddingModel", "STRING", "记忆 Embedding 模型", "m3e"));
        defs.add(makeDef("MEMORY", "memory.vectorDimension", "NUMBER", "向量维度", 1536));
        return defs;
    }

    // ==================== OBSERVER 配置项 ====================

    private List<ConfigDefinition> getObserverDefinitions() {
        List<ConfigDefinition> defs = new ArrayList<>();
        defs.add(makeDef("OBSERVER", "observer.evaluationMode", "STRING", "Observer 评估模式", "auto"));
        defs.add(makeDef("OBSERVER", "observer.optimizationThreshold", "NUMBER", "Observer 优化阈值", 0.6));
        defs.add(makeDef("OBSERVER", "observer.autoOptimization", "BOOLEAN", "Observer 自动优化", false));
        defs.add(makeDef("OBSERVER", "observer.reportingInterval", "NUMBER", "Observer 报告间隔(分钟)", 60));
        return defs;
    }

    // ==================== SKILL 配置项 ====================

    private List<ConfigDefinition> getSkillDefinitions() {
        List<ConfigDefinition> defs = new ArrayList<>();
        defs.add(makeDef("SKILL", "skill.visibility", "STRING", "Skill 可见性", "private"));
        defs.add(makeDef("SKILL", "skill.publishStrategy", "STRING", "Skill 发布策略", "manual"));
        defs.add(makeDef("SKILL", "skill.allowedCategories", "LIST", "Skill 允许的分类", null));
        return defs;
    }

    // ==================== TOOL 配置项 ====================

    private List<ConfigDefinition> getToolDefinitions() {
        List<ConfigDefinition> defs = new ArrayList<>();
        defs.add(makeDef("TOOL", "tool.category", "STRING", "Tool 分类", "utility"));
        defs.add(makeDef("TOOL", "tool.parameters", "OBJECT", "Tool 参数定义", null));
        defs.add(makeDef("TOOL", "tool.version", "STRING", "Tool 版本", "1.0.0"));
        defs.add(makeDef("TOOL", "tool.dependencies", "LIST", "Tool 依赖", null));
        defs.add(makeDef("TOOL", "tool.enabled", "BOOLEAN", "Tool 启用状态", true));
        return defs;
    }

    // ==================== WORKFLOW/RUNTIME 配置项 ====================

    private List<ConfigDefinition> getRuntimeDefinitions() {
        List<ConfigDefinition> defs = new ArrayList<>();

        // Workflow 配置
        defs.add(makeDef("GLOBAL", "workflow.maxIterations", "NUMBER", "工作流最大迭代次数", 50));
        defs.add(makeDef("GLOBAL", "evaluation.maxDurationMs", "NUMBER", "评估最大耗时(毫秒)", 60000));
        defs.add(makeDef("GLOBAL", "evaluation.maxTokens", "NUMBER", "评估最大 Token 数", 4096));

        // BASH/EXEC 配置
        defs.add(makeDef("GLOBAL", "bash.defaultJobTtlMs", "NUMBER", "默认任务 TTL(毫秒)", 300000));
        defs.add(makeDef("GLOBAL", "bash.minJobTtlMs", "NUMBER", "最小任务 TTL(毫秒)", 60000));
        defs.add(makeDef("GLOBAL", "bash.maxJobTtlMs", "NUMBER", "最大任务 TTL(毫秒)", 3600000));
        defs.add(makeDef("GLOBAL", "bash.defaultMaxOutputChars", "NUMBER", "默认最大输出字符", 100000));
        defs.add(makeDef("GLOBAL", "bash.defaultTailChars", "NUMBER", "默认尾部字符数", 5000));
        defs.add(makeDef("GLOBAL", "exec.defaultTimeoutSeconds", "NUMBER", "执行默认超时(秒)", 300));
        defs.add(makeDef("GLOBAL", "exec.maxOutputLength", "NUMBER", "执行最大输出长度", 100000));
        defs.add(makeDef("GLOBAL", "filetools.maxMatches", "NUMBER", "文件搜索最大匹配数", 100));
        defs.add(makeDef("GLOBAL", "webfetch.maxBodyChars", "NUMBER", "Web 获取最大字符数", 500000));
        defs.add(makeDef("GLOBAL", "webfetch.requestTimeout", "NUMBER", "Web 获取请求超时(秒)", 30));
        defs.add(makeDef("GLOBAL", "websearch.timeout", "NUMBER", "Web 搜索超时(秒)", 30));

        // Sandbox 配置
        defs.add(makeDef("GLOBAL", "sandbox.workspace.root", "STRING", "Sandbox 工作空间根目录", "/tmp/agent-sandbox"));
        defs.add(makeDef("GLOBAL", "sandbox.docker.image", "STRING", "Docker 镜像", "dragon-sandbox:latest"));
        defs.add(makeDef("GLOBAL", "sandbox.docker.containerPrefix", "STRING", "容器名称前缀", "agent-sandbox"));
        defs.add(makeDef("GLOBAL", "sandbox.docker.idleHours", "NUMBER", "空闲容器清理阈值(小时)", 24));
        defs.add(makeDef("GLOBAL", "sandbox.docker.maxAgeDays", "NUMBER", "容器最大保留天数", 7));
        defs.add(makeDef("GLOBAL", "sandbox.browser.cdpPort", "NUMBER", "CDP 端口", 9222));
        defs.add(makeDef("GLOBAL", "sandbox.browser.vncPort", "NUMBER", "VNC 端口", 5900));
        defs.add(makeDef("GLOBAL", "sandbox.browser.novncPort", "NUMBER", "noVNC 端口", 6080));

        return defs;
    }

    // ==================== 外部服务配置项 ====================

    private List<ConfigDefinition> getExternalServiceDefinitions() {
        List<ConfigDefinition> defs = new ArrayList<>();

        // SMS 配置
        defs.add(makeDef("GLOBAL", "sms.aliyun.access-key", "STRING", "阿里云 SMS Access Key", null));
        defs.add(makeDef("GLOBAL", "sms.aliyun.access-secret", "STRING", "阿里云 SMS Access Secret", null));
        defs.add(makeDef("GLOBAL", "sms.aliyun.sign-name", "STRING", "阿里云 SMS 签名", null));
        defs.add(makeDef("GLOBAL", "sms.aliyun.template-code", "STRING", "阿里云 SMS 模板码", null));

        // WeChat 配置
        defs.add(makeDef("GLOBAL", "wechat.app-id", "STRING", "微信 App ID", null));
        defs.add(makeDef("GLOBAL", "wechat.app-secret", "STRING", "微信 App Secret", null));

        // Feishu 配置
        defs.add(makeDef("GLOBAL", "channel.feishu.appId", "STRING", "飞书 App ID", null));
        defs.add(makeDef("GLOBAL", "channel.feishu.appSecret", "STRING", "飞书 App Secret", null));
        defs.add(makeDef("GLOBAL", "channel.feishu.robotOpenId", "STRING", "飞书 Robot OpenID", null));
        defs.add(makeDef("GLOBAL", "channel.feishu.whitelist", "LIST", "飞书白名单", null));
        defs.add(makeDef("GLOBAL", "channel.feishu.wakeWord", "STRING", "飞书唤醒词", null));
        defs.add(makeDef("GLOBAL", "channel.feishu.whitelist.enabled", "BOOLEAN", "飞书白名单启用", false));

        // Skill Storage 配置
        defs.add(makeDef("GLOBAL", "skill.storage.s3.bucket", "STRING", "S3 Bucket 名称", null));
        defs.add(makeDef("GLOBAL", "skill.storage.local.base-path", "STRING", "本地存储路径", "./skills"));
        defs.add(makeDef("GLOBAL", "skill.workspace.template-dir", "STRING", "Skill 模板目录", null));
        defs.add(makeDef("GLOBAL", "skill.workspace.exec-dir", "STRING", "Skill 执行目录", null));

        return defs;
    }

    // ==================== 工厂方法 ====================

    /**
     * 创建配置项定义
     *
     * @param scopeType 作用域类型
     * @param configKey 配置键（不含 scopeType 前缀）
     * @param valueType 值类型：NUMBER, STRING, BOOLEAN, LIST, OBJECT
     * @param description 描述
     * @param defaultValue 默认值
     * @return ConfigDefinition
     */
    private ConfigDefinition makeDef(String scopeType, String configKey, String valueType,
                                      String description, Object defaultValue) {
        String id = scopeType + ":" + configKey;
        return ConfigDefinition.builder()
                .id(id)
                .scopeType(scopeType)
                .configKey(configKey)
                .valueType(valueType)
                .description(description)
                .defaultValue(defaultValue)
                .version(1)
                .build();
    }
}