package org.partiql.lang.typesystem.builtin.types

import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.typesystem.interfaces.type.ParametricType
import org.partiql.lang.typesystem.interfaces.type.ScalarType
import org.partiql.lang.typesystem.interfaces.type.TypeParameters
import org.partiql.lang.util.asIonInt

/**
 * The standard sql type CHARACTER(n), where "n" is the fixed number of characters of the string literal
 */
object CharType : ScalarType, ParametricType {
    override val typeAliases: List<String>
        get() = listOf("char", "character")

    override val exprValueType: ExprValueType
        get() = ExprValueType.STRING

    override val requiredParameters: List<ScalarType> = emptyList()

    override val optionalParameters: List<Pair<ScalarType, ExprValue?>> = listOf(
        IntType to null
    )

    override fun validateParameters(parameters: TypeParameters) {
        // Check if length is larger than 0
        val length = parameters[0]
        val value = length.value.ionValue.asIonInt().longValue()
        if (value <= 0) {
            throw IllegalArgumentException("Compile Error: The parameter of $this type, length, should be larger than or equal to 0")
        }
    }
}
