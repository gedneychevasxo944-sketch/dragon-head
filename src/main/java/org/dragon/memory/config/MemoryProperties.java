package org.dragon.memory.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 记忆系统配置属性类
 *
 * @author binarytom
 * @version 1.0
 */
@Data
@ConfigurationProperties(prefix = "dragon.memory")
public class MemoryProperties {

    private String rootDir = "./data/memory";
    private int maxIndexLines = 200;
    private int maxIndexBytes = 25_000;
    private int defaultRecallLimit = 5;
    private boolean enableSessionCheckpoint = true;
}
