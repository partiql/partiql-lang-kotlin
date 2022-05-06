package org.partiql.lang.typesystem.interfaces.function

import org.partiql.lang.typesystem.interfaces.type.Type
import org.partiql.lang.typesystem.interfaces.type.ValueWithType

/**
 * Used to define a sql function.
 */
interface SqlFunction {
    /**
     * Function name
     */
    val funcName: String

    /**
     * Required arguments of this function. Empty list means no required arguments.
     */
    val requiredArgTypes: List<Type>

    /**
     * Optional arguments of this function. Empty list means no optional arguments.
     */
    val optionalArgTypes: List<Type>

    /**
     * Variadic argument of this function. Null value means no variadic argument.
     */
    val variadicArgType: Type?

    /**
     * Type assigned to the return value
     */
    val returnType: Type

    /**
     * Function evaluation
     *
     * [arguments] represents values of arguments with assigned type passed to this function
     */
    fun invoke(arguments: List<ValueWithType>): ValueWithType
}
