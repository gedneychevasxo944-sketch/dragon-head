package org.dragon.channel.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dragon.channel.entity.ChannelBinding;
import org.dragon.channel.entity.ChannelConfig;
import org.dragon.channel.store.ChannelBindingStore;
import org.dragon.channel.store.ChannelConfigStore;
import org.dragon.workspace.WorkspaceRegistry;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * ChannelBindingService 渠道绑定管理服务
 * 提供后台管理所需的全部 CRUD 操作：
 * 1. 管理渠道配置（ChannelConfig）：创建/更新/删除 Bot 凭证
 * 2. 管理绑定关系（ChannelBinding）：将 IM 会话与 Workspace 绑定
 * 3. 路由查询：根据 (channelName, chatId) 解析目标 workspaceId
 *
 * @author zhz
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelBindingService {

    private final ChannelBindingStore channelBindingStore;
    private final ChannelConfigStore channelConfigStore;
    private final WorkspaceRegistry workspaceRegistry;

    // ==================== ChannelConfig 管理 ====================

    /**
     * 创建渠道配置（后台新增一个 Bot）
     *
     * @param config 渠道配置（id/channelType/credentials 必填）
     * @return 创建后的配置
     */
    public ChannelConfig createChannelConfig(ChannelConfig config) {
        if (config.getId() == null || config.getId().isEmpty()) {
            throw new IllegalArgumentException("ChannelConfig id is required");
        }
        if (channelConfigStore.exists(config.getId())) {
            throw new IllegalArgumentException("ChannelConfig already exists: " + config.getId());
        }

        LocalDateTime now = LocalDateTime.now();
        config.setCreatedAt(now);
        config.setUpdatedAt(now);
        channelConfigStore.save(config);

        log.info("[ChannelBindingService] Created channel config: {} (type: {})",
                config.getId(), config.getChannelType());
        return config;
    }

    /**
     * 更新渠道配置
     */
    public ChannelConfig updateChannelConfig(ChannelConfig config) {
        channelConfigStore.findById(config.getId())
                .orElseThrow(() -> new IllegalArgumentException("ChannelConfig not found: " + config.getId()));

        config.setUpdatedAt(LocalDateTime.now());
        channelConfigStore.update(config);

        log.info("[ChannelBindingService] Updated channel config: {}", config.getId());
        return config;
    }

    /**
     * 删除渠道配置
     * 注意：会同步删除该 config 下的所有绑定关系
     */
    public void deleteChannelConfig(String configId) {
        channelConfigStore.findById(configId)
                .orElseThrow(() -> new IllegalArgumentException("ChannelConfig not found: " + configId));

        channelConfigStore.delete(configId);
        log.info("[ChannelBindingService] Deleted channel config: {}", configId);
    }

    /**
     * 查询渠道配置
     */
    public Optional<ChannelConfig> getChannelConfig(String configId) {
        return channelConfigStore.findById(configId);
    }

    /**
     * 查询指定类型的所有渠道配置
     */
    public List<ChannelConfig> listChannelConfigs(String channelType) {
        if (channelType != null) {
            return channelConfigStore.findByChannelType(channelType);
        }
        return channelConfigStore.findAll();
    }

    /**
     * 查询所有已启用的渠道配置
     */
    public List<ChannelConfig> listEnabledChannelConfigs() {
        return channelConfigStore.findAllEnabled();
    }

    // ==================== ChannelBinding 管理 ====================

    /**
     * 创建绑定关系（后台将某个 IM 群/私聊与 Workspace 绑定）
     *
     * @param channelName 渠道名称（如 "Feishu"）
     * @param chatId      会话 ID（如群的 oc_xxx 或用户的 ou_xxx）
     * @param chatType    会话类型（"group" 或 "p2p"）
     * @param workspaceId 目标 Workspace ID
     * @param description 描述（可选）
     * @return 创建后的绑定关系
     */
    public ChannelBinding createBinding(String channelName, String chatId,
                                        String chatType, String workspaceId, String description) {
        // 校验 workspace 存在
        workspaceRegistry.get(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + workspaceId));

        String bindingId = ChannelBinding.createId(channelName, chatId);

        // 防止重复绑定
        if (channelBindingStore.exists(bindingId)) {
            throw new IllegalArgumentException(
                    "Binding already exists for channel=" + channelName + " chatId=" + chatId
                            + ", please delete it first");
        }

        LocalDateTime now = LocalDateTime.now();
        ChannelBinding binding = ChannelBinding.builder()
                .id(bindingId)
                .workspaceId(workspaceId)
                .channelName(channelName)
                .chatId(chatId)
                .chatType(chatType)
                .description(description)
                .enabled(true)
                .createdAt(now)
                .updatedAt(now)
                .build();

        channelBindingStore.save(binding);
        log.info("[ChannelBindingService] Created binding: channel={} chatId={} -> workspace={}",
                channelName, chatId, workspaceId);
        return binding;
    }

    /**
     * 更新绑定关系（如将某个群从一个 Workspace 改绑到另一个）
     *
     * @param channelName    渠道名称
     * @param chatId         会话 ID
     * @param newWorkspaceId 新的目标 Workspace ID
     */
    public ChannelBinding updateBinding(String channelName, String chatId, String newWorkspaceId) {
        // 校验新 workspace 存在
        workspaceRegistry.get(newWorkspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + newWorkspaceId));

        String bindingId = ChannelBinding.createId(channelName, chatId);
        ChannelBinding binding = channelBindingStore.findById(bindingId)
                .orElseThrow(() -> new IllegalArgumentException("Binding not found: " + bindingId));

        String oldWorkspaceId = binding.getWorkspaceId();
        binding.setWorkspaceId(newWorkspaceId);
        binding.setUpdatedAt(LocalDateTime.now());
        channelBindingStore.update(binding);

        log.info("[ChannelBindingService] Updated binding: channel={} chatId={} workspace: {} -> {}",
                channelName, chatId, oldWorkspaceId, newWorkspaceId);
        return binding;
    }

    /**
     * 启用/禁用绑定关系
     *
     * @param channelName 渠道名称
     * @param chatId      会话 ID
     * @param enabled     是否启用
     */
    public void setBindingEnabled(String channelName, String chatId, boolean enabled) {
        String bindingId = ChannelBinding.createId(channelName, chatId);
        ChannelBinding binding = channelBindingStore.findById(bindingId)
                .orElseThrow(() -> new IllegalArgumentException("Binding not found: " + bindingId));

        binding.setEnabled(enabled);
        binding.setUpdatedAt(LocalDateTime.now());
        channelBindingStore.update(binding);

        log.info("[ChannelBindingService] Set binding {} enabled={}", bindingId, enabled);
    }

    /**
     * 删除绑定关系
     */
    public void deleteBinding(String channelName, String chatId) {
        String bindingId = ChannelBinding.createId(channelName, chatId);
        channelBindingStore.findById(bindingId)
                .orElseThrow(() -> new IllegalArgumentException("Binding not found: " + bindingId));

        channelBindingStore.delete(bindingId);
        log.info("[ChannelBindingService] Deleted binding: channel={} chatId={}", channelName, chatId);
    }

    /**
     * 查询单条绑定关系
     */
    public Optional<ChannelBinding> getBinding(String channelName, String chatId) {
        return channelBindingStore.findByChannelNameAndChatId(channelName, chatId);
    }

    /**
     * 查询某个 Workspace 的所有绑定
     */
    public List<ChannelBinding> listBindingsByWorkspace(String workspaceId) {
        return channelBindingStore.findByWorkspaceId(workspaceId);
    }

    /**
     * 查询某个渠道的所有绑定
     */
    public List<ChannelBinding> listBindingsByChannel(String channelName) {
        return channelBindingStore.findByChannelName(channelName);
    }

    /**
     * 查询所有绑定
     */
    public List<ChannelBinding> listAllBindings() {
        return channelBindingStore.findAll();
    }

    // ==================== 路由解析（核心：供 Gateway 使用）====================

    /**
     * 根据 (channelName, chatId) 解析目标 workspaceId
     * 这是 Gateway 在消息分发时调用的核心路由方法
     *
     * @param channelName 渠道名称
     * @param chatId      会话 ID
     * @return workspaceId（如果绑定存在且启用）；否则 empty
     */
    public Optional<String> resolveWorkspaceId(String channelName, String chatId) {
        return channelBindingStore.findByChannelNameAndChatId(channelName, chatId)
                .filter(ChannelBinding::isEnabled)
                .map(ChannelBinding::getWorkspaceId);
    }
}
