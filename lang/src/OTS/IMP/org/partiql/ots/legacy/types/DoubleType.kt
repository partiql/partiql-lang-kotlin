package OTS.IMP.org.partiql.ots.legacy.types

import OTS.ITF.org.partiql.ots.type.ScalarType
import org.partiql.lang.eval.ExprValueType

object DoubleType : ScalarType {
    override val id = "double_precision"

    override val names = listOf("double precision")

    override val runTimeType: ExprValueType
        get() = ExprValueType.FLOAT
}
