package org.partiql.lang.typesystem.builtin.types.primitive

import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.typesystem.builtin.types.valueFactory
import org.partiql.lang.typesystem.interfaces.type.BuiltInType
import org.partiql.lang.typesystem.interfaces.type.ParametricType
import org.partiql.lang.typesystem.interfaces.type.SqlType
import org.partiql.lang.typesystem.interfaces.type.TypeParameters
import org.partiql.lang.util.asIonInt

object FloatType : BuiltInType(), ParametricType {
    override val typeAliases: List<String>
        get() = listOf("float")

    override val exprValueType: ExprValueType
        get() = ExprValueType.FLOAT

    override val isPrimitiveType: Boolean
        get() = true

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
