package org.partiql.eval

import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental

public sealed interface PartiQLResult {

    @OptIn(PartiQLValueExperimental::class)
    public data class Value(public val value: PartiQLValue) : PartiQLResult

    public data class Error(public val cause: Throwable) : PartiQLResult
}
