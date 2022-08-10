package org.partiql.lang.ots.plugins.standard.types

import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.ots.interfaces.ScalarType
import org.partiql.lang.ots.interfaces.TypeParameters

object Int8Type : ScalarType {
    override val id: String
        get() = "int8"

    override val runTimeType: ExprValueType
        get() = ExprValueType.INT

    override fun createType(parameters: TypeParameters): CompileTimeInt8Type = CompileTimeInt8Type
}
