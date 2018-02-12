package com.amazon.ionsql.eval.builtins

import com.amazon.ion.*
import com.amazon.ionsql.eval.*
import com.amazon.ionsql.util.*

internal class BuiltinFunctionFactory(private val ion: IonSystem) {

    fun createFunctionMap(): Map<String, ExprFunction> = mapOf("upper" to this.upper(),
                                                               "lower" to this.lower(),
                                                               "date_add" to DateAddExprFunction(ion),
                                                               "date_diff" to DateDiffExprFunction(ion),
                                                               "exists" to this.exists(),
                                                               "extract" to ExtractExprFunction(ion),
                                                               "substring" to SubstringExprFunction(ion),
                                                               "char_length" to this.charLength(),
                                                               "character_length" to this.charLength(),
                                                               "trim" to TrimExprFunction(ion),
                                                               "to_string" to ToStringExprFunction(ion),
                                                               "to_timestamp" to ToTimestampExprFunction(ion),
                                                               "utcnow" to this.utcNow(),
                                                               "size" to SizeExprFunction(ion))

    fun exists(): ExprFunction = ExprFunction.over { _, args ->
        when (args.size) {
            1    -> {
                args[0].asSequence().any().exprValue(ion)
            }
            else -> errNoContext("Expected a single argument for exists but found: ${args.size}", internal = false)
        }
    }

    private fun charLength(): ExprFunction = makeOneArgExprFunction("char_length") { _, arg ->
        val s = arg.stringValue()
        s.codePointCount(0, s.length).exprValue(ion)
    }

    private fun upper(): ExprFunction = makeOneArgExprFunction("upper") { _, arg ->
        arg.stringValue().toUpperCase().exprValue(ion)
    }

    private fun lower(): ExprFunction = makeOneArgExprFunction("lower") { _, arg ->
        arg.stringValue().toLowerCase().exprValue(ion)
    }

    fun utcNow(): ExprFunction = ExprFunction.over { env, args ->
        if(args.isNotEmpty()) errNoContext("utcnow() takes no arguments", internal = false)

        ion.newTimestamp(env.session.now).exprValue()
    }

    /**
     * This function can be used to create simple functions taking only a single argument with null/missing propagation
     *
     * Provides default behaviors:
     *  - Validates that only one argument has been passed.
     *  - If that argument is null, returns null.
     *  - If that argument is missing, returns missing.
     */
    private fun makeOneArgExprFunction(name: String, func: (Environment, ExprValue) -> ExprValue) =
        object : NullPropagatingExprFunction(name, 1, ion) {
            override fun eval(env: Environment, args: List<ExprValue>): ExprValue = func(env, args[0])
    }
}
