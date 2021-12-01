package com.ivannikolaev.tus4j.man;

import io.netty.buffer.ByteBuf;

import java.io.IOException;
import java.nio.file.Path;

public interface TusUploadManager {
    UploadDescriptor getUpload(String uploadId);
    UploadDescriptor createUpload(String uploadId) throws IOException;
    UploadDescriptor createUpload() throws IOException;
    Path getUploadedFile(String uploadId);
    void processUpload(UploadDescriptor uploadDescriptor, ByteBuf content) throws IOException;
    void completeUpload(UploadDescriptor uploadDescriptor) throws IOException;
    default int getTusMaxSize() {
        return -1;
    }
}
