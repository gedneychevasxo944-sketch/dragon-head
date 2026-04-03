package org.dragon.memv2.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

/**
 * 文件操作工具类
 * 提供便捷的文件读写、目录创建等操作方法
 *
 * @author binarytom
 * @version 1.0
 */
public class FileIO {
    private FileIO() {
        // 工具类不允许实例化
    }

    /**
     * 确保目录存在，不存在则创建
     *
     * @param dirPath 目录路径
     * @return 创建好的目录路径
     */
    public static Path ensureDirectory(String dirPath) {
        return ensureDirectory(Paths.get(dirPath));
    }

    /**
     * 确保目录存在，不存在则创建
     *
     * @param dirPath 目录路径
     * @return 创建好的目录路径
     */
    public static Path ensureDirectory(Path dirPath) {
        if (!Files.exists(dirPath)) {
            try {
                Files.createDirectories(dirPath);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create directory: " + dirPath, e);
            }
        }
        return dirPath;
    }

    /**
     * 读取文件内容
     *
     * @param filePath 文件路径
     * @return 文件内容字符串
     */
    public static String readFile(String filePath) {
        return readFile(Paths.get(filePath));
    }

    /**
     * 读取文件内容
     *
     * @param filePath 文件路径
     * @return 文件内容字符串
     */
    public static String readFile(Path filePath) {
        try {
            if (!Files.exists(filePath)) {
                return null;
            }
            return Files.readString(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file: " + filePath, e);
        }
    }

    /**
     * 写入文件内容（覆盖模式）
     *
     * @param filePath 文件路径
     * @param content 文件内容
     */
    public static void writeFile(String filePath, String content) {
        writeFile(Paths.get(filePath), content);
    }

    /**
     * 写入文件内容（覆盖模式）
     *
     * @param filePath 文件路径
     * @param content 文件内容
     */
    public static void writeFile(Path filePath, String content) {
        ensureDirectory(filePath.getParent());
        try {
            Files.writeString(filePath, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write file: " + filePath, e);
        }
    }

    /**
     * 追加写入文件内容
     *
     * @param filePath 文件路径
     * @param content 追加的内容
     */
    public static void appendFile(String filePath, String content) {
        appendFile(Paths.get(filePath), content);
    }

    /**
     * 追加写入文件内容
     *
     * @param filePath 文件路径
     * @param content 追加的内容
     */
    public static void appendFile(Path filePath, String content) {
        ensureDirectory(filePath.getParent());
        try {
            Files.writeString(filePath, content, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new RuntimeException("Failed to append file: " + filePath, e);
        }
    }

    /**
     * 读取文件所有行
     *
     * @param filePath 文件路径
     * @return 所有行的列表
     */
    public static List<String> readAllLines(String filePath) {
        return readAllLines(Paths.get(filePath));
    }

    /**
     * 读取文件所有行
     *
     * @param filePath 文件路径
     * @return 所有行的列表
     */
    public static List<String> readAllLines(Path filePath) {
        try {
            if (!Files.exists(filePath)) {
                return List.of();
            }
            return Files.readAllLines(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file lines: " + filePath, e);
        }
    }

    /**
     * 写入多行内容
     *
     * @param filePath 文件路径
     * @param lines 行内容列表
     */
    public static void writeAllLines(String filePath, List<String> lines) {
        writeAllLines(Paths.get(filePath), lines);
    }

    /**
     * 写入多行内容
     *
     * @param filePath 文件路径
     * @param lines 行内容列表
     */
    public static void writeAllLines(Path filePath, List<String> lines) {
        ensureDirectory(filePath.getParent());
        try {
            Files.write(filePath, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write file lines: " + filePath, e);
        }
    }

    /**
     * 检查文件是否存在
     *
     * @param filePath 文件路径
     * @return 是否存在
     */
    public static boolean fileExists(String filePath) {
        return fileExists(Paths.get(filePath));
    }

    /**
     * 检查文件是否存在
     *
     * @param filePath 文件路径
     * @return 是否存在
     */
    public static boolean fileExists(Path filePath) {
        return Files.exists(filePath) && Files.isRegularFile(filePath);
    }

    /**
     * 删除文件
     *
     * @param filePath 文件路径
     * @return 是否删除成功
     */
    public static boolean deleteFile(String filePath) {
        return deleteFile(Paths.get(filePath));
    }

    /**
     * 删除文件
     *
     * @param filePath 文件路径
     * @return 是否删除成功
     */
    public static boolean deleteFile(Path filePath) {
        try {
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                return true;
            }
            return false;
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete file: " + filePath, e);
        }
    }

    /**
     * 获取文件大小
     *
     * @param filePath 文件路径
     * @return 文件大小（字节）
     */
    public static long getFileSize(String filePath) {
        return getFileSize(Paths.get(filePath));
    }

    /**
     * 获取文件大小
     *
     * @param filePath 文件路径
     * @return 文件大小（字节）
     */
    public static long getFileSize(Path filePath) {
        try {
            if (!Files.exists(filePath)) {
                return 0;
            }
            return Files.size(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to get file size: " + filePath, e);
        }
    }

    /**
     * 列出目录下的所有文件
     *
     * @param dirPath 目录路径
     * @return 文件路径列表
     */
    public static List<Path> listFiles(String dirPath) {
        return listFiles(Paths.get(dirPath));
    }

    /**
     * 列出目录下的所有文件
     *
     * @param dirPath 目录路径
     * @return 文件路径列表
     */
    public static List<Path> listFiles(Path dirPath) {
        try {
            if (!Files.exists(dirPath) || !Files.isDirectory(dirPath)) {
                return List.of();
            }
            return Files.list(dirPath)
                    .filter(Files::isRegularFile)
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException("Failed to list files in directory: " + dirPath, e);
        }
    }
}
