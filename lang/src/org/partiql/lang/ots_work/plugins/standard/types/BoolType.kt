package org.partiql.lang.ots_work.plugins.standard.types

import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.ots_work.interfaces.ScalarType

object BoolType : ScalarType {
    override val id: String
        get() = "bool"

    override val runTimeType: ExprValueType
        get() = ExprValueType.BOOL
}
