package org.dragon.channel.store;

import io.ebean.Database;
import org.dragon.datasource.entity.ChannelConfigEntity;
import org.dragon.channel.entity.ChannelConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MySqlChannelConfigStore 渠道配置MySQL存储实现
 */
@Component
public class MySqlChannelConfigStore implements ChannelConfigStore {

    private final Database mysqlDb;

    public MySqlChannelConfigStore(@Qualifier("mysqlEbeanDatabase") Database mysqlDb) {
        this.mysqlDb = mysqlDb;
    }

    @Override
    public void save(ChannelConfig config) {
        ChannelConfigEntity entity = ChannelConfigEntity.fromChannelConfig(config);
        mysqlDb.save(entity);
    }

    @Override
    public void update(ChannelConfig config) {
        ChannelConfigEntity entity = ChannelConfigEntity.fromChannelConfig(config);
        mysqlDb.update(entity);
    }

    @Override
    public void delete(String id) {
        mysqlDb.delete(ChannelConfigEntity.class, id);
    }

    @Override
    public Optional<ChannelConfig> findById(String id) {
        ChannelConfigEntity entity = mysqlDb.find(ChannelConfigEntity.class, id);
        return entity != null ? Optional.of(entity.toChannelConfig()) : Optional.empty();
    }

    @Override
    public List<ChannelConfig> findByChannelType(String channelType) {
        return mysqlDb.find(ChannelConfigEntity.class)
                .where()
                .eq("channelType", channelType)
                .findList()
                .stream()
                .map(ChannelConfigEntity::toChannelConfig)
                .collect(Collectors.toList());
    }

    @Override
    public List<ChannelConfig> findAllEnabled() {
        return mysqlDb.find(ChannelConfigEntity.class)
                .where()
                .eq("enabled", true)
                .findList()
                .stream()
                .map(ChannelConfigEntity::toChannelConfig)
                .collect(Collectors.toList());
    }

    @Override
    public List<ChannelConfig> findAll() {
        return mysqlDb.find(ChannelConfigEntity.class)
                .findList()
                .stream()
                .map(ChannelConfigEntity::toChannelConfig)
                .collect(Collectors.toList());
    }

    @Override
    public boolean exists(String id) {
        return mysqlDb.find(ChannelConfigEntity.class, id) != null;
    }
}
