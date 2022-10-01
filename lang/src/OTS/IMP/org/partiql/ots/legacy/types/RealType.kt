package OTS.IMP.org.partiql.ots.legacy.types

import OTS.ITF.org.partiql.ots.type.NonParametricType
import org.partiql.lang.eval.ExprValueType

object RealType : NonParametricType() {
    override val typeName = "real"

    override val aliases = listOf("real")

    override val runTimeType: ExprValueType
        get() = ExprValueType.FLOAT
}
