package OTS.IMP.org.partiql.ots.legacy.types

import OTS.ITF.org.partiql.ots.CompileTimeType
import OTS.ITF.org.partiql.ots.TypeParameters
import OTS.ITF.org.partiql.ots.type.ScalarType
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.eval.numberValue

object Int2Type : ScalarType {
    val validRange = Short.MIN_VALUE.toLong()..Short.MAX_VALUE.toLong()

    val compileTimeType: CompileTimeType = CompileTimeType(this, emptyList())

    override val id: String
        get() = "int2"

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
