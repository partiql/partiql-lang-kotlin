package OTS.IMP.org.partiql.ots.legacy.types

import OTS.ITF.org.partiql.ots.type.ScalarType
import org.partiql.lang.eval.ExprValueType

object TimestampType : ScalarType {
    override val id = "timestamp"

    override val names = listOf("timestamp")

    override val runTimeType: ExprValueType
        get() = ExprValueType.TIMESTAMP
}
