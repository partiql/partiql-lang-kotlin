package org.partiql.planner;

import org.jetbrains.annotations.NotNull;
import org.partiql.spi.errors.PErrorListener;

/**
 * This class is the default mechanism used to build a {@link PlannerConfig} object.
 */
public class PlannerConfigBuilder {

    private PErrorListener _listener = null;

    /**
     * If this is not invoked, the default, aborting error listener is used.
     * @param listener the listener that is invoked when the parser encounters errors/warnings.
     * @return the builder with the registered listener.
     * @see PErrorListener#abortOnError()
     */
    @NotNull
    public PlannerConfigBuilder setErrorListener(@NotNull PErrorListener listener) {
        _listener = listener;
        return this;
    }

    /**
     * @return a built {@link PlannerConfig}.
     */
    @NotNull
    public PlannerConfig build() {
        PErrorListener listener = _listener == null ? PErrorListener.abortOnError() : _listener;
        return new PlannerConfig() {
            @NotNull
            @Override
            public PErrorListener getErrorListener() {
                return listener;
            }
        };
    }
}
