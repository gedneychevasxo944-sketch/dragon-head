package org.dragon.workspace.material;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Office 文档物料解析器（支持 Word、Excel、PPT）
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Component
public class OfficeMaterialParser implements MaterialParser {

    private static final List<String> SUPPORTED_TYPES = List.of(
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation"
    );

    @Override
    public ParseResult parse(Material material, InputStream inputStream) {
        log.warn("[OfficeMaterialParser] Office parsing not fully implemented for material {}", material.getId());
        return ParseResult.builder()
                .materialId(material.getId())
                .success(false)
                .errorMessage("Office parsing requires additional library (e.g. Apache POI)")
                .build();
    }

    @Override
    public Map<String, ParseResult> parseAll(List<Material> materials) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> supportedTypes() {
        return SUPPORTED_TYPES;
    }
}
