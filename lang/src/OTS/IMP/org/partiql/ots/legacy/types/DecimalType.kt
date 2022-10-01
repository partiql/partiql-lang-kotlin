package OTS.IMP.org.partiql.ots.legacy.types

import OTS.ITF.org.partiql.ots.CompileTimeType
import OTS.ITF.org.partiql.ots.TypeParameters
import OTS.ITF.org.partiql.ots.type.ScalarType
import org.partiql.lang.ast.passes.SemanticException
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueType
import java.math.BigDecimal

object DecimalType : ScalarType {
    val compileTimeType: CompileTimeType = CompileTimeType(this, listOf())

    override val typeName = "decimal"

    override val aliases = listOf("decimal", "dec", "numeric")

    override fun validateParameters(typeParameters: TypeParameters) {
        require(typeParameters.size < 3) { error("$typeName type requires at most 2 parameter") }

        val decimalTypeParameters = DecimalTypeParameters(typeParameters)
        val precision = decimalTypeParameters.precision
        val scale = decimalTypeParameters.scale

        if (precision != null && scale !in 0..precision) {
            throw SemanticException(
                "Scale $scale should be between 0 and precision $precision",
                ErrorCode.SEMANTIC_INVALID_DECIMAL_PARAMETERS,
            )
        }
    }

    override val runTimeType: ExprValueType
        get() = ExprValueType.DECIMAL

    override fun validateValue(value: ExprValue, parameters: TypeParameters): Boolean {
        if (value.type != ExprValueType.DECIMAL) {
            return false
        }

        val decimalTypeParameters = DecimalTypeParameters(parameters)
        val precision = decimalTypeParameters.precision
            ?: return true // Unlimited DECIMAL
        val scale = decimalTypeParameters.scale

        val decimalValue = value.scalar.numberValue() as BigDecimal
        val dv = decimalValue.stripTrailingZeros()
        val integerDigits = dv.precision() - dv.scale()
        val expectedIntegerDigits = precision - scale

        return integerDigits <= expectedIntegerDigits && dv.scale() <= scale
    }
}

data class DecimalTypeParameters(val precision: Int?, val scale: Int) {
    /**
     * The maximum number of digits a decimal can hold after reserving digits for scale
     *
     * For example: The maximum value a DECIMAL(5,2) can represent is 999.99, therefore the maximum
     *  number of digits it can hold is 3 (i.e up to 999).
     */
    val maxDigits = (precision ?: Int.MAX_VALUE) - scale

    constructor(typeParameters: TypeParameters) : this(
        typeParameters.getOrElse(0) { null }, // Null indicates unlimited precision
        typeParameters.getOrElse(1) { 0 } // Sql standard requires DECIMAL type to have scale of 1 by default
    )
}
