package org.partiql.lang.typesystem.builtin.types.nonprimitive

import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.typesystem.builtin.types.primitive.IntType
import org.partiql.lang.typesystem.builtin.types.valueFactory
import org.partiql.lang.typesystem.interfaces.type.BuiltInType
import org.partiql.lang.typesystem.interfaces.type.ParametricType
import org.partiql.lang.typesystem.interfaces.type.SqlType
import org.partiql.lang.typesystem.interfaces.type.TypeParameters
import org.partiql.lang.util.asIonInt

/**
 * The standard sql type CHARACTER(n), where "n" is the fixed number of characters of the string literal
 */
object CharType : BuiltInType(), ParametricType {
    private const val unboundedLength = Integer.MAX_VALUE
    private const val maxLength = Integer.MAX_VALUE - 1

    override val typeAliases: List<String>
        get() = listOf("char", "character")

    override val exprValueType: ExprValueType
        get() = ExprValueType.STRING

    override val isPrimitiveType: Boolean
        get() = false

    override val requiredParameters: List<SqlType> = emptyList()

    override val optionalParameters: List<Pair<SqlType, ExprValue>> = listOf(
        IntType to valueFactory.newInt(unboundedLength)
    )

    override fun validateParameters(parameters: TypeParameters) {
        // Check if length is larger than or equal to 0
        val length = parameters[0]
        val value = length.value.ionValue.asIonInt().longValue()
        if (value < 0) {
            throw IllegalArgumentException("Compile Error: The parameter of $this type, length, should be larger than or equal to 0")
        }
        if (value > maxLength) {
            throw IllegalArgumentException("Compile Error: The parameter of $this type, length, should be less than or equal to $maxLength")
        }
    }
}
