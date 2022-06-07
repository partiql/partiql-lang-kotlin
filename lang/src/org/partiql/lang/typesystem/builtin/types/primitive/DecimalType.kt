package org.partiql.lang.typesystem.builtin.types.primitive

import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.typesystem.builtin.types.valueFactory
import org.partiql.lang.typesystem.interfaces.type.BuiltInType
import org.partiql.lang.typesystem.interfaces.type.ParametricType
import org.partiql.lang.typesystem.interfaces.type.SqlType
import org.partiql.lang.typesystem.interfaces.type.TypeParameters
import org.partiql.lang.util.asIonInt
import com.amazon.ion.IonType

/**
 * The standard sql type DECIMAL(p, s), where "p" refers to precision (maximum number of digits)
 * and "s" refers to scale (number of digits to the right of the decimal point)
 *
 * The value of "s" should be always less than the value of "p"
 *
 * Also refers to [IonType.DECIMAL]
 */
object DecimalType : BuiltInType(), ParametricType {
    override val typeAliases: List<String>
        get() = listOf("dec", "decimal")

    override val exprValueType: ExprValueType
        get() = ExprValueType.DECIMAL

    override val isPrimitiveType: Boolean
        get() = true

    override val requiredParameters: List<SqlType> = emptyList()

    override val optionalParameters: List<Pair<SqlType, ExprValue>> = listOf(
        IntType to valueFactory.newInt(Integer.MAX_VALUE),
        IntType to valueFactory.newInt(0)
    )

    override fun validateParameters(parameters: TypeParameters) {
        // Check if value of precision is larger than or equal to 0
        val precision = parameters[0]
        val valueOfPrecision = precision.value.ionValue.asIonInt().longValue()
        if (valueOfPrecision < 0) {
            throw IllegalArgumentException("Compile Error: The first parameter of $this type, precision, must be larger than or equal to 0")
        }

        // Check if the value of scale is larger than or equal to 0, and also less than value of precision
        val scale = parameters[1]
        val valueOfScale = precision.value.ionValue.asIonInt().longValue()
        if (valueOfScale < 0) {
            throw IllegalArgumentException("Compile Error: The second parameter of $this type, scale, must be larger than or equal to 0")
        }
        if (valueOfScale >= valueOfPrecision) {
            throw IllegalArgumentException("Compile Error: The second parameter of $this type, scale, must be less than value of precision")
        }
    }
}
