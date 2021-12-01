package com.ivannikolaev.tus4j.proto;

public class TusHeaders {
    private TusHeaders() {
    }
    public static final String TUS_RESUMABLE = "Tus-Resumable";
    public static final String TUS_VERSION = "Tus-Version";
    public static final String TUS_MAX_SIZE = "Tus-Max-Size";
    public static final String TUS_EXTENSION = "Tus-Extension";
    public static final String UPLOAD_OFFSET = "Upload-Offset";
    public static final String UPLOAD_LENGTH = "Upload-Length";
    public static final String METHOD_OVERRIDE = "X-HTTP-Method-Override";
    public static final String UPLOAD_DEFER_LENGTH = "Upload-Defer-Length";
}
