package OTS.ITF.org.partiql.ots.type

import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueType

/**
 * This interface is used to define a scalar type
 */
interface ScalarType {
    /**
     * The type id, recognized by users or developers to know which type it is. One example use case
     * is that, if we want to throw an error mentioning a scalar type in the error message, we can
     * refer to its [id].
     *
     * Types cannot share the same type id
     */
    val id: String

    /**
     * Type names, recognized by program (PartiQL compiler). One use case is that, e.g., in the expression
     * `CAST(1 AS <type_names>)`, the target type of the CAST operator corresponds to the scalar type whose
     * [names] has `<type_name>`
     *
     * One type can have multiple type names
     *
     * Types cannot share the same type name
     *
     * Type name cannot be a PartiQL keywords e.g. 'select'
     */
    val names: List<String>

    /**
     * Validate type parameters, which is called during compiler time by PartiQL compiler
     *
     * The default implementation is for non-parametric types
     */
    fun validateParameters(typeParameters: TypeParameters) {
        require(typeParameters.isEmpty()) { "${id.toUpperCase()} type requires no type parameter" }
    }

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

interface ParametricType : ScalarType {
    /**
     * Forcing implementation with declaring override without a body;
     */
    override fun validateParameters(typeParameters: TypeParameters)
}
