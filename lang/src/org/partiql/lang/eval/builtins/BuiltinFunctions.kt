/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

package org.partiql.lang.eval.builtins

import org.partiql.lang.eval.*

internal fun createBuiltinFunctions(valueFactory: ExprValueFactory) =
    listOf(
        createUpper(valueFactory),
        createLower(valueFactory),
        createExists(valueFactory),
        createCharLength(valueFactory),
        createCharacterLength(valueFactory),
        createUtcNow(valueFactory),
        CoalesceExprFunction(valueFactory),
        DateAddExprFunction(valueFactory),
        DateDiffExprFunction(valueFactory),
        ExtractExprFunction(valueFactory),
        NullIfExprFunction(valueFactory),
        SubstringExprFunction(valueFactory),
        TrimExprFunction(valueFactory),
        ToStringExprFunction(valueFactory),
        ToTimestampExprFunction(valueFactory),
        SizeExprFunction(valueFactory),
        FromUnixTimeFunction(valueFactory),
        UnixTimestampFunction(valueFactory))


internal fun createExists(valueFactory: ExprValueFactory): ExprFunction = object : ExprFunction {
    override val name = "exists"

    override fun call(env: Environment, args: List<ExprValue>): ExprValue =
        when (args.size) {
            1    -> {
                valueFactory.newBoolean(args[0].asSequence().any())
            }
            else -> errNoContext("Expected a single argument for exists but found: ${args.size}", internal = false)
        }
}

internal fun createUtcNow(valueFactory: ExprValueFactory): ExprFunction = object : ExprFunction {
    override val name = "utcnow"
    override fun call(env: Environment, args: List<ExprValue>): ExprValue {
        if (args.isNotEmpty()) errNoContext("utcnow() takes no arguments", internal = false)
        return valueFactory.newTimestamp(env.session.now)
    }
}

/**
 * This function can be used to create simple functions taking only a single argument with null/missing propagation
 *
 * Provides default behaviors:
 *  - Validates that only one argument has been passed.
 *  - If that argument is null, returns null.
 *  - If that argument is missing, returns missing.
 */
private fun makeOneArgExprFunction(valueFactory: ExprValueFactory, funcName: String, func: (Environment, ExprValue) -> ExprValue) =
    object : NullPropagatingExprFunction(funcName, 1, valueFactory) {
        override val name = funcName
        override fun eval(env: Environment, args: List<ExprValue>): ExprValue = func(env, args[0])
    }

internal fun createCharLength(valueFactory: ExprValueFactory): ExprFunction = makeOneArgExprFunction(valueFactory, "char_length") { _, arg ->
    charLengthImpl(arg, valueFactory)
}

internal fun createCharacterLength(valueFactory: ExprValueFactory): ExprFunction = makeOneArgExprFunction(valueFactory, "character_length") { _, arg ->
    charLengthImpl(arg, valueFactory)
}

private fun charLengthImpl(arg: ExprValue, valueFactory: ExprValueFactory): ExprValue {
    val s = arg.stringValue()
    return valueFactory.newInt(s.codePointCount(0, s.length))
}

internal fun createUpper(valueFactory: ExprValueFactory): ExprFunction = makeOneArgExprFunction(valueFactory, "upper") { _, arg ->
    valueFactory.newString(arg.stringValue().toUpperCase())
}

internal fun createLower(valueFactory: ExprValueFactory): ExprFunction = makeOneArgExprFunction(valueFactory, "lower") { _, arg ->
    valueFactory.newString(arg.stringValue().toLowerCase())
}
