package org.partiql.lang.typesystem.builtin.types.primitive

import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.typesystem.interfaces.type.BuiltInType

object DateType : BuiltInType() {
    override val typeAliases: List<String>
        get() = listOf("date")

    override val exprValueType: ExprValueType
        get() = ExprValueType.DATE

    override val isPrimitiveType: Boolean
        get() = true
}
