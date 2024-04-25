package org.partiql.eval

import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental

/**
 * Represents a compiled PartiQL Plan ready for execution.
 */
sealed interface PartiQLStatement<T> {

    public fun execute(): T

    @OptIn(PartiQLValueExperimental::class)
    interface Query : PartiQLStatement<PartiQLValue>

    @OptIn(PartiQLValueExperimental::class)
    interface Ddl : PartiQLStatement<PartiQLValue>
}
