package org.partiql.eval;

import org.jetbrains.annotations.NotNull;
import org.partiql.spi.errors.ErrorListener;

/**
 * Represents the configuration for the {@link PartiQLEngine}.
 *
 * @see PartiQLEngine
 * @see CompilerConfigBuilder
 */
public interface CompilerConfig {
    /**
     * @return The error listener to be used by the compiler.
     */
    @NotNull
    ErrorListener getErrorListener();

    /**
     * @return The mode to be used by the compiler.
     */
    @NotNull
    PartiQLEngine.Mode getMode();
}
