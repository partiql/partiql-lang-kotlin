package OTS.IMP.org.partiql.ots.legacy.types

import OTS.ITF.org.partiql.ots.type.ScalarType
import org.partiql.lang.eval.ExprValueType

object SymbolType : ScalarType {
    override val id: String
        get() = "symbol"

    override val runTimeType: ExprValueType
        get() = ExprValueType.SYMBOL
}
