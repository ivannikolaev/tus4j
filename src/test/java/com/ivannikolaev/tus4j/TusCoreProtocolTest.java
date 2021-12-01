package com.ivannikolaev.tus4j;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.ivannikolaev.tus4j.handler.TusServerHandler;
import com.ivannikolaev.tus4j.man.TusDefaultUploadManager;
import com.ivannikolaev.tus4j.man.UploadDescriptor;
import com.ivannikolaev.tus4j.proto.TusContentTypes;
import com.ivannikolaev.tus4j.proto.TusHeaders;
import com.ivannikolaev.tus4j.proto.TusVersions;
import com.ivannikolaev.tus4j.util.ArrayUtil;
import com.ivannikolaev.tus4j.util.TusHeadersUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.util.UUID;

import static com.ivannikolaev.tus4j.proto.TusHeaders.TUS_RESUMABLE;
import static com.ivannikolaev.tus4j.proto.TusHeaders.TUS_VERSION;
import static io.netty.handler.codec.http.HttpMethod.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("TUS protocol")
public class TusCoreProtocolTest {
    private final String URL = "/contextroot";
    private EmbeddedChannel channel;
    private TusDefaultUploadManager uploadManager;

    @Test
    void testMethodOverride() throws IOException {
        String uploadId = UUID.randomUUID().toString();
        UploadDescriptor uploadDescriptor = uploadManager.createUpload(uploadId);
        byte[] uploadBytes = new byte[0];
        uploadDescriptor.setOffset(0);
        uploadDescriptor.setLength(0);
        HttpHeaders httpHeaders = new DefaultHttpHeaders().add(TUS_RESUMABLE, TusVersions.preferred());
        httpHeaders.add(TusHeaders.UPLOAD_OFFSET, uploadDescriptor.getOffset())
                .add(TusHeaders.UPLOAD_LENGTH, uploadDescriptor.getLength())
                .add(TusHeaders.METHOD_OVERRIDE, "PATCH")
                .add(HttpHeaderNames.CONTENT_TYPE, TusContentTypes.APPLICATION_OFFSET_OCTET_STREAM);
        HttpRequest httpRequest = new DefaultFullHttpRequest(HTTP_1_1, POST, URL + "/" + uploadId, Unpooled.EMPTY_BUFFER, httpHeaders, EmptyHttpHeaders.INSTANCE);
        HttpContent httpContent = new DefaultHttpContent(Unpooled.wrappedBuffer(uploadBytes));
        LastHttpContent lastHttpContent = new DefaultLastHttpContent(Unpooled.EMPTY_BUFFER);
        channel.writeInbound(httpRequest, httpContent, lastHttpContent);
        Object o = channel.readOutbound();
        HttpResponse httpResponse = (HttpResponse) o;
        assertEquals(HTTP_1_1, httpResponse.protocolVersion());
        assertEquals(NO_CONTENT, httpResponse.status());
        assertThat(httpResponse.headers().get(TUS_RESUMABLE)).isEqualTo(TusVersions.preferred());
        assertThat(httpResponse.headers().get(HttpHeaderNames.CACHE_CONTROL)).isEqualToIgnoringCase(HttpHeaderValues.NO_STORE);
        assertThat(httpResponse.headers().get(TusHeaders.UPLOAD_OFFSET)).isEqualTo(String.valueOf(0));
        assertThat(httpResponse.headers().get(TusHeaders.UPLOAD_LENGTH)).isEqualTo(String.valueOf(0));
    }

    @BeforeEach
    public void setUp() throws IOException {
        FileSystem fs = Jimfs.newFileSystem(Configuration.unix());
        uploadManager = TusDefaultUploadManager.create(fs.getPath("/tmp"));
        channel = new EmbeddedChannel(new TusServerHandler("contextroot", uploadManager));
    }

    @Nested
    @DisplayName("OPTIONS request")
    class OptionsRequest {
        private HttpResponse httpResponse;

        @BeforeEach
        void setUp() {
            HttpRequest httpRequest = new DefaultFullHttpRequest(HTTP_1_1, OPTIONS, URL);
            channel.writeInbound(httpRequest);
            httpResponse = channel.readOutbound();
        }

        @Test
        @DisplayName("returns 204 No Content status")
        void returnsNoContent() {
            assertEquals(NO_CONTENT, httpResponse.status());
        }

        @Test
        @DisplayName("sets Tus-Resumable: supported version")
        void containsResumable() {
            assertThat(httpResponse.headers().get(TUS_RESUMABLE)).isEqualTo(TusVersions.preferred());
        }

