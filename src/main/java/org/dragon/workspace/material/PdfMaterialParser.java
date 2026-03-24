package org.dragon.workspace.material;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * PDF 物料解析器
 * 使用 Apache PDFBox 提取 PDF 文本内容，元数据
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
        try {
            // 加载 PDF 文档
            PDDocument document = Loader.loadPDF(inputStream.readAllBytes());

            // 提取文本内容
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String textContent = stripper.getText(document);

            // 获取元数据
            PDDocumentInformation info = document.getDocumentInformation();
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("pageCount", document.getNumberOfPages());
            metadata.put("title", info.getTitle());
            metadata.put("author", info.getAuthor());
            metadata.put("subject", info.getSubject());
            metadata.put("creator", info.getCreator());
            metadata.put("producer", info.getProducer());
            metadata.put("creationDate", info.getCreationDate());
            metadata.put("modificationDate", info.getModificationDate());

            document.close();

            log.info("[PdfMaterialParser] Successfully parsed PDF material {}, {} pages", material.getId(), document.getNumberOfPages());

            return ParseResult.builder()
                    .materialId(material.getId())
                    .success(true)
                    .textContent(textContent)
                    .metadata(metadata)
                    .build();

        } catch (Exception e) {
            log.error("[PdfMaterialParser] Failed to parse PDF material {}: {}", material.getId(), e.getMessage());
            return ParseResult.builder()
                    .materialId(material.getId())
                    .success(false)
                    .errorMessage("PDF parsing failed: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public Map<String, ParseResult> parseAll(List<Material> materials) {
        Map<String, ParseResult> results = new HashMap<>();
        for (Material material : materials) {
            if (material.getType() != null && material.getType().equalsIgnoreCase("application/pdf")) {
                results.put(material.getId(), ParseResult.builder()
                        .materialId(material.getId())
                        .success(false)
                        .errorMessage("parseAll requires InputStream from material storage")
                        .build());
            }
        }
        return results;
    }

    @Override
    public List<String> supportedTypes() {
        return SUPPORTED_TYPES;
    }
}
