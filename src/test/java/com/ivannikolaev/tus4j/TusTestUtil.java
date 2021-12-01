package com.ivannikolaev.tus4j;

import com.ivannikolaev.tus4j.proto.TusContentTypes;
import com.ivannikolaev.tus4j.proto.TusHeaders;
import com.ivannikolaev.tus4j.proto.TusVersions;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.*;

import static com.ivannikolaev.tus4j.proto.TusHeaders.TUS_RESUMABLE;
import static io.netty.handler.codec.http.HttpMethod.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class TusTestUtil {
    private TusTestUtil() {
    }
    public static HttpResponse createUpload(String url, HttpHeaders additionalHeaders, EmbeddedChannel channel) {
        HttpHeaders httpHeaders = new DefaultHttpHeaders()
                .add(TUS_RESUMABLE, TusVersions.preferred())
                .add(additionalHeaders);
        HttpRequest httpRequest = new DefaultFullHttpRequest(HTTP_1_1, POST, url, Unpooled.EMPTY_BUFFER, httpHeaders, EmptyHttpHeaders.INSTANCE);
        channel.writeInbound(httpRequest);
        return channel.readOutbound();
    }
    public static HttpRequest createHeadRequest(String url) {
        HttpHeaders httpHeaders = new DefaultHttpHeaders().add(TUS_RESUMABLE, TusVersions.preferred());
        return new DefaultFullHttpRequest(HTTP_1_1, HEAD, url, Unpooled.EMPTY_BUFFER, httpHeaders, EmptyHttpHeaders.INSTANCE);
    }
    public static HttpRequest createPatchRequest(String url, byte[] content) {
        HttpHeaders httpHeaders = new DefaultHttpHeaders()
                .add(TUS_RESUMABLE, TusVersions.preferred())
                .add(TusHeaders.UPLOAD_LENGTH, content.length)
                .add(HttpHeaderNames.CONTENT_TYPE, TusContentTypes.APPLICATION_OFFSET_OCTET_STREAM);
        return new DefaultFullHttpRequest(HTTP_1_1, PATCH, url, Unpooled.wrappedBuffer(content), httpHeaders, EmptyHttpHeaders.INSTANCE);
    }
}