        @Test
        @DisplayName("sets Tus-Version: list of supported versions")
        void containsVersions() {
            assertThat(TusHeadersUtil.headerAsList(httpResponse.headers(), TUS_VERSION)).hasSameElementsAs(TusVersions.supportedAsList());
        }
    }

    @Nested
    @DisplayName("HEAD request")
    class HeadRequest {

        @Nested
        @DisplayName("when no upload with given id exists")
        class NoUpload {
            private HttpResponse httpResponse;

            @BeforeEach
            void setUp() {
                HttpHeaders httpHeaders = new DefaultHttpHeaders().add(TUS_RESUMABLE, TusVersions.preferred());
                HttpRequest httpRequest = new DefaultFullHttpRequest(HTTP_1_1, HEAD, URL + "/" + UUID.randomUUID(), Unpooled.EMPTY_BUFFER, httpHeaders, EmptyHttpHeaders.INSTANCE);
                channel.writeInbound(httpRequest);
                httpResponse = channel.readOutbound();
            }

            @Test
            @DisplayName("returns NOT FOUND")
            void returnsNotFoundIfNoUpload() {
                assertEquals(NOT_FOUND, httpResponse.status());
            }

            @Test
            @DisplayName("sets Tus-Resumable: supported version")
            void setsResumable() {
                assertThat(httpResponse.headers().get(TUS_RESUMABLE)).isEqualTo(TusVersions.preferred());
            }

            @Test
            @DisplayName("sets Cache-Control: no-store")
            void setsCacheControl() {
                assertThat(httpResponse.headers().get(HttpHeaderNames.CACHE_CONTROL)).isEqualToIgnoringCase(HttpHeaderValues.NO_STORE);
            }
        }

        @Nested
        @DisplayName("when upload exists")
        class UploadExists {
            private HttpResponse httpResponse;
            private UploadDescriptor uploadDescriptor;

            @BeforeEach
            void setUp() throws IOException {
                String uploadId = UUID.randomUUID().toString();
                uploadDescriptor = uploadManager.createUpload(uploadId);
                uploadDescriptor.setLength(100);
                HttpHeaders httpHeaders = new DefaultHttpHeaders().add(TUS_RESUMABLE, TusVersions.preferred());
                HttpRequest httpRequest = new DefaultFullHttpRequest(HTTP_1_1, HEAD, URL + "/" + uploadId, Unpooled.EMPTY_BUFFER, httpHeaders, EmptyHttpHeaders.INSTANCE);
                channel.writeInbound(httpRequest);
                httpResponse = channel.readOutbound();
            }

            @Test
            @DisplayName("returns OK")
            void returnsOk() {
                assertEquals(OK, httpResponse.status());
            }

            @Test
            @DisplayName("sets Tus-Resumable: supported version")
            void setsResumable() {
                assertThat(httpResponse.headers().get(TUS_RESUMABLE)).isEqualTo(TusVersions.preferred());
            }

            @Test
            @DisplayName("sets Cache-Control: no-store")
            void setsCacheControl() {
                assertThat(httpResponse.headers().get(HttpHeaderNames.CACHE_CONTROL)).isEqualToIgnoringCase(HttpHeaderValues.NO_STORE);
            }

            @Test
            @DisplayName("sets Upload-Offset: uploaded data offset")
            void setsUploadOffset() {
                assertThat(httpResponse.headers().get(TusHeaders.UPLOAD_OFFSET)).isEqualTo(String.valueOf(uploadDescriptor.getOffset()));
            }

            @Test
            @DisplayName("sets Upload-Length: total upload length")
            void setsUploadLength() {
                assertThat(httpResponse.headers().get(TusHeaders.UPLOAD_LENGTH)).isEqualTo(String.valueOf(uploadDescriptor.getLength()));
            }
        }
    }

    @Nested
    @DisplayName("PATCH request")
    class PatchRequest {
        private HttpHeaders httpHeaders;

        @BeforeEach
        void setUp() {
            httpHeaders = new DefaultHttpHeaders().add(TUS_RESUMABLE, TusVersions.preferred());
        }

        @Nested
        @DisplayName("when upload exists")
        class UploadExists {
            private UploadDescriptor uploadDescriptor;
            private String uploadId;
            private byte[] uploadBytes;
            private byte[] lastBytes;

