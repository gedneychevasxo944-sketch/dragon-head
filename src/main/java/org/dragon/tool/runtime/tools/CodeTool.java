package org.dragon.tool.runtime.tools;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.dragon.tool.runtime.AbstractTool;
import org.dragon.tool.runtime.ToolProgress;
import org.dragon.tool.runtime.ToolResult;
import org.dragon.tool.runtime.ToolResultBlockParam;
import org.dragon.tool.runtime.ToolDefinition;
import org.dragon.tool.runtime.ToolUseContext;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * CODE 类型工具。
 *
 * <p>执行用户上传的脚本（Python / Shell 等）。
 * 执行配置来自 {@link ToolDefinition#getExecutionConfig()}：
 * <pre>
 * {
 *   "language":      "python",
 *   "scriptContent": "def main(params): ...",
 *   "entrypoint":    "main"
 * }
 * </pre>
 *
 * <p>实例由 {@link org.dragon.tool.runtime.factory.CodeToolFactory} 创建。
 * CODE 工具无调用级内部状态，实例可跨调用复用（Factory 声明 {@code isSingleton=true}）。
 *
 * <p>TODO: 沙箱隔离（Docker / gVisor）。
 */
@Slf4j
public class CodeTool extends AbstractTool<JsonNode, String> {

    private static final int TIMEOUT_SECONDS = 30;

    private final String language;
    private final String scriptContent;
    private final String toolId;

    /**
     * @param runtime 工具运行时快照（提供 name / description / executionConfig）
     */
    public CodeTool(ToolDefinition runtime) {
        super(runtime.getName(), runtime.getDescription(), JsonNode.class);
        this.toolId = runtime.getToolId();

        JsonNode config = runtime.getExecutionConfig();
        this.language = config != null ? config.path("language").asText("python") : "python";
        this.scriptContent = config != null ? config.path("scriptContent").asText(null) : null;
    }

    @Override
    protected CompletableFuture<ToolResult<String>> doCall(JsonNode input,
                                                           ToolUseContext context,
                                                           Consumer<ToolProgress> progress) {
        return CompletableFuture.supplyAsync(() -> {
            if (scriptContent == null || scriptContent.isEmpty()) {
                return ToolResult.fail("CodeTool: missing 'scriptContent' in executionConfig for tool '"
                        + toolId + "'");
            }

            String paramsJson = input != null ? input.toString() : "{}";

            try {
                log.info("[CodeTool] 执行脚本: tool={}, language={}", getName(), language);
                String result = executeScript(language, scriptContent, paramsJson);
                log.info("[CodeTool] 脚本执行完成: tool={}", getName());
                return ToolResult.ok(result);
            } catch (Exception e) {
                return ToolResult.fail("CodeTool: script execution failed for tool '"
                        + toolId + "': " + e.getMessage());
            }
        });
    }

    @Override
    public ToolResultBlockParam mapToolResultToToolResultBlockParam(String output, String toolUseId) {
        return ToolResultBlockParam.ofText(toolUseId, output != null ? output : "");
    }

    @Override
    public boolean isReadOnly(JsonNode input) {
        return false;
    }

    @Override
    public boolean isDestructive(JsonNode input) {
        return false;
    }

    // ── 脚本执行（ProcessBuilder，待替换为真实沙箱） ────────────────

    private String executeScript(String language, String scriptContent, String paramsJson) throws Exception {
        File scriptFile = createTempScript(language, scriptContent);
        try {
            List<String> command = buildCommand(language, scriptFile, paramsJson);

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (output.length() > 0) output.append("\n");
                    output.append(line);
                }
            }

            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new RuntimeException("Script execution timed out after " + TIMEOUT_SECONDS + "s");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                throw new RuntimeException("Script exited with code " + exitCode + ". Output: " + output);
            }

            return output.toString();
        } finally {
            scriptFile.delete();
        }
    }

    private File createTempScript(String language, String scriptContent) throws Exception {
        String ext = "python".equalsIgnoreCase(language) ? ".py" : ".sh";
        File tmpFile = Files.createTempFile("tool_script_", ext).toFile();
        tmpFile.setExecutable(true);

        String finalContent = scriptContent;
        if ("python".equalsIgnoreCase(language)) {
            finalContent = "import sys, json\n"
                    + "_PARAMS = json.loads(sys.argv[1] if len(sys.argv) > 1 else '{}')\n"
                    + scriptContent;
        }

        try (FileWriter writer = new FileWriter(tmpFile)) {
            writer.write(finalContent);
        }
        return tmpFile;
    }

    private List<String> buildCommand(String language, File scriptFile, String paramsJson) {
        List<String> command = new ArrayList<>();
        if ("python".equalsIgnoreCase(language) || "python3".equalsIgnoreCase(language)) {
            command.add("python3");
            command.add(scriptFile.getAbsolutePath());
            command.add(paramsJson);
        } else {
            command.add("sh");
            command.add(scriptFile.getAbsolutePath());
        }
        return command;
    }
}

