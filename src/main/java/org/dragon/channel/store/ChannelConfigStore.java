package org.dragon.channel.store;

import org.dragon.channel.entity.ChannelConfig;
import org.dragon.store.Store;

import java.util.List;
import java.util.Optional;


/**
 * ChannelConfigStore 渠道配置存储接口
 * 管理各渠道 Bot 的凭证配置（appId/appSecret 等）
 *
 * @author zhz
 * @version 1.0
 */
public interface ChannelConfigStore extends Store {
    /**
     * 保存渠道配置
     */
    void save(ChannelConfig config);
    /**
     * 更新渠道配置
     */
    void update(ChannelConfig config);
    /**
     * 删除渠道配置
     */
    void delete(String id);
    /**
     * 根据 ID 查询
     */
    Optional<ChannelConfig> findById(String id);
    /**
     * 根据渠道类型查询所有配置
     */
    List<ChannelConfig> findByChannelType(String channelType);
    /**
     * 查询所有已启用的配置
     */
    List<ChannelConfig> findAllEnabled();
    /**
     * 查询所有配置
     */
    List<ChannelConfig> findAll();
    /**
     * 检查是否存在
     */
    boolean exists(String id);
}
