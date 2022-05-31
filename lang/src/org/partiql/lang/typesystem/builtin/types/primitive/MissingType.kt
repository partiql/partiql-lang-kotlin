package org.partiql.lang.typesystem.builtin.types.primitive

import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.typesystem.interfaces.type.BuiltInType
import org.partiql.lang.typesystem.interfaces.type.SqlType

object MissingType : BuiltInType() {
    override val typeAliases: List<String>
        get() = listOf("missing")

    override val exprValueType: ExprValueType
        get() = ExprValueType.MISSING

    override val parentType: SqlType?
        get() = null

    override val isPrimitiveType: Boolean
        get() = true
}
