package OTS.IMP.org.partiql.ots.legacy.types

import OTS.ITF.org.partiql.ots.type.NonParametricType
import org.partiql.lang.eval.ExprValueType

object TimeStampType : NonParametricType() {
    override val id = "timestamp"

    override val names = listOf("timestamp")

    override val runTimeType: ExprValueType
        get() = ExprValueType.TIMESTAMP
}
