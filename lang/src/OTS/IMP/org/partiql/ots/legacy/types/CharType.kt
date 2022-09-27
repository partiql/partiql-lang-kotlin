package OTS.IMP.org.partiql.ots.legacy.types

import OTS.ITF.org.partiql.ots.TypeParameters
import OTS.ITF.org.partiql.ots.type.ScalarType
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueType

object CharType : ScalarType {
    override val id: String
        get() = "character"

    override val runTimeType: ExprValueType
        get() = ExprValueType.STRING

    override fun validateValue(value: ExprValue, parameters: TypeParameters): Boolean =
        when (value.type) {
            ExprValueType.STRING -> {
                val str = value.scalar.stringValue()!!
                val length = parameters[0]
                length == str.codePointCount(0, str.length)
            }
            else -> false
        }
}
