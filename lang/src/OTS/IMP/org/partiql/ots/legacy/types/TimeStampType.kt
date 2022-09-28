package OTS.IMP.org.partiql.ots.legacy.types

import OTS.ITF.org.partiql.ots.type.ScalarType
import org.partiql.lang.eval.ExprValueType

object TimeStampType : ScalarType {
    override val id: String
        get() = "timestamp"

    override val runTimeType: ExprValueType
        get() = ExprValueType.TIMESTAMP
}
