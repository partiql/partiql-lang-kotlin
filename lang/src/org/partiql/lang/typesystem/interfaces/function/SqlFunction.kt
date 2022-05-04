package org.partiql.lang.typesystem.interfaces.function

import org.partiql.lang.eval.ExprValue
import org.partiql.lang.typesystem.interfaces.type.Type
import org.partiql.lang.typesystem.interfaces.type.TypeWithParameters

/**
 * Used to define a sql function.
 */
interface SqlFunction {
    /**
     * Function name
     */
    fun getFuncName(): String

    /**
     * Required arguments of this function. Empty list means no required arguments.
     */
    fun getRequiredArgTypes(): List<Type>

    /**
     * Optional arguments of this function. Empty list means no optional arguments.
     */
    fun getOptionalArgTypes(): List<Type>

    /**
     * Variadic argument of this function. Null value means no variadic argument.
     */
    fun getVariadicArgType(): Type?

    /**
     * Function return type inference
     */
    fun inferReturnType(): List<TypeWithParameters>

    /**
     * Function evaluation
     *
     * [arguments] consists of required arguments and optional arguments
     */
    fun invoke(arguments: List<ExprValue>): ExprValue
}