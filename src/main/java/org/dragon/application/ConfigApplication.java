package org.dragon.application;

import lombok.extern.slf4j.Slf4j;
import org.dragon.api.dto.PageResponse;
import org.dragon.config.store.ConfigKey;
import org.dragon.config.store.ConfigStore;
import org.dragon.store.StoreFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ConfigApplication 配置中心应用服务
 *
 * <p>对应前端 /config 页面，聚合配置项管理、变更记录、影响分析等业务逻辑。
 * 当前配置系统底层为 ConfigStore（KV 存储），高级特性（草稿、发布、变更记录）为占位实现。
 *
 * @author zhz
 * @version 1.0
 */
@Slf4j
@Service
public class ConfigApplication {

    private final StoreFactory storeFactory;

    public ConfigApplication(StoreFactory storeFactory) {
        this.storeFactory = storeFactory;
    }

    private ConfigStore configStore() {
        return storeFactory.get(ConfigStore.class);
    }

    // ==================== 配置项管理 ====================

    /**
     * 分页获取配置项列表，支持按域、作用域、关键词筛选。
     *
     * @param domain      配置域筛选
     * @param scopeType   作用域类型筛选
     * @param scopeId     特定作用域 ID
     * @param search      关键词搜索
     * @param isDraft     是否只看草稿
     * @param hasOverride 是否有覆盖
     * @param page        页码
     * @param pageSize    每页数量
     * @return 分页配置项
     */
    public PageResponse<Map<String, Object>> listConfigItems(String domain, String scopeType, String scopeId,
                                                             String search, Boolean isDraft, Boolean hasOverride,
                                                             int page, int pageSize) {
        log.info("[ConfigApplication] listConfigItems domain={} scopeType={} scopeId={}", domain, scopeType, scopeId);
        // 从 ConfigStore 中读取所有配置
        Map<String, Object> allConfig;
        if (scopeId != null && !scopeId.isBlank() && scopeType != null && !scopeType.isBlank()) {
            allConfig = configStore().getAll(ConfigKey.of("default", scopeType, scopeId, null));
        } else if (scopeType != null && !scopeType.isBlank()) {
            allConfig = configStore().getAll(ConfigKey.of(scopeType, null));
        } else {
            allConfig = configStore().getAll(ConfigKey.of((String) null));
        }

        List<Map<String, Object>> items = new ArrayList<>();
        for (Map.Entry<String, Object> entry : allConfig.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (search != null && !search.isBlank() && !key.toLowerCase().contains(search.toLowerCase())) {
                continue;
            }
            Map<String, Object> item = buildConfigItemMap(key, value, domain, scopeType, scopeId);
            items.add(item);
        }

        long total = items.size();
        int fromIndex = Math.max(0, (page - 1) * pageSize);
        int toIndex = Math.min(fromIndex + pageSize, items.size());
        List<Map<String, Object>> pageData = fromIndex >= items.size() ? List.of() : items.subList(fromIndex, toIndex);
        return PageResponse.of(pageData, total, page, pageSize);
    }

    /**
     * 更新配置项（支持草稿模式）。
     *
     * @param configId    配置项标识（格式：scopeType:entityId:key）
     * @param value       新的配置值
     * @param saveAsDraft 是否保存为草稿
     * @return 更新后的配置项
     */
    public Map<String, Object> updateConfigItem(String configId, Object value, Boolean saveAsDraft) {
        log.info("[ConfigApplication] updateConfigItem configId={} saveAsDraft={}", configId, saveAsDraft);
        // 解析 configId（格式: namespace:key 或 entityType:entityId:key）
        String[] parts = configId.split(":", 3);
        ConfigKey key;
        if (parts.length == 3) {
            key = ConfigKey.of("default", parts[0], parts[1], parts[2]);
        } else if (parts.length == 2) {
            key = ConfigKey.of(parts[0], parts[1]);
        } else {
            key = ConfigKey.of(configId);
        }

        if (Boolean.TRUE.equals(saveAsDraft)) {
            // 草稿模式：保存到 draft 前缀键
            ConfigKey draftKey = ConfigKey.of("draft", configId);
            configStore().set(draftKey, value);
            log.info("[ConfigApplication] Saved as draft: {}", configId);
        } else {
            configStore().set(key, value);
            log.info("[ConfigApplication] Updated config: {}", configId);
        }

        return buildConfigItemMap(configId, value, null, null, null);
    }

