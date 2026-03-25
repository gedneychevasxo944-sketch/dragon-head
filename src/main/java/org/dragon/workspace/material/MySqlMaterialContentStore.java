package org.dragon.workspace.material;

import io.ebean.Database;
import org.dragon.datasource.entity.ParsedMaterialContentEntity;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * MySqlMaterialContentStore 物料解析内容MySQL存储实现
 */
@Component
public class MySqlMaterialContentStore implements MaterialContentStore {

    private final Database mysqlDb;

    public MySqlMaterialContentStore(@Qualifier("mysqlEbeanDatabase") Database mysqlDb) {
        this.mysqlDb = mysqlDb;
    }

    @Override
    public void saveParsedContent(ParsedMaterialContent content) {
        ParsedMaterialContentEntity entity = ParsedMaterialContentEntity.fromParsedMaterialContent(content);
        mysqlDb.save(entity);
    }

    @Override
    public Optional<ParsedMaterialContent> findById(String id) {
        ParsedMaterialContentEntity entity = mysqlDb.find(ParsedMaterialContentEntity.class, id);
        return entity != null ? Optional.of(entity.toParsedMaterialContent()) : Optional.empty();
    }

    @Override
    public Optional<ParsedMaterialContent> findByMaterialId(String materialId) {
        ParsedMaterialContentEntity entity = mysqlDb.find(ParsedMaterialContentEntity.class)
                .where()
                .eq("materialId", materialId)
                .orderBy()
                .desc("parsedAt")
                .findOne();
        return entity != null ? Optional.of(entity.toParsedMaterialContent()) : Optional.empty();
    }

    @Override
    public void delete(String id) {
        mysqlDb.delete(ParsedMaterialContentEntity.class, id);
    }
}
