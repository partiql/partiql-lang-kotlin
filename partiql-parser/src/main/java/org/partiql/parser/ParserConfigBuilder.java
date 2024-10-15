package org.partiql.parser;

import org.jetbrains.annotations.NotNull;
import org.partiql.spi.errors.ErrorListener;

/**
 * This class is the default mechanism used to build a {@link ParserConfig} object.
 */
public class ParserConfigBuilder {

    private ErrorListener _listener = null;

    /**
     * If this is not invoked, the default, aborting error listener is used.
     * @param listener the listener that is invoked when the parser encounters errors/warnings.
     * @return the builder with the registered listener.
     * @see ErrorListener#abortOnError()
     */
    @NotNull
    public ParserConfigBuilder setErrorListener(@NotNull ErrorListener listener) {
        _listener = listener;
        return this;
    }

    /**
     * @return a built {@link ParserConfig}
     */
    @NotNull
    public ParserConfig build() {
        ErrorListener listener = _listener == null ? ErrorListener.abortOnError() : _listener;
        return new ParserConfig() {
            @NotNull
            @Override
            public ErrorListener getErrorListener() {
                return listener;
            }
        };
    }
}
