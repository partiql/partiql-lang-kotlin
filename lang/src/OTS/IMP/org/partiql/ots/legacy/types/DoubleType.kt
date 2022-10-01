package OTS.IMP.org.partiql.ots.legacy.types

import OTS.ITF.org.partiql.ots.type.NonParametricType
import org.partiql.lang.eval.ExprValueType

object DoubleType : NonParametricType() {
    override val typeName = "double_precision"

    override val aliases = listOf("double_precision")

    override val runTimeType: ExprValueType
        get() = ExprValueType.FLOAT
}
