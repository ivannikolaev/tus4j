package com.ivannikolaev.tus4j.handler.ops;

import com.ivannikolaev.tus4j.exceptions.UnsupportedHttpMethodException;
import com.ivannikolaev.tus4j.proto.TusHeaders;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Locale;

public interface TusOperation {
    static TusOperation create(HttpObject msg) {
        if (msg instanceof HttpRequest) {
            HttpRequest request = (HttpRequest) msg;
            String methodOverride = request.headers().get(TusHeaders.METHOD_OVERRIDE);
            HttpMethod method = request.method();
            if (methodOverride != null) {
                method = HttpMethod.valueOf(methodOverride.toUpperCase(Locale.ROOT));
            }
            if (method == HttpMethod.HEAD) {
                return new ProcessHead();
            } else if (method == HttpMethod.OPTIONS) {
                return new ProcessOptions();
            } else if (method == HttpMethod.PATCH) {
                return new ProcessPatch();
            } else if (method == HttpMethod.POST) {
                return new ProcessPost(request.uri());
            }
            throw new UnsupportedHttpMethodException("Method " + method + " is not allowed");
        } else if (msg instanceof HttpContent) {
            return new ProcessContent();
        }
        throw new UnsupportedOperationException("Message type " + msg.getClass().getName() + " is not supported");
    }

    void process(ChannelHandlerContext ctx, TusOperationContext tusCtx, HttpObject msg) throws IOException, URISyntaxException;
}
