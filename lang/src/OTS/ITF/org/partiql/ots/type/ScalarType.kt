package OTS.ITF.org.partiql.ots.type

import OTS.ITF.org.partiql.ots.TypeParameters
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueType

/**
 * This interface is used to define a scalar type
 */
interface ScalarType {
    /**
     * a type ID
     */
    val id: String

    /**
     * Run-time type
     */
    val runTimeType: ExprValueType

    /**
     * used to validate a value of this type
     *
     * [value] is the value we want to validate
     * [parameters] is type parameters of this type
     */
    fun validateValue(value: ExprValue, parameters: TypeParameters): Boolean =
        value.type == runTimeType
}
