package org.partiql.lang.ots.plugins.standard.types

import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.eval.numberValue
import org.partiql.lang.ots.interfaces.CompileTimeType
import org.partiql.lang.ots.interfaces.ScalarType

object CompileTimeIntType : CompileTimeType {
    val validRange = Long.MIN_VALUE..Long.MAX_VALUE

    override val type: ScalarType = IntType

    override fun validateValue(value: ExprValue): Boolean {
        if (value.type != ExprValueType.INT) {
            return false
        }

        val longValue = value.numberValue().toLong()

        return validRange.contains(longValue)
    }
}
