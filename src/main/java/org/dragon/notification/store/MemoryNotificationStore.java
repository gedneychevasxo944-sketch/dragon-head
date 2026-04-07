package org.dragon.notification.store;

import org.dragon.notification.dto.NotificationEntity;
import org.dragon.notification.dto.NotificationType;
import org.dragon.store.StoreType;
import org.dragon.store.StoreTypeAnn;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * MemoryNotificationStore 通知内存存储实现
 */
@Component
@StoreTypeAnn(StoreType.MEMORY)
public class MemoryNotificationStore implements NotificationStore {

    private final Map<String, NotificationEntity> store = new ConcurrentHashMap<>();

    @Override
    public void save(NotificationEntity notification) {
        if (notification.getId() == null) {
            throw new IllegalArgumentException("Notification id cannot be null");
        }
        store.put(notification.getId(), notification);
    }

    @Override
    public void update(NotificationEntity notification) {
        if (notification.getId() == null || !store.containsKey(notification.getId())) {
            throw new IllegalArgumentException("Notification not found: " + notification.getId());
        }
        store.put(notification.getId(), notification);
    }

    @Override
    public void delete(String id) {
        store.remove(id);
    }

    @Override
    public Optional<NotificationEntity> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<NotificationEntity> findByUserId(Long userId) {
        return store.values().stream()
                .filter(n -> userId.equals(n.getUserId()))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .collect(Collectors.toList());
    }

    @Override
    public List<NotificationEntity> findUnreadByUserId(Long userId) {
        return store.values().stream()
                .filter(n -> userId.equals(n.getUserId()) && !Boolean.TRUE.equals(n.getIsRead()))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .collect(Collectors.toList());
    }

    @Override
    public List<NotificationEntity> findByUserIdAndType(Long userId, NotificationType type) {
        return store.values().stream()
                .filter(n -> userId.equals(n.getUserId()) && type == n.getType())
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .collect(Collectors.toList());
    }

    @Override
    public long countUnreadByUserId(Long userId) {
        return store.values().stream()
                .filter(n -> userId.equals(n.getUserId()) && !Boolean.TRUE.equals(n.getIsRead()))
                .count();
    }

    @Override
    public void markAllAsReadByUserId(Long userId) {
        store.values().stream()
                .filter(n -> userId.equals(n.getUserId()) && !Boolean.TRUE.equals(n.getIsRead()))
                .forEach(n -> {
                    n.setIsRead(true);
                });
    }

    @Override
    public void deleteByUserId(Long userId) {
        store.values().removeIf(n -> userId.equals(n.getUserId()));
    }
}
