package org.dragon.workspace.material;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.dragon.material.Material;
import org.dragon.material.MaterialContentStore;
import org.dragon.material.MaterialEventPublisher;
import org.dragon.material.MaterialParser;
import org.dragon.material.MaterialStorage;
import org.dragon.material.MaterialStore;
import org.dragon.store.StoreFactory;
import org.dragon.task.Task;
import org.dragon.task.TaskStore;
import org.dragon.workspace.WorkspaceRegistry;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * WorkspaceMaterialService 物料管理服务
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkspaceMaterialService {

    private final MaterialStorage materialStorage;
    private final WorkspaceRegistry workspaceRegistry;
    private final List<MaterialParser> materialParsers;
    private final MaterialEventPublisher materialEventPublisher;
    private final StoreFactory storeFactory;

    private MaterialStore getMaterialStore() {
        return storeFactory.get(MaterialStore.class);
    }

    private TaskStore getTaskStore() {
        return storeFactory.get(TaskStore.class);
    }

    private MaterialContentStore getMaterialContentStore() {
        return storeFactory.get(MaterialContentStore.class);
    }

    /**
     * 根据物料类型选择合适的解析器
     */
    private MaterialParser selectParser(Material material) {
        String contentType = material.getType();
        for (MaterialParser parser : materialParsers) {
            if (parser.supportedTypes().contains(contentType)) {
                return parser;
            }
        }
        // 找不到匹配的解析器，返回默认解析器
        return materialParsers.stream()
                .filter(parser -> parser instanceof org.dragon.material.DefaultMaterialParser)
                .findFirst()
                .orElse(materialParsers.get(0));
    }

    /**
     * 上传物料
     *
     * @param workspaceId 工作空间 ID
     * @param inputStream 输入流
     * @param filename 文件名
     * @param size 文件大小
     * @param contentType 内容类型
     * @param uploader 上传者 ID
     * @return 物料
     */
    public Material upload(String workspaceId, InputStream inputStream, String filename,
                          long size, String contentType, String uploader) {
        // 验证工作空间存在
        workspaceRegistry.get(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + workspaceId));

        // 存储文件
        String storageKey = materialStorage.store(workspaceId, inputStream, filename);

        // 创建物料元数据
        Material material = Material.builder()
                .id(UUID.randomUUID().toString())
                .workspaceId(workspaceId)
                .name(filename)
                .size(size)
                .type(contentType)
                .storageKey(storageKey)
                .uploader(uploader)
                .uploadedAt(LocalDateTime.now())
                .build();

        getMaterialStore().save(material);
        log.info("[WorkspaceMaterialService] Uploaded material: {} to workspace: {}", material.getId(), workspaceId);

        // 发布上传事件，触发异步摘要生成
        materialEventPublisher.publishUploaded(material);

        return material;
    }

    /**
     * 下载物料
     *
     * @param materialId 物料 ID
     * @return 输入流
     */
    public InputStream download(String materialId) {
        Material material = getMaterialStore().findById(materialId)
                .orElseThrow(() -> new IllegalArgumentException("Material not found: " + materialId));

        InputStream inputStream = materialStorage.retrieve(material.getStorageKey());
        if (inputStream == null) {
            throw new IllegalStateException("Material content not found: " + materialId);
        }

        return inputStream;
    }

    /**
     * 获取物料元数据
     *
     * @param materialId 物料 ID
     * @return 物料
     */
    public Optional<Material> get(String materialId) {
        return getMaterialStore().findById(materialId);
    }

    /**
     * 删除物料
     *
     * @param materialId 物料 ID
     */
    public void delete(String materialId) {
        Material material = getMaterialStore().findById(materialId)
                .orElseThrow(() -> new IllegalArgumentException("Material not found: " + materialId));

        // 删除存储的内容
        materialStorage.delete(material.getStorageKey());

        // 删除元数据
        getMaterialStore().delete(materialId);
        log.info("[WorkspaceMaterialService] Deleted material: {}", materialId);
    }

    /**
     * 获取工作空间的所有物料
     *
     * @param workspaceId 工作空间 ID
     * @return 物料列表
     */
    public java.util.List<Material> listByWorkspace(String workspaceId) {
        return getMaterialStore().findByWorkspaceId(workspaceId);
    }

    // ==================== NormalizedFile 接入方法 ====================

    /**
     * 从 NormalizedFile 列表摄入物料
     * 用于将渠道附件接入 Workspace 物料系统
     *
     * @param workspaceId Workspace ID
     * @param files NormalizedFile 列表
     * @param uploader 上传者 ID
     * @param context 扩展上下文
     * @return 创建的物料列表
     */
    public java.util.List<Material> ingestNormalizedFiles(String workspaceId,
            java.util.List<org.dragon.channel.entity.NormalizedFile> files,
            String uploader, java.util.Map<String, Object> context) {
        if (files == null || files.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        java.util.List<Material> materials = new java.util.ArrayList<>();
        for (org.dragon.channel.entity.NormalizedFile file : files) {
            try {
                Material material = Material.builder()
                        .id(java.util.UUID.randomUUID().toString())
                        .workspaceId(workspaceId)
                        .name(file.getFileName() != null ? file.getFileName() : "unnamed")
                        .type(file.getMimeType())
                        .size(file.getFileSize() != null ? file.getFileSize() : 0)
                        .storageKey(file.getStorageKey())
                        .sourceChannel(file.getSourceChannel())
                        .sourceMessageId(file.getMessageId())
                        .uploader(uploader)
                        .uploadedAt(java.time.LocalDateTime.now())
                        .parseStatus("PENDING")
                        .build();

                getMaterialStore().save(material);
                // 发布上传事件，触发异步摘要生成
                materialEventPublisher.publishUploaded(material);
                materials.add(material);
                log.info("[WorkspaceMaterialService] Ingested NormalizedFile as material: {}", material.getId());
            } catch (Exception e) {
                log.error("[WorkspaceMaterialService] Failed to ingest NormalizedFile: {}", e.getMessage(), e);
            }
        }
        return materials;
    }

    /**
     * 解析物料
     *
     * @param materialId 物料 ID
     * @return 解析后的内容
     */
    public org.dragon.material.ParsedMaterialContent parseMaterial(String materialId) {
        Material material = getMaterialStore().findById(materialId)
                .orElseThrow(() -> new IllegalArgumentException("Material not found: " + materialId));

        java.io.InputStream inputStream = null;
        try {
            inputStream = materialStorage.retrieve(material.getStorageKey());
            if (inputStream == null) {
                return null;
            }
            MaterialParser.ParseResult result = selectParser(material).parse(material, inputStream);

            // 保存解析内容
            org.dragon.material.ParsedMaterialContent content =
                    org.dragon.material.ParsedMaterialContent.builder()
                            .id(java.util.UUID.randomUUID().toString())
                            .materialId(materialId)
                            .textContent(result.getTextContent())
                            .structuredContent(result.getStructuredContent())
                            .metadata(result.getMetadata())
                            .status(result.isSuccess()
                                    ? org.dragon.material.ParsedMaterialContent.ParseStatus.SUCCESS
                                    : org.dragon.material.ParsedMaterialContent.ParseStatus.FAILED)
                            .errorMessage(result.getErrorMessage())
                            .parsedAt(java.time.LocalDateTime.now())
                            .build();

            // 通过 MaterialContentStore 保存解析内容
            getMaterialContentStore().saveParsedContent(content);

            // 更新物料状态
            material.setParseStatus(content.getStatus().name());
            material.setParsedContentId(content.getId());
            getMaterialStore().update(material);

            return content;
        } catch (Exception e) {
            log.error("[WorkspaceMaterialService] Failed to parse material {}: {}", materialId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 获取解析后的内容
     *
     * @param materialId 物料 ID
     * @return 解析内容（如果存在）
     */
    public java.util.Optional<org.dragon.material.ParsedMaterialContent> getParsedContent(String materialId) {
        Material material = getMaterialStore().findById(materialId).orElse(null);
        if (material == null || material.getParsedContentId() == null) {
            return java.util.Optional.empty();
        }
        // 通过 MaterialContentStore 查询解析内容
        return getMaterialContentStore().findByMaterialId(materialId);
    }

    /**
     * 更新解析内容的摘要
     *
     * @param contentId 解析内容 ID
     * @param summary 摘要
     */
    public void updateParsedContentSummary(String contentId, String summary) {
        getMaterialContentStore().findById(contentId).ifPresent(content -> {
            content.setSummary(summary);
            getMaterialContentStore().update(content);
        });
    }

    // ==================== 任务关联方法 ====================

    /**
     * 上传并关联到任务
     *
     * @param workspaceId Workspace ID
     * @param inputStream 输入流
     * @param filename 文件名
     * @param size 文件大小
     * @param contentType 内容类型
     * @param uploader 上传者 ID
     * @param taskId 关联的任务 ID（可选）
     * @return 物料
     */
    public Material uploadAndAttachToTask(String workspaceId, InputStream inputStream,
            String filename, long size, String contentType, String uploader, String taskId) {
        // 上传物料
        Material material = upload(workspaceId, inputStream, filename, size, contentType, uploader);

        // 如果有关联的任务，附加解析结果
        if (taskId != null && !taskId.isEmpty()) {
            attachToTask(taskId, material);
        }

        return material;
    }

    /**
     * 附加物料到任务
     *
     * @param taskId 任务 ID
     * @param material 物料
     */
    public void attachToTask(String taskId, Material material) {
        Task task = getTaskStore().findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        // 添加物料 ID
        if (task.getMaterialIds() == null) {
            task.setMaterialIds(new java.util.ArrayList<>());
        }
        if (!task.getMaterialIds().contains(material.getId())) {
            task.getMaterialIds().add(material.getId());
            getTaskStore().update(task);
        }

        // 解析物料并存储结果
        try {
            InputStream inputStream = download(material.getId());
            MaterialParser.ParseResult parseResult = selectParser(material).parse(material, inputStream);

            // 将解析结果存储到 task metadata（不再追加到 task.input）
            if (task.getMetadata() == null) {
                task.setMetadata(new HashMap<>());
            }

            // 存储解析结果
            @SuppressWarnings("unchecked")
            Map<String, Object> materialResults = (Map<String, Object>) task.getMetadata()
                    .getOrDefault("materialResults", new HashMap<String, Object>());
            materialResults.put(material.getId(), parseResult);
            task.getMetadata().put("materialResults", materialResults);

            getTaskStore().update(task);
            log.info("[WorkspaceMaterialService] Attached material {} to task {}", material.getId(), taskId);

        } catch (Exception e) {
            log.error("[WorkspaceMaterialService] Failed to parse and attach material {} to task {}: {}",
                    material.getId(), taskId, e.getMessage());
        }
    }

    /**
     * 解析并关联多个物料到任务
     *
     * @param taskId 任务 ID
     * @param materialIds 物料 ID 列表
     */
    public void attachMaterialsToTask(String taskId, List<String> materialIds) {
        Task task = getTaskStore().findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        for (String materialId : materialIds) {
            Optional<Material> materialOpt = get(materialId);
            if (materialOpt.isPresent()) {
                attachToTask(taskId, materialOpt.get());
            }
        }
    }

    /**
     * 获取任务的物料列表
     *
     * @param taskId 任务 ID
     * @return 物料列表
     */
    public List<Material> getTaskMaterials(String taskId) {
        Task task = getTaskStore().findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        if (task.getMaterialIds() == null || task.getMaterialIds().isEmpty()) {
            return List.of();
        }

        return task.getMaterialIds().stream()
                .map(getMaterialStore()::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    /**
     * 解析任务的物料
     *
     * @param taskId 任务 ID
     * @return 解析结果映射
     */
    public Map<String, MaterialParser.ParseResult> parseTaskMaterials(String taskId) {
        Task task = getTaskStore().findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        if (task.getMetadata() == null || !task.getMetadata().containsKey("materialResults")) {
            return Map.of();
        }

        @SuppressWarnings("unchecked")
        Map<String, MaterialParser.ParseResult> results =
                (Map<String, MaterialParser.ParseResult>) task.getMetadata().get("materialResults");
        return results;
    }
}
