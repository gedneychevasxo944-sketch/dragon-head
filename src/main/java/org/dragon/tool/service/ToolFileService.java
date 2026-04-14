package org.dragon.tool.service;

import org.dragon.tool.domain.ToolStorageInfoVO;
import org.dragon.tool.enums.ToolStorageType;

import java.util.Map;

/**
 * Tool 文件存储抽象接口。
 *
 * <p>屏蔽底层存储差异，调用方（{@link ToolRegisterService}）面向此接口编程，
 * 通过配置项 {@code tool.storage.type} 自动选择实现：
 * <ul>
 *   <li>{@code local}（默认）→ {@link LocalToolFileService}</li>
 *   <li>{@code s3}          → {@link S3ToolFileService}</li>
 * </ul>
 *
 * <p><b>路径约定</b>（两种实现共用）：
 * <pre>
 * tools/{toolId}/v{version}/{relativePath}
 * </pre>
 *
 * <p>调用方将文件以 {@code Map<相对路径, 字节内容>} 的形式传入 {@link #upload}，
 * 接口返回 {@link ToolStorageInfoVO} 供序列化后写入 {@link org.dragon.tool.domain.ToolVersionDO#getStorageInfo()}。
 *
 * <p>超大工具执行结果（超过 50,000 字符）也通过 {@link #storeResult} 写入相同的存储后端，
 * 返回存储路径字符串，供 {@code ToolExecutionRecordDO.ResultStorageMeta} 记录。
 */
public interface ToolFileService {

    /**
     * 返回当前实现对应的存储类型，供调用方填充
     * {@code ToolExecutionRecordDO.ResultStorageMeta#storageType}。
     *
     * @return {@link ToolStorageType#LOCAL} 或 {@link ToolStorageType#S3}
     */
    ToolStorageType getStorageType();

    /**
     * 将 fileMap 中所有文件上传到存储后端，返回 {@link ToolStorageInfoVO}。
     *
     * @param toolId  工具业务 UUID
     * @param version 本次版本号
     * @param fileMap 相对路径 → 文件字节内容（如 {@code "main.py" → bytes}）
     * @return 填充好的 {@link ToolStorageInfoVO}，供序列化后写入 {@code storage_info} 字段
     */
    ToolStorageInfoVO upload(String toolId, int version, Map<String, byte[]> fileMap);

    /**
     * 下载单个文件的字节内容（供运行时加载脚本或展示内容使用）。
     *
     * @param storageInfo  存储元信息（由 {@code storage_info} 字段反序列化而来）
     * @param relativePath 相对路径，如 {@code "main.py"}
     * @return 文件字节内容
     */
    byte[] download(ToolStorageInfoVO storageInfo, String relativePath);

    /**
     * 存储工具执行结果文本（用于超大结果落存）。
     *
     * <p>路径约定：
     * <pre>
     * tool-results/{sessionId}/{toolUseId}.txt
     * </pre>
     *
     * @param sessionId 会话 ID，用于结果隔离
     * @param toolUseId 工具调用 ID（全局唯一），作为文件名
     * @param content   结果文本内容
     * @return 存储路径字符串（本地文件为绝对路径，S3 为 {@code s3://bucket/key} 格式）
     */
    String storeResult(String sessionId, String toolUseId, String content);
}
