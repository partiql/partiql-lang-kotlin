package org.partiql.eval;

import org.jetbrains.annotations.NotNull;
import org.partiql.spi.value.Datum;

/**
 * An executable statement.
 * <br>
 * Developer Note: Consider `Datum execute(Parameters parameters)` for DML.
 */
public interface Statement {

    /**
     * Executes the statement with no parameters.
     *
     * @return Datum execution result.
     */
    @NotNull
    public Datum execute();
}
