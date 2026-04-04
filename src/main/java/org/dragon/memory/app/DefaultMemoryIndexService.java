package org.dragon.memory.app;

import org.dragon.memory.core.MemoryIndexItem;
import org.dragon.memory.core.MemoryIndexService;
import org.dragon.memory.core.MemoryEntry;
import org.dragon.memory.storage.fs.FileCharacterMemoryRepository;
import org.dragon.memory.storage.fs.FileWorkspaceMemoryRepository;
import org.dragon.memory.storage.MemoryIndexParser;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 记忆索引服务默认实现
 * 负责管理记忆索引的创建、查询和更新
 *
 * @author binarytom
 * @version 1.0
 */
@Service
public class DefaultMemoryIndexService implements MemoryIndexService {
    private final FileCharacterMemoryRepository fileCharacterMemoryRepository;
    private final FileWorkspaceMemoryRepository fileWorkspaceMemoryRepository;
    private final MemoryIndexParser indexParser;

    public DefaultMemoryIndexService(FileCharacterMemoryRepository fileCharacterMemoryRepository,
                                     FileWorkspaceMemoryRepository fileWorkspaceMemoryRepository,
                                     MemoryIndexParser indexParser) {
        this.fileCharacterMemoryRepository = fileCharacterMemoryRepository;
        this.fileWorkspaceMemoryRepository = fileWorkspaceMemoryRepository;
        this.indexParser = indexParser;
    }

    @Override
    public void rebuildCharacterIndex(String characterId) {
        fileCharacterMemoryRepository.rebuildIndex(characterId);
    }

    @Override
    public void rebuildWorkspaceIndex(String workspaceId) {
        fileWorkspaceMemoryRepository.rebuildIndex(workspaceId);
    }

    @Override
    public List<MemoryIndexItem> queryCharacterIndex(String characterId) {
        List<MemoryEntry> entries = fileCharacterMemoryRepository.list(characterId);
        return convertToIndexItems(entries);
    }

    @Override
    public List<MemoryIndexItem> queryWorkspaceIndex(String workspaceId) {
        List<MemoryEntry> entries = fileWorkspaceMemoryRepository.list(workspaceId);
        return convertToIndexItems(entries);
    }

    @Override
    public List<MemoryIndexItem> searchCharacterIndex(String characterId, String keyword) {
        List<MemoryIndexItem> indexItems = queryCharacterIndex(characterId);
        return searchIndexItems(indexItems, keyword);
    }

    @Override
    public List<MemoryIndexItem> searchWorkspaceIndex(String workspaceId, String keyword) {
        List<MemoryIndexItem> indexItems = queryWorkspaceIndex(workspaceId);
        return searchIndexItems(indexItems, keyword);
    }

    /**
     * 将 MemoryEntry 转换为 MemoryIndexItem
     */
    private List<MemoryIndexItem> convertToIndexItems(List<MemoryEntry> entries) {
        return entries.stream().map(entry -> {
            MemoryIndexItem item = new MemoryIndexItem();
            item.setMemoryId(entry.getId());
            item.setTitle(entry.getTitle());
            item.setRelativePath(entry.getFileName());
            item.setSummaryLine(entry.getDescription());
            item.setType(entry.getType());
            item.setUpdatedAt(entry.getUpdatedAt());
            return item;
        }).collect(Collectors.toList());
    }

    /**
     * 搜索索引条目
     */
    private List<MemoryIndexItem> searchIndexItems(List<MemoryIndexItem> indexItems, String keyword) {
        String lowerKeyword = keyword.toLowerCase();
        return indexItems.stream()
                .filter(item -> item.getTitle().toLowerCase().contains(lowerKeyword) ||
                        item.getSummaryLine().toLowerCase().contains(lowerKeyword))
                .collect(Collectors.toList());
    }
}
