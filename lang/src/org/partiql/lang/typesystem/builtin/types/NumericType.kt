package org.partiql.lang.typesystem.builtin.types

import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.typesystem.interfaces.type.ParametricType
import org.partiql.lang.typesystem.interfaces.type.ScalarType
import org.partiql.lang.typesystem.interfaces.type.TypeParameters

/**
 * NUMERIC type is a synonym of DECIMAL type
 */
object NumericType : ScalarType, ParametricType {
    override val typeAliases: List<String>
        get() = listOf("numeric")

    override val exprValueType: ExprValueType
        get() = ExprValueType.DECIMAL

    override val requiredParameters: List<ScalarType> = emptyList()

    /**
     * Similar to [DecimalType]
     */
    override val optionalParameters: List<Pair<ScalarType, ExprValue>> = DecimalType.optionalParameters

    /**
     * Similar to [DecimalType]
     */
    override fun validateParameters(parameters: TypeParameters) {
        DecimalType.validateParameters(parameters)
    }
}
