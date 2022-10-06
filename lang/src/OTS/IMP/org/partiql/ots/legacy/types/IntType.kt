package OTS.IMP.org.partiql.ots.legacy.types

import OTS.ITF.org.partiql.ots.CompileTimeType
import OTS.ITF.org.partiql.ots.type.NonParametricType
import OTS.ITF.org.partiql.ots.type.TypeParameters
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.eval.numberValue

object IntType : NonParametricType() {
    val validRange = Long.MIN_VALUE..Long.MAX_VALUE

    val compileTimeType: CompileTimeType = CompileTimeType(this, emptyList())

    override val id = "integer"

    override val names = listOf("int", "integer")

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
