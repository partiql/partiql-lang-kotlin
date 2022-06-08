package org.partiql.lang.typesystem.builtin.types

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
}
