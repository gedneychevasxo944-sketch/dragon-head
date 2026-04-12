package org.dragon.api.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * MaterialResponse 物料文件响应DTO
 *
 * @author dragon
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialResponse {

    /**
     * 物料唯一标识
     */
    private String materialId;

    /**
     * 文件名
     */
    private String filename;

    /**
     * 文件类型 (document/image/data/other)
     */
    private String type;

    /**
     * 文件大小（字节）
     */
    private long size;

    /**
     * 格式化后的文件大小
     */
    private String sizeFormatted;

    /**
     * 上传者
     */
    private String uploader;

    /**
     * 上传时间
     */
    private LocalDateTime uploadTime;

    /**
     * 解析状态
     */
    private String parseStatus;

    /**
     * 从 Material 实体转换
     */
    public static MaterialResponse from(org.dragon.material.Material material) {
        return MaterialResponse.builder()
                .materialId(material.getId())
                .filename(material.getName())
                .type(resolveFileType(material))
                .size(material.getSize())
                .sizeFormatted(formatFileSize(material.getSize()))
                .uploader(material.getUploader())
                .uploadTime(material.getUploadedAt())
                .parseStatus(material.getParseStatus())
                .build();
    }

    /**
     * 根据 MIME type 转换为前端枚举
     */
    private static String resolveFileType(org.dragon.material.Material material) {
        String mimeType = material.getType();
        if (mimeType == null) {
            return "other";
        }
        if (mimeType.startsWith("image/")) {
            return "image";
        }
        if (mimeType.equals("text/csv") || mimeType.contains("spreadsheet") || mimeType.contains("excel")) {
            return "data";
        }
        if (mimeType.contains("pdf") || mimeType.contains("document") || mimeType.contains("word") || mimeType.contains("text")) {
            return "document";
        }
        return "other";
    }

    /**
     * 格式化文件大小
     */
    private static String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        }
        if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024));
        }
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
