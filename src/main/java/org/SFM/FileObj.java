package org.SFM;

public class FileObj {
    private String path;
    private String originalName;
    private String newName;

    public FileObj(String path, String originalName, String newName) {
        this.path = path;
        this.originalName = originalName;
        this.newName = newName;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public String getNewName() {
        return newName;
    }

    public void setNewName(String newName) {
        this.newName = newName;
    }
}

