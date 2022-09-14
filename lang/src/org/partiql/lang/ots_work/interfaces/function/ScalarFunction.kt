package org.partiql.lang.ots_work.interfaces.function

import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.errNoContext

sealed class Arguments
data class RequiredArgs(val required: List<ExprValue>) : Arguments()
data class RequiredWithOptional(val required: List<ExprValue>, val optional: ExprValue) : Arguments()
data class RequiredWithVariadic(val required: List<ExprValue>, val variadic: List<ExprValue>) : Arguments()

interface ScalarFunction {
    val signature: FunctionSignature

    fun callWithRequired(required: List<ExprValue>): ExprValue {
        errNoContext("Invalid implementation for ${signature.name}#call", ErrorCode.INTERNAL_ERROR, true)
    }

    fun callWithOptional(required: List<ExprValue>, optional: ExprValue): ExprValue {
        // Deriving ExprFunctions must implement this if they have a valid call form including required parameters and optional
        errNoContext("Invalid implementation for ${signature.name}#call", ErrorCode.INTERNAL_ERROR, true)
    }

    fun callWithVariadic(required: List<ExprValue>, variadic: List<ExprValue>): ExprValue {
        // Deriving ExprFunctions must implement this if they have a valid call form including required parameters and variadic
        errNoContext("Invalid implementation for ${signature.name}#call", ErrorCode.INTERNAL_ERROR, true)
    }
}

fun ScalarFunction.call(args: Arguments): ExprValue =
    when (args) {
        is RequiredArgs -> callWithRequired(args.required)
        is RequiredWithOptional -> callWithOptional(args.required, args.optional)
        is RequiredWithVariadic -> callWithVariadic(args.required, args.variadic)
    }
