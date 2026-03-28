package org.dragon.channel.store;

import io.ebean.Database;
import org.dragon.datasource.entity.ChannelBindingEntity;
import org.dragon.channel.entity.ChannelBinding;
import org.dragon.store.StoreType;
import org.dragon.store.StoreTypeAnn;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MySqlChannelBindingStore 渠道绑定MySQL存储实现
 */
@Component
@StoreTypeAnn(StoreType.MYSQL)
public class MySqlChannelBindingStore implements ChannelBindingStore {

    private final Database mysqlDb;

    public MySqlChannelBindingStore(@Qualifier("mysqlEbeanDatabase") Database mysqlDb) {
        this.mysqlDb = mysqlDb;
    }

    @Override
    public void save(ChannelBinding binding) {
        ChannelBindingEntity entity = ChannelBindingEntity.fromChannelBinding(binding);
        mysqlDb.save(entity);
    }

    @Override
    public void update(ChannelBinding binding) {
        ChannelBindingEntity entity = ChannelBindingEntity.fromChannelBinding(binding);
        mysqlDb.update(entity);
    }

    @Override
    public void delete(String id) {
        mysqlDb.delete(ChannelBindingEntity.class, id);
    }

    @Override
    public Optional<ChannelBinding> findById(String id) {
        ChannelBindingEntity entity = mysqlDb.find(ChannelBindingEntity.class, id);
        return entity != null ? Optional.of(entity.toChannelBinding()) : Optional.empty();
    }

    @Override
    public Optional<ChannelBinding> findByChannelNameAndChatId(String channelName, String chatId) {
        String id = ChannelBinding.createId(channelName, chatId);
        return findById(id);
    }

    @Override
    public List<ChannelBinding> findByWorkspaceId(String workspaceId) {
        return mysqlDb.find(ChannelBindingEntity.class)
                .where()
                .eq("workspaceId", workspaceId)
                .findList()
                .stream()
                .map(ChannelBindingEntity::toChannelBinding)
                .collect(Collectors.toList());
    }

    @Override
    public List<ChannelBinding> findByChannelName(String channelName) {
        return mysqlDb.find(ChannelBindingEntity.class)
                .where()
                .eq("channelName", channelName)
                .findList()
                .stream()
                .map(ChannelBindingEntity::toChannelBinding)
                .collect(Collectors.toList());
    }

    @Override
    public List<ChannelBinding> findAll() {
        return mysqlDb.find(ChannelBindingEntity.class)
                .findList()
                .stream()
                .map(ChannelBindingEntity::toChannelBinding)
                .collect(Collectors.toList());
    }

    @Override
    public boolean exists(String id) {
        return mysqlDb.find(ChannelBindingEntity.class, id) != null;
    }
}
