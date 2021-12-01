package com.ivannikolaev.tus4j.handler.ops;

import com.ivannikolaev.tus4j.proto.TusExtensions;
import com.ivannikolaev.tus4j.proto.TusVersions;
import com.ivannikolaev.tus4j.proto.TusHeaders;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;

public class ProcessOptions implements TusOperation {
    @Override
    public void process(ChannelHandlerContext ctx, TusOperationContext tusCtx, HttpObject msg) {
        HttpHeaders headers = new DefaultHttpHeaders()
                .add(TusHeaders.TUS_RESUMABLE, TusVersions.preferred())
                .add(TusHeaders.TUS_VERSION, TusVersions.supported())
                .add(TusHeaders.TUS_EXTENSION, TusExtensions.supported());
        FullHttpResponse fullHttpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NO_CONTENT, Unpooled.EMPTY_BUFFER, headers, EmptyHttpHeaders.INSTANCE);
        ctx.write(fullHttpResponse);
    }
}
