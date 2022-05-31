package org.partiql.lang.typesystem.builtin.types.nonprimitive

import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.typesystem.builtin.types.primitive.DecimalType
import org.partiql.lang.typesystem.interfaces.type.BuiltInType
import org.partiql.lang.typesystem.interfaces.type.SqlType

/**
 * DOUBLE_PRECISION type is actually FLOAT8
 */
object DoublePrecisionType : BuiltInType() {
    override val typeAliases: List<String>
        get() = listOf("double precision")

    override val exprValueType: ExprValueType
        get() = ExprValueType.DECIMAL

    override val parentType: SqlType
        get() = DecimalType

    override val isPrimitiveType: Boolean
        get() = false
}
