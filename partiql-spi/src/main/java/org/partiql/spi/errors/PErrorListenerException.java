package org.partiql.spi.errors;

/**
 * This, along with subclasses, are to be thrown when any registered {@link PErrorListener} wants to halt execution.
 * <br>
 * Application developers are encouraged to write their own subclass to provide quality error reporting in their
 * applications.
 * @see PError
 * @see PErrorListener
 */
public class PErrorListenerException extends Exception {
    /**
     * Creates an exception with a null message and cause.
     */
    public PErrorListenerException() {
        super();
    }

    /**
     * Creates an exception with a cause.
     * @param cause the cause of the exception.
     */
    public PErrorListenerException(Throwable cause) {
        super(cause);
    }

    /**
     * Creates an exception with a message and a cause.
     * @param message the detail message
     * @param cause the cause of the exception
     */
    public PErrorListenerException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates an exception with a message.
     * @param message the detail message
     */
    public PErrorListenerException(String message) {
        super(message);
    }
}
