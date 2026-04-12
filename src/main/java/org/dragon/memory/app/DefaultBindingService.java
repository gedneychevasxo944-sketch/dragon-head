package org.dragon.memory.app;

import lombok.extern.slf4j.Slf4j;
import org.dragon.api.controller.dto.PageResponse;
import org.dragon.api.controller.dto.memory.BindingDTO;
import org.dragon.api.controller.dto.memory.CreateBindingRequest;
import org.dragon.api.controller.dto.memory.UpdateBindingRequest;
import org.dragon.datasource.entity.BindingEntity;
import org.dragon.memory.core.BindingService;
import org.dragon.memory.core.MemoryEntry;
import org.dragon.memory.core.MemoryId;
import org.dragon.memory.core.MemoryIndexItem;
import org.dragon.memory.core.MemoryScope;
import org.dragon.memory.core.MemoryType;
import org.dragon.memory.storage.BindingsYmlManager;
import org.dragon.memory.storage.MemoryIndexParser;
import org.dragon.memory.storage.MemoryMarkdownParser;
import org.dragon.memory.storage.MemoryPathResolver;
import org.dragon.memory.store.BindingStore;
import org.dragon.store.StoreFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 绑定关系服务实现类
 * 负责将 chunk 文件绑定到 character/workspace 记忆空间，
 * 在目标目录生成快照文件并维护 MEMORY.md 索引和 bindings.yml
 *
 * @author binarytom
 * @version 1.0
 */
@Slf4j
@Service
public class DefaultBindingService implements BindingService {

    private final MemoryPathResolver pathResolver;
    private final MemoryMarkdownParser markdownParser;
    private final MemoryIndexParser indexParser;
    private final BindingsYmlManager bindingsYmlManager;
    private final BindingStore bindingStore;

    public DefaultBindingService(MemoryPathResolver pathResolver,
                                 MemoryMarkdownParser markdownParser,
                                 MemoryIndexParser indexParser,
                                 BindingsYmlManager bindingsYmlManager,
                                 StoreFactory storeFactory) {
        this.pathResolver = pathResolver;
        this.markdownParser = markdownParser;
        this.indexParser = indexParser;
        this.bindingsYmlManager = bindingsYmlManager;
        this.bindingStore = storeFactory.get(BindingStore.class);
    }

    @Override
    public PageResponse<BindingDTO> getBindings(String fileId, String targetType, String targetId, int page, int pageSize) {
        List<BindingEntity> all;
        if (fileId != null && !fileId.isBlank()) {
            all = bindingStore.findByFileId(fileId);
        } else if (targetType != null && targetId != null) {
            all = bindingStore.findByTarget(targetType, targetId);
        } else {
            // fileId 和 target 均未指定时按 fileId 空串查，返回空
            all = List.of();
        }

        // 内存分页
        int total = all.size();
        int fromIndex = Math.max(0, (page - 1) * pageSize);
        int toIndex = Math.min(fromIndex + pageSize, total);
        List<BindingDTO> pageList = all.subList(fromIndex, toIndex)
                .stream()
                .map(this::entityToDto)
                .collect(Collectors.toList());

        return PageResponse.<BindingDTO>builder()
                .list(pageList)
                .total((long) total)
                .page(page)
                .pageSize(pageSize)
                .build();
    }

    @Override
    public BindingDTO getBinding(String bindingId) {
        return bindingStore.findById(bindingId)
                .map(this::entityToDto)
                .orElse(null);
    }

