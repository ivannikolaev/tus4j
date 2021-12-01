package com.ivannikolaev.tus4j.util;

import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TusUrlTest {

    @Test
    void testUrlWithEmptyContextRoot() {
        String url = "/";
        TusUrl tusUrl = TusUrl.create(url);
        assertEquals("", tusUrl.contextRoot());
        assertEquals("", tusUrl.path());
    }

    @Test
    void testUrlWithNotEmptyContextRoot() {
        String url = "/contextroot";
        TusUrl tusUrl = TusUrl.create(url);
        assertEquals("contextroot", tusUrl.contextRoot());
        assertEquals("", tusUrl.path());
    }

    @Test
    void testUrlWithNotEmptyContextRootAndTrailingSlash() {
        String url = "/contextroot/";
        TusUrl tusUrl = TusUrl.create(url);
        assertEquals("contextroot", tusUrl.contextRoot());
        assertEquals("", tusUrl.path());
    }

    @Test
    void testUrlWithNotEmptyPath() {
        String url = "/contextroot/path1/path2";
        TusUrl tusUrl = TusUrl.create(url);
        assertEquals("contextroot", tusUrl.contextRoot());
        assertEquals("path1/path2", tusUrl.path());
    }

    @Test
    void testUrlWithNotEmptyPathAndTrailingSlash() {
        String url = "/contextroot/path1/path2/";
        TusUrl tusUrl = TusUrl.create(url);
        assertEquals("contextroot", tusUrl.contextRoot());
        assertEquals("path1/path2", tusUrl.path());
    }

    @Test
    void testUploadUrl() throws URISyntaxException {
        String url = "/contextroot";
        InetSocketAddress socketAddress = new InetSocketAddress("localhost", 80);
        String uploadUrl = TusUrlUtil.uploadUrl(socketAddress, "uploadId", "contextroot");
        assertEquals("http://localhost:80/contextroot/uploadId", uploadUrl);
    }
}