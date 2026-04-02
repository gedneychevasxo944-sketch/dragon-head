package org.dragon.memory.memv2.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 记忆系统配置属性类
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "memory")
public class MemoryProperties {
    /**
     * 记忆存储根目录
     */
    private String rootDir = "./data/memory";

    /**
     * 索引文件最大行数
     */
    private int maxIndexLines = 200;

    /**
     * 索引文件最大字节数
     */
    private int maxIndexBytes = 25_000;

    /**
     * 默认召回结果数量限制
     */
    private int defaultRecallLimit = 5;

    /**
     * 是否启用会话检查点
     */
    private boolean enableSessionCheckpoint = true;
}
