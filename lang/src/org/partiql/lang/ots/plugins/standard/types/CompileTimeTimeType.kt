package org.partiql.lang.ots.plugins.standard.types

import org.partiql.lang.eval.ExprValue
import org.partiql.lang.ots.interfaces.CompileTimeType
import org.partiql.lang.ots.interfaces.ScalarType

data class CompileTimeTimeType(
    val precision: Int? = null,
    val withTimeZone: Boolean = false
) : CompileTimeType {
    override val type: ScalarType = TimeType

    override fun validateValue(value: ExprValue): Boolean =
        value.type == type.runTimeType

    override fun toString(): String = when (withTimeZone) {
        true -> "time with time zone"
        false -> "time"
    }
}
