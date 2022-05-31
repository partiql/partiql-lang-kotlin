package org.partiql.lang.typesystem.builtin.types.nonprimitive

import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.typesystem.builtin.types.primitive.StringType
import org.partiql.lang.typesystem.interfaces.type.BuiltInType
import org.partiql.lang.typesystem.interfaces.type.ParametricType
import org.partiql.lang.typesystem.interfaces.type.SqlType
import org.partiql.lang.typesystem.interfaces.type.ValueWithType

object VarcharType : BuiltInType(), ParametricType {
    override val typeAliases: List<String>
        get() = listOf("varchar", "character varying")

    override val exprValueType: ExprValueType
        get() = ExprValueType.STRING

    override val parentType: SqlType
        get() = StringType

    override val isPrimitiveType: Boolean
        get() = false

    override val requiredParameters: List<SqlType> = emptyList()

    /**
     * Similar to [CharType]
     */
    override val optionalParameters: List<Pair<SqlType, ExprValue>> = CharType.optionalParameters

    /**
     * Similar to [CharType]
     */
    override fun validateParameters(parameters: List<ValueWithType>) {
        CharType.validateParameters(parameters)
    }
}
