package org.partiql.lang.typesystem.builtin.types.primitive

import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.typesystem.interfaces.type.BuiltInType

object StructType : BuiltInType() {
    override val typeAliases: List<String>
        get() = listOf("struct")

    override val exprValueType: ExprValueType
        get() = ExprValueType.STRUCT

    override val isPrimitiveType: Boolean
        get() = true
}
