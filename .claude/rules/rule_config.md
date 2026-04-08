# 配置管理规范

## 核心原则

所有配置的存取必须通过 `org.dragon.config.store.ConfigStore + ConfigKey` 完成，禁止散落在代码各处硬编码配置键字符串。

## 核心组件

### 2.1 ConfigKey 使用

**文件路径：** `org.dragon.config.store.ConfigKey`

- 使用语义明确的静态工厂方法构造 ConfigKey，**禁止直接 new ConfigKey(...)**

```java
// 正确示例
ConfigKey.character(workspace, characterId, "feishu.appId")
ConfigKey.model(modelId, "maxTokens")
ConfigKey.of("app.globalSwitch")

// 错误示例
new ConfigKey("default", "character", characterId, "feishu.appId")
```

### 2.2 ConfigProperties 使用

**文件路径：** `org.dragon.config.config.ConfigProperties`

- 与 Spring 配置文件（application.properties）绑定的配置，使用 `org.dragon.config.config.ConfigProperties`
- 使用 `@ConfigurationProperties(prefix = "dragon.xxx")` 注解
- **禁止散落使用 `@Value`**

```java
// 正确示例 —— 使用 ConfigProperties
@Service
public class XxxService {
    private final ConfigProperties config;

    public XxxService(ConfigProperties config) {
        this.config = config;
    }

    public void doSomething() {
        if (config.isEnabled()) {
            // ...
        }
    }
}

// 错误示例 —— 散落 @Value
@Value("${dragon.xxx.enabled}")
private boolean enabled;
```

## 配置属性命名规范

| 前缀 | 模块 | 示例 |
|------|------|------|
| `dragon.store` | 存储配置 | `dragon.store.type=MYSQL` |
| `dragon.config` | 配置中心 | `dragon.config.enabled=true` |
| `llm.xxx` | LLM 配置 | `llm.kimi.apiKey=${KIMI_API_KEY:}` |
| `channel.xxx` | 渠道配置 | `channel.feishu.appId=cli_xxx` |
| `jwt.xxx` | JWT 配置 | `jwt.secret=${JWT_SECRET:adeptify-256-bit-secret-key-for-jwt-signing}` |
| `sms.xxx` | 短信配置 | `sms.aliyun.access-key=${ALIYUN_ACCESS_KEY:}` |
| `wechat.xxx` | 微信配置 | `wechat.app-id=${WECHAT_APP_ID:}` |
| `skill.xxx` | Skill 配置 | `skill.storage.backend=local` |
| `sandbox.xxx` | 沙箱配置 | `sandbox.root-dir=/tmp/agent-sandbox` |

## 敏感配置处理

敏感配置必须使用环境变量占位，禁止明文硬编码：

```properties
# 正确 - 使用环境变量
llm.kimi.apiKey=${KIMI_API_KEY:}

# 错误 - 明文硬编码
llm.kimi.apiKey=your-actual-api-key
```

默认值通过 `:defaultValue` 语法指定：

```properties
jwt.secret=${JWT_SECRET:adeptify-256-bit-secret-key-for-jwt-signing}
jwt.access-token-validity=${JWT_ACCESS_VALIDITY:7200}
```

## 已有配置示例

| 配置文件 | 路径 |
|---------|------|
| `StoreProperties` | `org.dragon.store.config.StoreProperties` |
| `ConfigProperties` | `org.dragon.config.config.ConfigProperties` |
| `ScheduleProperties` | `org.dragon.schedule.config.ScheduleProperties` |
| `ApplicationProperties` | `src/main/resources/application.properties` |
