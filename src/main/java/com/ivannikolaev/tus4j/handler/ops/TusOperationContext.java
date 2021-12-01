package com.ivannikolaev.tus4j.handler.ops;

import com.ivannikolaev.tus4j.man.UploadDescriptor;
import com.ivannikolaev.tus4j.man.TusUploadManager;
import com.ivannikolaev.tus4j.util.TusUrl;

public class TusOperationContext {
    private String contextRoot;
    private TusUrl tusUrl;
    private TusUploadManager uploadManager;
    private UploadDescriptor uploadDescriptor;
    private boolean pendingCreationResponse;

    public TusUrl getTusUrl() {
        return tusUrl;
    }

    public void setTusUrl(TusUrl tusUrl) {
        this.tusUrl = tusUrl;
    }

    public TusUploadManager getUploadManager() {
        return uploadManager;
    }

    public void setUploadManager(TusUploadManager uploadResolver) {
        this.uploadManager = uploadResolver;
    }

    public UploadDescriptor getUploadDescriptor() {
        return uploadDescriptor;
    }

    public void setUploadDescriptor(UploadDescriptor uploadDescriptor) {
        this.uploadDescriptor = uploadDescriptor;
    }

    public String getContextRoot() {
        return contextRoot;
    }

    public void setContextRoot(String contextRoot) {
        this.contextRoot = contextRoot;
    }

    public boolean isPendingCreationResponse() {
        return pendingCreationResponse;
    }

    public void setPendingCreationResponse(boolean pendingCreationResponse) {
        this.pendingCreationResponse = pendingCreationResponse;
    }
}
