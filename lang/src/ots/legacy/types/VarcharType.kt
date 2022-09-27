package ots.legacy.types

import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueType
import ots.TypeParameters
import ots.type.ScalarType

object VarcharType : ScalarType {
    override val id: String
        get() = "varying_character"

    override val runTimeType: ExprValueType
        get() = ExprValueType.STRING

    override fun validateValue(value: ExprValue, parameters: TypeParameters): Boolean =
        when (value.type) {
            ExprValueType.STRING -> {
                val str = value.scalar.stringValue()!!
                val length = parameters[0]!! // Currently, if the parameter of CHAR or VARCHAR is not explicitly specified, it is considered as StringType
                length >= str.codePointCount(0, str.length)
            }
            else -> false
        }
}
