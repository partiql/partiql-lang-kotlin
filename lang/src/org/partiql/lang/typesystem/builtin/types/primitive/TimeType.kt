package org.partiql.lang.typesystem.builtin.types.primitive

import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.typesystem.builtin.types.valueFactory
import org.partiql.lang.typesystem.interfaces.type.BuiltInType
import org.partiql.lang.typesystem.interfaces.type.ParametricType
import org.partiql.lang.typesystem.interfaces.type.SqlType
import org.partiql.lang.typesystem.interfaces.type.TypeParameters
import org.partiql.lang.util.asIonInt

/**
 * The standard sql type TIME, e.g. '10:23:54'
 */
object TimeType : BuiltInType(), ParametricType {
    override val typeAliases: List<String>
        get() = listOf("time")

    override val exprValueType: ExprValueType
        get() = ExprValueType.TIME

    override val isPrimitiveType: Boolean
        get() = true

    override val requiredParameters: List<SqlType> = emptyList()

    override val optionalParameters: List<Pair<SqlType, ExprValue>> = listOf(
        IntType to valueFactory.newInt(Integer.MAX_VALUE)
    )

    override fun validateParameters(parameters: TypeParameters) {
        // Check if precision is larger than or equal to 0
        val precision = parameters[0]
        val value = precision.value.ionValue.asIonInt().longValue()
        if (value < 0) {
            throw IllegalArgumentException("Compile Error: The parameter of $this type, precision, should be larger than or equal to 0")
        }
    }
}
