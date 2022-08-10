package org.partiql.lang.ots.plugins.standard.types

import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.ots.interfaces.CompileTimeType
import org.partiql.lang.ots.interfaces.ScalarType

data class CompileTimeVarcharType(
    val length: Int
) : CompileTimeType {
    override val type: ScalarType = VarcharType

    override fun validateValue(value: ExprValue): Boolean =
        when (value.type) {
            ExprValueType.STRING -> {
                val str = value.scalar.stringValue()!!
                length >= str.codePointCount(0, str.length)
            }
            else -> false
        }
}
