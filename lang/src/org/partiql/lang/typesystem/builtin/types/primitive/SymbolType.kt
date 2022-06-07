package org.partiql.lang.typesystem.builtin.types.primitive

import com.amazon.ion.IonType
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.typesystem.interfaces.type.BuiltInType

/**
 * Refers to [IonType.SYMBOL]
 */
object SymbolType : BuiltInType() {
    override val typeAliases: List<String>
        get() = listOf("symbol")

    override val exprValueType: ExprValueType
        get() = ExprValueType.SYMBOL

    override val isPrimitiveType: Boolean
        get() = true
}
