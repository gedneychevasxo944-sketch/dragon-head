package org.dragon.memory.models;

public class ReadFileRequest {

    private String path;
    private int startLine;
    private int endLine;

    public ReadFileRequest() {
    }

    public ReadFileRequest(String path, int startLine, int endLine) {
        this.path = path;
        this.startLine = startLine;
        this.endLine = endLine;
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
}