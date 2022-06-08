package org.partiql.lang.typesystem.builtin.types

import com.amazon.ion.IonType
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.typesystem.interfaces.type.BuiltInType
import org.partiql.lang.typesystem.interfaces.type.ParametricType
import org.partiql.lang.typesystem.interfaces.type.SqlType
import org.partiql.lang.typesystem.interfaces.type.TypeParameters
import org.partiql.lang.util.asIonInt

/**
 * The standard sql type FLOAT(p), where "p" refers to precision. The value of "p" should
 * be larger than 0 and less than 54
 *
 * FLOAT(1) to FLOAT (24) is considered as REAL type, which is actually FLOAT4. FLOAT(25)
 * to FLOAT(53) is considered as DOUBLE_PRECISION type, which is actually FLOAT8
 *
 * Also refers to [IonType.FLOAT]
 */
object FloatType : BuiltInType(), ParametricType {
    override val typeAliases: List<String>
        get() = listOf("float")

    override val exprValueType: ExprValueType
        get() = ExprValueType.FLOAT

    override val requiredParameters: List<SqlType> = emptyList()

    override val optionalParameters: List<Pair<SqlType, ExprValue>> = listOf(
        IntType to valueFactory.newInt(53)
    )

    override fun validateParameters(parameters: TypeParameters) {
        // Check if precision is larger than or equal to 0 and less than 54
        val precision = parameters[0]
        val value = precision.value.ionValue.asIonInt().longValue()
        if (value < 0 || value >= 54) {
            throw IllegalArgumentException("Compile Error: The parameter of $this type, precision, should be larger than or equal to 0 and less than 54")
        }
    }
}
