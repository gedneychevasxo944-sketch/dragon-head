package org.dragon.tool.runtime.factory;

import com.fasterxml.jackson.databind.JsonNode;
import org.dragon.tool.enums.ToolType;
import org.dragon.tool.runtime.Tool;
import org.dragon.tool.runtime.ToolFactory;
import org.dragon.tool.runtime.ToolDefinition;
import org.dragon.tool.runtime.tools.CodeTool;

/**
 * CODE 类型工具 Factory。
 *
 * <p>CODE 工具的执行逻辑（脚本内容 + 语言类型）完全封装在 {@link CodeTool} 实例中，
 * Factory 无需额外依赖，直接根据运行时快照构造实例即可。
 *
 * <p>虽然脚本执行过程（ProcessBuilder）无调用级内部状态，实例可复用，
 * 但考虑到不同版本间 scriptContent 可能不同，{@link #isSingleton()} 返回 {@code true}
 * 仍然安全——ToolRegistry 按 toolId 缓存，版本更新时缓存失效，自动重建实例。
 */
public class CodeToolFactory implements ToolFactory {

    @Override
    public ToolType supportedType() {
        return ToolType.CODE;
    }

    /**
     * 构建绑定了 scriptContent + language 的 {@link CodeTool} 实例。
     *
     * @throws IllegalArgumentException 若 executionConfig 缺少 scriptContent
     */
    @Override
    public Tool<JsonNode, ?> create(ToolDefinition runtime) {
        JsonNode config = runtime.getExecutionConfig();
        if (config == null || !config.has("scriptContent")
                || config.path("scriptContent").asText().isEmpty()) {
            throw new IllegalArgumentException(
                    "CodeToolFactory: missing 'scriptContent' in executionConfig for tool '"
                            + runtime.getToolId() + "'");
        }
        return new CodeTool(runtime);
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}

