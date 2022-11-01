package OTS.IMP.org.partiql.ots.legacy.types

import OTS.ITF.org.partiql.ots.CompileTimeType
import OTS.ITF.org.partiql.ots.type.ScalarType
import OTS.ITF.org.partiql.ots.type.TypeParameters
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.eval.numberValue

object Int4Type : ScalarType {
    val validRange = Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()

    val compileTimeType: CompileTimeType = CompileTimeType(this, emptyList())

    override val id = "integer4"

    override val names = listOf("int4", "integer4")

    override val runTimeType: ExprValueType
        get() = ExprValueType.INT

    override fun validateValue(value: ExprValue, parameters: TypeParameters): Boolean {
        if (value.type != ExprValueType.INT) {
            return false
        }

        val longValue = value.numberValue().toLong()

        return validRange.contains(longValue)
    }
}
