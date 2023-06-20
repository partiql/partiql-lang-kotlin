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
import org.partiql.types.StaticType

/**
 * The name of this function is [DYNAMIC_LOOKUP_FUNCTION_NAME], which includes a unique prefix and suffix so as to
 * avoid clashes with user-defined functions.
 */
internal const val DYNAMIC_LOOKUP_FUNCTION_NAME = "\$__dynamic_lookup__"

/**
 * Performs dynamic variable resolution.  Query authors should never call this function directly (and indeed it is
 * named to avoid collision with the names of custom functions)--instead, the query planner injects call sites
 * to this function to perform dynamic variable resolution of undefined variables.  This provides a migration path
 * for legacy customers that depend on this behavior.
 *
 * Arguments:
 *
 * 1. variable name (must be a symbol)
 * 2. case sensitivity (must be a symbol; one of: `case_insensitive` or `case_sensitive`)
 * 3. lookup strategy (must be a symbol; one of: `globals_then_locals` or `locals_then_globals`)
 * 4. A variadic list of values to be searched.  Only struct are searched.  This is required because it is not
 * currently possible to know the types of these arguments within the variable resolution pass
 * ([org.partiql.lang.planner.transforms.LogicalToLogicalResolvedVisitorTransform]).  Therefore all variables
 * in the current scope must be included in the list of values to be searched.
 * TODO: when the open type system's static type inferencer is working, static type information can be used to identify
 * and remove non-struct types from call sites to this function.
 */
class DynamicLookupExprFunction : ExprFunction {

    override val signature = FunctionSignature(
        name = DYNAMIC_LOOKUP_FUNCTION_NAME,
        // Required parameters are: variable name, case sensitivity, lookup strategy and variadic list.
        requiredParameters = listOf(StaticType.SYMBOL, StaticType.SYMBOL, StaticType.SYMBOL, StaticType.LIST),
        returnType = StaticType.ANY
    )

    override fun callWithRequired(
        session: EvaluationSession,
        required: List<ExprValue>
    ): ExprValue {
        val variadic = required[3].toList()
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
