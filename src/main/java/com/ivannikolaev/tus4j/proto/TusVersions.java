package com.ivannikolaev.tus4j.proto;

import java.util.Collections;
import java.util.List;

public class TusVersions {
    private static final List<String> supportedVersionsList = Collections.singletonList("1.0.0");
    private static final String supported = String.join(",", supportedVersionsList);
    private static final String preferredVersion = "1.0.0";

    private TusVersions() {
    }

    public static List<String> supportedAsList() {
        return supportedVersionsList;
    }
    public static String supported() {
        return supported;
    }
    public static String preferred() {
        return preferredVersion;
    }

}
