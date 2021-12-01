package com.ivannikolaev.tus4j;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.ivannikolaev.tus4j.handler.TusServerHandler;
import com.ivannikolaev.tus4j.man.TusDefaultUploadManager;
import com.ivannikolaev.tus4j.proto.TusContentTypes;
import com.ivannikolaev.tus4j.proto.TusHeaders;
import com.ivannikolaev.tus4j.proto.TusVersions;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;

import static com.ivannikolaev.tus4j.TusTestUtil.*;
import static com.ivannikolaev.tus4j.proto.TusHeaders.*;
import static com.ivannikolaev.tus4j.util.TusProtocolUtil.location;
import static io.netty.handler.codec.http.HttpMethod.POST;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TUS extension")
public class TusExtensionsTest {
    private static final int TUS_MAX_SIZE_VALUE = 10000;
    private static final String CONTENT_STRING = "TUS extension test";
    private final String CONTEXT_ROOT = "contextroot";
    private final String URL = "/" + CONTEXT_ROOT;
    private EmbeddedChannel channel;
    private TusDefaultUploadManager uploadManager;

    @BeforeEach
    public void setUp() throws IOException {
        FileSystem fileSystem = Jimfs.newFileSystem(Configuration.unix());
        uploadManager = TusDefaultUploadManager.create(fileSystem.getPath("/tmp"), TUS_MAX_SIZE_VALUE);
        channel = new EmbeddedChannel(new TusServerHandler(CONTEXT_ROOT, uploadManager));
    }

    @Nested
    @DisplayName("Creation")
    class Creation {

        @Test
        @DisplayName("MUST return 201 Created status")
        void responseStatusIsCreated() {
            HttpResponse response = createUpload(URL, new DefaultHttpHeaders().add(UPLOAD_LENGTH, 10), channel);
            assertThat(response.status()).isEqualTo(CREATED);
        }

        @Test
        @DisplayName("MUST set the Location header to the URL of the created resource")
        void locationHeaderIsSet() {
            HttpResponse response = createUpload(URL, new DefaultHttpHeaders().add(UPLOAD_LENGTH, 10), channel);
            assertThat(response.headers().get(HttpHeaderNames.LOCATION)).isEqualToIgnoringCase(URL + "/" + uploadManager.uploadDescriptors().iterator().next().getId());
        }

        @Test
        @DisplayName("MUST respond with the 413 Request Entity Too Large status If the length of the upload exceeds Tus-Max-Size")
        void entityTooLarge() {
            HttpResponse response = createUpload(URL, new DefaultHttpHeaders().add(UPLOAD_LENGTH, TUS_MAX_SIZE_VALUE + 1), channel);
            assertThat(response.status()).isEqualTo(REQUEST_ENTITY_TOO_LARGE);
        }
    }

    @Nested
    @DisplayName("Creation with upload")
    class CreationWithUpload {
        private final byte[] content = CONTENT_STRING.getBytes(StandardCharsets.UTF_8);
        private HttpHeaders httpHeaders;

        @BeforeEach
        void setUp() {
            httpHeaders = new DefaultHttpHeaders().add(TUS_RESUMABLE, TusVersions.preferred())
                    .add(TusHeaders.UPLOAD_LENGTH, content.length)
                    .add(HttpHeaderNames.CONTENT_TYPE, TusContentTypes.APPLICATION_OFFSET_OCTET_STREAM);
        }

        @Test
        @DisplayName("Must return 100 Continue if expect is 100-continue")
        void expectsContinue() {
            httpHeaders.add(HttpHeaderNames.EXPECT, HttpHeaderValues.CONTINUE);
            HttpRequest httpRequest = new DefaultFullHttpRequest(HTTP_1_1, POST, URL, Unpooled.EMPTY_BUFFER, httpHeaders, EmptyHttpHeaders.INSTANCE);
            channel.writeInbound(httpRequest, new DefaultHttpContent(Unpooled.wrappedBuffer(content)), new DefaultLastHttpContent(Unpooled.EMPTY_BUFFER));
            HttpResponse httpResponse = channel.readOutbound();
            assertThat(CONTINUE).isEqualTo(httpResponse.status());
        }

