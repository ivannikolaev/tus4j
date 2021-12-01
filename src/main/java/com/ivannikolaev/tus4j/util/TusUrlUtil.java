package com.ivannikolaev.tus4j.util;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;

public class TusUrlUtil {
    private TusUrlUtil() {
    }

    public static String uploadUrl(SocketAddress localAddress, String uploadId, String contextRoot) throws URISyntaxException {
        String path = "/" + contextRoot + "/" + uploadId;
        if (localAddress instanceof InetSocketAddress) {
            InetSocketAddress inetSocketAddress = (InetSocketAddress) localAddress;
            String host = inetSocketAddress.getHostName();
            int port = inetSocketAddress.getPort();
            URI uploadUri = new URI("http", null, host, port, path, null, null);
            return uploadUri.resolve(uploadId).toString();
        } else {
            return path;
        }
    }

    public static String trim(String s, char c) {
        if (s == null || s.length() == 0) {
            return s;
        }
        boolean first = s.charAt(0) == c;
        boolean last = s.length() > 1 && s.charAt(s.length() - 1) == c;
        int begin = first ? 1 : 0;
        int end = last ? s.length() - 1 : s.length();
        return s.substring(begin, end);
    }
}
