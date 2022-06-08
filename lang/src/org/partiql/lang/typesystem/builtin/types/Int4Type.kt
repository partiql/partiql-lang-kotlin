package org.partiql.lang.typesystem.builtin.types

import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.typesystem.interfaces.type.BuiltInType

/**
 * The same as [IntType]
 */
object Int4Type : BuiltInType() {
    override val typeAliases: List<String>
        get() = listOf("int4", "integer4")

    override val exprValueType: ExprValueType
        get() = ExprValueType.INT
}
