package OTS.IMP.org.partiql.ots.legacy.types

import OTS.ITF.org.partiql.ots.TypeParameters
import OTS.ITF.org.partiql.ots.type.ScalarType
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueType

object VarcharType : ScalarType {
    override val id: String
        get() = "varying_character"

    override val runTimeType: ExprValueType
        get() = ExprValueType.STRING

    override fun validateValue(value: ExprValue, parameters: TypeParameters): Boolean =
        when (value.type) {
            ExprValueType.STRING -> {
                val str = value.scalar.stringValue()!!
                val length = VarcharTypeParameter(parameters).length
                length === null || length >= str.codePointCount(0, str.length)
            }
            else -> false
        }
}

data class VarcharTypeParameter(val length: Int?) {
    constructor(typeParameters: TypeParameters) : this(
        typeParameters.getOrElse(0) { null } // Null indicates unlimited length
    )
}
