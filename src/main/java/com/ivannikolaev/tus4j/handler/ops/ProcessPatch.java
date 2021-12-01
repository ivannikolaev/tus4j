package com.ivannikolaev.tus4j.handler.ops;

import com.ivannikolaev.tus4j.man.UploadDescriptor;
import com.ivannikolaev.tus4j.proto.TusContentTypes;
import com.ivannikolaev.tus4j.proto.TusHeaders;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;

import static com.ivannikolaev.tus4j.proto.TusHeaders.UPLOAD_LENGTH;
import static com.ivannikolaev.tus4j.proto.TusHeaders.UPLOAD_OFFSET;
import static com.ivannikolaev.tus4j.util.TusProtocolUtil.*;

public class ProcessPatch implements TusOperation {
    @Override
    public void process(ChannelHandlerContext ctx, TusOperationContext tusCtx, HttpObject msg) {
        HttpRequest request = (HttpRequest) msg;
        if (!TusContentTypes.APPLICATION_OFFSET_OCTET_STREAM.equalsIgnoreCase(request.headers().get(HttpHeaderNames.CONTENT_TYPE))) {
            writeUnsupportedResponse(ctx);
        }
        String uploadId = tusCtx.getTusUrl().path();
        UploadDescriptor uploadDescriptor = tusCtx.getUploadManager().getUpload(uploadId);
        if (uploadDescriptor == null) {
            writeNotFoundResponse(ctx);
        } else {
            tusCtx.setUploadDescriptor(uploadDescriptor);
            if (tusCtx.getUploadDescriptor().getLength() < 0 && request.headers().contains(TusHeaders.UPLOAD_LENGTH)) {
                Long length = parseLongValue(request.headers(), UPLOAD_LENGTH);
                if (length == null) {
                    writeBadRequestResponse(ctx);
                    return;
                } else if (exceedsMaxSize(tusCtx, length)) {
                    writeEntityTooLargeResponse(ctx, tusCtx);
                    return;
                } else {
                    tusCtx.getUploadDescriptor().setLength(length);
                }
            }
            Long offset = parseLongValue(request.headers(), UPLOAD_OFFSET);
            if (offset == null || offset != uploadDescriptor.getOffset()) {
                FullHttpResponse fullHttpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONFLICT, Unpooled.EMPTY_BUFFER, EmptyHttpHeaders.INSTANCE, EmptyHttpHeaders.INSTANCE);
                ctx.write(fullHttpResponse);
            }
        }

    }
}
