package com.ivannikolaev.tus4j.man;

public class UploadDescriptor {
    private String id;
    private long offset;
    private long length;
    private UploadStatus status;

    public UploadDescriptor(String id) {
        this.id = id;
        status = UploadStatus.NEW;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getOffset() {
        return offset;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }

    public long getLength() {
        return length;
    }

    public void setLength(long length) {
        this.length = length;
    }

    public UploadStatus getStatus() {
        return status;
    }

    public void setStatus(UploadStatus status) {
        this.status = status;
    }
}
