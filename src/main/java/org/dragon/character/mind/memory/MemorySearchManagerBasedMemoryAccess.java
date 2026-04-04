//package org.dragon.character.mind.memory;
//
//import org.dragon.memory.MemorySearchManager;
//import org.dragon.memory.MemorySearchManagerFactory;
//import org.dragon.memory.models.MemorySearchResult;
//import org.dragon.memory.models.SearchOptions;
//import org.springframework.stereotype.Component;
//
//import java.time.LocalDateTime;
//import java.util.Collections;
//import java.util.List;
//import java.util.Map;
//import java.util.stream.Collectors;
//
///**
// * 基于 MemorySearchManager 的 MemoryAccess 实现
// * 将 Memory 模块的功能与 Character 记忆系统连接起来
// *
// * @author wyj
// * @version 1.0
// */
//@Component("memorySearchManagerBasedMemoryAccess")
//public class MemorySearchManagerBasedMemoryAccess implements MemoryAccess {
//
//    private final MemorySearchManager memorySearchManager;
//
//    public MemorySearchManagerBasedMemoryAccess(MemorySearchManagerFactory memorySearchManagerFactory) {
//        // 使用默认配置创建 MemorySearchManager 实例
//        this.memorySearchManager = memorySearchManagerFactory.getMemorySearchManager(Map.of());
//    }
//
//    private String characterId;
//
//    @Override
//    public String getCharacterId() {
//        return characterId;
//    }
//
//    public void setCharacterId(String characterId) {
//        this.characterId = characterId;
//    }
//
//    @Override
//    public List<Memory> semanticSearch(String query, int topK) {
//        SearchOptions searchOptions = new SearchOptions();
//        searchOptions.setMaxResults(topK);
//        List<MemorySearchResult> searchResults = memorySearchManager.search(query, searchOptions);
//
//        // 将 MemorySearchResult 转换为 Memory 对象
//        // 这里只是一个简单的转换实现，实际项目中可能需要更复杂的映射
//        return searchResults.stream()
//                .map(result -> Memory.builder()
//                        .id("dummy-memory-id-" + result.getPath())
//                        .type(Memory.MemoryType.SYSTEM)
//                        .content(result.getSnippet())
//                        .timestamp(LocalDateTime.now())
//                        .build())
//                .collect(Collectors.toList());
//    }
//
//    @Override
//    public List<Memory> getByType(Memory.MemoryType type, LocalDateTime from, LocalDateTime to) {
//        // TODO: 实现按类型和时间范围的检索
//        return Collections.emptyList();
//    }
//
//    @Override
//    public void store(Memory memory) {
//        // TODO: 实现记忆存储功能
//        // 这里可以考虑将记忆存储到知识库或其他持久化层
//    }
//
//    @Override
//    public void storeBatch(List<Memory> memories) {
//        // TODO: 实现批量存储功能
//        memories.forEach(this::store);
//    }
//
//    @Override
//    public Memory get(String memoryId) {
//        // TODO: 实现记忆获取功能
//        return null;
//    }
//
//    @Override
//    public void delete(String memoryId) {
//        // TODO: 实现记忆删除功能
//    }
//
//    @Override
//    public void clear() {
//        // TODO: 实现清空功能
//    }
//
//    @Override
//    public int count() {
//        // TODO: 实现计数功能
//        return 0;
//    }
//}
