package org.partiql.lang.ots.plugins.standard.types

import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.ots.interfaces.ScalarType
import org.partiql.lang.ots.interfaces.TypeParameters

object BoolType : ScalarType {
    override val id: String
        get() = "bool"

    override val runTimeType: ExprValueType
        get() = ExprValueType.BOOL

    override fun createType(parameters: TypeParameters): CompileTimeBoolType = CompileTimeBoolType
}
