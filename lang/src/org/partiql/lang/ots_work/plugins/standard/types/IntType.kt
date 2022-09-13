package org.partiql.lang.ots_work.plugins.standard.types

import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.eval.numberValue
import org.partiql.lang.ots_work.interfaces.TypeParameters
import org.partiql.lang.ots_work.interfaces.type.ScalarType

object IntType : ScalarType {
    val validRange = Long.MIN_VALUE..Long.MAX_VALUE

    override val id: String
        get() = "int"

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
