package org.partiql.lang.typesystem.builtin.types.primitive

import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.typesystem.interfaces.type.BuiltInType

object SymbolType : BuiltInType() {
    override val typeAliases: List<String>
        get() = listOf("symbol")

    override val exprValueType: ExprValueType
        get() = ExprValueType.SYMBOL

    override val isPrimitiveType: Boolean
        get() = true
}
