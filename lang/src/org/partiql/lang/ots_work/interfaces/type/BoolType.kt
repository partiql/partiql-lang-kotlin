package org.partiql.lang.ots_work.interfaces.type

import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.ots_work.interfaces.CompileTimeType

object BoolType : ScalarType {
    val compileTimeType = CompileTimeType(this, emptyList())

    override val id: String
        get() = "bool"

    override val runTimeType: ExprValueType
        get() = ExprValueType.BOOL
}
