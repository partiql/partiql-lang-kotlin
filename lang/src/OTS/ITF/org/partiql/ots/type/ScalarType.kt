package OTS.ITF.org.partiql.ots.type

import OTS.ITF.org.partiql.ots.TypeParameters
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueType

/**
 * This interface is used to define a scalar type
 */
interface ScalarType {
    /**
     * A type name, recognized by users or developers to know which type it is. One example use case
     * is that, if we want to throw an error mentioning a scalar type in the error message, we can
     * use its [typeName] to refer to the type.
     *
     * Types cannot share the same type name
     */
    val typeName: String

    /**
     * Type aliases, recognized by program (PartiQL compiler). One example use case is that, in the expression
     * `CAST(1 AS <type_alias>)`, the target type of the CAST operator corresponds to the scalar type whose
     * [aliases] has `<type_alias>`
     *
     * Note that spaces are not allowed in type aliases. For special type aliases with space in standard sql, e.g.
     * `TIME WITH TIME ZONE` in the query `CAST(a AS TIME WITH TIME ZONE)`, the spaces there are replaced with
     * underscore '_' during parsing, so the query is actually transformed as `CAST(a AS TIME_WITH_TIME_ZONE)`.
     * All the special type aliases are:
     *      1. CHARACTER VARYING -> CHARACTER_VARYING
     *      2. DOUBLE PRECISION -> DOUBLE_PRECISION
     *      3. TIME WITH TIME ZONE -> TIME_WITH_TIME_ZONE
     *
     * One type can have multiple type aliases
     *
     * Types cannot share the same type alias
     *
     * Type alias cannot be a PartiQL keywords e.g. 'select'
     */
    val aliases: List<String>

    /**
     * Validate type parameters, which is called during compiler time by PartiQL compiler
     */
    fun validateParameters(typeParameters: TypeParameters)

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

abstract class NonParametricType : ScalarType {
    override fun validateParameters(typeParameters: TypeParameters) {
        require(typeParameters.isEmpty()) { "${typeName.toUpperCase()} type requires no type parameter" }
    }
}
