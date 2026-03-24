package org.dragon.workspace.material;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * 视频物料解析器
 * 第一版提供元数据提取和关键帧理解接口适配点
 * 实际分析需要接入视频理解服务（如多模态模型、视频切片等）
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Component
public class VideoMaterialParser implements MaterialParser {

    private static final List<String> SUPPORTED_TYPES = List.of(
            "video/mp4",
            "video/mpeg",
            "video/quicktime",
            "video/x-msvideo",
            "video/x-ms-wmv",
            "video/webm",
            "video/x-matroska"
    );

    @Override
    public ParseResult parse(Material material, InputStream inputStream) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("materialId", material.getId());
        metadata.put("materialName", material.getName());
        metadata.put("materialType", material.getType());
        metadata.put("size", material.getSize());

        // 视频理解需要外部多模态服务
        // 当前版本提供降级描述，后续可接入:
        // - GPT-4V / Gemini 多模态理解
        // - 视频关键帧提取 + 图像分析
        // - 飞书视频理解
        // - 阿里云视频理解

        String fallbackDescription = buildFallbackDescription(material);

        log.info("[VideoMaterialParser] Video material {} received, video analysis not yet implemented", material.getId());

        return ParseResult.builder()
                .materialId(material.getId())
                .success(true)
                .textContent(fallbackDescription)
                .metadata(metadata)
                .build();
    }

    /**
     * 构建降级描述
     */
    private String buildFallbackDescription(Material material) {
        StringBuilder sb = new StringBuilder();
        sb.append("【视频文件信息】\n");
        sb.append("文件名: ").append(material.getName()).append("\n");
        sb.append("类型: ").append(material.getType()).append("\n");
        sb.append("大小: ").append(formatFileSize(material.getSize())).append("\n");
        sb.append("\n说明: 此文件为视频文件，需要视频理解服务才能分析内容。");
        sb.append("\n当前版本暂不支持自动分析，请在任务中描述需要的操作。");
        sb.append("\n可考虑的关键帧提取方案:\n");
        sb.append("- 均匀采样关键帧\n");
        sb.append("- 场景切换检测\n");
        sb.append("- 镜头类型分析");
        return sb.toString();
    }

    private String formatFileSize(Long size) {
        if (size == null) {
            return "未知";
        }
        if (size < 1024) {
            return size + " B";
        }
        if (size < 1024 * 1024) {
            return String.format("%.2f KB", size / 1024.0);
        }
        if (size < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", size / (1024.0 * 1024));
        }
        return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
    }

    @Override
    public Map<String, ParseResult> parseAll(List<Material> materials) {
        Map<String, ParseResult> results = new HashMap<>();
        for (Material material : materials) {
            if (supports(material)) {
                results.put(material.getId(), parse(material, null));
            }
        }
        return results;
    }

    @Override
    public List<String> supportedTypes() {
        return SUPPORTED_TYPES;
    }
}
