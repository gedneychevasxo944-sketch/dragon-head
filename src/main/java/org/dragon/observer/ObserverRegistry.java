package org.dragon.observer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

/**
 * Observer 注册中心
 * 负责管理所有 Observer 的生命周期
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Component
public class ObserverRegistry {

    /**
     * Observer 注册表
     */
    private final Map<String, Observer> registry = new ConcurrentHashMap<>();

    /**
     * 默认 Observer ID
     */
    private volatile String defaultObserverId;

    /**
     * 所有 Observer 列表（用于事件监听）
     */
    private final List<Observer> allObservers = new CopyOnWriteArrayList<>();

    /**
     * 注册 Observer
     *
     * @param observer Observer 实例
     */
    public void register(Observer observer) {
        if (observer == null || observer.getId() == null) {
            throw new IllegalArgumentException("Observer or Observer id cannot be null");
        }

        // 设置创建/更新时间
        if (observer.getCreatedAt() == null) {
            observer.setCreatedAt(LocalDateTime.now());
        }
        observer.setUpdatedAt(LocalDateTime.now());

        // 如果是第一个 Observer，设为默认
        if (registry.isEmpty()) {
            defaultObserverId = observer.getId();
        }

        registry.put(observer.getId(), observer);
        allObservers.add(observer);
        log.info("[ObserverRegistry] Registered observer: {}, workspace: {}",
                observer.getId(), observer.getWorkspaceId());
    }

    /**
     * 注销 Observer
     *
     * @param observerId Observer ID
     */
    public void unregister(String observerId) {
        Observer removed = registry.remove(observerId);
        if (removed != null) {
            allObservers.remove(removed);
            log.info("[ObserverRegistry] Unregistered observer: {}", observerId);

            // 如果删除的是默认 Observer，选择下一个
            if (defaultObserverId != null && defaultObserverId.equals(observerId)) {
                defaultObserverId = registry.isEmpty() ? null : registry.keySet().iterator().next();
            }
        }
    }

    /**
     * 获取 Observer
     *
     * @param observerId Observer ID
     * @return Optional Observer
     */
    public Optional<Observer> get(String observerId) {
        return Optional.ofNullable(registry.get(observerId));
    }

    /**
     * 获取默认 Observer
     *
     * @return Optional Observer
     */
    public Optional<Observer> getDefaultObserver() {
        if (defaultObserverId == null) {
            return Optional.empty();
        }
        return get(defaultObserverId);
    }

    /**
     * 根据 Workspace ID 获取 Observer
     *
     * @param workspaceId Workspace ID
     * @return Optional Observer
     */
    public Optional<Observer> getByWorkspace(String workspaceId) {
        return registry.values().stream()
                .filter(obs -> workspaceId.equals(obs.getWorkspaceId()))
                .findFirst();
    }

    /**
     * 获取所有 Observer
     *
     * @return Observer 列表
     */
    public List<Observer> listAll() {
        return new CopyOnWriteArrayList<>(registry.values());
    }

    /**
     * 获取所有活跃的 Observer
     *
     * @return 活跃的 Observer 列表
     */
    public List<Observer> listActive() {
        return registry.values().stream()
                .filter(Observer::isActive)
                .collect(Collectors.toList());
    }

    /**
     * 更新 Observer
     *
     * @param observer Observer 实例
     */
    public void update(Observer observer) {
        if (observer == null || observer.getId() == null) {
            throw new IllegalArgumentException("Observer or Observer id cannot be null");
        }

        if (!registry.containsKey(observer.getId())) {
            throw new IllegalArgumentException("Observer not found: " + observer.getId());
        }

        observer.setUpdatedAt(LocalDateTime.now());
        registry.put(observer.getId(), observer);
        log.info("[ObserverRegistry] Updated observer: {}", observer.getId());
    }

    /**
     * 设置默认 Observer
     *
     * @param observerId Observer ID
     */
    public void setDefaultObserver(String observerId) {
        if (!registry.containsKey(observerId)) {
            throw new IllegalArgumentException("Observer not found: " + observerId);
        }
        defaultObserverId = observerId;
        log.info("[ObserverRegistry] Set default observer: {}", observerId);
    }

    /**
     * 激活 Observer
     *
     * @param observerId Observer ID
     */
    public void activate(String observerId) {
        get(observerId).ifPresent(observer -> {
            observer.activate();
            observer.setUpdatedAt(LocalDateTime.now());
            log.info("[ObserverRegistry] Activated observer: {}", observerId);
        });
    }

    /**
     * 暂停 Observer
     *
     * @param observerId Observer ID
     */
    public void pause(String observerId) {
        get(observerId).ifPresent(observer -> {
            observer.pause();
            observer.setUpdatedAt(LocalDateTime.now());
            log.info("[ObserverRegistry] Paused observer: {}", observerId);
        });
    }

    /**
     * 获取注册表大小
     *
     * @return 注册的 Observer 数量
     */
    public int size() {
        return registry.size();
    }

    /**
     * 检查 Observer 是否存在
     *
     * @param observerId Observer ID
     * @return 是否存在
     */
    public boolean exists(String observerId) {
        return registry.containsKey(observerId);
    }
}
