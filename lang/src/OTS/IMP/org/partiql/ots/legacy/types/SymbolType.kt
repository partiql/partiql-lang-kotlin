package OTS.IMP.org.partiql.ots.legacy.types

import OTS.ITF.org.partiql.ots.type.NonParametricType
import org.partiql.lang.eval.ExprValueType

object SymbolType : NonParametricType() {
    override val typeName = "symbol"

    override val aliases = listOf("symbol")

    override val runTimeType: ExprValueType
        get() = ExprValueType.SYMBOL
}
