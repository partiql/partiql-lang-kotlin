package OTS.IMP.org.partiql.ots.legacy.types

import OTS.ITF.org.partiql.ots.type.NonParametricType
import org.partiql.lang.eval.ExprValueType

object RealType : NonParametricType() {
    override val id = "real"

    override val names = listOf("real")

    override val runTimeType: ExprValueType
        get() = ExprValueType.FLOAT
}
