package org.partiql.lang.typesystem.builtin.types.primitive

import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.typesystem.interfaces.type.BuiltInType

object BooleanType : BuiltInType() {
    override val typeAliases: List<String>
        get() = listOf("bool", "boolean")

    override val exprValueType: ExprValueType
        get() = ExprValueType.BOOL

    override val isPrimitiveType: Boolean
        get() = true
}
