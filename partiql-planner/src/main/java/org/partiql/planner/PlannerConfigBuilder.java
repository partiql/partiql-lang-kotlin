package org.partiql.planner;

import org.jetbrains.annotations.NotNull;
import org.partiql.spi.errors.ErrorListener;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is the default mechanism used to build a {@link PlannerConfig} object.
 */
public class PlannerConfigBuilder {

    private final List<ErrorListener> _listeners = new ArrayList<>();

    @NotNull
    public PlannerConfigBuilder addErrorListener(@NotNull ErrorListener listener) {
        _listeners.add(listener);
        return this;
    }

    @NotNull
    public PlannerConfig build() {
        return new PlannerConfig() {
            @NotNull
            @Override
            public ErrorListener getErrorListener() {
                if (_listeners.isEmpty()) {
                    return ErrorListener.noop();
                }
                if (_listeners.size() == 1) {
                    return _listeners.get(0);
                }
                return ErrorListener.chain(_listeners.toArray(new ErrorListener[0]));
            }
        };
    }
}
