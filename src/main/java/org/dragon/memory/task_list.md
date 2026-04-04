# memv2 模块待办清单

## 架构文档与代码实现对比分析

通过对 `mem_architecture.md` 架构文档和现有代码的全面梳理，发现以下需要完善的部分：

## 一、核心功能完善

### 1. MemoryId 类缺失
- **当前问题**：直接使用 String 类型作为记忆ID，缺少统一包装类
- **架构文档建议**：应使用 MemoryId 类统一标识，便于审计和引用关系管理
- **待办**：创建 MemoryId 类，重构 MemoryEntry 以使用该类型

### 2. MemoryFacade 接口优化
- **当前问题**：缺少 recallForAgent() 方法，不支持 AgentMemoryContext 参数
- **架构文档建议**：应包含接受 AgentMemoryContext 的召回方法
- **待办**：添加 recallForAgent() 方法到 MemoryFacade 接口和 DefaultMemoryFacade 实现

## 二、配置与初始化完善

### 1. MemoryAutoConfiguration 装配优化
- **当前问题**：缺少对 SessionMemoryRepository 的装配
- **架构文档建议**：应统一装配所有 repository 实现
- **待办**：补充 MemoryAutoConfiguration，添加 SessionMemoryRepository 装配

### 2. application.yml 配置示例
- **当前问题**：resources 目录下缺少 memory 配置的详细说明
- **架构文档建议**：需要在 resources 目录下添加配置示例
- **待办**：创建 application.yml 配置文件示例

## 三、工具类完善

### 1. FileIO 工具类
- **当前问题**：缺少文件操作的工具类
- **架构文档建议**：需要提供文件读写、目录创建等工具方法
- **待办**：创建 FileIO 工具类

### 2. FileMemoryLookup 工具类
- **当前问题**：缺少文件系统记忆查找工具
- **架构文档建议**：需要提供按 ID 查找、列表查询等工具方法
- **待办**：创建 FileMemoryLookup 工具类

## 四、服务层优化

### 1. MemoryRecallService 会话记忆召回
- **当前问题**：DefaultMemoryRecallService 缺少会话记忆召回逻辑
- **架构文档建议**：recallComposite() 方法应包含会话记忆召回
- **待办**：完善 DefaultMemoryRecallService，添加会话记忆召回

## 五、API 接口完善

### 1. SessionController 接口补充
- **当前问题**：缺少会话启动、关闭和检查点接口
- **架构文档建议**：需要提供完整的会话生命周期管理接口
- **待办**：补充 SessionController，添加会话启动、关闭和检查点接口

### 2. MemoryController 查询接口
- **当前问题**：缺少记忆查询和列表接口
- **架构文档建议**：需要提供获取单个记忆和列表查询的接口
- **待办**：补充 MemoryController，添加查询和列表接口

## 六、测试与验证

### 1. 单元测试完善
- **当前问题**：缺少对核心组件的单元测试
- **待办**：为关键类创建单元测试

### 2. 集成测试
- **当前问题**：缺少模块间集成测试
- **待办**：创建集成测试，验证完整流程

## 优先级排序

### 高优先级（核心功能）
1. MemoryId 类创建
2. MemoryFacade 接口优化
3. MemoryAutoConfiguration 装配补充
4. FileIO 和 FileMemoryLookup 工具类创建

### 中优先级（重要功能）
1. MemoryRecallService 会话记忆召回
2. SessionController 接口补充
3. MemoryController 查询接口
4. application.yml 配置示例

### 低优先级（优化与测试）
1. 单元测试创建
2. 集成测试创建

## 已实现部分（90%）

### 核心组件
- ✅ MemoryEntry、SessionSnapshot、MemoryScope、MemoryType 数据模型
- ✅ MemoryPathResolver、MemoryMarkdownParser、MemoryIndexParser
- ✅ FileCharacterMemoryRepository、FileWorkspaceMemoryRepository、FileSessionMemoryRepository
- ✅ MemoryFacade、CharacterMemoryService、WorkspaceMemoryService、SessionMemoryService
- ✅ MemoryRecallService、MemoryExtractionService、MemoryRoutingPolicy
- ✅ MemoryDedupPolicy、MemoryRanker、MemoryValidationPolicy
- ✅ SessionToLongTermBridge、SessionCompressionService

### Web 层
- ✅ MemoryController（基本CRUD和召回）
- ✅ SessionController（更新和刷新）

### 配置与初始化
- ✅ MemoryProperties、MemoryAutoConfiguration
- ✅ SessionController

### 策略层
- ✅ DefaultMemoryRoutingPolicy（路由规则）
- ✅ DefaultMemoryDedupPolicy（去重逻辑）
- ✅ DefaultMemoryRanker（排序策略）

## 总体进度评估

**已实现部分（90%）**：
- 核心数据模型完整实现
- 所有三层存储实现（角色/工作空间/会话）
- 完整的服务框架和策略层
- 基本的Web接口

**待实现部分（10%）**：
- 缺少统一的MemoryId包装类
- 需要优化MemoryFacade接口
- 缺少工具类和配置示例
- 需要完善会话记忆召回和API接口

---

**评估结论**：memv2模块已完成大部分核心功能，但在统一标识、接口优化和工具类方面还有小的完善空间。建议按优先级逐步实现，确保系统的完整性和架构对齐。