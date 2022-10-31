package OTS.IMP.org.partiql.ots.legacy.types

import OTS.ITF.org.partiql.ots.CompileTimeType
import OTS.ITF.org.partiql.ots.type.ScalarType
import org.partiql.lang.eval.ExprValueType

object StringType : ScalarType {
    val compileTimeType: CompileTimeType = CompileTimeType(this, emptyList())

    override val id = "string"

    override val names = listOf("string")

    override val runTimeType: ExprValueType
        get() = ExprValueType.STRING
}
