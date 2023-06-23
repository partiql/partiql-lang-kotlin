package org.partiql.lang.eval.impl

import org.partiql.lang.eval.ArityMismatchException
import org.partiql.lang.eval.ExprFunction
import org.partiql.lang.eval.FunctionNotFoundException
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

    /**
     * Get function from the list by name, arity, and argumentTypes(ExprValue)
     */
    internal fun get(name: String, arity: Int, args: List<StaticType>): ExprFunction {
        val funcs = functionMap[name] ?: throw FunctionNotFoundException("Name check fails")

        val funcsMatchingArity = funcs.filter { it.signature.arity.contains(arity) }
        if (funcsMatchingArity.isEmpty()) {
            throw ArityMismatchException("Arity check fails", getMinMaxArities(funcs))
        }

        val errorList = mutableListOf<Exception>()
        for (func in funcsMatchingArity) {
            try {
                checkArgumentTypesEnableNullMissing(func.signature, args)
                return func
            } catch (e: Exception) {
                errorList.add(e)
            }
        }
        throw errorList.first()
    }

    /**
     * Check argument types(ExprValue) by requiredArgs, OptionalArgs and VariadicArgs. This function will pass all arg values including MISSING or NULL values.
     */
    internal fun checkArgumentTypesEnableNullMissing(signature: FunctionSignature, args: List<StaticType>) {
        fun checkArgumentType(formalStaticType: StaticType, actualStaticType: StaticType, position: Int) {
            val formalExprValueTypeDomain = StaticTypeUtils.getTypeDomain(formalStaticType)

            if (actualStaticType == StaticType.NULL || actualStaticType == StaticType.MISSING) {
            } else if (!StaticTypeUtils.isSubTypeOf(actualStaticType, formalStaticType)) {
                errInvalidArgumentType(
                    signature = signature,
                    position = position,
                    expectedTypes = formalExprValueTypeDomain.toList(),
                    actualType = actualStaticType
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
