package OTS.ITF.org.partiql.ots.type

import OTS.ITF.org.partiql.ots.CompileTimeType
import org.partiql.lang.eval.ExprValueType

object BoolType : ScalarType {
    val compileTimeType = CompileTimeType(this, emptyList())

    override val id: String
        get() = "bool"

    override val runTimeType: ExprValueType
        get() = ExprValueType.BOOL
}
