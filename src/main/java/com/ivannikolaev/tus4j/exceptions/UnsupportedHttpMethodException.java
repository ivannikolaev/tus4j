package com.ivannikolaev.tus4j.exceptions;

public class UnsupportedHttpMethodException extends RuntimeException {
    public UnsupportedHttpMethodException(String message) {
        super(message);
    }
}
