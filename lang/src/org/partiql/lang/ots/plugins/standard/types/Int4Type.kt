package org.partiql.lang.ots.plugins.standard.types

import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.eval.numberValue
import org.partiql.lang.ots.interfaces.CompileTimeType
import org.partiql.lang.ots.interfaces.TypeParameters
import org.partiql.lang.ots.interfaces.type.ScalarType

object Int4Type : ScalarType {
    val validRange = Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()

    val compileTimeType: CompileTimeType = CompileTimeType(this, emptyList())

    override val id: String
        get() = "int4"

    override val runTimeType: ExprValueType
        get() = ExprValueType.INT

    override fun validateValue(value: ExprValue, parameters: TypeParameters): Boolean {
        if (value.type != ExprValueType.INT) {
            return false
        }

        val longValue = value.numberValue().toLong()

        return validRange.contains(longValue)
    }
}
