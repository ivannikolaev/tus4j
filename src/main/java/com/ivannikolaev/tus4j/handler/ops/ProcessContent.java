package com.ivannikolaev.tus4j.handler.ops;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.LastHttpContent;

import java.io.IOException;
import java.net.URISyntaxException;

import static com.ivannikolaev.tus4j.util.TusProtocolUtil.writeCreationWithUploadResponse;
import static com.ivannikolaev.tus4j.util.TusProtocolUtil.writeUploadResponse;

public class ProcessContent implements TusOperation {
    @Override
    public void process(ChannelHandlerContext ctx, TusOperationContext tusCtx, HttpObject msg) throws IOException, URISyntaxException {
        if (tusCtx.getUploadDescriptor() == null) {
            throw new IllegalStateException("Upload descriptor undefined");
        }
        readContent(tusCtx, (HttpContent) msg);
        if (msg instanceof LastHttpContent) {
            if (tusCtx.isPendingCreationResponse()) {
                writeCreationWithUploadResponse(ctx, tusCtx);
                tusCtx.setPendingCreationResponse(false);
            } else {
                writeUploadResponse(ctx, tusCtx);
            }
            if (tusCtx.getUploadDescriptor().getLength() == tusCtx.getUploadDescriptor().getOffset()) {
                completeUpload(tusCtx);
            }
        }
    }

    private void completeUpload(TusOperationContext tusCtx) throws IOException {
        tusCtx.getUploadManager().completeUpload(tusCtx.getUploadDescriptor());
    }

    private void readContent(TusOperationContext tusCtx, HttpContent content) throws IOException {
        tusCtx.getUploadManager().processUpload(tusCtx.getUploadDescriptor(), content.content());
    }

}
