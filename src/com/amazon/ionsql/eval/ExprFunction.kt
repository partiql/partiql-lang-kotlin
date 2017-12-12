/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql.eval

import com.amazon.ionsql.eval.EvaluatingCompiler.*
import com.amazon.ionsql.eval.EvaluatingCompiler.UnknownsPropagationPolicy.*

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
