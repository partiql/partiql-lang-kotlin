package org.partiql.parser;

import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.partiql.spi.errors.PErrorListener;

/**
 * Represents the configuration for the {@link PartiQLParser}.
 *
 * @see PartiQLParser
 */
@lombok.Builder(builderClassName = "Builder")
public final class ParserContext {

    @lombok.Builder.Default
    @NonNull
    private final PErrorListener listener = PErrorListener.abortOnError();

    /**
     * @return The error listener to be used by the compiler.
     */
    @NotNull
    public PErrorListener getErrorListener() {
        return listener;
    }
}
