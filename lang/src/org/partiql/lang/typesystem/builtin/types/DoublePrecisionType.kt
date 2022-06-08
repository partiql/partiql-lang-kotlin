package org.partiql.lang.typesystem.builtin.types

import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.typesystem.interfaces.type.BuiltInType

/**
 * The standard sql type DOUBLE PRECISION, which is actually FLOAT8
 */
object DoublePrecisionType : BuiltInType() {
    override val typeAliases: List<String>
        get() = listOf("double precision")

    override val exprValueType: ExprValueType
        get() = ExprValueType.DECIMAL
}
