package org.dragon.memory.models;

import java.util.Map;

public class SearchOptions {

    private String sessionKey;
    private int maxResults;
    private double minScore;
    private double vectorWeight;
    private double textWeight;
    private boolean mmr;
    private boolean temporalDecay;
    private Map<String, Object> extra;

    public SearchOptions() {
    }

    // Getters and Setters
    public String getSessionKey() {
        return sessionKey;
    }

    public void setSessionKey(String sessionKey) {
        this.sessionKey = sessionKey;
    }

    public int getMaxResults() {
        return maxResults;
    }

    public void setMaxResults(int maxResults) {
        this.maxResults = maxResults;
    }

    public double getMinScore() {
        return minScore;
    }

    public void setMinScore(double minScore) {
        this.minScore = minScore;
    }

    public double getVectorWeight() {
        return vectorWeight;
    }

    public void setVectorWeight(double vectorWeight) {
        this.vectorWeight = vectorWeight;
    }

    public double getTextWeight() {
        return textWeight;
    }

    public void setTextWeight(double textWeight) {
        this.textWeight = textWeight;
    }

    public boolean isMmr() {
        return mmr;
    }

    public void setMmr(boolean mmr) {
        this.mmr = mmr;
    }

    public boolean isTemporalDecay() {
        return temporalDecay;
    }

    public void setTemporalDecay(boolean temporalDecay) {
        this.temporalDecay = temporalDecay;
    }

    public Map<String, Object> getExtra() {
        return extra;
    }

    public void setExtra(Map<String, Object> extra) {
        this.extra = extra;
    }
}