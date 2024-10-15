package org.partiql.parser;

import org.jetbrains.annotations.NotNull;
import org.partiql.spi.errors.ErrorListener;

/**
 * Represents the configuration for the {@link PartiQLParser}.
 *
 * @see PartiQLParser
 * @see ParserConfigBuilder
 */
public interface ParserConfig {
    /**
     * @return The error listener to be used by the compiler.
     */
    @NotNull
    ErrorListener getErrorListener();
}
