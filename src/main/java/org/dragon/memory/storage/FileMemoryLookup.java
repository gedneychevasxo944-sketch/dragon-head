package org.dragon.memory.storage;
import org.dragon.memory.entity.MemoryId;


import org.dragon.memory.entity.MemoryEntry;
import org.dragon.memory.constants.MemoryType;
import org.dragon.memory.constants.MemoryScope;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 文件系统记忆查找工具类
 * 提供便捷的文件系统记忆查找功能，支持按ID查找、类型查找、作用域查找等
 *
 * @author binarytom
 * @version 1.0
 */
public class FileMemoryLookup {
    private FileMemoryLookup() {
        // 工具类不允许实例化
    }

    /**
     * 按ID在指定目录中查找记忆条目
     *
     * @param memDir 记忆文件目录
     * @param memoryId 记忆ID
     * @param markdownParser Markdown解析器
     * @return 找到的记忆条目
     */
    public static Optional<MemoryEntry> findById(Path memDir, MemoryId memoryId, MemoryMarkdownParser markdownParser) {
        List<Path> mdFiles = listMarkdownFiles(memDir);
        for (Path mdFile : mdFiles) {
            try {
                String content = FileIO.readFile(mdFile);
                MemoryEntry entry = markdownParser.parse(content);
                if (entry.getId() != null && memoryId.equals(entry.getId())) {
                    entry.setFileName(mdFile.getFileName().toString());
                    entry.setFilePath(mdFile.toAbsolutePath().toString());
                    return Optional.of(entry);
                }
            } catch (Exception e) {
                // 忽略解析错误的文件
            }
        }
        return Optional.empty();
    }

    /**
     * 按ID在指定目录中查找记忆条目
     *
     * @param memDir 记忆文件目录（字符串）
     * @param memoryId 记忆ID
     * @param markdownParser Markdown解析器
     * @return 找到的记忆条目
     */
    public static Optional<MemoryEntry> findById(String memDir, String memoryId, MemoryMarkdownParser markdownParser) {
        return findById(Paths.get(memDir), MemoryId.of(memoryId), markdownParser);
    }

    /**
     * 列出指定目录下的所有记忆条目
     *
     * @param memDir 记忆文件目录
     * @param markdownParser Markdown解析器
     * @return 记忆条目列表
     */
    public static List<MemoryEntry> list(Path memDir, MemoryMarkdownParser markdownParser) {
        List<MemoryEntry> entries = new ArrayList<>();
        List<Path> mdFiles = listMarkdownFiles(memDir);
        for (Path mdFile : mdFiles) {
            try {
                String content = FileIO.readFile(mdFile);
                MemoryEntry entry = markdownParser.parse(content);
                entry.setFileName(mdFile.getFileName().toString());
                entry.setFilePath(mdFile.toAbsolutePath().toString());
                entries.add(entry);
            } catch (Exception e) {
                // 忽略解析错误的文件
            }
        }
        return entries;
    }

    /**
     * 列出指定目录下的所有记忆条目
     *
     * @param memDir 记忆文件目录（字符串）
     * @param markdownParser Markdown解析器
     * @return 记忆条目列表
     */
    public static List<MemoryEntry> list(String memDir, MemoryMarkdownParser markdownParser) {
        return list(Paths.get(memDir), markdownParser);
    }

    /**
     * 按类型查找记忆条目
     *
     * @param memDir 记忆文件目录
     * @param type 记忆类型
     * @param markdownParser Markdown解析器
     * @return 找到的记忆条目列表
     */
    public static List<MemoryEntry> findByType(Path memDir, MemoryType type, MemoryMarkdownParser markdownParser) {
        return list(memDir, markdownParser).stream()
                .filter(entry -> type.equals(entry.getType()))
                .collect(Collectors.toList());
    }

    /**
     * 按类型查找记忆条目
     *
     * @param memDir 记忆文件目录（字符串）
     * @param type 记忆类型
     * @param markdownParser Markdown解析器
     * @return 找到的记忆条目列表
     */
    public static List<MemoryEntry> findByType(String memDir, MemoryType type, MemoryMarkdownParser markdownParser) {
        return findByType(Paths.get(memDir), type, markdownParser);
    }

