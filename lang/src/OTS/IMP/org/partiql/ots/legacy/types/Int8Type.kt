package OTS.IMP.org.partiql.ots.legacy.types

import OTS.ITF.org.partiql.ots.CompileTimeType
import OTS.ITF.org.partiql.ots.TypeParameters
import OTS.ITF.org.partiql.ots.type.NonParametricType
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.eval.numberValue

object Int8Type : NonParametricType() {
    val validRange = Long.MIN_VALUE..Long.MAX_VALUE

    val compileTimeType: CompileTimeType = CompileTimeType(this, emptyList())

    override val typeName = "integer8"

    override val aliases = listOf("int8", "bigint", "integer8")

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
