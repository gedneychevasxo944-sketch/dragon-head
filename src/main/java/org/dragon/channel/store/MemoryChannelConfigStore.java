package org.dragon.channel.store;

import lombok.extern.slf4j.Slf4j;
import org.dragon.channel.entity.ChannelConfig;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * MemoryChannelConfigStore 渠道配置内存存储实现
 *
 * @author zhz
 * @version 1.0
 */
@Slf4j
@Component
public class MemoryChannelConfigStore implements ChannelConfigStore {

    private final Map<String, ChannelConfig> store = new ConcurrentHashMap<>();

    @Override
    public void save(ChannelConfig config) {
        if (config == null || config.getId() == null) {
            throw new IllegalArgumentException("ChannelConfig or id cannot be null");
        }
        store.put(config.getId(), config);
        log.debug("[ChannelConfigStore] Saved config: {} (type: {})", config.getId(), config.getChannelType());
    }

    @Override
    public void update(ChannelConfig config) {
        if (config == null || config.getId() == null) {
            throw new IllegalArgumentException("ChannelConfig or id cannot be null");
        }
        if (!store.containsKey(config.getId())) {
            throw new IllegalArgumentException("ChannelConfig not found: " + config.getId());
        }
        store.put(config.getId(), config);
        log.debug("[ChannelConfigStore] Updated config: {}", config.getId());
    }

    @Override
    public void delete(String id) {
        store.remove(id);
        log.debug("[ChannelConfigStore] Deleted config: {}", id);
    }

    @Override
    public Optional<ChannelConfig> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<ChannelConfig> findByChannelType(String channelType) {
        return store.values().stream()
                .filter(c -> channelType.equals(c.getChannelType()))
                .collect(Collectors.toList());
    }

    @Override
    public List<ChannelConfig> findAllEnabled() {
        return store.values().stream()
                .filter(ChannelConfig::isEnabled)
                .collect(Collectors.toList());
    }

    @Override
    public List<ChannelConfig> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public boolean exists(String id) {
        return store.containsKey(id);
    }

}
