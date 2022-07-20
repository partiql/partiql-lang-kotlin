package org.partiql.lang.typesystem.builtin.types

import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.typesystem.interfaces.type.ParametricType
import org.partiql.lang.typesystem.interfaces.type.ScalarType
import org.partiql.lang.typesystem.interfaces.type.TypeParameters
import org.partiql.lang.util.asIonInt

/**
 * The standard sql type TIME, e.g. '10:23:54'
 */
object TimeType : ScalarType, ParametricType {
    override val typeAliases: List<String>
        get() = listOf("time")

    override val exprValueType: ExprValueType
        get() = ExprValueType.TIME

    override val requiredParameters: List<ScalarType> = emptyList()

    override val optionalParameters: List<Pair<ScalarType, ExprValue>> = listOf(
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
