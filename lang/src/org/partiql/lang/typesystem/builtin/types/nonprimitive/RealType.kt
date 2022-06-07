package org.partiql.lang.typesystem.builtin.types.nonprimitive

import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.typesystem.interfaces.type.BuiltInType

/**
 * The standard sql type REAL, which is actually FLOAT4
 */
object RealType : BuiltInType() {
    override val typeAliases: List<String>
        get() = listOf("real")

    override val exprValueType: ExprValueType
        get() = ExprValueType.DECIMAL

    override val isPrimitiveType: Boolean
        get() = false
}
