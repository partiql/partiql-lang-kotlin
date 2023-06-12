package org.partiql.lang.eval.impl

import com.amazon.ionelement.api.MetaContainer
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.eval.Arguments
import org.partiql.lang.eval.ExprFunction
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.RequiredArgs
import org.partiql.lang.eval.RequiredWithOptional
import org.partiql.lang.eval.RequiredWithVariadic
import org.partiql.lang.eval.err
import org.partiql.lang.eval.errInvalidArgumentType
import org.partiql.lang.eval.errorContextFrom
import org.partiql.lang.types.FunctionSignature
import org.partiql.lang.types.StaticTypeUtils
import org.partiql.types.StaticType

/**
 * Replaces the map of functions to achieve function overloading.
 * Supports getting all functions and getting the function map by name
 */
class FunctionManager(
    public val functions: List<ExprFunction>
) {
    private val functionMap: Map<String, List<ExprFunction>> = functions.groupBy { it.signature.name }

    /**
     * Get function from the list by name, arity, and argumentTypes
     */
    fun get(name: String, arity: Int, args: List<ExprValue>, metas: MetaContainer): Pair<ExprFunction?, Arguments?> {
        val funcs = functionMap[name] ?: err(
            "No such function: $name",
            ErrorCode.EVALUATOR_NO_SUCH_FUNCTION,
            errorContextFrom(metas).also {
                it[Property.FUNCTION_NAME] = name
            },
            internal = false
        )
        // Filter for functions with matching arity
        val funcsMatchingArity = funcs.filter { it.signature.arity.contains(arity) }
        if (funcsMatchingArity.isEmpty()) {
            val (minArity, maxArity) = getMinMaxArities(funcs)
            val errorContext = errorContextFrom(metas).also {
                it[Property.FUNCTION_NAME] = name
                it[Property.EXPECTED_ARITY_MIN] = minArity
                it[Property.EXPECTED_ARITY_MAX] = maxArity
                it[Property.ACTUAL_ARITY] = arity
            }
            err(
                "No function found with matching arity: $name",
                ErrorCode.EVALUATOR_INCORRECT_NUMBER_OF_ARGUMENTS_TO_FUNC_CALL,
                errorContext,
                internal = false
            )
        }
        // Try to find a function where the types of all arguments match
        val errorList = mutableListOf<Exception>()
        for (func in funcsMatchingArity) {
            try {
                val checkedArgs = checkArgumentTypesEnableNullMissing(func.signature, args)
                // If we reach here, it means no exception was thrown by checkArgumentTypes, i.e., types match
                return Pair(func, checkedArgs)
            } catch (e: Exception) {
                // Add the error to the list and continue with the next function
                errorList.add(e)
            }
        }

        if (errorList.isNotEmpty()) {
            // Throw the first error in the list (or merge them, or create a new exception, etc.)
            throw errorList.first()
        }
        return Pair(null, null)
    }

    /**
     * Check argument types by requiredArgs, OptionalArgs and VariadicArgs
     */
    fun checkArgumentTypes(signature: FunctionSignature, args: List<ExprValue>): Arguments {
        fun checkArgumentType(formalStaticType: StaticType, actualArg: ExprValue, position: Int) {
            val formalExprValueTypeDomain = StaticTypeUtils.getTypeDomain(formalStaticType)

            val actualExprValueType = actualArg.type
            val actualStaticType = StaticTypeUtils.staticTypeFromExprValue(actualArg)

            if (!StaticTypeUtils.isSubTypeOf(actualStaticType, formalStaticType)) {
                errInvalidArgumentType(
                    signature = signature,
                    position = position,
                    expectedTypes = formalExprValueTypeDomain.toList(),
                    actualType = actualExprValueType
                )
            }
        }

        val required = args.take(signature.requiredParameters.size)
        val rest = args.drop(signature.requiredParameters.size)

        signature.requiredParameters.zip(required).forEachIndexed { idx, (expected, actual) ->
            checkArgumentType(expected, actual, idx + 1)
        }

        return if (signature.optionalParameter != null && rest.isNotEmpty()) {
            val opt = rest.last()
            checkArgumentType(signature.optionalParameter, opt, required.size + 1)
            RequiredWithOptional(required, opt)
        } else if (signature.variadicParameter != null) {
            rest.forEachIndexed { idx, arg ->
                checkArgumentType(signature.variadicParameter.type, arg, required.size + 1 + idx)
            }
            RequiredWithVariadic(required, rest)
        } else {
            RequiredArgs(required)
        }
    }

    /**
     * Check argument types by requiredArgs, OptionalArgs and VariadicArgs. This function will pass all arg values including MISSING or NULL values.
     */
    fun checkArgumentTypesEnableNullMissing(signature: FunctionSignature, args: List<ExprValue>): Arguments {
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

        val required = args.take(signature.requiredParameters.size)
        val rest = args.drop(signature.requiredParameters.size)

        signature.requiredParameters.zip(required).forEachIndexed { idx, (expected, actual) ->
            checkArgumentType(expected, actual, idx + 1)
        }

        return if (signature.optionalParameter != null && rest.isNotEmpty()) {
            val opt = rest.last()
            checkArgumentType(signature.optionalParameter, opt, required.size + 1)
            RequiredWithOptional(required, opt)
        } else if (signature.variadicParameter != null) {
            rest.forEachIndexed { idx, arg ->
                checkArgumentType(signature.variadicParameter.type, arg, required.size + 1 + idx)
            }
            RequiredWithVariadic(required, rest)
        } else {
            RequiredArgs(required)
        }
    }

    /**
     * Get minArity and maxArity by looping candidate functions filtered by function name.
     */
    fun getMinMaxArities(funcs: List<ExprFunction>): Pair<Int, Int> {
        var minArity = Int.MAX_VALUE
        var maxArity = Int.MIN_VALUE

        funcs.forEach { func ->
            val currentMin = func.signature.arity.first
            val currentMax = func.signature.arity.last

            if (currentMin < minArity) minArity = currentMin
            if (currentMax > maxArity) maxArity = currentMax
        }

        return Pair(minArity, maxArity)
    }

    internal fun getAllFunctions(): List<ExprFunction> = functions
}
