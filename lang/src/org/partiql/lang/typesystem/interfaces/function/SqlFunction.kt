package org.partiql.lang.typesystem.interfaces.function

import org.partiql.lang.eval.ExprValue
import org.partiql.lang.typesystem.interfaces.type.CompileTimeType
import org.partiql.lang.typesystem.interfaces.type.SqlType
import org.partiql.lang.typesystem.interfaces.type.TypeParameters

/**
 * Used to define a sql function, which takes arguments of sql types.
 */
interface SqlFunction {
    /**
     * Function name
     */
    val funcName: String

    /**
     * Required arguments of this function. Empty list means no required arguments.
     */
    val requiredArgTypes: List<SqlType>

    /**
     * Optional arguments of this function. Empty list means no optional arguments.
     */
    val optionalArgTypes: List<SqlType>

    /**
     * Variadic argument of this function. Null value means no variadic argument.
     */
    val variadicArgType: SqlType?

    /**
     * Return type inference
     *
     * [argsTypeParameters] represents type parameters of arguments passed to this function during compile time.
     */
    fun inferReturnType(argsTypeParameters: List<TypeParameters>): CompileTimeType

    /**
     * Function evaluation
     *
     * [argsValue] represents value of each argument passed to this function during evaluation time.
     * [argsTypeParameters] represents type parameters of each argument passed to this function during compile time.
     * This is needed in some cases. e.g. For the expression `char_length(x)`, where `x` has value of a string 'abc'
     * with type of `CHAR(7)`, it should be evaluated to 7, which is the value of type parameter of the `CHAR` type.
     */
    fun invoke(argsValue: List<ExprValue>, argsTypeParameters: List<TypeParameters>): ExprValue
}