    /**
     * 按作用域查找记忆条目
     *
     * @param memDir 记忆文件目录
     * @param scope 记忆作用域
     * @param markdownParser Markdown解析器
     * @return 找到的记忆条目列表
     */
    public static List<MemoryEntry> findByScope(Path memDir, MemoryScope scope, MemoryMarkdownParser markdownParser) {
        return list(memDir, markdownParser).stream()
                .filter(entry -> scope.equals(entry.getScope()))
                .collect(Collectors.toList());
    }

    /**
     * 按作用域查找记忆条目
     *
     * @param memDir 记忆文件目录（字符串）
     * @param scope 记忆作用域
     * @param markdownParser Markdown解析器
     * @return 找到的记忆条目列表
     */
    public static List<MemoryEntry> findByScope(String memDir, MemoryScope scope, MemoryMarkdownParser markdownParser) {
        return findByScope(Paths.get(memDir), scope, markdownParser);
    }

    /**
     * 删除指定ID的记忆文件
     *
     * @param memDir 记忆文件目录
     * @param memoryId 记忆ID
     * @param markdownParser Markdown解析器
     * @return 是否删除成功
     */
    public static boolean delete(Path memDir, String memoryId, MemoryMarkdownParser markdownParser) {
        Optional<MemoryEntry> entryOpt = findById(memDir, MemoryId.of(memoryId), markdownParser);
        if (entryOpt.isPresent()) {
            Path filePath = Paths.get(entryOpt.get().getFilePath());
            return FileIO.deleteFile(filePath);
        }
        return false;
    }

    /**
     * 删除指定ID的记忆文件
     *
     * @param memDir 记忆文件目录（字符串）
     * @param memoryId 记忆ID
     * @param markdownParser Markdown解析器
     * @return 是否删除成功
     */
    public static boolean delete(String memDir, String memoryId, MemoryMarkdownParser markdownParser) {
        return delete(Paths.get(memDir), memoryId, markdownParser);
    }

    /**
     * 列出目录下的所有Markdown文件
     *
     * @param dir 目录路径
     * @return Markdown文件路径列表
     */
    private static List<Path> listMarkdownFiles(Path dir) {
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            return new ArrayList<>();
        }
        try {
            return Files.list(dir)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".md"))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * 搜索包含指定关键词的记忆条目
     *
     * @param memDir 记忆文件目录
     * @param keyword 关键词
     * @param markdownParser Markdown解析器
     * @return 匹配的记忆条目列表
     */
    public static List<MemoryEntry> search(Path memDir, String keyword, MemoryMarkdownParser markdownParser) {
        String lowerKeyword = keyword.toLowerCase();
        return list(memDir, markdownParser).stream()
                .filter(entry -> matchesKeyword(entry, lowerKeyword))
                .collect(Collectors.toList());
    }

    /**
     * 搜索包含指定关键词的记忆条目
     *
     * @param memDir 记忆文件目录（字符串）
     * @param keyword 关键词
     * @param markdownParser Markdown解析器
     * @return 匹配的记忆条目列表
     */
    public static List<MemoryEntry> search(String memDir, String keyword, MemoryMarkdownParser markdownParser) {
        return search(Paths.get(memDir), keyword, markdownParser);
    }

    /**
     * 检查记忆条目是否包含关键词
     *
     * @param entry 记忆条目
     * @param lowerKeyword 小写关键词
     * @return 是否匹配
     */
    private static boolean matchesKeyword(MemoryEntry entry, String lowerKeyword) {
        return (entry.getTitle() != null && entry.getTitle().toLowerCase().contains(lowerKeyword)) ||
               (entry.getDescription() != null && entry.getDescription().toLowerCase().contains(lowerKeyword)) ||
               (entry.getContent() != null && entry.getContent().toLowerCase().contains(lowerKeyword));
    }
}
