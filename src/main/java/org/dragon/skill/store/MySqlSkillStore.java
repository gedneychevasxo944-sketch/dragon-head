package org.dragon.skill.store;

import org.dragon.skill.domain.SkillDO;
import org.dragon.datasource.entity.SkillEntity;
import io.ebean.Database;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MySqlSkillStore — 基于 Ebean ORM 的 MySQL 存储实现。
 *
 * <p>查询"最新版本"的核心逻辑：
 * <pre>
 *   SELECT * FROM skills
 *   WHERE skill_id = ?
 *   ORDER BY version DESC
 *   LIMIT 1
 * </pre>
 */
@Component
public class MySqlSkillStore implements SkillStore {

    private final Database mysqlDb;

    public MySqlSkillStore(@Qualifier("mysqlEbeanDatabase") Database mysqlDb) {
        this.mysqlDb = mysqlDb;
    }

    // ── 写操作 ────────────────────────────────────────────────────────

    @Override
    public void save(SkillDO skill) {
        SkillEntity entity = SkillEntity.fromDomain(skill);
        mysqlDb.save(entity);
        // 回填自增主键
        skill.setId(entity.getId());
    }

    @Override
    public void update(SkillDO skill) {
        SkillEntity entity = SkillEntity.fromDomain(skill);
        // Fetch existing entity first to preserve fields not being updated
        SkillEntity existing = mysqlDb.find(SkillEntity.class, skill.getId());
        if (existing != null) {
            mergeIfNotNull(entity, existing);
        }
        mysqlDb.update(entity);
    }

    private void mergeIfNotNull(SkillEntity target, SkillEntity source) {
        if (target.getSkillId() == null) {
            target.setSkillId(source.getSkillId());
        }
        if (target.getName() == null) {
            target.setName(source.getName());
        }
        if (target.getDisplayName() == null) {
            target.setDisplayName(source.getDisplayName());
        }
        if (target.getDescription() == null) {
            target.setDescription(source.getDescription());
        }
        if (target.getContent() == null) {
            target.setContent(source.getContent());
        }
        if (target.getAliases() == null) {
            target.setAliases(source.getAliases());
        }
        if (target.getWhenToUse() == null) {
            target.setWhenToUse(source.getWhenToUse());
        }
        if (target.getArgumentHint() == null) {
            target.setArgumentHint(source.getArgumentHint());
        }
        if (target.getAllowedTools() == null) {
            target.setAllowedTools(source.getAllowedTools());
        }
        if (target.getModel() == null) {
            target.setModel(source.getModel());
        }
        if (target.getDisableModelInvocation() == null) {
            target.setDisableModelInvocation(source.getDisableModelInvocation());
        }
        if (target.getUserInvocable() == null) {
            target.setUserInvocable(source.getUserInvocable());
        }
        if (target.getExecutionContext() == null) {
            target.setExecutionContext(source.getExecutionContext());
        }
        if (target.getEffort() == null) {
            target.setEffort(source.getEffort());
        }
        if (target.getCategory() == null) {
            target.setCategory(source.getCategory());
        }
        if (target.getVisibility() == null) {
            target.setVisibility(source.getVisibility());
        }
        if (target.getCreatorType() == null) {
            target.setCreatorType(source.getCreatorType());
        }
        if (target.getCreatorId() == null) {
            target.setCreatorId(source.getCreatorId());
        }
        if (target.getCreatorName() == null) {
            target.setCreatorName(source.getCreatorName());
        }
        if (target.getEditorId() == null) {
            target.setEditorId(source.getEditorId());
        }
        if (target.getEditorName() == null) {
            target.setEditorName(source.getEditorName());
        }
        if (target.getStatus() == null) {
            target.setStatus(source.getStatus());
        }
        if (target.getVersion() == null) {
            target.setVersion(source.getVersion());
        }
        if (target.getStorageType() == null) {
            target.setStorageType(source.getStorageType());
        }
        if (target.getStorageInfo() == null) {
            target.setStorageInfo(source.getStorageInfo());
        }
        if (target.getCreatedAt() == null) {
            target.setCreatedAt(source.getCreatedAt());
        }
        if (target.getPublishedAt() == null) {
            target.setPublishedAt(source.getPublishedAt());
        }
        if (target.getPersist() == null) {
            target.setPersist(source.getPersist());
        }
        if (target.getPersistMode() == null) {
            target.setPersistMode(source.getPersistMode());
        }
        if (target.getTags() == null) {
            target.setTags(source.getTags());
        }
    }

