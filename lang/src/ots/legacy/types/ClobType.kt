package ots.legacy.types

import org.partiql.lang.eval.ExprValueType
import ots.type.ScalarType

object ClobType : ScalarType {
    override val id: String
        get() = "clob"

    override val runTimeType: ExprValueType
        get() = ExprValueType.CLOB
}
