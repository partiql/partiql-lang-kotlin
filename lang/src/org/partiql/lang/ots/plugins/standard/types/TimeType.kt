package org.partiql.lang.ots.plugins.standard.types

import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.ots.interfaces.ScalarType

data class TimeType(
    val withTimeZone: Boolean = false
) : ScalarType {
    override val id: String
        get() = "time"

    override val runTimeType: ExprValueType
        get() = ExprValueType.TIME

    override fun toString(): String = when (withTimeZone) {
        true -> "time with time zone"
        false -> "time"
    }
}
