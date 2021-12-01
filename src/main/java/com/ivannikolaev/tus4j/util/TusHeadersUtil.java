package com.ivannikolaev.tus4j.util;

import io.netty.handler.codec.http.HttpHeaders;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class TusHeadersUtil {
    private TusHeadersUtil() {

    }

    public static List<String> headerAsList(HttpHeaders headers, String name) {
        String headerValue = headers.get(name);
        return headerValue == null
                ? Collections.emptyList()
                : Arrays.stream(headerValue.split(",")).map(String::trim).collect(Collectors.toList());
    }
}
