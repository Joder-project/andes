package org.andes.lock.core;

public class AndesException extends RuntimeException {
    public AndesException() {
    }

    public AndesException(String message) {
        super(message);
    }

    public AndesException(String message, Throwable cause) {
        super(message, cause);
    }

    public AndesException(Throwable cause) {
        super(cause);
    }

    public AndesException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
