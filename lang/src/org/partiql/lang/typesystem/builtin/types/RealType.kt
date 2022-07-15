package org.partiql.lang.typesystem.builtin.types

import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.typesystem.interfaces.type.ScalarType

/**
 * The standard sql type REAL, which is actually FLOAT4
 */
object RealType : ScalarType {
    override val typeAliases: List<String>
        get() = listOf("real")

    override val exprValueType: ExprValueType
        get() = ExprValueType.DECIMAL
}
