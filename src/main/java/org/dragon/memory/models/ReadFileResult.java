package org.dragon.memory.models;

public class ReadFileResult {

    private String path;
    private String content;
    private int totalLines;

    public ReadFileResult() {
    }

    public ReadFileResult(String path, String content, int totalLines) {
        this.path = path;
        this.content = content;
        this.totalLines = totalLines;
    }

    // Getters and Setters
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getTotalLines() {
        return totalLines;
    }

    public void setTotalLines(int totalLines) {
        this.totalLines = totalLines;
    }
}