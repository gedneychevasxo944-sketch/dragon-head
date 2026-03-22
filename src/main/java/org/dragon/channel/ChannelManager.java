package org.dragon.channel;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.dragon.channel.adapter.ChannelAdapter;
import org.dragon.channel.adapter.FeishuChannelAdaptor;
import org.dragon.channel.entity.ActionMessage;
import org.dragon.channel.entity.ChannelConfig;
import org.dragon.channel.store.ChannelConfigStore;
import org.dragon.gateway.Gateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ChannelManager 渠道管理器
 *
 * <p>职责：
 * <ol>
 *   <li>启动时自动注册所有 Spring 注入的 {@link ChannelAdapter} 实例</li>
 *   <li>启动时从 {@link ChannelConfigStore} 加载动态配置并应用</li>
 *   <li>提供后台管理界面调用的 {@link #reloadChannelConfig(ChannelConfig)} 热重载接口</li>
 *   <li>定时健康巡检并自动重连</li>
 *   <li>提供统一的下行消息路由入口 {@link #routeMessageOutbound(ActionMessage)}</li>
 * </ol>
 *
 * @author zhz
 * @version 2.0
 */
@Service
@Slf4j
public class ChannelManager {

    /**
     * 渠道注册表：channelName -> ChannelAdapter 实例
     */
    private final Map<String, ChannelAdapter> registry = new ConcurrentHashMap<>();

    private final Gateway gateway;
    private final ChannelConfigStore channelConfigStore;

    @Autowired
    public ChannelManager(List<ChannelAdapter> adapters, Gateway gateway,
                          ChannelConfigStore channelConfigStore) {
        this.gateway = gateway;
        this.channelConfigStore = channelConfigStore;
        for (ChannelAdapter adapter : adapters) {
            registry.put(adapter.getChannelName(), adapter);
            log.info("[ChannelManager] 注册渠道插件: {}", adapter.getChannelName());
        }
    }

    /**
     * 启动时：先应用动态配置，再启动所有渠道监听
     */
    @PostConstruct
    public void startAllChannels() {
        // 1. 从数据库/Store 加载已保存的动态配置并应用（覆盖 application.yml 默认值）
        applyStoredConfigs();

        // 2. 启动所有已注册渠道的监听
        log.info("[ChannelManager] 正在启动所有渠道监听...");
        for (ChannelAdapter adapter : registry.values()) {
            CompletableFuture.runAsync(() -> {
                try {
                    adapter.startListening(gateway);
                } catch (Exception e) {
                    log.error("[ChannelManager] 渠道 {} 启动失败", adapter.getChannelName(), e);
                }
            });
        }
    }

    /**
     * 后台管理界面调用：热重载指定渠道的配置
     * 会先 stop 再用新配置重新 start，无需重启 JVM
     *
     * @param config 新的渠道配置
     */
    public void reloadChannelConfig(ChannelConfig config) {
        String channelType = config.getChannelType();
        ChannelAdapter adapter = registry.get(channelType);
        if (adapter == null) {
            throw new IllegalArgumentException("Channel not found: " + channelType);
        }

        log.info("[ChannelManager] 热重载渠道配置: channelType={}, configId={}", channelType, config.getId());

        // 将动态配置注入到 Adaptor（目前只有 Feishu 支持，后续可扩展接口）
        if (adapter instanceof FeishuChannelAdaptor) {
            ((FeishuChannelAdaptor) adapter).configure(config);
        }

        // 重启监听（stop 旧连接，用新凭证 start 新连接）
        CompletableFuture.runAsync(() -> {
            try {
                adapter.stop();
                adapter.startListening(gateway);
                log.info("[ChannelManager] 渠道 {} 热重载完成", channelType);
            } catch (Exception e) {
                log.error("[ChannelManager] 渠道 {} 热重载失败", channelType, e);
            }
        });
    }

    /**
     * 定时健康巡检（每 30 秒）
     */
    @Scheduled(fixedRate = 30000)
    public void healthCheckProbes() {
        log.debug("[Watchdog] 开始执行渠道健康巡检...");

        for (ChannelAdapter adapter : registry.values()) {
            try {
                if (!adapter.isHealthy()) {
                    log.error("[Watchdog] 渠道 {} 检测到异常，准备重连", adapter.getChannelName());
                    CompletableFuture.runAsync(() -> {
                        try {
                            adapter.restart();
                            log.info("[Watchdog] 渠道 {} 重连成功", adapter.getChannelName());
                        } catch (Exception e) {
                            log.error("[Watchdog] 渠道 {} 重连失败", adapter.getChannelName(), e);
                        }
                    });
                }
            } catch (Exception e) {
                log.error("[Watchdog] 渠道 {} 健康巡检失败", adapter.getChannelName(), e);
            }
        }
    }

    /**
     * 下行消息路由：根据 channelName 找到对应 Adapter 发送消息
     *
     * @param message 下行消息
     * @return 异步结果
     */
    public CompletableFuture<Void> routeMessageOutbound(ActionMessage message) {
        ChannelAdapter adapter = registry.get(message.getChannelName());
        if (adapter == null) {
            CompletableFuture<Void> failed = new CompletableFuture<>();
            failed.completeExceptionally(new IllegalArgumentException("未找到对应渠道: " + message.getChannelName()));
            return failed;
        }
        return adapter.sendMessage(message);
    }

    /**
     * 服务停止时优雅关闭所有渠道
     */
    @PreDestroy
    public void stopAllChannels() {
        log.info("[ChannelManager] 服务中止，正在关闭所有渠道...");
        registry.values().forEach(ChannelAdapter::stop);
    }

    // ==================== 私有方法 ====================

    /**
     * 启动时从 ChannelConfigStore 中加载所有已启用的动态配置并应用到对应 Adapter
     */
    private void applyStoredConfigs() {
        List<ChannelConfig> enabledConfigs = channelConfigStore.findAllEnabled();
        if (enabledConfigs.isEmpty()) {
            log.info("[ChannelManager] 未找到已启用的动态渠道配置，使用 application.yml 默认值");
            return;
        }

        for (ChannelConfig config : enabledConfigs) {
            ChannelAdapter adapter = registry.get(config.getChannelType());
            if (adapter == null) {
                log.warn("[ChannelManager] 配置 {} 对应的渠道类型 {} 未找到对应适配器，跳过",
                        config.getId(), config.getChannelType());
                continue;
            }
            if (adapter instanceof FeishuChannelAdaptor) {
                ((FeishuChannelAdaptor) adapter).configure(config);
                log.info("[ChannelManager] 已应用动态配置: configId={} -> channelType={}",
                        config.getId(), config.getChannelType());
            }
        }
    }
}
