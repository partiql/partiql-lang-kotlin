package org.partiql.eval;

import org.jetbrains.annotations.NotNull;
import org.partiql.spi.value.Datum;

/**
 * An executable statement.
 */
public interface Statement {

    /**
     * Executes the prepared statement.
     *
     * @return Datum execution result.
     */
    @NotNull
    public Datum execute();
}
