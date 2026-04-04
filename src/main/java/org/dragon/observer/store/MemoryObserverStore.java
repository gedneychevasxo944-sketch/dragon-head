package org.dragon.observer.store;

import org.dragon.observer.Observer;
import org.dragon.store.StoreType;
import org.dragon.store.StoreTypeAnn;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * MemoryObserverStore Observer内存存储实现
 */
@Component
@StoreTypeAnn(StoreType.MEMORY)
public class MemoryObserverStore implements ObserverStore {

    private final Map<String, Observer> store = new ConcurrentHashMap<>();

    @Override
    public void save(Observer observer) {
        if (observer == null || observer.getId() == null) {
            throw new IllegalArgumentException("Observer or Observer id cannot be null");
        }
        store.put(observer.getId(), observer);
    }

    @Override
    public void update(Observer observer) {
        if (observer == null || observer.getId() == null) {
            throw new IllegalArgumentException("Observer or Observer id cannot be null");
        }
        if (!store.containsKey(observer.getId())) {
            throw new IllegalArgumentException("Observer not found: " + observer.getId());
        }
        store.put(observer.getId(), observer);
    }

    @Override
    public void delete(String id) {
        store.remove(id);
    }

    @Override
    public Optional<Observer> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Observer> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public List<Observer> findByWorkspaceId(String workspaceId) {
        return store.values().stream()
                .filter(o -> workspaceId != null && workspaceId.equals(o.getWorkspaceId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Observer> findActive() {
        return store.values().stream()
                .filter(Observer::isActive)
                .collect(Collectors.toList());
    }

    @Override
    public List<Observer> findByStatus(Observer.Status status) {
        return store.values().stream()
                .filter(o -> o.getStatus() == status)
                .collect(Collectors.toList());
    }

    @Override
    public boolean exists(String id) {
        return store.containsKey(id);
    }

    @Override
    public int count() {
        return store.size();
    }
}