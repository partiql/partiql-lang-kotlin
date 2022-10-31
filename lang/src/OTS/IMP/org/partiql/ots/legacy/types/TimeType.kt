package OTS.IMP.org.partiql.ots.legacy.types

import OTS.ITF.org.partiql.ots.type.ParametricType
import OTS.ITF.org.partiql.ots.type.TypeParameters
import org.partiql.lang.eval.ExprValueType

/**
 * We might need to remove [withTimeZone] in TIME type and separate it into TIME_WITH_TIME_ZONE
 * type & TIME_WITHOUT_TIME_ZONE type later
 *
 * The current modeling inherits the implementation of TIME type in the `main` branch
 */
data class TimeType(
    val withTimeZone: Boolean = false
) : ParametricType {
    override val id: String
        get() = when (withTimeZone) {
            true -> "time_with_time_zone"
            false -> "time"
        }

    override val names: List<String>
        get() = when (withTimeZone) {
            true -> listOf("time with time zone")
            false -> listOf("time")
        }

    override fun validateParameters(typeParameters: TypeParameters) {
        when (typeParameters.size) {
            0 -> {}
            1 -> require(typeParameters[0] > 0)
            2 -> error("$id type requires at most 1 parameter")
        }
    }

    override val runTimeType: ExprValueType
        get() = ExprValueType.TIME

    override fun toString(): String = when (withTimeZone) {
        true -> "time with time zone"
        false -> "time"
    }
}

data class TimeTypeParameter(val precision: Int?) {
    constructor(typeParameters: TypeParameters) : this(
        typeParameters.getOrElse(0) { null } // Null indicates unlimited precision
    )
}
