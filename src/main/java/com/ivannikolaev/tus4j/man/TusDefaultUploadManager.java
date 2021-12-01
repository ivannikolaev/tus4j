package com.ivannikolaev.tus4j.man;

import io.netty.buffer.ByteBuf;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TusDefaultUploadManager implements TusUploadManager {
    private final Map<String, UploadDescriptor> uploadMap;
    private final Map<String, FileChannel> fileChannelMap;
    private final Path uploadFolder;
    private final int tusMaxSize;

    private TusDefaultUploadManager(Path uploadFolder, int tusMaxSize) {
        this.uploadFolder = uploadFolder;
        this.tusMaxSize = tusMaxSize;
        this.uploadMap = new ConcurrentHashMap<>();
        this.fileChannelMap = new ConcurrentHashMap<>();
    }

    @Override
    public UploadDescriptor getUpload(String uploadId) {
        return uploadMap.get(uploadId);
    }

    @Override
    public UploadDescriptor createUpload(String uploadId) throws IOException {
        Files.createFile(uploadFolder.resolve(uploadId));
        UploadDescriptor uploadDescriptor = new UploadDescriptor(uploadId);
        uploadMap.put(uploadId, uploadDescriptor);
        return uploadDescriptor;
    }

    @Override
    public UploadDescriptor createUpload() throws IOException {
        String uploadId = UUID.randomUUID().toString();
        return createUpload(uploadId);
    }

    @Override
    public Path getUploadedFile(String uploadId) {
        return uploadFolder.resolve(uploadId);
    }

    @Override
    public int getTusMaxSize() {
        return tusMaxSize;
    }

    @Override
    public void processUpload(UploadDescriptor uploadDescriptor, ByteBuf content) throws IOException {
        FileChannel fileChannel = fileChannelMap.get(uploadDescriptor.getId());
        if (uploadDescriptor.getStatus() == UploadStatus.NEW || uploadDescriptor.getStatus() == UploadStatus.UPLOADING) {
            if (fileChannel == null) {
                fileChannel = FileChannel.open(uploadFolder.resolve(uploadDescriptor.getId()), StandardOpenOption.WRITE, StandardOpenOption.CREATE);
                fileChannelMap.put(uploadDescriptor.getId(), fileChannel);
            }
        } else {
            throw new IllegalStateException("Upload " + uploadDescriptor.getId() + " has been completed");
        }

        if (content.isReadable()) {
            uploadDescriptor.setOffset(content.readBytes(fileChannel, uploadDescriptor.getOffset(), content.readableBytes()) + uploadDescriptor.getOffset());
        }
    }

    @Override
    public void completeUpload(UploadDescriptor uploadDescriptor) throws IOException {
        if (uploadDescriptor.getStatus() == UploadStatus.COMPLETED) {
            throw new IllegalStateException("Upload " + uploadDescriptor.getId() + " has been completed");
        }
        FileChannel fileChannel = fileChannelMap.get(uploadDescriptor.getId());
        if (fileChannel != null) {
            fileChannel.close();
            fileChannelMap.remove(uploadDescriptor.getId());
        }
    }

    public Collection<UploadDescriptor> uploadDescriptors() {
        return Collections.unmodifiableCollection(uploadMap.values());
    }

    public static TusDefaultUploadManager create(Path uploadFolder, int tusMaxSize) throws IOException {
        Files.createDirectories(uploadFolder);
        return new TusDefaultUploadManager(uploadFolder, tusMaxSize);
    }

    public static TusDefaultUploadManager create(Path uploadFolder) throws IOException {
        return create(uploadFolder, -1);
    }

}
