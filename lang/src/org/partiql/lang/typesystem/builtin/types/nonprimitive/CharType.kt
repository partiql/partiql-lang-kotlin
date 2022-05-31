package org.partiql.lang.typesystem.builtin.types.nonprimitive

import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.typesystem.builtin.types.primitive.IntType
import org.partiql.lang.typesystem.builtin.types.primitive.StringType
import org.partiql.lang.typesystem.builtin.types.valueFactory
import org.partiql.lang.typesystem.interfaces.type.BuiltInType
import org.partiql.lang.typesystem.interfaces.type.ParametricType
import org.partiql.lang.typesystem.interfaces.type.SqlType
import org.partiql.lang.typesystem.interfaces.type.ValueWithType
import org.partiql.lang.util.asIonInt

object CharType : BuiltInType(), ParametricType {
    override val typeAliases: List<String>
        get() = listOf("char", "character")

    override val exprValueType: ExprValueType
        get() = ExprValueType.STRING

    override val parentType: SqlType
        get() = StringType

    override val isPrimitiveType: Boolean
        get() = false

    override val requiredParameters: List<SqlType> = emptyList()

    override val optionalParameters: List<Pair<SqlType, ExprValue>> = listOf(
        IntType to valueFactory.newInt(Integer.MAX_VALUE)
    )

    override fun validateParameters(parameters: List<ValueWithType>) {
        // Check if length is larger than or equal to 0
        val length = parameters[0]
        val value = length.value.ionValue.asIonInt().longValue()
        if (value < 0) {
            throw IllegalArgumentException("Compile Error: The parameter of $this type, length, should be larger than or equal to 0")
        }
    }
}
