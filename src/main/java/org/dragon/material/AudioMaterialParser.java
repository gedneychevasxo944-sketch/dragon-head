package org.dragon.material;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * 音频物料解析器
 * 第一版提供转写接口适配点和降级描述
 * 实际转写需要接入语音识别服务（如 ASR、Whisper 等）
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Component
public class AudioMaterialParser implements MaterialParser {

    private static final List<String> SUPPORTED_TYPES = List.of(
            "audio/mpeg",
            "audio/mp3",
            "audio/wav",
            "audio/ogg",
            "audio/m4a",
            "audio/aac",
            "audio/x-m4a",
            "video/mp4",
            "video/quicktime",
            "video/x-msvideo"
    );

    @Override
    public ParseResult parse(Material material, InputStream inputStream) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("materialId", material.getId());
        metadata.put("materialName", material.getName());
        metadata.put("materialType", material.getType());
        metadata.put("size", material.getSize());

        // 音频/视频转写需要外部 ASR 服务
        // 当前版本提供降级描述，后续可接入:
        // - Whisper API
        // - 飞书语音识别
        // - 阿里云 ASR
        // - 腾讯云 ASR

        String fallbackDescription = buildFallbackDescription(material);

        log.info("[AudioMaterialParser] Audio material {} received, transcription not yet implemented", material.getId());

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
        sb.append("【音频文件信息】\n");
        sb.append("文件名: ").append(material.getName()).append("\n");
        sb.append("类型: ").append(material.getType()).append("\n");
        sb.append("大小: ").append(formatFileSize(material.getSize())).append("\n");
        sb.append("\n说明: 此文件为音频/视频文件，需要语音转文字服务才能提取内容。");
        sb.append("\n当前版本暂不支持自动转写，请在任务中描述需要的操作，或手动提供文字版本。");
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
