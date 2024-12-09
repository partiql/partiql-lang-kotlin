package org.partiql.spi.errors;

import org.jetbrains.annotations.NotNull;

/**
 * Interface for receiving errors from the PartiQL ecosystem.
 * @see PError
 * @see PErrorListenerException
 */
public interface PErrorListener {
    /**
     * This method is called when an error/warning is emitted.
     * @param error The reported error/warning.
     * @throws PErrorListenerException when the {@link PErrorListener} wants to halt execution of whichever component
     * the listener is registered to.
     */
    void report(@NotNull PError error) throws PErrorListenerException;

    /**
     * Provides an implementation of {@link PErrorListener} that aborts upon encountering an error by throwing an
     * {@link PErrorException}.
     * @return an {@link PErrorListener} that throws an {@link PErrorException} upon encountering an error.
     */
    // TODO: Should this be a static variable?
    @NotNull
    static PErrorListener abortOnError() {
        return error -> {
            if (error.severity.code() == Severity.ERROR) {
                if (error.code() == PError.INTERNAL_ERROR) {
                    Throwable cause = error.getOrNull("CAUSE", Throwable.class);
                    if (cause != null) {
                        throw new PErrorException(error, cause);
                    }
                }
                throw new PErrorException(error);
            }
        };
    }

    /**
     * Returns an instance of {@link PErrorListener} that chains the provided listeners.
     * @param listeners the listeners to chain.
     * @return a listener that invokes all provided listeners.
     */
    static PErrorListener chain(PErrorListener... listeners) {
        return new PErrorListener() {
            @Override
            public void report(@NotNull PError error) throws PErrorListenerException {
                for (PErrorListener listener : listeners) {
                    listener.report(error);
                }
            }
        };
    }
}
