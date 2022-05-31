package org.partiql.lang.typesystem.builtin.types.nonprimitive

import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.typesystem.builtin.types.primitive.DecimalType
import org.partiql.lang.typesystem.interfaces.type.BuiltInType
import org.partiql.lang.typesystem.interfaces.type.ParametricType
import org.partiql.lang.typesystem.interfaces.type.SqlType
import org.partiql.lang.typesystem.interfaces.type.TypeParameters

/**
 * NUMERIC type is a synonym of DECIMAL type
 */
object NumericType : BuiltInType(), ParametricType {
    override val typeAliases: List<String>
        get() = listOf("numeric")

    override val exprValueType: ExprValueType
        get() = ExprValueType.DECIMAL

    override val parentType: SqlType
        get() = DecimalType

    override val isPrimitiveType: Boolean
        get() = false

    override val requiredParameters: List<SqlType> = emptyList()

    /**
     * Similar to [DecimalType]
     */
    override val optionalParameters: List<Pair<SqlType, ExprValue>> = DecimalType.optionalParameters

    /**
     * Similar to [DecimalType]
     */
    override fun validateParameters(parameters: TypeParameters) {
        DecimalType.validateParameters(parameters)
    }
}
