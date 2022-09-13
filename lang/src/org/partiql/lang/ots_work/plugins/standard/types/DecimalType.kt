package org.partiql.lang.ots_work.plugins.standard.types

import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.ots_work.interfaces.TypeParameters
import org.partiql.lang.ots_work.interfaces.type.ScalarType
import java.math.BigDecimal

object DecimalType : ScalarType {
    override val id: String
        get() = "decimal"

    override val runTimeType: ExprValueType
        get() = ExprValueType.DECIMAL

    override fun validateValue(value: ExprValue, parameters: TypeParameters): Boolean {
        if (value.type != ExprValueType.DECIMAL) {
            return false
        }

        val precision = parameters[0]
        val scale = parameters[1] ?: 0 // Scale of DECIMAL by default is 0

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
