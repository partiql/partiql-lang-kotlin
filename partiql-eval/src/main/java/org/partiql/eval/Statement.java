package org.partiql.eval;

import org.jetbrains.annotations.NotNull;
import org.partiql.spi.errors.PErrorException;
import org.partiql.spi.value.Datum;

/**
 * An executable statement.
 */
public interface Statement {

    /**
     * Converts the compiled statement into a (sometimes lazily-evaluated) {@link Datum} that can be used to execute
     * the statement. For scalar expressions, this is typically materialized immediately. For non-scalar expressions,
     * this may be materialized lazily. Consumers of this API should be aware that the returned {@link Datum} may be
     * evaluated multiple times. Please see more documentation on <a href="https://www.partiql.org">the PartiQL website</a>.
     * @return Datum execution result.
     * @throws PErrorException if an error was encountered during execution
     */
    @NotNull
    public Datum execute() throws PErrorException;
}
