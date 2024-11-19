package org.partiql.spi.errors;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * An implementation of an {@link PErrorListenerException} that wraps an {@link PError}.
 */
public class PErrorException extends PErrorListenerException {
    /**
     * The {@link PError} that is wrapped.
     */
    @NotNull
    public PError error;

    /**
     * Creates an exception that holds an error.
     * @param error the error that is wrapped
     */
    public PErrorException(@NotNull PError error) {
        this.error = error;
    }

    /**
     * Creates an exception that holds an error.
     * @param error the error that is wrapped
     * @param cause the cause of the error
     */
    public PErrorException(@NotNull PError error, @NotNull Throwable cause) {
        super(cause);
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
        if (!(o instanceof PErrorException)) return false;
        PErrorException that = (PErrorException) o;
        return Objects.equals(error, that.error);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(error);
    }
}
