package org.dragon.memory.builtin;

import org.dragon.memory.config.ResolvedMemorySearchConfig;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MemoryFileScanner {

    private final ResolvedMemorySearchConfig searchConfig;

    public MemoryFileScanner(ResolvedMemorySearchConfig searchConfig) {
        this.searchConfig = searchConfig;
    }

    public List<String> scanMemoryFiles() {
        List<String> files = new ArrayList<>();
        String currentDir = System.getProperty("user.dir");

        // 扫描 MEMORY.md 和 memory.md
        scanFile(currentDir, "MEMORY.md", files);
        scanFile(currentDir, "memory.md", files);

        // 扫描 memory/ 目录
        File memoryDir = new File(currentDir + File.separator + "memory");
        if (memoryDir.exists() && memoryDir.isDirectory()) {
            scanDirectory(memoryDir, files);
        }

        // 扫描额外路径
        if (searchConfig.getExtraPaths() != null) {
            for (String extraPath : searchConfig.getExtraPaths()) {
                File extraDir = new File(extraPath);
                if (extraDir.exists() && extraDir.isDirectory()) {
                    scanDirectory(extraDir, files);
                } else {
                    scanFile(extraPath, "", files);
                }
            }
        }

        return files;
    }

    private void scanDirectory(File directory, List<String> files) {
        File[] subFiles = directory.listFiles();
        if (subFiles == null) {
            return;
        }

        for (File file : subFiles) {
            if (file.isDirectory()) {
                // 跳过 .git、node_modules 等目录
                if (shouldSkipFile(file)) {
                    continue;
                }
                scanDirectory(file, files);
            } else {
                if (shouldSkipFile(file)) {
                    continue;
                }
                // 只扫描 Markdown 文件
                if (file.getName().endsWith(".md")) {
                    files.add(file.getAbsolutePath());
                }
            }
        }
    }

    private void scanFile(String directory, String filename, List<String> files) {
        File file = new File(directory + File.separator + filename);
        if (file.exists() && !shouldSkipFile(file)) {
            files.add(file.getAbsolutePath());
        }
    }

    public boolean shouldSkipFile(File file) {
        String fileName = file.getName();
        return fileName.startsWith(".") || // 隐藏文件/目录
                fileName.equals("node_modules") ||
                fileName.equals(".git") ||
                fileName.equals(".svn") ||
                fileName.equals(".hg") ||
                fileName.equals("CVS");
    }
}
