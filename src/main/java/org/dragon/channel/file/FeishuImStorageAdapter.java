package org.dragon.channel.file;

import com.lark.oapi.Client;
import com.lark.oapi.service.im.v1.model.GetMessageResourceReq;
import com.lark.oapi.service.im.v1.model.GetMessageResourceResp;

import org.dragon.channel.entity.ChannelConfig;
import org.dragon.channel.entity.NormalizedFile;
import org.dragon.channel.enums.FileSource;
import org.dragon.channel.store.ChannelConfigStore;
import org.dragon.store.StoreFactory;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * 飞书 IM 文件存储适配器
 * 通过飞书开放 API 下载/上传文件
 *
 * @author zhz
 * @version 1.0
 */
@Slf4j
@Component
public class FeishuImStorageAdapter implements FileStorageAdapter {

    private final ChannelConfigStore channelConfigStore;
    private volatile Client apiClient;
    private volatile String currentConfigId;
    private volatile boolean initialized = false;

    public FeishuImStorageAdapter(StoreFactory storeFactory) {
        this.channelConfigStore = storeFactory.get(ChannelConfigStore.class);
        // 延迟初始化，不在构造函数中调用数据库查询
    }

    /**
     * 确保 API 客户端已初始化
     */
    private void ensureInitialized() {
        if (!initialized) {
            synchronized (this) {
                if (!initialized) {
                    initializeApiClient();
                    initialized = true;
                }
            }
        }
    }

    /**
     * 初始化 API 客户端
     * 从 ChannelConfigStore 获取飞书配置
     */
    private void initializeApiClient() {
        try {
            // 查询飞书渠道配置
            var configs = channelConfigStore.findByChannelType("Feishu");
            if (configs == null || configs.isEmpty()) {
                log.warn("[FeishuImStorageAdapter] No Feishu config found, will retry on first use");
                return;
            }

            // 使用第一个启用的配置
            ChannelConfig config = configs.stream()
                    .filter(ChannelConfig::isEnabled)
                    .findFirst()
                    .orElse(null);

            if (config == null) {
                log.warn("[FeishuImStorageAdapter] No enabled Feishu config found");
                return;
            }

            var credentials = config.getCredentials();
            if (credentials == null) {
                log.warn("[FeishuImStorageAdapter] Feishu config has no credentials");
                return;
            }

            String appId = (String) credentials.get("appId");
            String appSecret = (String) credentials.get("appSecret");

            if (appId == null || appSecret == null) {
                log.warn("[FeishuImStorageAdapter] Feishu credentials incomplete");
                return;
            }

            this.apiClient = Client.newBuilder(appId, appSecret).build();
            this.currentConfigId = config.getId();
            log.info("[FeishuImStorageAdapter] Initialized with config: {}", currentConfigId);
        } catch (Exception e) {
            log.warn("[FeishuImStorageAdapter] Failed to initialize: {}, will retry on first use", e.getMessage());
        }
    }

    @Override
    public FileSource getSupportedSource() {
        return FileSource.FEISHU_IM;
    }

    @Override
    public NormalizedFile download(NormalizedFile fileMeta) throws Exception {
        // 确保客户端已初始化
        ensureInitialized();
        if (apiClient == null) {
            throw new IllegalStateException("Feishu API client not initialized");
        }

        // 创建请求对象
        GetMessageResourceReq req = GetMessageResourceReq.newBuilder()
                .messageId(fileMeta.getMessageId())
                .fileKey(fileMeta.getFileKey())
                .type(fileMeta.getMimeType())
                .build();

        // 发起请求
        GetMessageResourceResp resp = apiClient.im().v1().messageResource().get(req);

        // 处理服务端错误
        if (!resp.success()) {
            throw new RuntimeException("Feishu file download failed: " + resp.getCode() + " " + resp.getMsg());
        }

        // 标记存储 key
        fileMeta.setStorageKey(fileMeta.getFileKey() + ":" + fileMeta.getMessageId());
        return fileMeta;
    }

    @Override
    public NormalizedFile upload(NormalizedFile file) throws Exception {
        throw new UnsupportedOperationException("不支持的保存类型");
    }

}
