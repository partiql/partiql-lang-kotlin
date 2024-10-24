package org.partiql.spi;

import org.jetbrains.annotations.NotNull;
import org.partiql.spi.errors.PErrorListener;

/**
 * Represents a common set of arguments across multiple major PartiQL components.
 * @see Context#standard()
 * @see Context#of(PErrorListener)
 */
public interface Context {

    /**
     * The default utilizes {@link PErrorListener#abortOnError()}.
     * @return the registered {@link PErrorListener}.
     */
    @NotNull
    default PErrorListener getErrorListener() {
        return PErrorListener.abortOnError();
    }

    /**
     * The default utilizes {@link PErrorListener#abortOnError()} for the {@link Context#getErrorListener()}.
     * @return the default implementation of {@link Context}.
     */
    // TODO: Should this be a static variable?
    static Context standard() {
        return new Context() {};
    }

    /**
     * 
     * @param listener the {@link PErrorListener} to provide to {@link Context#getErrorListener()}.
     * @return an implementation of {@link Context}.
     */
    static Context of(@NotNull PErrorListener listener) {
        return new Context() {
            @NotNull
            @Override
            public PErrorListener getErrorListener() {
                return listener;
            }
        };
    }
}
