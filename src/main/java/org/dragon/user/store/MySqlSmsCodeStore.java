package org.dragon.user.store;

import io.ebean.Database;
import org.dragon.user.entity.SmsCodeEntity;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * MySqlSmsCodeStore 短信验证码MySQL存储实现
 */
@Component
public class MySqlSmsCodeStore implements SmsCodeStore {

    private final Database mysqlDb;

    public MySqlSmsCodeStore(@Qualifier("mysqlEbeanDatabase") Database mysqlDb) {
        this.mysqlDb = mysqlDb;
    }

    @Override
    public void save(SmsCodeEntity smsCode) {
        mysqlDb.save(smsCode);
    }

    @Override
    public Optional<SmsCodeEntity> findLatest(String phone, String type) {
        return mysqlDb.find(SmsCodeEntity.class)
                .where()
                .eq("phone", phone)
                .eq("type", type)
                .eq("used", false)
                .gt("expiresAt", LocalDateTime.now())
                .orderBy()
                .desc("createTime")
                .setMaxRows(1)
                .findOneOrEmpty();
    }

    @Override
    public void markUsed(String id) {
        SmsCodeEntity entity = mysqlDb.find(SmsCodeEntity.class, id);
        if (entity != null) {
            entity.setUsed(true);
            mysqlDb.update(entity);
        }
    }

    @Override
    public void deleteExpired(LocalDateTime before) {
        mysqlDb.find(SmsCodeEntity.class)
                .where()
                .lt("expiresAt", before)
                .delete();
    }
}
