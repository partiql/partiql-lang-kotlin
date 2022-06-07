package org.partiql.lang.typesystem.builtin.types.primitive

import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.typesystem.interfaces.type.BuiltInType

object ClobType : BuiltInType() {
    override val typeAliases: List<String>
        get() = listOf("clob")

    override val exprValueType: ExprValueType
        get() = ExprValueType.CLOB

    override val isPrimitiveType: Boolean
        get() = true
}
