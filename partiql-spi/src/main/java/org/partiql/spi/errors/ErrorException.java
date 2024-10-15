package org.partiql.spi.errors;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * An implementation of an {@link ErrorListenerException} that wraps an {@link Error}.
 */
public class ErrorException extends ErrorListenerException {
    /**
     * The error that is wrapped.
     */
    @NotNull
    public Error error;

    /**
     * Creates an exception that holds an error.
     * @param error the error that is wrapped
     */
    public ErrorException(@NotNull Error error) {
        this.error = error;
    }

    @Override
    public String toString() {
        return "ErrorException{" +
                "error=" + error +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ErrorException)) return false;
        ErrorException that = (ErrorException) o;
        return Objects.equals(error, that.error);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(error);
    }
}
