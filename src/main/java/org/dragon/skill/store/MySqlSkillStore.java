package org.dragon.skill.store;

import io.ebean.Database;
import org.dragon.datasource.entity.SkillEntity;
import org.dragon.skill.domain.SkillDO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * MySqlSkillStore — 基于 Ebean ORM 的 MySQL 存储实现。
 */
@Component
public class MySqlSkillStore implements SkillStore {

    private final Database mysqlDb;

    public MySqlSkillStore(@Qualifier("mysqlEbeanDatabase") Database mysqlDb) {
        this.mysqlDb = mysqlDb;
    }

    @Override
    public void save(SkillDO skill) {
        SkillEntity entity = SkillEntity.fromDomain(skill);
        mysqlDb.save(entity);
        skill.setId(entity.getId());
    }

    @Override
    public void update(SkillDO skill) {
        SkillEntity entity = SkillEntity.fromDomain(skill);
        mysqlDb.update(entity);
    }

    @Override
    public java.util.Optional<SkillDO> findById(String id) {
        SkillEntity entity = mysqlDb.find(SkillEntity.class, id);
        return entity != null ? java.util.Optional.of(entity.toDomain()) : java.util.Optional.empty();
    }

    @Override
    public java.util.Optional<SkillDO> findBySkillId(String skillId) {
        SkillEntity entity = mysqlDb.find(SkillEntity.class)
                .where()
                .eq("id", skillId)
                .findOne();
        return entity != null ? java.util.Optional.of(entity.toDomain()) : java.util.Optional.empty();
    }

    @Override
    public java.util.Optional<SkillDO> findLatestActiveBySkillId(String skillId) {
        SkillEntity entity = mysqlDb.find(SkillEntity.class)
                .where()
                .eq("skillId", skillId)
                .eq("status", "active")
                .isNotNull("publishedVersionId")
                .findOne();
        return entity != null ? java.util.Optional.of(entity.toDomain()) : java.util.Optional.empty();
    }

    @Override
    public List<SkillDO> findAllBuiltin() {
        return mysqlDb.find(SkillEntity.class)
                .where()
                .eq("builtin", true)
                .eq("status", "active")
                .isNotNull("publishedVersionId")
                .orderBy("name asc")
                .findList()
                .stream()
                .map(SkillEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsBySkillId(String skillId) {
        return mysqlDb.find(SkillEntity.class)
                .where()
                .eq("skillId", skillId)
                .exists();
    }

    // ── 管理页查询 ────────────────────────────────────────────────────

    @Override
    public List<SkillDO> search(String keyword, String status, String category,
                                String visibility, Long creatorId,
                                String sortBy, String sortOrder,
                                int offset, int limit) {
        StringBuilder where = buildSearchWhere(keyword, status, category, visibility, creatorId);
        String safeSort = resolveSortColumn(sortBy);
        String safeOrder = "asc".equalsIgnoreCase(sortOrder) ? "ASC" : "DESC";

        String sql = "SELECT s.* FROM skill s WHERE s.deleted_at IS NULL"
                + where
                + " ORDER BY s." + safeSort + " " + safeOrder
                + " LIMIT :limit OFFSET :offset";

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

        String sql = "SELECT COUNT(*) AS cnt FROM skill s WHERE s.deleted_at IS NULL" + where;

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

    private StringBuilder buildSearchWhere(String keyword, String status, String category,
                                          String visibility, Long creatorId) {
        StringBuilder sb = new StringBuilder();
        boolean hasWhere = false;

        if (keyword != null && !keyword.isBlank()) {
            sb.append(hasWhere ? " AND " : "");
            sb.append("(s.name LIKE :keyword OR s.introduction LIKE :keyword)");
            hasWhere = true;
        }
        if (status != null && !status.isBlank()) {
            sb.append(hasWhere ? " AND " : "");
            sb.append("s.status = :status");
            hasWhere = true;
        }
        if (category != null && !category.isBlank()) {
            sb.append(hasWhere ? " AND " : "");
            sb.append("s.category = :category");
            hasWhere = true;
        }
        if (visibility != null && !visibility.isBlank()) {
            sb.append(hasWhere ? " AND " : "");
            sb.append("s.visibility = :visibility");
            hasWhere = true;
        }
        if (creatorId != null) {
            sb.append(hasWhere ? " AND " : "");
            sb.append("s.creator_id = :creatorId");
        }
        return sb;
    }

    private String resolveSortColumn(String sortBy) {
        if (sortBy == null) return "created_at";
        return switch (sortBy.toLowerCase()) {
            case "name"        -> "name";
            case "publishedat" -> "published_version_id";
            case "createdat"   -> "created_at";
            default            -> "created_at";
        };
    }

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
