package org.partiql.lang.typesystem.builtin.types.primitive

import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.typesystem.interfaces.type.BuiltInType

object ListType : BuiltInType() {
    override val typeAliases: List<String>
        get() = listOf("list")

    override val exprValueType: ExprValueType
        get() = ExprValueType.LIST

    override val isPrimitiveType: Boolean
        get() = true
}