    // ── 读操作 ────────────────────────────────────────────────────────

    @Override
    public Optional<SkillDO> findById(Long id) {
        SkillEntity entity = mysqlDb.find(SkillEntity.class, id);
        return entity != null ? Optional.of(entity.toDomain()) : Optional.empty();
    }

    @Override
    public Optional<SkillDO> findLatestBySkillId(String skillId) {
        SkillEntity entity = mysqlDb.find(SkillEntity.class)
                .where()
                .eq("skillId", skillId)
                .orderBy("version desc")
                .setMaxRows(1)
                .findOne();
        return entity != null ? Optional.of(entity.toDomain()) : Optional.empty();
    }

    @Override
    public Optional<SkillDO> findBySkillIdAndVersion(String skillId, int version) {
        SkillEntity entity = mysqlDb.find(SkillEntity.class)
                .where()
                .eq("skillId", skillId)
                .eq("version", version)
                .findOne();
        return entity != null ? Optional.of(entity.toDomain()) : Optional.empty();
    }

    @Override
    public Optional<SkillDO> findLatestActiveBySkillId(String skillId) {
        SkillEntity entity = mysqlDb.find(SkillEntity.class)
                .where()
                .eq("skillId", skillId)
                .eq("status", "active")
                .orderBy("version desc")
                .setMaxRows(1)
                .findOne();
        return entity != null ? Optional.of(entity.toDomain()) : Optional.empty();
    }

