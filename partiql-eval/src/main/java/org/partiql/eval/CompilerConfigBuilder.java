package org.partiql.eval;

import org.jetbrains.annotations.NotNull;
import org.partiql.spi.errors.ErrorListener;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is the default mechanism used to build a {@link CompilerConfig} object.
 */
public class CompilerConfigBuilder {

    private PartiQLEngine.Mode _mode = PartiQLEngine.Mode.STRICT;

    private final List<ErrorListener> _listeners = new ArrayList<>();

    @NotNull
    public CompilerConfigBuilder addErrorListener(@NotNull ErrorListener listener) {
        _listeners.add(listener);
        return this;
    }

    @NotNull
    public CompilerConfigBuilder setMode(@NotNull PartiQLEngine.Mode mode) {
        _mode = mode;
        return this;
    }

    @NotNull
    public CompilerConfig build() {
        return new CompilerConfig() {
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

            @NotNull
            @Override
            public PartiQLEngine.Mode getMode() {
                return null;
            }
        };
    }
}
