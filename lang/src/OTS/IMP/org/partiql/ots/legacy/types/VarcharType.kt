package OTS.IMP.org.partiql.ots.legacy.types

import OTS.ITF.org.partiql.ots.type.ParametricType
import OTS.ITF.org.partiql.ots.type.TypeParameters
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueType

object VarcharType : ParametricType {
    override val id = "character_varying"

    override val names = listOf("varchar", "character varying")

    override fun validateParameters(typeParameters: TypeParameters) {
        when (typeParameters.size) {
            0 -> {}
            1 -> require(typeParameters[0] > 0)
            2 -> error("$id type requires at most 1 parameter")
        }
    }

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
