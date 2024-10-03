package org.partiql.parser;

import org.jetbrains.annotations.NotNull;
import org.partiql.spi.errors.ErrorListener;

/**
 * Configuration options for the PartiQL parser.
 */
public interface ParserConfig {
    /**
     * @return The error listener to be used by the compiler.
     */
    @NotNull
    ErrorListener getErrorListener();
}
