package org.partiql.eval;

import org.jetbrains.annotations.NotNull;
import org.partiql.spi.errors.ErrorListener;

/**
 * This class is the default mechanism used to build a {@link CompilerConfig} object.
 */
public class CompilerConfigBuilder {

    private PartiQLEngine.Mode _mode = PartiQLEngine.Mode.STRICT;

    private ErrorListener _listener = null;

    /**
     * If this is not invoked, the default, aborting error listener is used.
     * @param listener the listener that is invoked when the parser encounters errors/warnings.
     * @return the builder with the registered listener.
     * @see ErrorListener#abortOnError()
     */
    @NotNull
    public CompilerConfigBuilder setErrorListener(@NotNull ErrorListener listener) {
        _listener = listener;
        return this;
    }

    /**
     * If this is not invoked, {@link PartiQLEngine.Mode#STRICT} is enabled by default.
     * @param mode the mode for execution.
     * @return the builder with the set mode.
     */
    @NotNull
    public CompilerConfigBuilder setMode(@NotNull PartiQLEngine.Mode mode) {
        _mode = mode;
        return this;
    }

    /**
     * @return a built {@link CompilerConfig}.
     */
    @NotNull
    public CompilerConfig build() {
        ErrorListener listener = _listener == null ? ErrorListener.abortOnError() : _listener;
        return new CompilerConfig() {
            @NotNull
            @Override
            public ErrorListener getErrorListener() {
                return listener;
            }

            @NotNull
            @Override
            public PartiQLEngine.Mode getMode() {
                return _mode;
            }
        };
    }
}
