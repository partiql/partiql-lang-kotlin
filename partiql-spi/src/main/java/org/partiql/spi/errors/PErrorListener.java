package org.partiql.spi.errors;

import org.jetbrains.annotations.NotNull;

/**
 * Interface for receiving errors from the PartiQL ecosystem.
 * @see PError
 * @see PRuntimeException
 */
public interface PErrorListener {
    /**
     * This method is called when an error/warning is emitted.
     * @param error The reported error/warning.
     * @throws PRuntimeException when the {@link PErrorListener} wants to halt execution of whichever component
     * the listener is registered to.
     */
    void report(@NotNull PError error) throws PRuntimeException;

    /**
     * Provides an implementation of {@link PErrorListener} that aborts upon encountering an error by throwing an
     * {@link PRuntimeException}.
     * @return an {@link PErrorListener} that throws an {@link PRuntimeException} upon encountering an error.
     */
    @NotNull
    static PErrorListener abortOnError() {
        return error -> {
            if (error.severity.code() == Severity.ERROR) {
                throw new PRuntimeException(error);
            }
        };
    }

    /**
     * Returns an instance of {@link PErrorListener} that chains the provided listeners.
     * @param listeners the listeners to chain.
     * @return a listener that invokes all provided listeners.
     */
    @NotNull
    static PErrorListener chain(PErrorListener... listeners) {
        return new PErrorListener() {
            @Override
            public void report(@NotNull PError error) throws PRuntimeException {
                for (PErrorListener listener : listeners) {
                    listener.report(error);
                }
            }
        };
    }
}
