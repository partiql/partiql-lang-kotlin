package org.partiql.parser;

import org.jetbrains.annotations.NotNull;
import org.partiql.spi.errors.ErrorListener;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is the default mechanism used to build a {@link ParserConfig} object.
 */
public class ParserConfigBuilder {

    private final List<ErrorListener> _listeners = new ArrayList<>();

    @NotNull
    public ParserConfigBuilder addErrorListener(@NotNull ErrorListener listener) {
        _listeners.add(listener);
        return this;
    }

    @NotNull
    public ParserConfig build() {
        return new ParserConfig() {
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
