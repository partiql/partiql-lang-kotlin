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
                val length = CharTypeParameter(parameters).length
                length == str.codePointCount(0, str.length)
            }
            else -> false
        }
}

data class CharTypeParameter(val length: Int) {
    constructor(typeParameters: TypeParameters) : this(
        typeParameters.getOrElse(0) { 1 } // Sql standard requires CHAR type to have length of 1 by default
    )
}
