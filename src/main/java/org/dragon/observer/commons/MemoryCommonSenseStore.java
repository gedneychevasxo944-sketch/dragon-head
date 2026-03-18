package org.dragon.observer.commons;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

/**
 * CommonSense 常识内存存储实现
 * 使用 ConcurrentHashMap 保证线程安全
 *
 * @author wyj
 * @version 1.0
 */
@Component
public class MemoryCommonSenseStore implements CommonSenseStore {

    private final Map<String, CommonSense> store = new ConcurrentHashMap<>();

    /**
     * 默认构造函数
     * 初始化一些基础常识
     */
    public MemoryCommonSenseStore() {
        initDefaultCommonSense();
    }

    /**
     * 初始化默认常识
     */
    private void initDefaultCommonSense() {
        // 隐私保护类
        save(CommonSense.builder()
                .id("cs-privacy-001")
                .name("用户隐私保护")
                .description("不得泄露用户隐私信息，包括但不限于姓名、联系方式、地址等")
                .category(CommonSense.Category.PRIVACY)
                .rule("{\"forbidden\": [\"泄露用户个人信息\", \"未经授权访问用户数据\"]}")
                .severity(CommonSense.Severity.CRITICAL)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .createdBy("system")
                .build());

        // 安全合规类
        save(CommonSense.builder()
                .id("cs-safety-001")
                .name("法律法规遵守")
                .description("所有任务必须遵循当地法律法规和道德规范")
                .category(CommonSense.Category.SAFETY)
                .rule("{\"constraint\": [\"合法合规\", \"道德规范\"]}")
                .severity(CommonSense.Severity.CRITICAL)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .createdBy("system")
                .build());

        // 性能约束类
        save(CommonSense.builder()
                .id("cs-performance-001")
                .name("响应时间限制")
                .description("系统响应时间不得超过 30 秒")
                .category(CommonSense.Category.PERFORMANCE)
                .rule("{\"maxResponseTime\": 30000}")
                .severity(CommonSense.Severity.HIGH)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .createdBy("system")
                .build());

        // 业务规则类
        save(CommonSense.builder()
                .id("cs-business-001")
                .name("数据持久化要求")
                .description("核心业务数据必须持久化存储")
                .category(CommonSense.Category.BUSINESS)
                .rule("{\"mustPersist\": [\"核心业务数据\", \"用户生成内容\"]}")
                .severity(CommonSense.Severity.HIGH)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .createdBy("system")
                .build());

        // 系统约束类
        save(CommonSense.builder()
                .id("cs-system-001")
                .name("资源使用限制")
                .description("单次任务 token 消耗不得超过 100000")
                .category(CommonSense.Category.SYSTEM)
                .rule("{\"maxTokens\": 100000}")
                .severity(CommonSense.Severity.MEDIUM)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .createdBy("system")
                .build());
    }

    @Override
    public CommonSense save(CommonSense commonSense) {
        if (commonSense.getId() == null) {
            throw new IllegalArgumentException("CommonSense id cannot be null");
        }
        commonSense.setUpdatedAt(LocalDateTime.now());
        if (commonSense.getCreatedAt() == null) {
            commonSense.setCreatedAt(LocalDateTime.now());
        }
        store.put(commonSense.getId(), commonSense);
        return commonSense;
    }

    @Override
    public Optional<CommonSense> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<CommonSense> findAll() {
        return new CopyOnWriteArrayList<>(store.values());
    }

    @Override
    public List<CommonSense> findByCategory(CommonSense.Category category) {
        return store.values().stream()
                .filter(cs -> cs.getCategory() == category)
                .collect(Collectors.toList());
    }

    @Override
    public List<CommonSense> findEnabled() {
        return store.values().stream()
                .filter(CommonSense::isEnabled)
                .collect(Collectors.toList());
    }

    @Override
    public List<CommonSense> findBySeverity(CommonSense.Severity severity) {
        return store.values().stream()
                .filter(cs -> cs.getSeverity() == severity)
                .collect(Collectors.toList());
    }

    @Override
    public boolean delete(String id) {
        return store.remove(id) != null;
    }

    @Override
    public boolean exists(String id) {
        return store.containsKey(id);
    }

    @Override
    public int count() {
        return store.size();
    }

    @Override
    public void clear() {
        store.clear();
    }
}
