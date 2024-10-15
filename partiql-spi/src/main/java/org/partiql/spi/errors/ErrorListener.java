package org.partiql.spi.errors;

import org.jetbrains.annotations.NotNull;

/**
 * Interface for receiving errors from the PartiQL ecosystem.
 */
public interface ErrorListener {
    /**
     * This method is called when an execution error occurs.
     * @param error The reported error.
     */
    void error(@NotNull Error error) throws ErrorListenerException;

    /**
     * This method is called when a warning occurs.
     * @param error The reported warning.
     */
    void warning(@NotNull Error error) throws ErrorListenerException;

    /**
     * Provides an implementation of {@link ErrorListener} that aborts upon encountering an error by throwing an
     * {@link ErrorException}.
     * @return an {@link ErrorListener} that throws an {@link ErrorException} upon encountering an error.
     */
    @NotNull
    static ErrorListener abortOnError() {
        return new ErrorListener() {
            @Override
            public void error(@NotNull Error error) throws ErrorException {
                throw new ErrorException(error);
            }

            @Override
            public void warning(@NotNull Error error) {
                // Do nothing
            }
        };
    }

    /**
     * Returns an instance of {@link ErrorListener} that chains the provided listeners.
     * @param listeners the listeners to chain.
     * @return a listener that invokes all provided listeners.
     */
    static ErrorListener chain(ErrorListener... listeners) {
        return new ErrorListener() {
            @Override
            public void error(@NotNull Error error) throws ErrorListenerException {
                for (ErrorListener listener : listeners) {
                    listener.error(error);
                }
            }

            @Override
            public void warning(@NotNull Error error) throws ErrorListenerException {
                for (ErrorListener listener : listeners) {
                    listener.warning(error);
                }
            }
        };
    }
}