    /**
     * 发布配置草稿。
     *
     * @param configId 配置项 ID
     * @return 发布后的配置项
     */
    public Map<String, Object> publishConfigItem(String configId) {
        log.info("[ConfigApplication] publishConfigItem configId={}", configId);
        ConfigKey draftKey = ConfigKey.of("draft", configId);
        Object draftValue = configStore().get(draftKey).orElse(null);
        if (draftValue != null) {
            String[] parts = configId.split(":", 3);
            ConfigKey realKey;
            if (parts.length == 3) {
                realKey = ConfigKey.of("default", parts[0], parts[1], parts[2]);
            } else if (parts.length == 2) {
                realKey = ConfigKey.of(parts[0], parts[1]);
            } else {
                realKey = ConfigKey.of(configId);
            }
            configStore().set(realKey, draftValue);
            configStore().delete(draftKey);
            return buildConfigItemMap(configId, draftValue, null, null, null);
        }
        return buildConfigItemMap(configId, null, null, null, null);
    }

    /**
     * 回滚配置（占位）。
     *
     * @param configId       配置项 ID
     * @param changeRecordId 回滚到的变更记录 ID
     * @return 回滚后的配置项
     */
    public Map<String, Object> rollbackConfigItem(String configId, String changeRecordId) {
        log.info("[ConfigApplication] rollbackConfigItem configId={} changeRecordId={}", configId, changeRecordId);
        // 占位：需要变更历史存储支持
        return buildConfigItemMap(configId, null, null, null, null);
    }

    /**
     * 获取配置项生效链（占位）。
     *
     * @param configId 配置项 ID
     * @return 生效链节点列表
     */
    public List<Map<String, Object>> getEffectChain(String configId) {
        log.info("[ConfigApplication] getEffectChain configId={}", configId);
        // 占位：展示全局 -> 命名空间 -> 实体的生效链
        List<Map<String, Object>> chain = new ArrayList<>();
        Map<String, Object> globalNode = new HashMap<>();
        globalNode.put("level", "global");
        globalNode.put("value", null);
        globalNode.put("effective", true);
        chain.add(globalNode);
        return chain;
    }

    // ==================== 变更记录 ====================

    /**
     * 获取配置变更记录列表（占位）。
     *
     * @param domain       配置域筛选
     * @param configItemId 配置项 ID
     * @param page         页码
     * @param pageSize     每页数量
     * @return 分页变更记录
     */
    public PageResponse<Map<String, Object>> listChangeRecords(String domain, String configItemId,
                                                               int page, int pageSize) {
        log.info("[ConfigApplication] listChangeRecords domain={} configItemId={}", domain, configItemId);
        return PageResponse.of(List.of(), 0, page, pageSize);
    }

    // ==================== 影响分析 ====================

    /**
     * 获取配置项变更影响分析（占位）。
     *
     * @param configId 配置项 ID
     * @return 影响分析
     */
    public Map<String, Object> getImpactAnalysis(String configId) {
        log.info("[ConfigApplication] getImpactAnalysis configId={}", configId);
        Map<String, Object> impact = new HashMap<>();
        impact.put("configId", configId);
        impact.put("affectedScopes", List.of());
        impact.put("affectedCount", 0);
        impact.put("riskLevel", "low");
        return impact;
    }

    // ==================== 内部工具 ====================

    private Map<String, Object> buildConfigItemMap(String key, Object value, String domain,
                                                   String scopeType, String scopeId) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", key);
        item.put("key", key);
        item.put("name", key);
        item.put("domain", domain != null ? domain : "GLOBAL");
        item.put("scopeType", scopeType != null ? scopeType : "GLOBAL");
        item.put("scopeId", scopeId);
        item.put("currentValue", value);
        item.put("effectiveValue", value);
        item.put("dataType", value instanceof Boolean ? "BOOLEAN"
                : value instanceof Number ? "NUMBER" : "STRING");
        item.put("isDraft", false);
        item.put("lastModified", LocalDateTime.now().toString());
        item.put("modifiedBy", "system");
        return item;
    }
}
