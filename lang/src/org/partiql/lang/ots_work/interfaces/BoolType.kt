package org.partiql.lang.ots_work.interfaces

import org.partiql.lang.eval.ExprValueType

object BoolType : ScalarType {
    override val id: String
        get() = "bool"

    override val runTimeType: ExprValueType
        get() = ExprValueType.BOOL
}
