package org.dragon.memory.qmd;

import java.io.File;
import java.nio.file.Paths;

public class QmdDocPathResolver {

    public String resolveDocIdToPath(String docId) {
        // 简单实现：假设 docId 是相对于工作区根目录的文件路径
        // 在实际应用中，可能需要根据 qmd 的配置或数据库来解析路径
        File file = new File(docId);
        if (file.exists()) {
            return docId;
        }

        // 尝试在常见位置查找文件
        String[] searchPaths = {
                System.getProperty("user.home") + "/.config/qmd/index/" + docId,
                System.getProperty("user.dir") + "/" + docId
        };

        for (String path : searchPaths) {
            file = new File(path);
            if (file.exists()) {
                return path;
            }
        }

        return docId; // 如果找不到文件，返回原始 docId
    }

    public String resolveFileToDocId(String path) {
        // 简单实现：假设 docId 是相对于工作区根目录的文件路径
        String normalizedPath = Paths.get(path).normalize().toString();
        return normalizedPath;
    }
}