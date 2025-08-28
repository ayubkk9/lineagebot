package com.lineagebot;

public class ScreenReadException extends Exception {
    public ScreenReadException(String message) {
        super(message);
    }
    public ScreenReadException(String message, Throwable cause) {
        super(message, cause);
    }
}