        @Test
        @DisplayName("Must acknowledge a successful upload creation with the 201 Created status")
        void successfulUploadStatus() {
            HttpRequest httpRequest = new DefaultFullHttpRequest(HTTP_1_1, POST, URL, Unpooled.EMPTY_BUFFER, httpHeaders, EmptyHttpHeaders.INSTANCE);
            channel.writeInbound(httpRequest, new DefaultHttpContent(Unpooled.wrappedBuffer(content)), new DefaultLastHttpContent(Unpooled.EMPTY_BUFFER));
            HttpResponse httpResponse = channel.readOutbound();
            assertThat(CREATED).isEqualTo(httpResponse.status());
        }

        @Test
        @DisplayName("MUST set Upload-Length header")
        void successfulUploadLength() {
            HttpRequest httpRequest = new DefaultFullHttpRequest(HTTP_1_1, POST, URL, Unpooled.EMPTY_BUFFER, httpHeaders, EmptyHttpHeaders.INSTANCE);
            channel.writeInbound(httpRequest, new DefaultHttpContent(Unpooled.wrappedBuffer(content)), new DefaultLastHttpContent(Unpooled.EMPTY_BUFFER));
            HttpResponse httpResponse = channel.readOutbound();
            assertThat(httpResponse.headers().get(UPLOAD_LENGTH)).isEqualTo(String.valueOf(content.length));
        }

        @Test
        @DisplayName("MUST set Upload-Offset header")
        void successfulUploadOffset() {
            HttpRequest httpRequest = new DefaultFullHttpRequest(HTTP_1_1, POST, URL, Unpooled.EMPTY_BUFFER, httpHeaders, EmptyHttpHeaders.INSTANCE);
            channel.writeInbound(httpRequest, new DefaultHttpContent(Unpooled.wrappedBuffer(content)), new DefaultLastHttpContent(Unpooled.EMPTY_BUFFER));
            HttpResponse httpResponse = channel.readOutbound();
            assertThat(httpResponse.headers().get(UPLOAD_OFFSET)).isEqualTo(String.valueOf(content.length));
        }
    }

    @Nested
    @DisplayName("Deferring length")
    class CreationDeferLength {
        @Test
        @DisplayName("MUST set Upload-Defer-Length: 1 in all responses to HEAD requests")
        void uploadDeferLengthHeaderIsSet() {
            String location = location(createUpload(URL, new DefaultHttpHeaders().add(UPLOAD_DEFER_LENGTH, 1), channel));
            channel.writeInbound(createHeadRequest(location));
            HttpResponse response = channel.readOutbound();
            assertThat(response.headers().contains(UPLOAD_DEFER_LENGTH, "1", true)).isTrue();
        }

        @Test
        @DisplayName("MUST return a 400 Bad Request status if Upload-Defer-Length != 1")
        void uploadDeferLengthIsNotOne() {
            HttpResponse response = createUpload(URL, new DefaultHttpHeaders().add(UPLOAD_DEFER_LENGTH, 2), channel);
            assertThat(response.status()).isEqualTo(BAD_REQUEST);
        }

        @Test
        @DisplayName("MUST accept Upload-Length in subsequent PATCH request")
        void uploadDeferLengthPatch() {
            byte[] content = CONTENT_STRING.getBytes();
            String location = location(createUpload(URL, new DefaultHttpHeaders().add(UPLOAD_DEFER_LENGTH, 1), channel));
            channel.writeInbound(createPatchRequest(location, content));
            channel.readOutbound();
            channel.writeInbound(createHeadRequest(location));
            HttpResponse response = channel.readOutbound();
            assertThat(response.headers().get(UPLOAD_LENGTH)).isEqualTo(String.valueOf(content.length));
        }

    }
}
