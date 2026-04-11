package org.dragon.material;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Office 文档物料解析器（支持 Word、Excel、PPT）
 * 使用 Apache POI 提取文本内容
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
        String type = material.getType();
        try {
            if (type == null) {
                return buildErrorResult(material, "Unknown file type");
            }

            // Word 文档 (.docx)
            if (type.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) {
                return parseWordDocx(inputStream, material);
            }
            // Excel 文档 (.xls, .xlsx)
            if (type.equals("application/vnd.ms-excel") ||
                type.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) {
                return parseExcel(inputStream, material);
            }
            // 旧版 Word (.doc) - 暂时不支持
            if (type.equals("application/msword")) {
                return buildFallbackResult(material, "Legacy Word (.doc) parsing not supported, please convert to .docx");
            }
            // PowerPoint 文档 - 暂时不支持
            if (type.contains("powerpoint") || type.contains("presentation")) {
                return buildFallbackResult(material, "PowerPoint parsing not yet supported");
            }

            return buildErrorResult(material, "Unsupported Office type: " + type);

        } catch (Exception e) {
            log.error("[OfficeMaterialParser] Failed to parse Office material {}: {}", material.getId(), e.getMessage(), e);
            return buildErrorResult(material, "Office parsing failed: " + e.getMessage());
        }
    }

    private ParseResult parseWordDocx(InputStream inputStream, Material material) throws Exception {
        XWPFDocument document = new XWPFDocument(inputStream);
        StringBuilder sb = new StringBuilder();
        for (XWPFParagraph paragraph : document.getParagraphs()) {
            String text = paragraph.getText();
            if (text != null && !text.isEmpty()) {
                sb.append(text).append("\n");
            }
        }
        String textContent = sb.toString().trim();
        document.close();

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("type", "Word Document (.docx)");
        metadata.put("paragraphCount", document.getParagraphs().size());
        metadata.put("textLength", textContent.length());

        return ParseResult.builder()
                .materialId(material.getId())
                .success(true)
                .textContent(textContent)
                .metadata(metadata)
                .build();
    }

    private ParseResult parseExcel(InputStream inputStream, Material material) throws Exception {
        Workbook workbook = WorkbookFactory.create(inputStream);
        StringBuilder sb = new StringBuilder();

        for (Sheet sheet : workbook) {
            sb.append("=== Sheet: ").append(sheet.getSheetName()).append(" ===\n");
            for (Row row : sheet) {
                StringBuilder rowBuilder = new StringBuilder();
                for (Cell cell : row) {
                    if (rowBuilder.length() > 0) {
                        rowBuilder.append("\t");
                    }
                    rowBuilder.append(getCellValue(cell));
                }
                sb.append(rowBuilder.toString()).append("\n");
            }
            sb.append("\n");
        }
        String textContent = sb.toString().trim();
        workbook.close();

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("type", "Excel Spreadsheet");
        metadata.put("sheetCount", workbook.getNumberOfSheets());
        metadata.put("textLength", textContent.length());

        return ParseResult.builder()
                .materialId(material.getId())
                .success(true)
                .textContent(textContent)
                .metadata(metadata)
                .build();
    }

    private String getCellValue(Cell cell) {
        if (cell == null) {
            return "";
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> "";
        };
    }

    private ParseResult buildErrorResult(Material material, String errorMessage) {
        return ParseResult.builder()
                .materialId(material.getId())
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }

    private ParseResult buildFallbackResult(Material material, String message) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("fallback", true);
        metadata.put("message", message);
        return ParseResult.builder()
                .materialId(material.getId())
                .success(false)
                .errorMessage(message)
                .metadata(metadata)
                .build();
    }

    @Override
    public Map<String, ParseResult> parseAll(List<Material> materials) {
        Map<String, ParseResult> results = new HashMap<>();
        for (Material material : materials) {
            results.put(material.getId(), ParseResult.builder()
                    .materialId(material.getId())
                    .success(false)
                    .errorMessage("parseAll requires batch InputStream handling")
                    .build());
        }
        return results;
    }

    @Override
    public List<String> supportedTypes() {
        return SUPPORTED_TYPES;
    }
}
