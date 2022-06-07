package org.partiql.lang.typesystem.builtin.types.nonprimitive

import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.typesystem.interfaces.type.BuiltInType

object TupleType : BuiltInType() {
    override val typeAliases: List<String>
        get() = listOf("tuple")

    override val exprValueType: ExprValueType
        get() = ExprValueType.STRUCT

    override val isPrimitiveType: Boolean
        get() = false
}
