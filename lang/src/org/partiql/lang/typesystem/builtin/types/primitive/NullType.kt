package org.partiql.lang.typesystem.builtin.types.primitive

import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.typesystem.interfaces.type.BuiltInType
import org.partiql.lang.typesystem.interfaces.type.SqlType

object NullType : BuiltInType() {
    override val typeAliases: List<String>
        get() = listOf("null")

    override val exprValueType: ExprValueType
        get() = ExprValueType.NULL

    override val parentType: SqlType?
        get() = null

    override val isPrimitiveType: Boolean
        get() = true
}
