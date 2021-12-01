package com.ivannikolaev.tus4j.proto;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TusExtensions {
    private static final String supportedExtensions;
    private static final List<String> supportedExtensionsList;

    static {
        ArrayList<String> extensions = new ArrayList<>();
        Collections.addAll(extensions, "creation", "creation-with-upload", "creation-defer-length");
        supportedExtensionsList = Collections.unmodifiableList(extensions);
        supportedExtensions = String.join(",", supportedExtensionsList);
    }

    private TusExtensions() {
    }

    public static String supported() {
        return supportedExtensions;
    }

    public static List<String> supportedAsList() {
        return supportedExtensionsList;
    }
}
