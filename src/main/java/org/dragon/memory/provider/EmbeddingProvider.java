package org.dragon.memory.provider;

import java.util.ArrayList;
import java.util.List;

public interface EmbeddingProvider {

    List<Double> getEmbedding(String text);

    List<List<Double>> getBatchEmbeddings(List<String> texts);

    int getDimensions();

    String getProviderKey();
}

class DummyEmbeddingProvider implements EmbeddingProvider {

    @Override
    public List<Double> getEmbedding(String text) {
        List<Double> embedding = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            embedding.add(Math.random());
        }
        return embedding;
    }

    @Override
    public List<List<Double>> getBatchEmbeddings(List<String> texts) {
        List<List<Double>> embeddings = new ArrayList<>();
        for (String text : texts) {
            embeddings.add(getEmbedding(text));
        }
        return embeddings;
    }

    @Override
    public int getDimensions() {
        return 10;
    }

    @Override
    public String getProviderKey() {
        return "dummy";
    }
}
