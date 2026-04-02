package org.dragon.memory.provider;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Kimi (Moonshot AI) 向量化提供器实现
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Component
public class KimiEmbeddingProvider implements EmbeddingProvider {

    private final HttpClient httpClient;
    private final Gson gson;

    @Value("${llm.kimi.apiKey:}")
    private String apiKey;

    @Value("${llm.kimi.baseUrl:https://api.moonshot.cn/v1}")
    private String baseUrl;

    @Value("${llm.kimi.embeddingModel:moonshot-v1-embed-256d}")
    private String embeddingModel;

    public KimiEmbeddingProvider() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.gson = new Gson();
    }

    @Override
    public List<Double> getEmbedding(String text) {
        try {
            String url = baseUrl + "/embeddings";

            JsonObject body = new JsonObject();
            body.addProperty("model", embeddingModel);
            body.addProperty("input", text);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest,
                    HttpResponse.BodyHandlers.ofString());

            return parseEmbeddingResponse(response.body());

        } catch (Exception e) {
            log.error("[KimiEmbedding] 调用失败: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<List<Double>> getBatchEmbeddings(List<String> texts) {
        List<List<Double>> embeddings = new ArrayList<>();

        // 分批处理，避免请求过大
        final int batchSize = 10;
        for (int i = 0; i < texts.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, texts.size());
            List<String> batch = texts.subList(i, endIndex);

            try {
                String url = baseUrl + "/embeddings";

                JsonObject body = new JsonObject();
                body.addProperty("model", embeddingModel);

                JsonArray inputArray = new JsonArray();
                for (String text : batch) {
                    inputArray.add(text);
                }
                body.add("input", inputArray);

                HttpRequest httpRequest = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + apiKey)
                        .timeout(Duration.ofSeconds(60))
                        .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                        .build();

                HttpResponse<String> response = httpClient.send(httpRequest,
                        HttpResponse.BodyHandlers.ofString());

                embeddings.addAll(parseBatchEmbeddingResponse(response.body()));

            } catch (Exception e) {
                log.error("[KimiEmbedding] 批量调用失败: {}", e.getMessage(), e);
                // 为失败的请求添加空列表
                for (int j = 0; j < batch.size(); j++) {
                    embeddings.add(Collections.emptyList());
                }
            }
        }

        return embeddings;
    }

    @Override
    public int getDimensions() {
        // 根据配置的模型返回维度
        if (embeddingModel.contains("256d")) {
            return 256;
        } else if (embeddingModel.contains("512d")) {
            return 512;
        } else if (embeddingModel.contains("1024d")) {
            return 1024;
        }
        // 默认返回 256 维
        return 256;
    }

    @Override
    public String getProviderKey() {
        return "kimi";
    }

    private List<Double> parseEmbeddingResponse(String responseBody) {
        try {
            JsonObject json = gson.fromJson(responseBody, JsonObject.class);

            // 检查错误
            if (json.has("error")) {
                JsonObject error = json.getAsJsonObject("error");
                String errorMsg = error.get("message").getAsString();
                log.error("[KimiEmbedding] API 错误: {}", errorMsg);
                return Collections.emptyList();
            }

            JsonArray dataArray = json.getAsJsonArray("data");
            if (dataArray.size() > 0) {
                JsonObject dataItem = dataArray.get(0).getAsJsonObject();
                JsonArray embedding = dataItem.getAsJsonArray("embedding");
                List<Double> result = new ArrayList<>();
                for (int i = 0; i < embedding.size(); i++) {
                    result.add(embedding.get(i).getAsDouble());
                }
                return result;
            }

            return Collections.emptyList();

        } catch (Exception e) {
            log.error("[KimiEmbedding] 解析响应失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<List<Double>> parseBatchEmbeddingResponse(String responseBody) {
        try {
            JsonObject json = gson.fromJson(responseBody, JsonObject.class);

            // 检查错误
            if (json.has("error")) {
                JsonObject error = json.getAsJsonObject("error");
                String errorMsg = error.get("message").getAsString();
                log.error("[KimiEmbedding] API 错误: {}", errorMsg);
                return Collections.emptyList();
            }

            JsonArray dataArray = json.getAsJsonArray("data");
            List<List<Double>> results = new ArrayList<>();

            for (int i = 0; i < dataArray.size(); i++) {
                JsonObject dataItem = dataArray.get(i).getAsJsonObject();
                JsonArray embedding = dataItem.getAsJsonArray("embedding");
                List<Double> result = new ArrayList<>();
                for (int j = 0; j < embedding.size(); j++) {
                    result.add(embedding.get(j).getAsDouble());
                }
                results.add(result);
            }

            return results;

        } catch (Exception e) {
            log.error("[KimiEmbedding] 解析响应失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
