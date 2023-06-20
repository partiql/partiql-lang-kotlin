package org.partiql.lang.eval.impl

import org.partiql.lang.eval.ExprFunction
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.errInvalidArgumentType
import org.partiql.lang.types.FunctionSignature
import org.partiql.lang.types.StaticTypeUtils
import org.partiql.types.StaticType

/**
 * Replaces the map of functions to achieve function overloading.
 * Supports getting all functions and getting the function map by name
 */
internal class FunctionManager(
    val functions: List<ExprFunction>
) {
    val functionMap: Map<String, List<ExprFunction>> = functions.groupBy { it.signature.name }
    data class ResultFormat(val function: ExprFunction?, val errorMessage: String?, val arity: Pair<Int, Int>)

    /**
     * Get function from the list by name, arity, and argumentTypes(ExprValue)
     */
    internal fun get(name: String, arity: Int, args: List<ExprValue>): ResultFormat {
        val funcs = functionMap[name] ?: return ResultFormat(null, "name check fails", Pair(0, 0))

        // Filter for functions with matching arity
        val funcsMatchingArity = funcs.filter { it.signature.arity.contains(arity) }
        if (funcsMatchingArity.isEmpty()) {
            return ResultFormat(null, "arity check fails", getMinMaxArities(funcs))
        }

        // Try to find a function where the types of all arguments match
        val errorList = mutableListOf<Exception>()
        for (func in funcsMatchingArity) {
            try {
                checkArgumentTypesEnableNullMissing(func.signature, args)
                // If we reach here, it means no exception was thrown by checkArgumentTypes, i.e., types match
                return ResultFormat(func, null, Pair(0, 0))
            } catch (e: Exception) {
                // Add the error to the list and continue with the next function
                errorList.add(e)
            }
        }
        throw errorList.first()
    }

    /**
     * Check argument types(ExprValue) by requiredArgs, OptionalArgs and VariadicArgs. This function will pass all arg values including MISSING or NULL values.
     */
    internal fun checkArgumentTypesEnableNullMissing(signature: FunctionSignature, args: List<ExprValue>) {
        fun checkArgumentType(formalStaticType: StaticType, actualArg: ExprValue, position: Int) {
            val formalExprValueTypeDomain = StaticTypeUtils.getTypeDomain(formalStaticType)

            val actualExprValueType = actualArg.type
            val actualStaticType = StaticTypeUtils.staticTypeFromExprValue(actualArg)

            if (actualStaticType == StaticType.NULL || actualStaticType == StaticType.MISSING) {
            } else if (!StaticTypeUtils.isSubTypeOf(actualStaticType, formalStaticType)) {
                errInvalidArgumentType(
                    signature = signature,
                    position = position,
                    expectedTypes = formalExprValueTypeDomain.toList(),
                    actualType = actualExprValueType
                )
            }
        }

        signature.requiredParameters.zip(args).forEachIndexed { idx, (expected, actual) ->
            checkArgumentType(expected, actual, idx + 1)
        }
    }

    /**
     * Get minArity and maxArity by looping candidate functions filtered by function name.
     */
    internal fun getMinMaxArities(funcs: List<ExprFunction>): Pair<Int, Int> {
        var minArity = Int.MAX_VALUE
        var maxArity = Int.MIN_VALUE

        funcs.forEach { func ->
            val currentArityMin = func.signature.arity.first
            val currentArityMax = func.signature.arity.last
            if (currentArityMin < minArity) minArity = currentArityMin
            if (currentArityMax > maxArity) maxArity = currentArityMax
        }

        return Pair(minArity, maxArity)
    }
}
