package ots.legacy.types

import org.partiql.lang.eval.ExprValueType
import ots.type.ScalarType

object SymbolType : ScalarType {
    override val id: String
        get() = "symbol"

    override val runTimeType: ExprValueType
        get() = ExprValueType.SYMBOL
}
