package ots.legacy.types

import org.partiql.lang.eval.ExprValueType
import ots.type.ScalarType

object DateType : ScalarType {
    override val id: String
        get() = "date"

    override val runTimeType: ExprValueType
        get() = ExprValueType.DATE
}
