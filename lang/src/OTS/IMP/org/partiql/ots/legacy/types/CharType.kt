package OTS.IMP.org.partiql.ots.legacy.types

import OTS.ITF.org.partiql.ots.type.ParametricType
import OTS.ITF.org.partiql.ots.type.TypeParameters
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueType

object CharType : ParametricType {
    override val id = "character"

    override val names = listOf("character", "char")

    override fun validateParameters(typeParameters: TypeParameters) {
        when (typeParameters.size) {
            0 -> {}
            1 -> require(typeParameters[0] > 0)
            else -> error("$id type requires at most 1 parameter")
        }
    }

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
