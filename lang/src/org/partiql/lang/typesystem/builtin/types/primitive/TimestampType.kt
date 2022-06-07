package org.partiql.lang.typesystem.builtin.types.primitive

import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.typesystem.interfaces.type.BuiltInType

object TimestampType : BuiltInType() {
    override val typeAliases: List<String>
        get() = listOf("timestamp")

    override val exprValueType: ExprValueType
        get() = ExprValueType.TIMESTAMP

    override val isPrimitiveType: Boolean
        get() = true
}
