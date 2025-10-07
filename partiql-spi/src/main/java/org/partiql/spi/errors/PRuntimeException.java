package org.partiql.spi.errors;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * An implementation of a {@link RuntimeException} that wraps a {@link PError}. This class is open for extension and is
 * used for PartiQL's error reporting mechanism. For more information, please see relevant documentation on
 * <a href="https://www.partiql.org">the PartiQL website</a>.
 * @see PRuntimeException#error
 */
public class PRuntimeException extends RuntimeException {

    @NotNull
    private final PError error;

    /**
     * The {@link PError} that is wrapped.
     * @see PError
     * @see PError#code()
     * @see PError#getOrNull(String, Class)
     * @see PError#getListOrNull(String, Class)
     */
    @NotNull
    public PError getError() {
        return error;
    }

    /**
     * Creates an exception that holds an error.
     * @param error the error that is wrapped
     */
    public PRuntimeException(@NotNull PError error) {
        this.error = error;
    }

    @Override
    public String toString() {
        return "PRuntimeException{" +
                "error=" + error +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PRuntimeException)) return false;
        PRuntimeException that = (PRuntimeException) o;
        return Objects.equals(error, that.error);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(error);
    }

    /**
     * This will return null.
     * @see PError#getOrNull(String, Class)
     * @see PError#INTERNAL_ERROR
     * @return null
     */
    @Override
    public synchronized Throwable getCause() {
        // DEVELOPERS: You may TEMPORARILY uncomment the following 4 lines during debugging. Do not commit the lines uncommented.
        // Throwable t = error.getOrNull("CAUSE", Throwable.class);
        // if (t != null) {
        //     return t;
        // }
        return null;
    }

    /**
     * This intentionally does not fill in the stack trace, as it is extremely expensive, especially in permissive mode.
     * @return the same, untouched exception.
     */
    @Override
    public synchronized Throwable fillInStackTrace() {
        // DEVELOPERS: You may TEMPORARILY uncomment the following line during debugging. Do not commit the lines uncommented.
        // return super.fillInStackTrace();
        return this;
    }
}
