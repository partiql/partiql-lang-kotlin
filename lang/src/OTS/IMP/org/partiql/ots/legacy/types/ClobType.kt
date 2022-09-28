package OTS.IMP.org.partiql.ots.legacy.types

import OTS.ITF.org.partiql.ots.type.ScalarType
import org.partiql.lang.eval.ExprValueType

object ClobType : ScalarType {
    override val id: String
        get() = "clob"

    override val runTimeType: ExprValueType
        get() = ExprValueType.CLOB
}
