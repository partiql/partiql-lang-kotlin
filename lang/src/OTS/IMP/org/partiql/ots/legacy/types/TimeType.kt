package OTS.IMP.org.partiql.ots.legacy.types

import OTS.ITF.org.partiql.ots.type.ScalarType
import org.partiql.lang.eval.ExprValueType

data class TimeType(
    val withTimeZone: Boolean = false
) : ScalarType {
    override val id: String
        get() = "time"

    override val runTimeType: ExprValueType
        get() = ExprValueType.TIME

    override fun toString(): String = when (withTimeZone) {
        true -> "time with time zone"
        false -> "time"
    }
}
