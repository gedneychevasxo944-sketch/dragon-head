# Asset 资产模块规范

## 模块定位

`org.dragon.asset` 是系统的基础架构模块，为所有资产（Character、Skill、Workspace、Memory、Trait、Observer 等）提供**统一的 Owner 管理、发布状态控制和关联关系维护**。该模块被 23+ 个其他服务引用，是系统的共享内核。

**禁止在其他业务服务中重复 owner/status/association 相关逻辑。**

## 包结构

```
org.dragon.asset/
├── dto/               # 数据传输对象
│   ├── CollaboratorDTO.java
│   ├── AssetMemberDTO.java
│   ├── AssetAssociationDTO.java
│   └── CreateAssociationRequest.java
├── enums/             # 枚举类
│   ├── PublishStatus.java      # 资产发布状态
│   └── AssociationType.java    # 资产关联类型
├── service/           # 核心业务服务
│   ├── AssetMemberService.java        # Owner/Collaborator 管理
│   ├── AssetPublishStatusService.java # 发布状态状态机
│   ├── AssetAssociationService.java   # 资产关联关系
│   └── CollaboratorService.java       # 协作邀请高级封装
└── store/             # 存储层接口
    ├── AssetMemberStore.java
    ├── AssetPublishStatusStore.java
    └── AssetAssociationStore.java
```

## 核心组件

### 1. AssetMemberService（Owner/Collaborator 管理）

**文件路径：** `org.dragon.asset.service.AssetMemberService`

负责资产的 Owner 和 Collaborator（成员）管理，是权限系统的基础。

| 关键方法 | 说明 |
|---------|------|
| `addOwnerDirectly()` | 资产创建时同步添加 Owner |
| `transferOwner()` | 转移所有权（原 Owner 降为 Admin） |
| `isOwner()` / `isCollaborator()` | 权限校验 |
| `getMyAssets()` | 获取用户所有有权限的资产 |
| `getCollaborators()` | 获取资产的协作者列表 |

**Owner 业务规则：**
- Owner 不可删除，只能转移
- 转移后原 Owner 自动降为 ADMIN 角色
- 资产创建时必须同步调用 `addOwnerDirectly()`

### 2. AssetPublishStatusService（发布状态状态机）

**文件路径：** `org.dragon.asset.service.AssetPublishStatusService`

管理资产生命周期状态流转。

**状态流转：**
```
DRAFT ──→ PENDING ──→ PUBLISHED ──→ ARCHIVED
  ↑         ↓
  └─────────┘ (reject/withdraw)

Only PUBLISHED 可 UNARCHIVE 回 PUBLISHED
Only ARCHIVED 可 REVERT 回 DRAFT
```

| 关键方法 | 说明 |
|---------|------|
| `initializeStatus()` | 初始化发布状态 |
| `publish()` | 发布资产（审批通过后调用） |
| `archive()` / `unarchive()` | 归档/取消归档 |
| `revertToDraft()` | 撤回至草稿 |
| `isPublished()` | 是否已发布 |
| `getPublishedAssetIds()` | 查询已发布资产列表 |

**默认发布状态（按资源类型）：**

| 资源类型 | 默认状态 | 说明 |
|---------|---------|------|
| CHARACTER, SKILL, OBSERVER, TRAIT, WORKSPACE, COMMONSENSE | DRAFT | 需审批 |
| MODEL, TEMPLATE | PUBLISHED | 直接发布 |

### 3. AssetAssociationService（资产关联关系）

**文件路径：** `org.dragon.asset.service.AssetAssociationService`

维护资产间的关联关系图谱，是组合和依赖追踪的基础。

**关联类型（AssociationType）：**

| 关联类型 | 说明 |
|---------|------|
| `CHARACTER_WORKSPACE` | Character 添加到 Workspace |
| `MEMORY_CHARACTER` / `MEMORY_WORKSPACE` | Memory 挂载到 Character/Workspace |
| `TOOL_SKILL` | Tool 被 Skill 引用 |
| `OBSERVER_WORKSPACE` | Observer 挂载到 Workspace |
| `SKILL_CHARACTER` / `TRAIT_CHARACTER` | Skill/Trait 关联到 Character |

| 关键方法 | 说明 |
|---------|------|
| `createAssociation()` | 创建关联（幂等） |
| `removeAssociation()` | 移除关联 |
| `getCharactersInWorkspace()` | 查询 Workspace 下的所有 Character |
| `getMemoriesForCharacter()` | 查询 Character 关联的 Memory |
| `getTraitsForCharacter()` | 查询 Character 关联的 Trait |
| `getObserversForWorkspace()` | 查询 Workspace 关联的 Observer |
| `getToolsForSkill()` | 查询 Skill 引用的 Tool |

