package org.dragon.skill.store;

import io.ebean.Database;
import io.ebean.SqlQuery;
import io.ebean.SqlRow;
import org.dragon.datasource.entity.SkillUsageEntity;
import org.dragon.skill.domain.SkillUsageDO;
import org.dragon.skill.service.SkillUsageService.SkillRankItem;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * MySqlSkillUsageStore — 基于 Ebean ORM 的 MySQL 实现。
 *
 * <p>排序聚合查询使用原生 SQL（GROUP BY + ORDER BY），
 * 明细查询使用 Ebean Query API。
 */
@Component
public class MySqlSkillUsageStore implements SkillUsageStore {

    private final Database mysqlDb;

    public MySqlSkillUsageStore(@Qualifier("mysqlEbeanDatabase") Database mysqlDb) {
        this.mysqlDb = mysqlDb;
    }

    // ── 写操作 ────────────────────────────────────────────────────────

    @Override
    public void save(SkillUsageDO usage) {
        SkillUsageEntity entity = SkillUsageEntity.fromDomain(usage);
        mysqlDb.save(entity);
        // 回填自增主键
        usage.setId(entity.getId());
    }

    // ── 排序查询（聚合 SQL） ──────────────────────────────────────────

    @Override
    public List<SkillRankItem> rankByUsageCount(LocalDateTime since, int limit) {
        StringBuilder sql = new StringBuilder("""
                SELECT skill_id, skill_name,
                       COUNT(*)                                     AS total_count,
                       SUM(CASE WHEN success = 1 THEN 1 ELSE 0 END) AS success_count,
                       AVG(duration_ms)                             AS avg_duration_ms
                FROM skill_usage_log
                """);
        List<Object> params = new ArrayList<>();
        if (since != null) {
            sql.append("WHERE invoked_at >= ? ");
            params.add(since);
        }
        sql.append("GROUP BY skill_id, skill_name ORDER BY total_count DESC LIMIT ?");
        params.add(limit);

        SqlQuery query = mysqlDb.sqlQuery(sql.toString());
        bindParams(query, params);
        return query.findList().stream()
                .map(this::toRankItem)
                .collect(Collectors.toList());
    }

    @Override
    public List<SkillRankItem> rankByUsageCountForCharacter(String characterId,
                                                             LocalDateTime since, int limit) {
        StringBuilder sql = new StringBuilder("""
                SELECT skill_id, skill_name,
                       COUNT(*)                                     AS total_count,
                       SUM(CASE WHEN success = 1 THEN 1 ELSE 0 END) AS success_count,
                       AVG(duration_ms)                             AS avg_duration_ms
                FROM skill_usage_log
                WHERE character_id = ?
                """);
        List<Object> params = new ArrayList<>();
        params.add(characterId);
        if (since != null) {
            sql.append("AND invoked_at >= ? ");
            params.add(since);
        }
        sql.append("GROUP BY skill_id, skill_name ORDER BY total_count DESC LIMIT ?");
        params.add(limit);

        SqlQuery query = mysqlDb.sqlQuery(sql.toString());
        bindParams(query, params);
        return query.findList().stream()
                .map(this::toRankItem)
                .collect(Collectors.toList());
    }

    @Override
    public List<SkillRankItem> rankByUsageCountForWorkspace(String workspaceId,
                                                             LocalDateTime since, int limit) {
        StringBuilder sql = new StringBuilder("""
                SELECT skill_id, skill_name,
                       COUNT(*)                                     AS total_count,
                       SUM(CASE WHEN success = 1 THEN 1 ELSE 0 END) AS success_count,
                       AVG(duration_ms)                             AS avg_duration_ms
                FROM skill_usage_log
                WHERE workspace_id = ?
                """);
        List<Object> params = new ArrayList<>();
        params.add(workspaceId);
        if (since != null) {
            sql.append("AND invoked_at >= ? ");
            params.add(since);
        }
        sql.append("GROUP BY skill_id, skill_name ORDER BY total_count DESC LIMIT ?");
        params.add(limit);

        SqlQuery query = mysqlDb.sqlQuery(sql.toString());
        bindParams(query, params);
        return query.findList().stream()
                .map(this::toRankItem)
                .collect(Collectors.toList());
    }

    // ── 明细查询 ──────────────────────────────────────────────────────

    @Override
    public List<SkillUsageDO> findRecentBySkillId(String skillId, int limit) {
        return mysqlDb.find(SkillUsageEntity.class)
                .where()
                .eq("skillId", skillId)
                .orderBy("invokedAt desc")
                .setMaxRows(limit)
                .findList()
                .stream()
                .map(SkillUsageEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<SkillUsageDO> findRecentByCharacterId(String characterId, int limit) {
        return mysqlDb.find(SkillUsageEntity.class)
                .where()
                .eq("characterId", characterId)
                .orderBy("invokedAt desc")
                .setMaxRows(limit)
                .findList()
                .stream()
                .map(SkillUsageEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long countBySkillId(String skillId, LocalDateTime since) {
        var query = mysqlDb.find(SkillUsageEntity.class)
                .where()
                .eq("skillId", skillId);
        if (since != null) {
            query = query.ge("invokedAt", since);
        }
        return query.findCount();
    }

    // ── 私有工具方法 ─────────────────────────────────────────────────

    /** 将 SqlRow 映射为 SkillRankItem */
    private SkillRankItem toRankItem(SqlRow row) {
        Long avgDur = row.get("avg_duration_ms") != null
                ? ((Number) row.get("avg_duration_ms")).longValue() : null;
        return new SkillRankItem(
                row.getString("skill_id"),
                row.getString("skill_name"),
                row.getLong("total_count"),
                row.getLong("success_count"),
                avgDur
        );
    }

    /** 按位置顺序绑定参数（Ebean SqlQuery 不支持批量绑定，逐一设置） */
    private void bindParams(SqlQuery query, List<Object> params) {
        for (int i = 0; i < params.size(); i++) {
            query.setParameter(i + 1, params.get(i));
        }
    }
}

