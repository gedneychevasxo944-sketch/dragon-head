package org.dragon.channel.store;

import lombok.extern.slf4j.Slf4j;
import org.dragon.channel.entity.ChannelBinding;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * MemoryChannelBindingStore 渠道绑定内存存储实现
 *
 * @author zhz
 * @version 1.0
 */
@Slf4j
@Component
public class MemoryChannelBindingStore implements ChannelBindingStore {

    /**
     * 主索引: bindingId (channelName_chatId) -> ChannelBinding
     */
    private final Map<String, ChannelBinding> store = new ConcurrentHashMap<>();


    @Override
    public void save(ChannelBinding binding) {
        if (binding == null || binding.getId() == null) {
            throw new IllegalArgumentException("ChannelBinding or id cannot be null");
        }
        store.put(binding.getId(), binding);
        log.debug("[ChannelBindingStore] Saved binding: {} -> workspace: {}", binding.getId(), binding.getWorkspaceId());
    }

    @Override
    public void update(ChannelBinding binding) {
        if (binding == null || binding.getId() == null) {
            throw new IllegalArgumentException("ChannelBinding or id cannot be null");
        }
        if (!store.containsKey(binding.getId())) {
            throw new IllegalArgumentException("ChannelBinding not found: " + binding.getId());
        }
        store.put(binding.getId(), binding);
        log.debug("[ChannelBindingStore] Updated binding: {}", binding.getId());
    }

    @Override
    public void delete(String id) {
        ChannelBinding removed = store.remove(id);
        if (removed != null) {
            log.debug("[ChannelBindingStore] Deleted binding: {}", id);
        }
    }

    @Override
    public Optional<ChannelBinding> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Optional<ChannelBinding> findByChannelNameAndChatId(String channelName, String chatId) {
        String id = ChannelBinding.createId(channelName, chatId);
        return findById(id);
    }

    @Override
    public List<ChannelBinding> findByWorkspaceId(String workspaceId) {
        return store.values().stream()
                .filter(b -> workspaceId.equals(b.getWorkspaceId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<ChannelBinding> findByChannelName(String channelName) {
        return store.values().stream()
                .filter(b -> channelName.equals(b.getChannelName()))
                .collect(Collectors.toList());
    }

    @Override
    public List<ChannelBinding> findAll() {
        return new java.util.ArrayList<>(store.values());
    }

    @Override
    public boolean exists(String id) {
        return store.containsKey(id);
    }

}