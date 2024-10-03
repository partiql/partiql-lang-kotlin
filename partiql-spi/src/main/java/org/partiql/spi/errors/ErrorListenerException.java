package org.partiql.spi.errors;

/**
 * This is to be thrown when any registered {@link ErrorListener} wants to halt execution. This wraps the actual
 * error to potentially be presented to the end user.
 */
public class ErrorListenerException extends Exception {
    public ErrorListenerException(Throwable cause) {
        super(cause);
    }
}
