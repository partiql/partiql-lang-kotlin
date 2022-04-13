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

import com.amazon.ion.system.IonSystemBuilder
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprFunction
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueFactory
import org.partiql.lang.eval.stringValue
import org.partiql.lang.eval.unnamedValue
import org.partiql.lang.types.AnyOfType
import org.partiql.lang.types.FunctionSignature
import org.partiql.lang.types.StaticType
import org.partiql.lang.types.UnknownArguments
import java.util.TreeSet

internal fun createBuiltinFunctionSignatures(): Map<String, FunctionSignature> =
    // Creating a new IonSystem in this instance is not the problem it would normally be since we are
    // discarding the created instances of the built-in functions after extracting all of the [FunctionSignature].
    createBuiltinFunctions(ExprValueFactory.standard(IonSystemBuilder.standard().build()))
        .map { it.signature }
        .associateBy { it.name }

internal fun createBuiltinFunctions(valueFactory: ExprValueFactory) =
    listOf(
        createUpper(valueFactory),
        createLower(valueFactory),
        createExists(valueFactory),
        createCharacterLength("character_length", valueFactory),
        createCharacterLength("char_length", valueFactory),
        createUtcNow(valueFactory),
        createFilterDistinct(valueFactory),
        DateAddExprFunction(valueFactory),
        DateDiffExprFunction(valueFactory),
        ExtractExprFunction(valueFactory),
        MakeDateExprFunction(valueFactory),
        MakeTimeExprFunction(valueFactory),
        SubstringExprFunction(valueFactory),
        TrimExprFunction(valueFactory),
        ToStringExprFunction(valueFactory),
        ToTimestampExprFunction(valueFactory),
        SizeExprFunction(valueFactory),
        FromUnixTimeFunction(valueFactory),
        UnixTimestampFunction(valueFactory)
    )

internal fun createExists(valueFactory: ExprValueFactory): ExprFunction = object : ExprFunction {
    override val signature = FunctionSignature(
        "exists",
        listOf(StaticType.unionOf(StaticType.SEXP, StaticType.LIST, StaticType.BAG, StaticType.STRUCT)),
        returnType = StaticType.BOOL,
        unknownArguments = UnknownArguments.PASS_THRU
    )

    override fun callWithRequired(session: EvaluationSession, required: List<ExprValue>): ExprValue =
        valueFactory.newBoolean(required[0].any())
}

internal fun createUtcNow(valueFactory: ExprValueFactory): ExprFunction = object : ExprFunction {
    override val signature = FunctionSignature(
        "utcnow",
        listOf(),
        returnType = StaticType.TIMESTAMP
    )

    override fun callWithRequired(session: EvaluationSession, required: List<ExprValue>): ExprValue =
        valueFactory.newTimestamp(session.now)
}

internal fun createFilterDistinct(valueFactory: ExprValueFactory): ExprFunction = object : ExprFunction {
    override val signature = FunctionSignature(
        "filter_distinct",
        listOf(StaticType.unionOf(StaticType.BAG, StaticType.LIST, StaticType.SEXP, StaticType.STRUCT)),
        returnType = StaticType.BAG
    )

    override fun callWithRequired(session: EvaluationSession, required: List<ExprValue>): ExprValue {
        val argument = required.first()
        // We cannot use a [HashSet] here because [ExprValue] does not implement .equals() and .hashCode()
        val encountered = TreeSet(DEFAULT_COMPARATOR)
        return valueFactory.newBag(
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

internal fun createCharacterLength(name: String, valueFactory: ExprValueFactory): ExprFunction =
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
            return valueFactory.newInt(s.codePointCount(0, s.length))
        }
    }

internal fun createUpper(valueFactory: ExprValueFactory): ExprFunction = object : ExprFunction {
    override val signature: FunctionSignature
        get() = FunctionSignature(
            "upper",
            listOf(AnyOfType(setOf(StaticType.STRING, StaticType.SYMBOL))),
            returnType = StaticType.STRING
        )

    override fun callWithRequired(session: EvaluationSession, required: List<ExprValue>): ExprValue =
        valueFactory.newString(required.first().stringValue().toUpperCase())
}

internal fun createLower(valueFactory: ExprValueFactory): ExprFunction = object : ExprFunction {
    override val signature: FunctionSignature
        get() = FunctionSignature(
            "lower",
            listOf(AnyOfType(setOf(StaticType.STRING, StaticType.SYMBOL))),
            returnType = StaticType.STRING
        )

    override fun callWithRequired(session: EvaluationSession, required: List<ExprValue>): ExprValue =
        valueFactory.newString(required.first().stringValue().toLowerCase())
}
