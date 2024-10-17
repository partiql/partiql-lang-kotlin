package org.partiql.eval;

import org.jetbrains.annotations.NotNull;
import org.partiql.spi.errors.PErrorListener;

/**
 * This class is the default mechanism used to build a {@link CompilerConfig} object.
 */
public class CompilerConfigBuilder {

    private PartiQLEngine.Mode _mode = PartiQLEngine.Mode.STRICT;

    private PErrorListener _listener = null;

    /**
     * If this is not invoked, the default, aborting error listener is used.
     * @param listener the listener that is invoked when the parser encounters errors/warnings.
     * @return the builder with the registered listener.
     * @see PErrorListener#abortOnError()
     */
    @NotNull
    public CompilerConfigBuilder setErrorListener(@NotNull PErrorListener listener) {
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
        PErrorListener listener = _listener == null ? PErrorListener.abortOnError() : _listener;
        return new CompilerConfig() {
            @NotNull
            @Override
            public PErrorListener getErrorListener() {
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
