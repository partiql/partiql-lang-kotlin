package OTS.IMP.org.partiql.ots.legacy.types

import OTS.ITF.org.partiql.ots.type.NonParametricType
import org.partiql.lang.eval.ExprValueType

object DoubleType : NonParametricType() {
    override val id = "double_precision"

    override val names = listOf("double_precision")

    override val runTimeType: ExprValueType
        get() = ExprValueType.FLOAT
}
