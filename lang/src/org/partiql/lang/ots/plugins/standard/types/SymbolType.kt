package org.partiql.lang.ots.plugins.standard.types

import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.ots.interfaces.ScalarType
import org.partiql.lang.ots.interfaces.TypeParameters

object SymbolType : ScalarType {
    override val id: String
        get() = "symbol"

    override val runTimeType: ExprValueType
        get() = ExprValueType.SYMBOL

    override fun createType(parameters: TypeParameters): CompileTimeSymbolType = CompileTimeSymbolType
}
