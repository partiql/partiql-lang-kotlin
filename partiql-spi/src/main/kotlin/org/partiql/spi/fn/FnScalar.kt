package org.partiql.spi.fn

import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental

/**
 * Represents an SQL row-value expression call.
 */
@FnExperimental
public interface FnScalar : Fn {

    /**
     * Scalar function signature.
     */
    public override val signature: FnSignature.Scalar

    /**
     * Invoke the routine with the given arguments.
     *
     * @param args
     * @return
     */
    @OptIn(PartiQLValueExperimental::class)
    public fun invoke(args: Array<PartiQLValue>): PartiQLValue
}
