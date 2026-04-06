package org.dragon.memory.config;

import org.dragon.config.context.InheritanceContext;
import org.dragon.config.service.ConfigApplication;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 记忆系统配置属性类
 *
 * @author binarytom
 * @version 1.0
 */
@Data
@Component
public class MemoryProperties {

    private String rootDir = "./data/memory";
    private int maxIndexLines = 200;
    private int maxIndexBytes = 25_000;
    private int defaultRecallLimit = 5;
    private boolean enableSessionCheckpoint = true;

    @Autowired
    public MemoryProperties(ConfigApplication configApplication) {
        InheritanceContext ctx = InheritanceContext.forGlobal();
        this.rootDir = configApplication.getStringValue("memory.root-dir", ctx, "./data/memory");
        this.maxIndexLines = configApplication.getIntValue("memory.max-index-lines", ctx, 200);
        this.maxIndexBytes = configApplication.getIntValue("memory.max-index-bytes", ctx, 25_000);
        this.defaultRecallLimit = configApplication.getIntValue("memory.default-recall-limit", ctx, 5);
        this.enableSessionCheckpoint = configApplication.getBooleanValue("memory.enable-session-checkpoint", ctx, true);
    }
}
