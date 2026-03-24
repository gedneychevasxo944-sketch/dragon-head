package org.dragon.workspace.material;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 物料解析器注册中心
 * 根据 MIME 类型分发到对应的解析器
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MaterialParserRegistry {

    private final List<MaterialParser> parsers;
    private final Map<String, MaterialParser> parserMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        for (MaterialParser parser : parsers) {
            for (String type : parser.supportedTypes()) {
                parserMap.put(type.toLowerCase(), parser);
                log.info("[MaterialParserRegistry] Registered parser for type: {}", type);
            }
        }
    }

    /**
     * 根据 MIME 类型获取解析器
     */
    public MaterialParser getParser(String mimeType) {
        if (mimeType == null) {
            return null;
        }
        return parserMap.get(mimeType.toLowerCase());
    }

    /**
     * 检查是否有支持的解析器
     */
    public boolean isSupported(String mimeType) {
        return getParser(mimeType) != null;
    }
}