    @Override
    public BindingDTO createBinding(CreateBindingRequest request) {
        log.info("[DefaultBindingService] Creating binding: fileId={}, targetType={}, targetId={}",
                request.getFileId(), request.getTargetType(), request.getTargetId());

        String bindingId = UUID.randomUUID().toString();
        MemoryId memoryId = MemoryId.generate();

        // 1. 生成快照文件名（mem/<bindingId>.md）
        String snapshotFileName = "mem/" + bindingId + ".md";
        String snapshotFileBaseName = bindingId + ".md";

        // 2. 构建 MemoryEntry 用于生成快照内容
        MemoryEntry entry = buildMemoryEntry(memoryId, request);

        // 3. 确定目标目录并写入快照文件
        Path memDir = resolveMemDir(request.getTargetType(), request.getTargetId());
        Path indexPath = resolveIndexPath(request.getTargetType(), request.getTargetId());
        Path bindingsPath = resolveBindingsPath(request.getTargetType(), request.getTargetId());

        writeSnapshotFile(memDir, snapshotFileBaseName, entry);

        // 4. 更新 MEMORY.md 索引
        appendToIndex(indexPath, entry, snapshotFileBaseName);

        // 5. 更新 bindings.yml
        String bindingsKey = (request.getFileId() != null ? request.getFileId() : "unknown")
                + "-" + memoryId.getValue();
        bindingsYmlManager.writeBinding(bindingsPath, bindingsKey, snapshotFileName);

        // 6. 持久化绑定关系
        BindingEntity entity = BindingEntity.builder()
                .id(bindingId)
                .fileId(request.getFileId())
                .chunkId(request.getSelectedChunkIds() != null && !request.getSelectedChunkIds().isEmpty()
                        ? request.getSelectedChunkIds().get(0) : null)
                .targetType(request.getTargetType())
                .targetId(request.getTargetId())
                .targetName(request.getTargetName())
                .mountType(request.getMountType())
                .snapshotFileName(snapshotFileName)
                .sourceId(request.getFileId())
                .memoryId(memoryId.getValue())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        bindingStore.save(entity);

        log.info("[DefaultBindingService] Binding created: id={}, snapshot={}", bindingId, snapshotFileName);
        return entityToDto(entity);
    }

    @Override
    public BindingDTO updateBinding(String bindingId, UpdateBindingRequest request) {
        Optional<BindingEntity> opt = bindingStore.findById(bindingId);
        if (opt.isEmpty()) {
            log.warn("[DefaultBindingService] Binding not found for update: {}", bindingId);
            return null;
        }
        BindingEntity entity = opt.get();
        entity.setMountType(request.getMountType());
        entity.setUpdatedAt(Instant.now());
        bindingStore.save(entity);
        return entityToDto(entity);
    }

    @Override
    public boolean deleteBinding(String bindingId) {
        log.info("[DefaultBindingService] Deleting binding: {}", bindingId);
        Optional<BindingEntity> opt = bindingStore.findById(bindingId);
        if (opt.isEmpty()) {
            log.warn("[DefaultBindingService] Binding not found for delete: {}", bindingId);
            return false;
        }
        BindingEntity entity = opt.get();

        // 1. 删除快照文件
        Path memDir = resolveMemDir(entity.getTargetType(), entity.getTargetId());
        if (entity.getSnapshotFileName() != null) {
            String baseName = entity.getSnapshotFileName().replaceFirst("^mem/", "");
            Path snapshotFile = memDir.resolve(baseName);
            try {
                Files.deleteIfExists(snapshotFile);
            } catch (IOException e) {
                log.error("[DefaultBindingService] Failed to delete snapshot file: {}", snapshotFile, e);
            }
        }

        // 2. 从 MEMORY.md 移除索引项
        Path indexPath = resolveIndexPath(entity.getTargetType(), entity.getTargetId());
        removeFromIndex(indexPath, bindingId);

        // 3. 从 bindings.yml 移除记录
        Path bindingsPath = resolveBindingsPath(entity.getTargetType(), entity.getTargetId());
        if (entity.getSourceId() != null && entity.getMemoryId() != null) {
            String bindingsKey = entity.getSourceId() + "-" + entity.getMemoryId();
            bindingsYmlManager.deleteBinding(bindingsPath, bindingsKey);
        }

        // 4. 从 store 删除
        return bindingStore.deleteById(bindingId);
    }

    // ---- 私有辅助方法 ----

