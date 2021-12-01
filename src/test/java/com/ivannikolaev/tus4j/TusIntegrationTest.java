package com.ivannikolaev.tus4j;

import io.tus.java.client.*;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TusIntegrationTest {
    @Test
    void simpleUpload() throws IOException, ProtocolException, InterruptedException {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        TusDummyServer tusDummyServer = new TusDummyServer();
        executorService.submit(()->{
            try {
                tusDummyServer.run();
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        });
        TusClient client = new TusClient();
        client.setUploadCreationURL(new URL("http://localhost:8088/files"));
        client.enableResuming(new TusURLMemoryStore());
        File file = getFileFromTestResources("testFile.txt");
        final TusUpload upload = new TusUpload(file);
        System.out.println("Starting upload...");
        TusExecutor executor = new TusExecutor() {
            @Override
            protected void makeAttempt() throws IOException, ProtocolException {
                TusUploader uploader = client.resumeOrCreateUpload(upload);
                uploader.setChunkSize(1024);
                do {
                    long totalBytes = upload.getSize();
                    long bytesUploaded = uploader.getOffset();
                    double progress = (double) bytesUploaded / totalBytes * 100;
                    System.out.printf("Upload at %06.2f%%.\n", progress);
                } while(uploader.uploadChunk() > -1);
                uploader.finish();
                System.out.println("Upload finished.");
                System.out.format("Upload available at: %s", uploader.getUploadURL().toString());
            }
        };
        executor.makeAttempts();
        tusDummyServer.stop();
        executorService.shutdown();
        if (!executorService.awaitTermination(15, TimeUnit.SECONDS)) {
            executorService.shutdownNow();
        }
    }

    private File getFileFromTestResources(String fileName) throws FileNotFoundException {
        URL url = Thread.currentThread().getContextClassLoader().getResource(fileName);
        if (url == null) {
            throw new FileNotFoundException();
        }
        return new File(url.getPath());
    }
}
