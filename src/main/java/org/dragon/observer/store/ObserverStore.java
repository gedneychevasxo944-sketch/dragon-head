package org.dragon.observer.store;

import org.dragon.observer.Observer;
import org.dragon.store.Store;

import java.util.List;
import java.util.Optional;

/**
 * ObserverStore Observer存储接口
 */
public interface ObserverStore extends Store {

    /**
     * 保存Observer
     */
    void save(Observer observer);

    /**
     * 更新Observer
     */
    void update(Observer observer);

    /**
     * 删除Observer
     */
    void delete(String id);

    /**
     * 根据ID获取Observer
     */
    Optional<Observer> findById(String id);

    /**
     * 获取所有Observer
     */
    List<Observer> findAll();

    /**
     * 根据Workspace ID获取Observer列表
     */
    List<Observer> findByWorkspaceId(String workspaceId);

    /**
     * 获取所有活跃的Observer
     */
    List<Observer> findActive();

    /**
     * 根据状态获取Observer列表
     */
    List<Observer> findByStatus(Observer.Status status);

    /**
     * 检查Observer是否存在
     */
    boolean exists(String id);

    /**
     * 获取Observer数量
     */
    int count();
}