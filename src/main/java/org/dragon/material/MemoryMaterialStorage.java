package org.dragon.material;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * MemoryMaterialStorage 物料内存存储实现
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Component
public class MemoryMaterialStorage implements MaterialStorage {

    private final Map<String, byte[]> storage = new ConcurrentHashMap<>();

    @Override
    public String store(String workspaceId, InputStream inputStream, String filename) {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] data = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, bytesRead);
            }
            buffer.flush();

            String key = workspaceId + "/" + UUID.randomUUID().toString();
            storage.put(key, buffer.toByteArray());
            log.debug("[MemoryMaterialStorage] Stored material with key: {}", key);
            return key;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store material", e);
        }
    }

    @Override
    public InputStream retrieve(String key) {
        byte[] data = storage.get(key);
        if (data == null) {
            return null;
        }
        return new ByteArrayInputStream(data);
    }

    @Override
    public void delete(String key) {
        storage.remove(key);
        log.debug("[MemoryMaterialStorage] Deleted material with key: {}", key);
    }

    @Override
    public boolean exists(String key) {
        return storage.containsKey(key);
    }
}
