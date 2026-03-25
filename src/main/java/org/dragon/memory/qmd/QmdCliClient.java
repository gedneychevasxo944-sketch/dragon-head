package org.dragon.memory.qmd;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class QmdCliClient {

    public String executeCommand(String command, String[] args) {
        StringBuilder result = new StringBuilder();

        try {
            // 构建完整命令
            String[] fullCommand = new String[args.length + 1];
            fullCommand[0] = command;
            System.arraycopy(args, 0, fullCommand, 1, args.length);

            // 执行命令
            Process process = Runtime.getRuntime().exec(fullCommand);

            // 读取标准输出
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line).append("\n");
                }
            }

            // 读取错误输出（用于调试）
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.err.println("QMD Error: " + line);
                }
            }

            // 等待命令完成
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.err.println("QMD command failed with exit code: " + exitCode);
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Error executing QMD command: " + e.getMessage());
        }

        return result.toString();
    }
}