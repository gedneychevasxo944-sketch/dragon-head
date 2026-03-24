package org.dragon.workspace.material;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * PDF 物料解析器
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Component
public class PdfMaterialParser implements MaterialParser {

    private static final List<String> SUPPORTED_TYPES = List.of("application/pdf");

    @Override
    public ParseResult parse(Material material, InputStream inputStream) {
        log.warn("[PdfMaterialParser] PDF parsing not fully implemented for material {}", material.getId());
        return ParseResult.builder()
                .materialId(material.getId())
                .success(false)
                .errorMessage("PDF parsing requires additional library (e.g. Apache PDFBox)")
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
