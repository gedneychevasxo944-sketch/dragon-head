# Dragon Head 项目规范

## 编码规范

编写代码时必须遵守 `.claude/rules/` 下的所有规范文件：

| 规范文件 | 内容 |
|---------|------|
| [rule_prompt.md](.claude/rules/rule_prompt.md) | Prompt 管理规范 |
| [rule_config.md](.claude/rules/rule_config.md) | 配置管理规范 |
| [rule_llm.md](.claude/rules/rule_llm.md) | LLM 调用规范 |
| [rule_react.md](.claude/rules/rule_react.md) | ReAct 执行规范 |
| [rule_character_workspace.md](.claude/rules/rule_character_workspace.md) | Character 与 Workspace 规范 |
| [rule_store.md](.claude/rules/rule_store.md) | 存储层规范 |
| [rule_general.md](.claude/rules/rule_general.md) | 通用代码规范 |

## 快速检查清单

提交代码前，逐项确认：
- [ ] 没有在业务代码中硬编码 prompt 字符串
- [ ] 新 prompt key 已在 PromptKeys 中定义为常量
- [ ] 新 prompt 默认值已在 PromptInitializer 中注册
- [ ] 所有 LLM 调用走 LLMCaller 接口，未直接引用具体实现类
- [ ] ConfigStore 的读写使用了 ConfigKey 的语义工厂方法
- [ ] 存储层依赖注入的是接口而非 Memory 实现类
- [ ] 所有 public 方法有 Javadoc
- [ ] 日志格式包含类名前缀
