package org.partiql.eval

import org.partiql.value.PartiQLCursor
import org.partiql.value.PartiQLValueExperimental

public sealed interface PartiQLResult {

    @OptIn(PartiQLValueExperimental::class)
    public data class Value(public val value: PartiQLCursor) : PartiQLResult

    public data class Error(public val cause: Throwable) : PartiQLResult
}
