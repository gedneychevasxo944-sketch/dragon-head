package org.dragon.channel.store;

import org.dragon.channel.entity.ChannelBinding;
import org.dragon.store.Store;

import java.util.List;
import java.util.Optional;

/**
 * ChannelBindingStore 渠道绑定存储接口
 * 管理 (channelName + chatId) → workspaceId 的映射关系
 *
 * @author zhz
 * @version 1.0
 */
public interface ChannelBindingStore extends Store {
    /**
     * 保存绑定关系
     */
    void save(ChannelBinding binding);
    /**
     * 更新绑定关系
     */
    void update(ChannelBinding binding);
    /**
     * 删除绑定关系
     *
     * @param id 绑定 ID (channelName_chatId)
     */
    void delete(String id);
    /**
     * 根据 ID 查询
     */
    Optional<ChannelBinding> findById(String id);
    /**
     * 根据 channelName + chatId 查询（路由核心方法）
     *
     * @param channelName 渠道名称
     * @param chatId 会话 ID
     * @return 绑定关系
     */
    Optional<ChannelBinding> findByChannelNameAndChatId(String channelName, String chatId);
    /**
     * 根据 workspaceId 查询所有绑定
     */
    List<ChannelBinding> findByWorkspaceId(String workspaceId);
    /**
     * 根据 channelName 查询所有绑定
     */
    List<ChannelBinding> findByChannelName(String channelName);
    /**
     * 查询所有绑定
     */
    List<ChannelBinding> findAll();
    /**
     * 检查是否存在
     */
    boolean exists(String id);

}
