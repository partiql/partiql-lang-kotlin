package ots.legacy.types

import org.partiql.lang.eval.ExprValueType
import ots.type.ScalarType

object TimeStampType : ScalarType {
    override val id: String
        get() = "timestamp"

    override val runTimeType: ExprValueType
        get() = ExprValueType.TIMESTAMP
}
