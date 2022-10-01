package OTS.ITF.org.partiql.ots.type

import OTS.ITF.org.partiql.ots.CompileTimeType
import org.partiql.lang.eval.ExprValueType

object BoolType : NonParametricType() {
    val compileTimeType = CompileTimeType(this, emptyList())

    override val typeName = "boolean"

    override val aliases = listOf("bool", "boolean")

    override val runTimeType: ExprValueType
        get() = ExprValueType.BOOL
}
