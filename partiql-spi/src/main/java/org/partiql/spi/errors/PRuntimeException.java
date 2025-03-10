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
     * You may TEMPORARILY uncomment this. Do not commit this overridden function.
     * @return the cause of this exception
     */
    @Override
    public synchronized Throwable getCause() {
        Throwable t = error.getOrNull("CAUSE", Throwable.class);
        if (t != null) {
            return t;
        }
        return super.getCause();
    }

    /**
     * This DOES NOT fill in the stack trace. This is intentional, as it incurs a hefty computational cost.
     * @return the same, untouched exception.
     */
    @Override
    public synchronized Throwable fillInStackTrace() {
        // This method is normally extremely expensive, especially in permissive mode.
        // TODO!
        return super.fillInStackTrace();
    }
}
