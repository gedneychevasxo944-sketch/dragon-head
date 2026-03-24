package org.dragon.channel.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.dragon.channel.enums.FileSource;

/**
 * Description:
 * Author: zhz
 * Version: 1.0
 * Create Date Time: 2026/3/13 23:07
 * Update Date Time:
 *
 */
@Data
@NoArgsConstructor
public class NormalizedFile {
    // 通用元数据信息
    private FileSource fileSource;  // 数据来源
    private String fileUri;         // URI
    private String fileKey;         // 文件key
    private String fileName;        // 文件名称，如a.txt
    private String mimeType;        // 文件类型，如 "image/png", "audio/ogg"
    private Long fileSize;          // 文件大小（字节 Byte）
    // 渠道来源信息
    private String sourceChannel;  // 来源渠道
    private String sourceChatId;    // 来源会话 ID
    // 存储相关信息
    private String storageKey;     // 存储 key
    private String localPath;       // 本地路径
    // 媒体时长（音视频）
    private Long durationMs;        // 媒体时长（毫秒）
    // 关联消息 ID
    private String messageId;
    // 扩展元数据
    private java.util.Map<String, Object> extraMeta;
}
