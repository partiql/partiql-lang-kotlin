package org.partiql.lang.typesystem.builtin.types.nonprimitive

import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.typesystem.builtin.types.primitive.TimeType
import org.partiql.lang.typesystem.interfaces.type.BuiltInType
import org.partiql.lang.typesystem.interfaces.type.ParametricType
import org.partiql.lang.typesystem.interfaces.type.SqlType
import org.partiql.lang.typesystem.interfaces.type.TypeParameters

object TimeWithTimeZoneType : BuiltInType(), ParametricType {
    override val typeAliases: List<String>
        get() = listOf("time with time zone")

    override val exprValueType: ExprValueType
        get() = ExprValueType.TIME

    override val parentType: SqlType
        get() = TimeType

    override val isPrimitiveType: Boolean
        get() = false

    override val requiredParameters: List<SqlType> = emptyList()

    /**
     * Similar to [TimeType]
     */
    override val optionalParameters: List<Pair<SqlType, ExprValue>> = TimeType.optionalParameters

    /**
     * Similar to [TimeType]
     */
    override fun validateParameters(parameters: TypeParameters) {
        TimeType.validateParameters(parameters)
    }
}
