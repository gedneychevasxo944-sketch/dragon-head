package org.dragon.memory.models;

public class ReadFileRequest {

    private String path;
    private Integer startLine;
    private Integer endLine;

    public ReadFileRequest() {
    }

    public ReadFileRequest(String path, Integer startLine, Integer endLine) {
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

    public Integer getStartLine() {
        return startLine;
    }

    public void setStartLine(Integer startLine) {
        this.startLine = startLine;
    }

    public Integer getEndLine() {
        return endLine;
    }

    public void setEndLine(Integer endLine) {
        this.endLine = endLine;
    }
}