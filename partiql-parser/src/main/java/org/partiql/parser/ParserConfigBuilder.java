package org.partiql.parser;

import org.jetbrains.annotations.NotNull;
import org.partiql.spi.errors.PErrorListener;

/**
 * This class is the default mechanism used to build a {@link ParserConfig} object.
 */
public class ParserConfigBuilder {

    private PErrorListener _listener = null;

    /**
     * If this is not invoked, the default, aborting error listener is used.
     * @param listener the listener that is invoked when the parser encounters errors/warnings.
     * @return the builder with the registered listener.
     * @see PErrorListener#abortOnError()
     */
    @NotNull
    public ParserConfigBuilder setErrorListener(@NotNull PErrorListener listener) {
        _listener = listener;
        return this;
    }

    /**
     * @return a built {@link ParserConfig}
     */
    @NotNull
    public ParserConfig build() {
        PErrorListener listener = _listener == null ? PErrorListener.abortOnError() : _listener;
        return new ParserConfig() {
            @NotNull
            @Override
            public PErrorListener getErrorListener() {
                return listener;
            }
        };
    }
}
