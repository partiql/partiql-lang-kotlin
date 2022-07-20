package org.partiql.lang.typesystem.builtin.types

import com.amazon.ion.IonType
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.typesystem.interfaces.type.ScalarType

/**
 * Refers to [IonType.SYMBOL]
 */
object SymbolType : ScalarType {
    override val typeAliases: List<String>
        get() = listOf("symbol")

    override val exprValueType: ExprValueType
        get() = ExprValueType.SYMBOL
}
