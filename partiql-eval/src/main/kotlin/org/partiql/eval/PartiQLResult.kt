package org.partiql.eval

import org.partiql.eval.value.Datum

public sealed interface PartiQLResult {

    public data class Value(public val value: Datum) : PartiQLResult

    public data class Error(public val cause: Throwable) : PartiQLResult
}
