package org.partiql.eval

import org.partiql.spi.catalog.Session

/**
 * Represents a compiled PartiQL statement ready for execution.
 */
public interface PartiQLStatement {

    public fun execute(session: Session): PartiQLResult
}
