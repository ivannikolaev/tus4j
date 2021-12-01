package com.ivannikolaev.tus4j.handler.ops;

import com.ivannikolaev.tus4j.man.UploadDescriptor;
import com.ivannikolaev.tus4j.proto.TusHeaders;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;

import java.io.IOException;
import java.net.URISyntaxException;

import static com.ivannikolaev.tus4j.util.TusProtocolUtil.*;

public class ProcessPost implements TusOperation {
    private final String uri;

    public ProcessPost(String uri) {
        this.uri = uri;
    }

    @Override
    public void process(ChannelHandlerContext ctx, TusOperationContext tusCtx, HttpObject msg) throws IOException, URISyntaxException {
        HttpRequest httpRequest = (HttpRequest) msg;
        Long uploadLength = parseLongValue(httpRequest.headers(), TusHeaders.UPLOAD_LENGTH);
        if (uploadLength == null) {
            if (isDeferringLength(httpRequest)) {
                uploadLength = (long) -1;
            } else {
                writeBadRequestResponse(ctx);
                return;
            }
        } else if (exceedsMaxSize(tusCtx, uploadLength)) {
            writeEntityTooLargeResponse(ctx, tusCtx);
            return;
        }
        UploadDescriptor uploadDescriptor = tusCtx.getUploadManager().createUpload();
        uploadDescriptor.setLength(uploadLength);
        tusCtx.setUploadDescriptor(uploadDescriptor);
        if (isCreationWithUpload(httpRequest)) {
            tusCtx.setPendingCreationResponse(true);
            if (isExpectingContinue(httpRequest)) {
                writeContinueResponse(ctx);
            }
        } else {
            writeCreationResponse(ctx, tusCtx);
        }
    }
}
