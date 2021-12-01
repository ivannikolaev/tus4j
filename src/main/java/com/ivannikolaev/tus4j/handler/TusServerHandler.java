package com.ivannikolaev.tus4j.handler;

import com.ivannikolaev.tus4j.exceptions.UnsupportedHttpMethodException;
import com.ivannikolaev.tus4j.handler.ops.TusOperation;
import com.ivannikolaev.tus4j.handler.ops.TusOperationContext;
import com.ivannikolaev.tus4j.man.TusUploadManager;
import com.ivannikolaev.tus4j.util.TusUrl;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;

public class TusServerHandler extends SimpleChannelInboundHandler<HttpObject> {
    private boolean isTusUpload;
    private final String contextRoot;
    private final TusOperationContext tusCtx;

    public TusServerHandler(String contextRoot, TusUploadManager uploadManager) {
        this.contextRoot = contextRoot;
        tusCtx = new TusOperationContext();
        tusCtx.setUploadManager(uploadManager);
        tusCtx.setContextRoot(contextRoot);
    }

    @Override
    public boolean acceptInboundMessage(Object msg) throws Exception {
        boolean accept = super.acceptInboundMessage(msg);
        if (accept) {
            if (msg instanceof HttpRequest) {
                TusUrl tusUrl = TusUrl.create(((HttpRequest) msg).uri());
                accept = tusUrl.contextRoot().equals(contextRoot);
                isTusUpload = accept;
                tusCtx.setTusUrl(tusUrl);
            } else {
                accept = isTusUpload;
            }
        }
        return accept;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
        TusOperation operation = TusOperation.create(msg);
        operation.process(ctx, tusCtx, msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (cause instanceof UnsupportedHttpMethodException) {
            //todo
        }
        ctx.close();
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }
}
