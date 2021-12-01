package com.ivannikolaev.tus4j.util;

import com.ivannikolaev.tus4j.handler.ops.TusOperationContext;
import com.ivannikolaev.tus4j.proto.TusContentTypes;
import com.ivannikolaev.tus4j.proto.TusHeaders;
import com.ivannikolaev.tus4j.proto.TusVersions;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;

import java.net.URISyntaxException;

public class TusProtocolUtil {
    private TusProtocolUtil() {
    }

    public static void writeCreationResponse(ChannelHandlerContext ctx, TusOperationContext tusCtx) throws URISyntaxException {
        HttpHeaders headers = maxSizeHeader(locationHeader(commonHeaders(new DefaultHttpHeaders()), ctx, tusCtx), tusCtx);
        FullHttpResponse fullHttpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CREATED, Unpooled.EMPTY_BUFFER, headers, EmptyHttpHeaders.INSTANCE);
        ctx.write(fullHttpResponse);
    }

    public static void writeUploadResponse(ChannelHandlerContext ctx, TusOperationContext tusCtx) throws URISyntaxException {
        HttpHeaders headers = offsetLengthHeaders(locationHeader(commonHeaders(new DefaultHttpHeaders()), ctx, tusCtx), tusCtx);
        FullHttpResponse fullHttpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NO_CONTENT, Unpooled.EMPTY_BUFFER, headers, EmptyHttpHeaders.INSTANCE);
        ctx.write(fullHttpResponse);
    }

    public static void writeCreationWithUploadResponse(ChannelHandlerContext ctx, TusOperationContext tusCtx) throws URISyntaxException {
        HttpHeaders headers = offsetLengthHeaders(locationHeader(commonHeaders(new DefaultHttpHeaders()), ctx, tusCtx), tusCtx);
        FullHttpResponse fullHttpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CREATED, Unpooled.EMPTY_BUFFER, headers, EmptyHttpHeaders.INSTANCE);
        ctx.write(fullHttpResponse);
    }

    public static void writeNotFoundResponse(ChannelHandlerContext ctx) {
        FullHttpResponse fullHttpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND, Unpooled.EMPTY_BUFFER, commonHeaders(), EmptyHttpHeaders.INSTANCE);
        ctx.write(fullHttpResponse);
    }

    public static void writeUnsupportedResponse(ChannelHandlerContext ctx) {
        FullHttpResponse fullHttpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.UNSUPPORTED_MEDIA_TYPE, Unpooled.EMPTY_BUFFER, commonHeaders(), EmptyHttpHeaders.INSTANCE);
        ctx.write(fullHttpResponse);
    }

    public static void writeContinueResponse(ChannelHandlerContext ctx) {
        FullHttpResponse fullHttpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE, Unpooled.EMPTY_BUFFER, commonHeaders(), EmptyHttpHeaders.INSTANCE);
        ctx.write(fullHttpResponse);
    }

    public static void writeBadRequestResponse(ChannelHandlerContext ctx) {
        FullHttpResponse fullHttpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST, Unpooled.EMPTY_BUFFER, commonHeaders(), EmptyHttpHeaders.INSTANCE);
        ctx.write(fullHttpResponse);
    }

    public static void writeEntityTooLargeResponse(ChannelHandlerContext ctx, TusOperationContext tusCtx) {
        FullHttpResponse fullHttpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE, Unpooled.EMPTY_BUFFER, maxSizeHeader(commonHeaders(new DefaultHttpHeaders()), tusCtx), EmptyHttpHeaders.INSTANCE);
        ctx.write(fullHttpResponse);
    }

    public static boolean isExpectingContinue(HttpRequest httpRequest) {
        return httpRequest.headers().contains(HttpHeaderNames.EXPECT, HttpHeaderValues.CONTINUE, true);
    }

    public static boolean isCreationWithUpload(HttpRequest httpRequest) {
        return httpRequest.headers().contains(HttpHeaderNames.CONTENT_TYPE, TusContentTypes.APPLICATION_OFFSET_OCTET_STREAM, true);
    }

    public static boolean isDeferringLength(HttpRequest httpRequest) {
        return httpRequest.headers().contains(TusHeaders.UPLOAD_DEFER_LENGTH, "1", true);
    }

    public static boolean exceedsMaxSize(TusOperationContext tusCtx, Long uploadLength) {
        return tusCtx.getUploadManager().getTusMaxSize() >= 0 && uploadLength > tusCtx.getUploadManager().getTusMaxSize();
    }

    public static String location(HttpResponse response) {
        return response.headers().get(HttpHeaderNames.LOCATION);
    }

    private static HttpHeaders commonHeaders(HttpHeaders headers) {
        return headers.add(TusHeaders.TUS_RESUMABLE, TusVersions.preferred())
                .add(HttpHeaderNames.CACHE_CONTROL, HttpHeaderValues.NO_STORE);
    }

    private static HttpHeaders commonHeaders() {
        return commonHeaders(new DefaultHttpHeaders());
    }

    private static HttpHeaders locationHeader(HttpHeaders headers, ChannelHandlerContext ctx, TusOperationContext tusCtx) throws URISyntaxException {
        return headers.add(HttpHeaderNames.LOCATION, TusUrlUtil.uploadUrl(ctx.channel().localAddress(), tusCtx.getUploadDescriptor().getId(), tusCtx.getContextRoot()));
    }

    public static HttpHeaders maxSizeHeader(HttpHeaders headers, TusOperationContext tusCtx) {
        if (tusCtx.getUploadManager().getTusMaxSize() >= 0) {
            headers.add(TusHeaders.TUS_MAX_SIZE, tusCtx.getUploadManager().getTusMaxSize());
        }
        return headers;
    }

    private static HttpHeaders offsetLengthHeaders(HttpHeaders headers, TusOperationContext tusCtx) {
        headers.add(TusHeaders.UPLOAD_OFFSET, tusCtx.getUploadDescriptor().getOffset());
        if (tusCtx.getUploadDescriptor().getLength() >= 0) {
            headers.add(TusHeaders.UPLOAD_LENGTH, tusCtx.getUploadDescriptor().getLength());
        }
        return headers;
    }

    public static Long parseLongValue(HttpHeaders headers, String headerName) {
        String s = headers.get(headerName);
        Long offset = null;
        if (s != null && s.length() > 0) {
            try {
                offset = Long.parseLong(s);
            } catch (NumberFormatException ignored) {
            }
        }
        return offset;
    }
}
