package org.partiql.lang.eval.builtins

import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.eval.BindingCase
import org.partiql.lang.eval.BindingName
import org.partiql.lang.eval.EvaluationException
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprFunction
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.eval.physical.throwUndefinedVariableException
import org.partiql.lang.eval.stringValue
import org.partiql.lang.types.FunctionSignature
import org.partiql.lang.types.StaticType
import org.partiql.lang.types.VarargFormalParameter

const val DYNAMIC_LOOKUP_FUNCTION_NAME = "\$__dynamic_lookup__"

/**
 * Performs dynamic variable resolution.  Query authors should never call this function directly (and indeed it is
 * named to not be a likely collide with the names of custom functions)--instead, the query planner injects call sites
 * to this function to perform dynamic variable resolution of undefined variables.  This behavior allows legacy
 * customers that depend on this behavior a migration path to the new query planner.
 *
 * Arguments:
 *
 * - variable name
 * - case sensitivity
 * - lookup strategy (globals then locals or locals then globals)
 * - A variadic list of locations to be searched.
 *
 * The variadic arguments must be of type `any` because the planner doesn't yet have knowledge of static types
 * and therefore cannot filter out local variables types that are not structs.
 */
class DynamicLookupExprFunction : ExprFunction {
    override val signature: FunctionSignature
        get() {
            return FunctionSignature(
                name = DYNAMIC_LOOKUP_FUNCTION_NAME,
                // Required parameters are: variable name, case sensitivity and lookup strategy
                requiredParameters = listOf(StaticType.SYMBOL, StaticType.SYMBOL, StaticType.SYMBOL),
                variadicParameter = VarargFormalParameter(StaticType.ANY, 0..Int.MAX_VALUE),
                returnType = StaticType.ANY
            )
        }

    override fun callWithVariadic(
        session: EvaluationSession,
        required: List<ExprValue>,
        variadic: List<ExprValue>
    ): ExprValue {
        val variableName = required[0].stringValue()

        val caseSensitivity = when (val caseSensitivityParameterValue = required[1].stringValue()) {
            "case_sensitive" -> BindingCase.SENSITIVE
            "case_insensitive" -> BindingCase.INSENSITIVE
            else -> throw EvaluationException(
                message = "Invalid case sensitivity: $caseSensitivityParameterValue",
                errorCode = ErrorCode.INTERNAL_ERROR,
                internal = true
            )
        }

        val bindingName = BindingName(variableName, caseSensitivity)

        val globalsFirst = when (val lookupStrategyParameterValue = required[2].stringValue()) {
            "locals_then_globals" -> false
            "globals_then_locals" -> true
            else -> throw EvaluationException(
                message = "Invalid lookup strategy: $lookupStrategyParameterValue",
                errorCode = ErrorCode.INTERNAL_ERROR,
                internal = true
            )
        }

        val found = when {
            globalsFirst -> {
                session.globals[bindingName] ?: searchLocals(variadic, bindingName)
            }
            else -> {
                searchLocals(variadic, bindingName) ?: session.globals[bindingName]
            }
        }

        if (found == null) {
            // We don't know the metas inside ExprFunction implementations.  The ThunkFactory error handlers
            // should add line & col info to the exception & rethrow anyway.
            throwUndefinedVariableException(bindingName, metas = null)
        } else {
            return found
        }
    }

    private fun searchLocals(possibleLocations: List<ExprValue>, bindingName: BindingName) =
        possibleLocations.asSequence().map {
            when (it.type) {
                ExprValueType.STRUCT ->
                    it.bindings[bindingName]
                else ->
                    null
            }
        }.firstOrNull { it != null }
}
