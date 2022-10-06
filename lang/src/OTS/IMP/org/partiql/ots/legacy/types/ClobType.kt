package OTS.IMP.org.partiql.ots.legacy.types

import OTS.ITF.org.partiql.ots.type.NonParametricType
import org.partiql.lang.eval.ExprValueType

object ClobType : NonParametricType() {
    override val id = "clob"

    override val names = listOf("clob")

    override val runTimeType: ExprValueType
        get() = ExprValueType.CLOB
}
