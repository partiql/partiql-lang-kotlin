package OTS.IMP.org.partiql.ots.legacy.types

import OTS.ITF.org.partiql.ots.type.ScalarType
import org.partiql.lang.eval.ExprValueType

object DateType : ScalarType {
    override val id = "date"

    override val names = listOf("date")

    override val runTimeType: ExprValueType
        get() = ExprValueType.DATE
}