    @Override
    public List<SkillDO> findAllVersionsBySkillId(String skillId) {
        return mysqlDb.find(SkillEntity.class)
                .where()
                .eq("skillId", skillId)
                .orderBy("version asc")
                .findList()
                .stream()
                .map(SkillEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<SkillDO> findLatestVersions(int offset, int limit) {
        // Ebean 不直接支持 DISTINCT ON / GROUP BY 最大值子查询，
        // 改用原生 SQL：每个 skill_id 取 version 最大的一条。
        String sql = """
                SELECT s.*
                FROM skills s
                INNER JOIN (
                    SELECT skill_id, MAX(version) AS max_ver
                    FROM skills
                    GROUP BY skill_id
                ) t ON s.skill_id = t.skill_id AND s.version = t.max_ver
                ORDER BY s.created_at DESC
                LIMIT :limit OFFSET :offset
                """;
        return mysqlDb.findNative(SkillEntity.class, sql)
                .setParameter("limit", limit)
                .setParameter("offset", offset)
                .findList()
                .stream()
                .map(SkillEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<SkillDO> findLatestVersionsByStatus(String status, int offset, int limit) {
        String sql = """
                SELECT s.*
                FROM skills s
                INNER JOIN (
                    SELECT skill_id, MAX(version) AS max_ver
                    FROM skills
                    GROUP BY skill_id
                ) t ON s.skill_id = t.skill_id AND s.version = t.max_ver
                WHERE s.status = :status
                ORDER BY s.created_at DESC
                LIMIT :limit OFFSET :offset
                """;
        return mysqlDb.findNative(SkillEntity.class, sql)
                .setParameter("status", status)
                .setParameter("limit", limit)
                .setParameter("offset", offset)
                .findList()
                .stream()
                .map(SkillEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<SkillDO> findLatestActiveBySkillIds(List<String> skillIds) {
        if (skillIds == null || skillIds.isEmpty()) {
            return List.of();
        }
        List<SkillEntity> entities = mysqlDb.find(SkillEntity.class)
                .where()
                .in("skillId", skillIds)
                .eq("status", "active")
                .orderBy("skillId asc, version desc")
                .findList();

        // 按 skillId 去重，保留 version 最大的那条（结果已按 version desc 排列，第一次出现即最大）
        Map<String, SkillEntity> latestBySkillId = new LinkedHashMap<>();
        for (SkillEntity entity : entities) {
            latestBySkillId.putIfAbsent(entity.getSkillId(), entity);
        }
        return latestBySkillId.values().stream()
                .map(SkillEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public int findMaxVersionBySkillId(String skillId) {
        SkillEntity entity = mysqlDb.find(SkillEntity.class)
                .where()
                .eq("skillId", skillId)
                .orderBy("version desc")
                .setMaxRows(1)
                .findOne();
        return entity != null ? entity.getVersion() : 0;
    }

    @Override
    public int countDistinctSkills() {
        // 使用原生 SQL 统计 skill_id 去重数量
        String sql = "SELECT COUNT(DISTINCT skill_id) FROM skills";
        return mysqlDb.sqlQuery(sql)
                .findOne()
                .getInteger("COUNT(DISTINCT skill_id)");
    }

    @Override
    public boolean existsBySkillId(String skillId) {
        return mysqlDb.find(SkillEntity.class)
                .where()
                .eq("skillId", skillId)
                .exists();
    }

    @Override
    public List<SkillDO> findAllBuiltin() {
        // 查询 category='builtin' 的所有 skillId 各自最新 active 版本
        String sql = """
                SELECT s.*
                FROM skills s
                INNER JOIN (
                    SELECT skill_id, MAX(version) AS max_ver
                    FROM skills
                    WHERE category = 'builtin' AND status = 'active'
                    GROUP BY skill_id
                ) t ON s.skill_id = t.skill_id AND s.version = t.max_ver
                ORDER BY s.name ASC
                """;
        return mysqlDb.findNative(SkillEntity.class, sql)
                .findList()
                .stream()
                .map(SkillEntity::toDomain)
                .collect(Collectors.toList());
    }

    // ── 管理页查询 ────────────────────────────────────────────────────

    @Override
    public List<SkillDO> search(String keyword, String status, String category,
                                String visibility, Long creatorId,
                                String sortBy, String sortOrder,
                                int offset, int limit) {
        // 构建 WHERE 条件片段
        StringBuilder where = buildSearchWhere(keyword, status, category, visibility, creatorId);

        // 校验并白名单化排序参数，防止 SQL 注入
        String safeSort = resolveSortColumn(sortBy);
        String safeOrder = "asc".equalsIgnoreCase(sortOrder) ? "ASC" : "DESC";

        // 构建完整 SQL
        String sql = "SELECT s.* FROM skills s " +
                "INNER JOIN (SELECT skill_id, MAX(version) AS max_ver FROM skills WHERE status != 'deleted' GROUP BY skill_id) t " +
                "ON s.skill_id = t.skill_id AND s.version = t.max_ver " +
                where +
                " ORDER BY s." + safeSort + " " + safeOrder + " LIMIT :limit OFFSET :offset";

        io.ebean.Query<SkillEntity> query = mysqlDb.findNative(SkillEntity.class, sql)
                .setParameter("limit", limit)
                .setParameter("offset", offset);
        // 防御性绑定：确保所有 WHERE 条件中的参数都被设置
        bindSearchParams(query,
                (keyword == null || keyword.isBlank()) ? null : keyword,
                (status == null || status.isBlank()) ? null : status,
                (category == null || category.isBlank()) ? null : category,
                (visibility == null || visibility.isBlank()) ? null : visibility,
                creatorId);

        return query.findList()
                .stream()
                .map(SkillEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public int countSearch(String keyword, String status, String category,
                           String visibility, Long creatorId) {
        StringBuilder where = buildSearchWhere(keyword, status, category, visibility, creatorId);

        String sql = """
                SELECT COUNT(*) AS cnt
                FROM skills s
                INNER JOIN (
                    SELECT skill_id, MAX(version) AS max_ver
                    FROM skills
                    WHERE status != 'deleted'
                    GROUP BY skill_id
                ) t ON s.skill_id = t.skill_id AND s.version = t.max_ver
                """ + where;

        io.ebean.SqlQuery query = mysqlDb.sqlQuery(sql);
        bindSearchParams(query,
                (keyword == null || keyword.isBlank()) ? null : keyword,
                (status == null || status.isBlank()) ? null : status,
                (category == null || category.isBlank()) ? null : category,
                (visibility == null || visibility.isBlank()) ? null : visibility,
                creatorId);

        io.ebean.SqlRow row = query.findOne();
        return row != null ? row.getInteger("cnt") : 0;
    }

    // ── 私有辅助 ──────────────────────────────────────────────────────

    /**
     * 构建 search / countSearch 共用的 WHERE 片段。
     * deleted 状态已在子查询中排除，此处处理业务过滤条件。
     */
    private StringBuilder buildSearchWhere(String keyword, String status, String category,
                                           String visibility, Long creatorId) {
        StringBuilder sb = new StringBuilder();
        boolean hasWhere = false;

        if (keyword != null && !keyword.isBlank()) {
            sb.append(hasWhere ? " AND " : " WHERE ");
            sb.append("(s.name LIKE :keyword OR s.display_name LIKE :keyword OR s.description LIKE :keyword)");
            hasWhere = true;
        }
        if (status != null && !status.isBlank()) {
            sb.append(hasWhere ? " AND " : " WHERE ");
            sb.append("s.status = :status");
            hasWhere = true;
        }
        if (category != null && !category.isBlank()) {
            sb.append(hasWhere ? " AND " : " WHERE ");
            sb.append("s.category = :category");
            hasWhere = true;
        }
        if (visibility != null && !visibility.isBlank()) {
            sb.append(hasWhere ? " AND " : " WHERE ");
            sb.append("s.visibility = :visibility");
            hasWhere = true;
        }
        if (creatorId != null) {
            sb.append(hasWhere ? " AND " : " WHERE ");
            sb.append("s.creator_id = :creatorId");
        }
        return sb;
    }

    /** 将用户传入的 sortBy 映射为安全的列名（白名单保护）。 */
    private String resolveSortColumn(String sortBy) {
        if (sortBy == null) return "created_at";
        return switch (sortBy.toLowerCase()) {
            case "name"        -> "name";
            case "publishedat" -> "published_at";
            case "createdat"   -> "created_at";
            default            -> "created_at";
        };
    }

    /** 向 findNative 查询绑定动态参数（keyword 等）。 */
    private void bindSearchParams(io.ebean.Query<?> query,
                                  String keyword, String status, String category,
                                  String visibility, Long creatorId) {
        if (keyword != null && !keyword.isBlank()) {
            query.setParameter("keyword", "%" + keyword + "%");
        }
        if (status != null && !status.isBlank()) {
            query.setParameter("status", status);
        }
        if (category != null && !category.isBlank()) {
            query.setParameter("category", category);
        }
        if (visibility != null && !visibility.isBlank()) {
            query.setParameter("visibility", visibility);
        }
        if (creatorId != null) {
            query.setParameter("creatorId", creatorId);
        }
    }

    /** 向 SqlQuery 绑定动态参数（countSearch 使用）。 */
    private void bindSearchParams(io.ebean.SqlQuery query,
                                  String keyword, String status, String category,
                                  String visibility, Long creatorId) {
        if (keyword != null && !keyword.isBlank()) {
            query.setParameter("keyword", "%" + keyword + "%");
        }
        if (status != null && !status.isBlank()) {
            query.setParameter("status", status);
        }
        if (category != null && !category.isBlank()) {
            query.setParameter("category", category);
        }
        if (visibility != null && !visibility.isBlank()) {
            query.setParameter("visibility", visibility);
        }
        if (creatorId != null) {
            query.setParameter("creatorId", creatorId);
        }
    }
}

