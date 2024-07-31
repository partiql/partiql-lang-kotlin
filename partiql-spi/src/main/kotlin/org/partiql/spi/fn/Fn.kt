package org.partiql.spi.fn

import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental

/**
 * Represents a scalar function (SQL row-value call expression).
 */
public interface Fn {

    /**
     * Scalar function signature.
     */
    public val signature: FnSignature

    /**
     * Invoke the routine with the given arguments.
     *
     * @param args
     * @return
     */
    @OptIn(PartiQLValueExperimental::class)
    public fun invoke(args: Array<PartiQLValue>): PartiQLValue
}
