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

import org.partiql.lang.eval.DEFAULT_COMPARATOR
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprFunction
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.exprBag
import org.partiql.lang.eval.exprBoolean
import org.partiql.lang.eval.exprInt
import org.partiql.lang.eval.exprString
import org.partiql.lang.eval.exprTimestamp
import org.partiql.lang.eval.stringValue
import org.partiql.lang.eval.unnamedValue
import org.partiql.lang.types.AnyOfType
import org.partiql.lang.types.FunctionSignature
import org.partiql.lang.types.StaticType
import org.partiql.lang.types.UnknownArguments
import java.util.TreeSet

internal const val DYNAMIC_LOOKUP_FUNCTION_NAME = "\$__dynamic_lookup__"

internal fun createBuiltinFunctionSignatures(): Map<String, FunctionSignature> =
    // Creating a new IonSystem in this instance is not the problem it would normally be since we are
    // discarding the created instances of the built-in functions after extracting all of the [FunctionSignature].
    createBuiltinFunctions()
        .map { it.signature }
        .associateBy { it.name }

internal fun createBuiltinFunctions() =
    listOf(
        createUpper(),
        createLower(),
        createExists(),
        createCharacterLength("character_length"),
        createCharacterLength("char_length"),
        createUtcNow(),
        createFilterDistinct(),
        DateAddExprFunction(),
        DateDiffExprFunction(),
        ExtractExprFunction(),
        MakeDateExprFunction(),
        MakeTimeExprFunction(),
        SubstringExprFunction(),
        TrimExprFunction(),
        ToStringExprFunction(),
        ToTimestampExprFunction(),
        SizeExprFunction(),
        FromUnixTimeFunction(),
        UnixTimestampFunction()
    ) + MathFunctions.create() + CollectionAggregationFunction.createAll()

internal fun createExists(): ExprFunction = object : ExprFunction {
    override val signature = FunctionSignature(
        "exists",
        listOf(StaticType.unionOf(StaticType.SEXP, StaticType.LIST, StaticType.BAG, StaticType.STRUCT)),
        returnType = StaticType.BOOL,
        unknownArguments = UnknownArguments.PASS_THRU
    )

    override fun callWithRequired(session: EvaluationSession, required: List<ExprValue>): ExprValue =
        exprBoolean(required[0].any())
}

internal fun createUtcNow(): ExprFunction = object : ExprFunction {
    override val signature = FunctionSignature(
        "utcnow",
        listOf(),
        returnType = StaticType.TIMESTAMP
    )

    override fun callWithRequired(session: EvaluationSession, required: List<ExprValue>): ExprValue =
        exprTimestamp(session.now)
}

internal fun createFilterDistinct(): ExprFunction = object : ExprFunction {
    override val signature = FunctionSignature(
        "filter_distinct",
        listOf(StaticType.unionOf(StaticType.BAG, StaticType.LIST, StaticType.SEXP, StaticType.STRUCT)),
        returnType = StaticType.BAG
    )

    override fun callWithRequired(session: EvaluationSession, required: List<ExprValue>): ExprValue {
        val argument = required.first()
        // We cannot use a [HashSet] here because [ExprValue] does not implement .equals() and .hashCode()
        val encountered = TreeSet(DEFAULT_COMPARATOR)
        return exprBag(
            sequence {
                argument.asSequence().forEach {
                    if (!encountered.contains(it)) {
                        encountered.add(it.unnamedValue())
                        yield(it)
                    }
                }
            }
        )
    }
}
internal fun createCharacterLength(name: String): ExprFunction =
    object : ExprFunction {
        override val signature: FunctionSignature
            get() {
                val element = AnyOfType(setOf(StaticType.STRING, StaticType.SYMBOL))
                return FunctionSignature(
                    name,
                    listOf(element),
                    returnType = StaticType.INT
                )
            }

        override fun callWithRequired(session: EvaluationSession, required: List<ExprValue>): ExprValue {
            val s = required.first().stringValue()
            return exprInt(s.codePointCount(0, s.length))
        }
    }

internal fun createUpper(): ExprFunction = object : ExprFunction {
    override val signature: FunctionSignature
        get() = FunctionSignature(
            "upper",
            listOf(AnyOfType(setOf(StaticType.STRING, StaticType.SYMBOL))),
            returnType = StaticType.STRING
        )

    override fun callWithRequired(session: EvaluationSession, required: List<ExprValue>): ExprValue =
        exprString(required.first().stringValue().toUpperCase())
}

internal fun createLower(): ExprFunction = object : ExprFunction {
    override val signature: FunctionSignature
        get() = FunctionSignature(
            "lower",
            listOf(AnyOfType(setOf(StaticType.STRING, StaticType.SYMBOL))),
            returnType = StaticType.STRING
        )

    override fun callWithRequired(session: EvaluationSession, required: List<ExprValue>): ExprValue =
        exprString(required.first().stringValue().toLowerCase())
}