            @BeforeEach
            void setUp() throws IOException {
                uploadId = UUID.randomUUID().toString();
                uploadDescriptor = uploadManager.createUpload(uploadId);
                uploadBytes = "test_upload_string".getBytes();
                lastBytes = "end".getBytes();
                uploadDescriptor.setOffset(0);
                int length = uploadBytes.length + lastBytes.length;
                uploadDescriptor.setLength(length);
                httpHeaders.add(TusHeaders.UPLOAD_LENGTH, uploadDescriptor.getLength())
                        .add(HttpHeaderNames.CONTENT_TYPE, TusContentTypes.APPLICATION_OFFSET_OCTET_STREAM);
            }

            @Nested
            @DisplayName("with correct offset")
            class CorrectOffset {
                private HttpResponse httpResponse;

                @BeforeEach
                void setUp() {
                    httpHeaders.add(TusHeaders.UPLOAD_OFFSET, uploadDescriptor.getOffset());
                    HttpRequest httpRequest = new DefaultFullHttpRequest(HTTP_1_1, PATCH, URL + "/" + uploadId, Unpooled.EMPTY_BUFFER, httpHeaders, EmptyHttpHeaders.INSTANCE);
                    HttpContent httpContent = new DefaultHttpContent(Unpooled.wrappedBuffer(uploadBytes));
                    LastHttpContent lastHttpContent = new DefaultLastHttpContent(Unpooled.wrappedBuffer(lastBytes));
                    channel.writeInbound(httpRequest, httpContent, lastHttpContent);
                    httpResponse = channel.readOutbound();
                }

                @Test
                @DisplayName("returns No Content status")
                void returnsNoContent() {
                    assertEquals(NO_CONTENT, httpResponse.status());
                }

                @Test
                @DisplayName("sets Tus-Resumable: supported version")
                void containsResumable() {
                    assertThat(httpResponse.headers().get(TUS_RESUMABLE)).isEqualTo(TusVersions.preferred());
                }

                @Test
                @DisplayName("sets Cache-Control: no-store")
                void setsCacheControl() {
                    assertThat(httpResponse.headers().get(HttpHeaderNames.CACHE_CONTROL)).isEqualToIgnoringCase(HttpHeaderValues.NO_STORE);
                }

                @Test
                @DisplayName("sets Upload-Offset: uploaded data offset")
                void setsUploadOffset() {
                    assertThat(httpResponse.headers().get(TusHeaders.UPLOAD_OFFSET)).isEqualTo(String.valueOf(uploadDescriptor.getOffset()));
                }

                @Test
                @DisplayName("sets Upload-Length: total upload length")
                void setsUploadLength() {
                    assertThat(httpResponse.headers().get(TusHeaders.UPLOAD_LENGTH)).isEqualTo(String.valueOf(uploadDescriptor.getLength()));
                }

                @Test
                @DisplayName("writes all data")
                void writesData() throws IOException {
                    assertThat(Files.readAllBytes(uploadManager.getUploadedFile(uploadId))).isEqualTo(ArrayUtil.concatenate(uploadBytes, lastBytes));
                }
            }

            @Nested
            @DisplayName("with incorrect offset")
            class IncorrectOffset {
                private HttpResponse httpResponse;

                @BeforeEach
                void setUp() {
                    httpHeaders.add(TusHeaders.UPLOAD_OFFSET, uploadDescriptor.getOffset() + 1);
                    HttpRequest httpRequest = new DefaultFullHttpRequest(HTTP_1_1, PATCH, URL + "/" + uploadId, Unpooled.EMPTY_BUFFER, httpHeaders, EmptyHttpHeaders.INSTANCE);
                    HttpContent httpContent = new DefaultHttpContent(Unpooled.wrappedBuffer(uploadBytes));
                    LastHttpContent lastHttpContent = new DefaultLastHttpContent(Unpooled.wrappedBuffer(lastBytes));
                    channel.writeInbound(httpRequest, httpContent, lastHttpContent);
                    httpResponse = channel.readOutbound();
                }

                @Test
                @DisplayName("returns Conflict status")
                void returnsConflict() {
                    assertThat(httpResponse.status()).isEqualTo(CONFLICT);
                }
            }
        }

        @Nested
        @DisplayName("when no upload exists")
        class NoUpload {
            @BeforeEach
            void setUp() {
                httpHeaders.add(HttpHeaderNames.CONTENT_TYPE, TusContentTypes.APPLICATION_OFFSET_OCTET_STREAM);
            }
            @Test
            @DisplayName("returns NOT FOUND status")
            void returnsNotFound() {

            }
        }
    }

}