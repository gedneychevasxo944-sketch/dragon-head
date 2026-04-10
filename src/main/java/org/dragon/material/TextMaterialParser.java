package org.dragon.material;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * 文本物料解析器
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Component
public class TextMaterialParser implements MaterialParser {

    private static final List<String> SUPPORTED_TYPES = List.of(
            "text/plain", "text/html", "text/markdown",
            "application/json", "application/xml", "text/csv"
    );

    @Override
    public ParseResult parse(Material material, InputStream inputStream) {
        try {
            String text = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                    .lines().collect(Collectors.joining("\n"));
            return ParseResult.builder()
                    .materialId(material.getId())
                    .success(true)
                    .textContent(text)
                    .build();
        } catch (Exception e) {
            return ParseResult.builder()
                    .materialId(material.getId())
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    @Override
    public java.util.Map<String, ParseResult> parseAll(List<Material> materials) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> supportedTypes() {
        return SUPPORTED_TYPES;
    }
}
