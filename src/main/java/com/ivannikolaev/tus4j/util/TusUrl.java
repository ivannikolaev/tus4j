package com.ivannikolaev.tus4j.util;

import java.util.Arrays;

public class TusUrl {
    private final String url;
    private String contextRoot;
    private String path;

    private TusUrl(String url) {
        this.url = url;
    }

    public String contextRoot() {
        return contextRoot;
    }

    public String path() {
        return path;
    }

    private TusUrl build() {
        String[] components = TusUrlUtil.trim(url, '/').split("/");
        contextRoot = components.length > 0
                ? components[0]
                : "";
        path = components.length > 1
                ? String.join("/", Arrays.copyOfRange(components, 1, components.length))
                : "";
        return this;
    }

    public static TusUrl create(String url) {
        TusUrl tusUrl = new TusUrl(url);
        return tusUrl.build();
    }

}
