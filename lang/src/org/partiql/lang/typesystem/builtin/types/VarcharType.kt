package org.partiql.lang.typesystem.builtin.types

import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.typesystem.interfaces.type.ParametricType
import org.partiql.lang.typesystem.interfaces.type.ScalarType
import org.partiql.lang.typesystem.interfaces.type.TypeParameters

/**
 * The standard sql type VARCHAR(n), where "n" is the maximum number of characters of the string literal
 */
object VarcharType : ScalarType, ParametricType {
    override val typeAliases: List<String>
        get() = listOf("varchar", "character varying")

    override val exprValueType: ExprValueType
        get() = ExprValueType.STRING

    override val requiredParameters: List<ScalarType> = emptyList()

    /**
     * Similar to [CharType]
     */
    override val optionalParameters: List<Pair<ScalarType, ExprValue?>> = CharType.optionalParameters

    /**
     * Similar to [CharType]
     */
    override fun validateParameters(parameters: TypeParameters) {
        CharType.validateParameters(parameters)
    }
}
