package ots.type

import org.partiql.lang.eval.ExprValueType
import ots.CompileTimeType

object BoolType : ScalarType {
    val compileTimeType = CompileTimeType(this, emptyList())

    override val id: String
        get() = "bool"

    override val runTimeType: ExprValueType
        get() = ExprValueType.BOOL
}
