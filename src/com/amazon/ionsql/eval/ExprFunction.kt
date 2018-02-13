/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql.eval

import com.amazon.ion.*
import com.amazon.ionsql.errors.*
import com.amazon.ionsql.util.*

/**
 * Represents a function that can be invoked from within an [EvaluatingCompiler]
 * compiled [Expression].
 */
interface ExprFunction {
    companion object {
        fun over(func: (env: Environment, args: List<ExprValue>) -> ExprValue): ExprFunction =
            object : ExprFunction {
                override fun call(env: Environment, args: List<ExprValue>) = func(env, args)
            }
    }

    /**
     * Invokes the function.
     *
     * Implementations are required to deal with being called with the wrong number
     * of arguments or the wrong argument types.
     *
     * @param env The calling environment.
     * @param args The argument list.
     */
    fun call(env: Environment, args: List<ExprValue>): ExprValue
}

/**
 * [ExprFunction] template that checks arity and propagates null arguments when any parameter is either null or
 * missing
 *
 * @param name function name
 * @param arity function arity
 * @param ion current Ion system
 */
abstract class NullPropagatingExprFunction(val name: String,
                                           val arity: IntRange,
                                           val ion: IonSystem) : ExprFunction {
    constructor(name: String, arity: Int, ion: IonSystem) : this(name, (arity..arity), ion)

    override fun call(env: Environment, args: List<ExprValue>): ExprValue {
        checkArity(args)

        return when {
            args.isAnyNull()    -> nullExprValue(ion)
            args.isAnyMissing() -> missingExprValue(ion)
            else                -> eval(env, args)
        }
    }

    private fun arityErrorMessage(argSize: Int) = when {
        arity.first == 1 && arity.last == 1-> "$name takes a single argument, received: $argSize"
        arity.first == arity.last -> "$name takes exactly ${arity.first} arguments, received: $argSize"
        else -> "$name takes between ${arity.first} and ${arity.last} arguments, received: $argSize"
    }

    protected fun checkArity(args: List<ExprValue>) {
        if (!arity.contains(args.size)) {
            val errorContext = PropertyValueMap()
            errorContext[Property.EXPECTED_ARITY_MIN] = arity.first
            errorContext[Property.EXPECTED_ARITY_MAX] = arity.last

            throw EvaluationException(arityErrorMessage(args.size),
                                      ErrorCode.EVALUATOR_INCORRECT_NUMBER_OF_ARGUMENTS_TO_FUNC_CALL,
                                      errorContext,
                                      internal = false)
        }
    }

    abstract fun eval(env: Environment, args: List<ExprValue>): ExprValue
}