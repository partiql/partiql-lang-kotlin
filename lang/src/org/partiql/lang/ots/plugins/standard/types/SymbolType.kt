package org.partiql.lang.ots.plugins.standard.types

import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.ots.interfaces.type.ScalarType

object SymbolType : ScalarType {
    override val id: String
        get() = "symbol"

    override val runTimeType: ExprValueType
        get() = ExprValueType.SYMBOL
}
