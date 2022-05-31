package org.partiql.lang.typesystem.builtin.types.primitive

import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.typesystem.interfaces.type.BuiltInType
import org.partiql.lang.typesystem.interfaces.type.SqlType

object StringType : BuiltInType() {
    override val typeAliases: List<String>
        get() = listOf("string")

    override val exprValueType: ExprValueType
        get() = ExprValueType.STRING

    override val parentType: SqlType?
        get() = null

    override val isPrimitiveType: Boolean
        get() = true
}
