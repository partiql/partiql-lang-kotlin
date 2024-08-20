package org.partiql.eval

import org.partiql.planner.catalog.Session

/**
 * Represents a compiled PartiQL statement ready for execution.
 */
public interface PartiQLStatement {

    public fun execute(session: Session): PartiQLResult
}
