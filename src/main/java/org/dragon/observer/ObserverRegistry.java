package org.dragon.observer;

import org.dragon.observer.store.ObserverStore;
import org.dragon.store.StoreFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

    private final ObserverStore observerStore;

    /**
     * 默认 Observer ID
     */
    // TODO [ConfigStore Migration]: 迁移到 ConfigStore GLOBAL scope，使用 ConfigKey.of("observer.default-id")
    private volatile String defaultObserverId;

    public ObserverRegistry(StoreFactory storeFactory) {
        this.observerStore = storeFactory.get(ObserverStore.class);
    }

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
        if (observerStore.count() == 0) {
            defaultObserverId = observer.getId();
        }

        observerStore.save(observer);
        log.info("[ObserverRegistry] Registered observer: {}, workspace: {}",
                observer.getId(), observer.getWorkspaceId());
    }

    /**
     * 注销 Observer
     *
     * @param observerId Observer ID
     */
    public void unregister(String observerId) {
        if (!observerStore.exists(observerId)) {
            return;
        }

        observerStore.delete(observerId);
        log.info("[ObserverRegistry] Unregistered observer: {}", observerId);

        // 如果删除的是默认 Observer，选择下一个
        if (defaultObserverId != null && defaultObserverId.equals(observerId)) {
            List<Observer> all = observerStore.findAll();
            defaultObserverId = all.isEmpty() ? null : all.get(0).getId();
        }
    }

    /**
     * 获取 Observer
     *
     * @param observerId Observer ID
     * @return Optional Observer
     */
    public Optional<Observer> get(String observerId) {
        return observerStore.findById(observerId);
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
        return observerStore.findByWorkspaceId(workspaceId).stream().findFirst();
    }

    /**
     * 获取所有 Observer
     *
     * @return Observer 列表
     */
    public List<Observer> listAll() {
        return observerStore.findAll();
    }

    /**
     * 获取所有活跃的 Observer
     *
     * @return 活跃的 Observer 列表
     */
    public List<Observer> listActive() {
        return observerStore.findActive();
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

        if (!observerStore.exists(observer.getId())) {
            throw new IllegalArgumentException("Observer not found: " + observer.getId());
        }

        observer.setUpdatedAt(LocalDateTime.now());
        observerStore.update(observer);
        log.info("[ObserverRegistry] Updated observer: {}", observer.getId());
    }

    /**
     * 设置默认 Observer
     *
     * @param observerId Observer ID
     */
    public void setDefaultObserver(String observerId) {
        if (!observerStore.exists(observerId)) {
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
            observerStore.update(observer);
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
            observerStore.update(observer);
            log.info("[ObserverRegistry] Paused observer: {}", observerId);
        });
    }

    /**
     * 获取注册表大小
     *
     * @return 注册的 Observer 数量
     */
    public int size() {
        return observerStore.count();
    }

    /**
     * 检查 Observer 是否存在
     *
     * @param observerId Observer ID
     * @return 是否存在
     */
    public boolean exists(String observerId) {
        return observerStore.exists(observerId);
    }
}