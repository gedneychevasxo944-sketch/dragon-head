package org.dragon.memory.models;

public class MemorySearchResult {

    private String path;
    private int startLine;
    private int endLine;
    private double score;
    private String snippet;
    private String source;
    private String citation;

    public MemorySearchResult() {
    }

    public MemorySearchResult(String path, int startLine, int endLine, double score, String snippet, String source, String citation) {
        this.path = path;
        this.startLine = startLine;
        this.endLine = endLine;
        this.score = score;
        this.snippet = snippet;
        this.source = source;
        this.citation = citation;
    }

    // Getters and Setters
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getStartLine() {
        return startLine;
    }

    public void setStartLine(int startLine) {
        this.startLine = startLine;
    }

    public int getEndLine() {
        return endLine;
    }

    public void setEndLine(int endLine) {
        this.endLine = endLine;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public String getSnippet() {
        return snippet;
    }

    public void setSnippet(String snippet) {
        this.snippet = snippet;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getCitation() {
        return citation;
    }

    public void setCitation(String citation) {
        this.citation = citation;
    }
}