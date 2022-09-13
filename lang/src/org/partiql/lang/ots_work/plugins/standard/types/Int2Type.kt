package org.partiql.lang.ots_work.plugins.standard.types

import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.eval.numberValue
import org.partiql.lang.ots_work.interfaces.TypeParameters
import org.partiql.lang.ots_work.interfaces.type.ScalarType

object Int2Type : ScalarType {
    val validRange = Short.MIN_VALUE.toLong()..Short.MAX_VALUE.toLong()

    override val id: String
        get() = "int2"

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