    private MemoryEntry buildMemoryEntry(MemoryId memoryId, CreateBindingRequest request) {
        MemoryScope scope = "workspace".equalsIgnoreCase(request.getTargetType())
                ? MemoryScope.WORKSPACE : MemoryScope.CHARACTER;
        return MemoryEntry.builder()
                .id(memoryId)
                .title("绑定记忆: " + (request.getTargetName() != null ? request.getTargetName() : request.getTargetId()))
                .description("来自文件 " + request.getFileId() + " 的绑定记忆快照")
                .content("来源文件: " + request.getFileId() + "\n挂载类型: " + request.getMountType())
                .type(MemoryType.REFERENCE)
                .scope(scope)
                .ownerId(request.getTargetId())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private void writeSnapshotFile(Path memDir, String fileName, MemoryEntry entry) {
        try {
            if (!Files.exists(memDir)) {
                Files.createDirectories(memDir);
            }
            Files.writeString(memDir.resolve(fileName), markdownParser.render(entry));
        } catch (IOException e) {
            throw new RuntimeException("Failed to write snapshot file: " + fileName, e);
        }
    }

    private void appendToIndex(Path indexPath, MemoryEntry entry, String snapshotFileBaseName) {
        try {
            List<MemoryIndexItem> items;
            if (Files.exists(indexPath)) {
                String existing = Files.readString(indexPath);
                items = indexParser.parse(existing);
            } else {
                items = new java.util.ArrayList<>();
            }
            MemoryIndexItem newItem = MemoryIndexItem.builder()
                    .memoryId(entry.getId())
                    .title(entry.getTitle())
                    .relativePath(snapshotFileBaseName)
                    .summaryLine(entry.getDescription())
                    .type(entry.getType())
                    .updatedAt(entry.getUpdatedAt())
                    .build();
            items.add(newItem);
            if (!Files.exists(indexPath.getParent())) {
                Files.createDirectories(indexPath.getParent());
            }
            Files.writeString(indexPath, indexParser.render(items));
        } catch (IOException e) {
            throw new RuntimeException("Failed to update MEMORY.md: " + indexPath, e);
        }
    }

    private void removeFromIndex(Path indexPath, String bindingId) {
        if (!Files.exists(indexPath)) {
            return;
        }
        try {
            String existing = Files.readString(indexPath);
            List<MemoryIndexItem> items = indexParser.parse(existing);
            // 移除 relativePath 包含 bindingId 的条目
            items.removeIf(item -> item.getRelativePath() != null
                    && item.getRelativePath().contains(bindingId));
            Files.writeString(indexPath, indexParser.render(items));
        } catch (IOException e) {
            log.error("[DefaultBindingService] Failed to remove index entry for binding: {}", bindingId, e);
        }
    }

    private Path resolveMemDir(String targetType, String targetId) {
        if ("workspace".equalsIgnoreCase(targetType)) {
            return pathResolver.resolveWorkspaceMemDir(targetId);
        }
        return pathResolver.resolveCharacterMemDir(targetId);
    }

    private Path resolveIndexPath(String targetType, String targetId) {
        if ("workspace".equalsIgnoreCase(targetType)) {
            return pathResolver.resolveWorkspaceIndex(targetId);
        }
        return pathResolver.resolveCharacterIndex(targetId);
    }

    private Path resolveBindingsPath(String targetType, String targetId) {
        if ("workspace".equalsIgnoreCase(targetType)) {
            return pathResolver.resolveWorkspaceBindings(targetId);
        }
        return pathResolver.resolveCharacterBindings(targetId);
    }

    private BindingDTO entityToDto(BindingEntity entity) {
        return BindingDTO.builder()
                .id(entity.getId())
                .fileId(entity.getFileId())
                .targetType(entity.getTargetType())
                .targetId(entity.getTargetId())
                .targetName(entity.getTargetName())
                .mountType(entity.getMountType())
                .mountedAt(entity.getCreatedAt())
                .build();
    }
}