package com.ivannikolaev.tus4j.handler.ops;

import com.ivannikolaev.tus4j.man.UploadDescriptor;
import com.ivannikolaev.tus4j.proto.TusVersions;
import com.ivannikolaev.tus4j.proto.TusHeaders;
import com.ivannikolaev.tus4j.util.TusProtocolUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;

public class ProcessHead implements TusOperation {
    @Override
    public void process(ChannelHandlerContext ctx, TusOperationContext tusCtx, HttpObject msg) {
        HttpHeaders headers = new DefaultHttpHeaders()
                .add(TusHeaders.TUS_RESUMABLE, TusVersions.preferred())
                .add(HttpHeaderNames.CACHE_CONTROL, HttpHeaderValues.NO_STORE);
        String uploadId = tusCtx.getTusUrl().path();
        UploadDescriptor uploadDescriptor = tusCtx.getUploadManager().getUpload(uploadId);
        if (uploadDescriptor == null) {
            FullHttpResponse fullHttpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND, Unpooled.EMPTY_BUFFER, headers, EmptyHttpHeaders.INSTANCE);
            ctx.write(fullHttpResponse);
        } else {
            headers.add(TusHeaders.UPLOAD_OFFSET, uploadDescriptor.getOffset());
            if (uploadDescriptor.getLength() >= 0) {
                headers.add(TusHeaders.UPLOAD_LENGTH, uploadDescriptor.getLength());
            } else {
                headers.add(TusHeaders.UPLOAD_DEFER_LENGTH, 1);
            }
            TusProtocolUtil.maxSizeHeader(headers, tusCtx);
            FullHttpResponse fullHttpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.EMPTY_BUFFER, headers, EmptyHttpHeaders.INSTANCE);
            ctx.write(fullHttpResponse);
        }
    }
}
