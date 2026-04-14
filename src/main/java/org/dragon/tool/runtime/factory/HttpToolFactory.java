package org.dragon.tool.runtime.factory;

import com.fasterxml.jackson.databind.JsonNode;
import org.dragon.tool.enums.ToolType;
import org.dragon.tool.runtime.Tool;
import org.dragon.tool.runtime.ToolFactory;
import org.dragon.tool.runtime.ToolDefinition;
import org.dragon.tool.runtime.tools.HttpTool;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Objects;

/**
 * HTTP 类型工具 Factory。
 *
 * <p>持有共享的 {@link HttpClient}（连接池复用），每次 {@link #create(ToolDefinition)} 调用
 * 构造一个绑定了该版本 executionConfig 的 {@link HttpTool} 实例。
 *
 * <p>创建出的实例可安全跨调用复用（无调用级状态），{@link #isSingleton()} 返回 {@code true}。
 */
public class HttpToolFactory implements ToolFactory {

    /** 所有 HttpTool 实例共享同一 HttpClient（连接池） */
    private final HttpClient httpClient;

    public HttpToolFactory() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /** 允许注入自定义 HttpClient（测试 / 代理场景） */
    public HttpToolFactory(HttpClient httpClient) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
    }

    @Override
    public ToolType supportedType() {
        return ToolType.HTTP;
    }

    /**
     * 构建绑定了 executionConfig 的 {@link HttpTool} 实例。
     *
     * @throws IllegalArgumentException 若 executionConfig 缺少 url 字段
     */
    @Override
    public Tool<JsonNode, ?> create(ToolDefinition runtime) {
        JsonNode config = runtime.getExecutionConfig();
        if (config == null || !config.has("url")) {
            throw new IllegalArgumentException(
                    "HttpToolFactory: missing 'url' in executionConfig for tool '"
                            + runtime.getToolId() + "'");
        }
        return new HttpTool(runtime, httpClient);
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}

