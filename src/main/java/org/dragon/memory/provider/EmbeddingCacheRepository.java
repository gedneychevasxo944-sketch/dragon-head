package org.dragon.memory.provider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EmbeddingCacheRepository {

    private final Map<String, List<Double>> cache = new HashMap<>();

    public List<Double> get(String text) {
        return cache.get(text);
    }

    public void put(String text, List<Double> embedding) {
        cache.put(text, embedding);
    }

    public boolean contains(String text) {
        return cache.containsKey(text);
    }

    public void clear() {
        cache.clear();
    }
}
