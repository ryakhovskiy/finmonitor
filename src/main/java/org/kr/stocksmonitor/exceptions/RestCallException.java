package org.kr.stocksmonitor.exceptions;

public class RestCallException extends Exception {
    public RestCallException(String message) {
        super(message);
    }

    public RestCallException(Throwable cause) {
        super(cause);
    }

    public RestCallException(String message, Throwable cause) {
        super(message, cause);
    }
}
