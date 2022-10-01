package OTS.IMP.org.partiql.ots.legacy.types

import OTS.ITF.org.partiql.ots.type.NonParametricType
import org.partiql.lang.eval.ExprValueType

object DateType : NonParametricType() {
    override val typeName = "date"

    override val aliases = listOf("date")

    override val runTimeType: ExprValueType
        get() = ExprValueType.DATE
}
