package org.partiql.eval;

import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.partiql.spi.errors.PErrorListener;

/**
 * Represents the configuration for the {@link PartiQLEngine}.
 *
 * @see PartiQLEngine
 */
@lombok.Builder(builderClassName = "Builder")
public final class CompilerContext {

    @lombok.Builder.Default
    @NonNull
    private final PErrorListener listener = PErrorListener.abortOnError();

    @lombok.Builder.Default
    @NonNull
    private final PartiQLEngine.Mode mode = PartiQLEngine.Mode.STRICT;

    /**
     * @return The error listener to be used by the compiler.
     */
    @NotNull
    public PErrorListener getErrorListener() {
        return listener;
    }

    /**
     * @return The mode to be used by the compiler.
     */
    @NotNull
    public PartiQLEngine.Mode getMode() {
        return mode;
    }
}
