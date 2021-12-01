package com.ivannikolaev.tus4j.util;

import java.util.Arrays;

public class ArrayUtil {
    private ArrayUtil() {
    }
    public static byte[] concatenate(byte[] a, byte[] b) {
        byte[] c = Arrays.copyOf(a, a.length + b.length);
        System.arraycopy(b, 0, c, a.length, b.length);
        return c;
    }
}
