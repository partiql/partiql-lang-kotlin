package org.partiql.lang.ots.plugins.standard.types

import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.ots.interfaces.CompileTimeType
import org.partiql.lang.ots.interfaces.ScalarType
import java.math.BigDecimal

data class CompileTimeDecimalType(
    val precision: Int?,
    val scale: Int
) : CompileTimeType {
    override val type: ScalarType = DecimalType

    override fun validateValue(value: ExprValue): Boolean {
        if (value.type != ExprValueType.DECIMAL) {
            return false
        }
        if (precision == null) { // Unlimited DECIMAL
            return true
        }

        val decimalValue = value.scalar.numberValue() as BigDecimal
        val dv = decimalValue.stripTrailingZeros()
        val integerDigits = dv.precision() - dv.scale()
        val expectedIntegerDigits = precision - scale

        return integerDigits <= expectedIntegerDigits && dv.scale() <= scale
    }
}