**Association 创建是幂等的**：重复创建相同关联不会报错，仅记录日志后跳过。

## 与其他模块的关系

| 模块 | 依赖方式 |
|------|---------|
| `org.dragon.approval` | 发布审批通过后调用 `publish()`；协作审批通过/拒绝调用 `acceptInvitationDirectly()` / `rejectInvitationDirectly()` |
| `org.dragon.workspace` | 创建时调用 `addOwnerDirectly()` 和 `initializeStatus()` |
| `org.dragon.character` | 通过 `getTraitsForCharacter()` 构建详情；通过 `getCharactersInWorkspace()` 查询 |
| `org.dragon.permission` | 基于 member role 做权限校验 |
| `org.dragon.notification` | 协作邀请/移除时发送通知 |

## 存储层

存储层遵循 `Store` 三层架构规范，接口定义在 `org.dragon.asset.store`，实现分 Memory 和 MySQL 两种：

| 接口 | Memory 实现 | MySQL 实现 |
|------|-----------|-----------|
| `AssetMemberStore` | `MemoryAssetMemberStore` | `MySqlAssetMemberStore` |
| `AssetPublishStatusStore` | `MemoryAssetPublishStatusStore` | `MySqlAssetPublishStatusStore` |
| `AssetAssociationStore` | `MemoryAssetAssociationStore` | `MySqlAssetAssociationStore` |

## 数据库表

对应三张核心表：

- `asset_member` - 成员关系表（userId, resourceType, resourceId, role, accepted...）
- `asset_publish_status` - 发布状态表（resourceType, resourceId, status, version...）
- `asset_association` - 关联关系表（associationType, sourceType, sourceId, targetType, targetId）

## 资产标签（asset_tag）

资产标签用于为任意资产附加标签，标签**不是独立资产**，无 Owner/PublishStatus，通过 `AssetTagService` 管理。

**Entity 放置：必须放在 `org.dragon.datasource.entity.AssetTagEntity`**，因为 Ebean 仅扫描 `datasource.entity` 包。

**表结构：**

```sql
CREATE TABLE asset_tag (
    name VARCHAR(128) NOT NULL,
    color VARCHAR(16),
    description VARCHAR(512),
    resource_type VARCHAR(32) NOT NULL,
    resource_id VARCHAR(64) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (resource_type, resource_id, name),
    INDEX idx_resource (resource_type, resource_id),
    INDEX idx_name (name)
);
```

## 标签规范

**所有资产的标签统一通过 `asset_tag` 表管理，其他实体（Skill、WorkspaceMember 等）禁止维护 `tags` 字段。**

标签存储位置：
- **Entity**: `org.dragon.datasource.entity.AssetTagEntity`
- **Service**: `org.dragon.asset.tag.service.AssetTagService`
- **Controller**: `org.dragon.asset.tag.controller.AssetTagController`

标签使用方式：
```java
// 绑定标签
assetTagService.tagAsset(ResourceType.SKILL, skillId, "数据分析");

// 批量绑定
assetTagService.tagAssets(ResourceType.SKILL, skillId, List.of("数据分析", "API"));

// 获取资产的所有标签
List<AssetTagDTO> tags = assetTagService.getTagsForAsset(ResourceType.SKILL, skillId);

// 按资产类型获取所有去重标签名
Set<String> tagNames = assetTagService.getTagNamesByResourceType(ResourceType.SKILL);
```

**禁止在其他 Entity 中添加 `tags` 字段**，如 SkillEntity、WorkspaceMemberEntity 等。标签信息全部通过 `asset_tag` 表关联。

## 禁止事项

```java
// 错误 —— 在业务服务中重复 owner 逻辑
if (!isOwner(userId, assetId)) {
    throw new UnauthorizedException();
}

// 正确 —— 使用 AssetMemberService
if (!assetMemberService.isOwner(userId, assetId)) {
    throw new UnauthorizedException();
}

// 错误 —— 在业务服务中硬编码发布状态判断
if (status != PublishStatus.PUBLISHED) {
    throw new NotPublishedException();
}

// 正确 —— 使用 AssetPublishStatusService
if (!publishStatusService.isPublished(resourceType, resourceId)) {
    throw new NotPublishedException();
}
```
