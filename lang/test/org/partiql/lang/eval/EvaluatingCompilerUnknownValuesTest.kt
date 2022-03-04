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

package org.partiql.lang.eval

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.types.FunctionSignature
import org.partiql.lang.types.StaticType
import org.partiql.lang.types.UnknownArguments
import org.partiql.lang.util.ArgumentsProviderBase
import org.partiql.lang.util.crossMap


/** Test cases for PartiQL unknown values `MISSING` and `NULL`, including their propagation. */
class EvaluatingCompilerUnknownValuesTest : EvaluatorTestBase() {

    @ParameterizedTest
    @ArgumentsSource(NAryUnknownPropagationCases::class)
    fun testUnknownPropagation(tc: EvaluatorTestCase) =
        runTestCase(
            tc = tc,
            session = EvaluationSession.standard(),
            compilerPipelineBuilderBlock = {
                addFunction(
                    object : ExprFunction {
                        override val signature: FunctionSignature
                            get() = FunctionSignature(
                                name = "simple_sum",
                                requiredParameters = listOf(StaticType.INT8, StaticType.INT8, StaticType.INT8),
                                returnType = StaticType.INT8,
                                // NOTE: we do not test UnknownArguments.PASS_THRU in this test class
                                // (this path is covered by [CoalesceEvaluationTest]).
                                unknownArguments = UnknownArguments.PROPAGATE)

                        override fun callWithRequired(env: Environment, required: List<ExprValue>): ExprValue =
                            valueFactory.newInt(required.map { it.numberValue().toLong() }.sum())
                    }
                )
            })

    /** Generates a few hundred test cases for most NAry operators as they relate to propagation of unknown values. */
    class NAryUnknownPropagationCases : ArgumentsProviderBase() {
        override fun getParameters() = listOf(
            // arithmetic operators
            createArithmeticTestCases("i.x + i.y", "6"),
            createArithmeticTestCases("i.x - i.y", "2"),
            createArithmeticTestCases("i.x * i.y", "8"),
            createArithmeticTestCases("i.x / i.y", "2"),
            createArithmeticTestCases("i.x % i.y", "0"),

            // comparison operators
            createArithmeticTestCases("i.x = i.y", "false"),
            createArithmeticTestCases("i.x <> i.y", "true"),
            createArithmeticTestCases("i.x > i.y", "true"),
            createArithmeticTestCases("i.x >= i.y", "true"),
            createArithmeticTestCases("i.x < i.y", "false"),
            createArithmeticTestCases("i.x <= i.y", "false"),

            // logical operators AND and OR
            createLogicalAndOrTestCases(),

            // logical NOT operator
            createLogicalNotCases(),

            // concatenation operator
            createConcatTestCases(),

            // between
            createBetweenTestCases(),

            // unary + and -
            createUnaryTestCases(),

            // IN
            createInTestCases(),

            // LIKE
            createLikeTestCases(),

            // Function call unknown propagation
            createFunctionCallTestCases()

            // TODO: insersect, intersect_all, except, except_all, union, union_all
        ).flatten()

        /**
         * Creates two test cases for each of the [TypingMode] enums.
         *
         * - [TypingMode.LEGACY] which propagates MISSING-as-NULL
         * - [TypingMode.PERMISSIVE] which propagates MISSING-as-MISSING.
         *
         * When creating the [TypingMode.LEGACY] test case, the text "missing" within [expectedResult] is replaced
         * with "null".  This makes it unnecessary to specify an expected result for [TypingMode.LEGACY] separately.
         */
        private fun createCasesForTypingModes(
            testCaseGroup: String,
            expression: String,
            input: String,
            expectedResult: String
        ): List<EvaluatorTestCase> {
            val sqlUnderTest = "SELECT $expression AS result FROM << $input >> AS i"
            return listOf(
                EvaluatorTestCase(
                    groupName = "$testCaseGroup : LEGACY",
                    sqlUnderTest = sqlUnderTest,
                    // dirty hack to simplify things.
                    // in [Typing] mode, missing values are propagated as
                    // null.  Swapping this here means we don't need to specify a legacy mode value separately.
                    expectedSql = expectedResult.replace("missing", "null"),
                    compOptions = CompOptions.STANDARD),
                EvaluatorTestCase(
                    groupName = "$testCaseGroup : PERMISSIVE",
                    sqlUnderTest = sqlUnderTest,
                    expectedSql = expectedResult,
                    compOptions = CompOptions.PERMISSIVE))
        }

        private val nullResult = "<< { 'result': null } >>"
        private val missingResult = "<< { 'result': missing } >>"

        /**
         * Creates one test case using the specified expression for every:
         *
         * - Possible combination of known, unknown and missing values (9 in total)
         * - Each of the [TypingMode] enums.
         *
         * Returning 18 test cases in total.
         */
        private fun createArithmeticTestCases(
            expression: String,
            theKnownResult: String
        ): List<EvaluatorTestCase> {
            fun testCases(expression: String, input: String, expectedResult: String) =
                createCasesForTypingModes("Arithmetic", expression, input, expectedResult)

            return listOf(
                testCases(expression, "{'x': 4, 'y': 2}", "<< { 'result': $theKnownResult } >>"),
                testCases(expression, "{'x': 4, 'y': null}", nullResult),
                testCases(expression, "{'x': null, 'y': 2}", nullResult),
                testCases(expression, "{'x': null, 'y': null}", nullResult),
                testCases(expression, "{'x': 4}", missingResult),
                testCases(expression, "{'y': 2}", missingResult),
                testCases(expression, "{'x': null}", missingResult),
                testCases(expression, "{'y': null}", missingResult),
                testCases(expression, "{}", missingResult)
            ).flatten()
        }

        /**
         * Generates test cases to ensure that the behavior of logical operators AND and OR matches the following
         * truth table:
         *
         * |    P    |    Q    | P AND Q |  P OR Q |
         * |:-------:|:-------:|:-------:|:-------:|
         * |   TRUE  |   TRUE  |   TRUE  |   TRUE  |
         * |   TRUE  |  FALSE  |  FALSE  |   TRUE  |
         * |   TRUE  |   NULL  |   NULL  |   TRUE  |
         * |   TRUE  | MISSING | MISSING |   TRUE  |
         * |         |         |         |         |
         * |  FALSE  |   TRUE  |  FALSE  |   TRUE  |
         * |  FALSE  |  FALSE  |  FALSE  |  FALSE  |
         * |  FALSE  |   NULL  |  FALSE  |   NULL  |
         * |  FALSE  | MISSING |  FALSE  | MISSING |
         * |         |         |         |         |
         * |   NULL  |   TRUE  |   NULL  |   TRUE  |
         * |   NULL  |  FALSE  |  FALSE  |   NULL  |
         * |   NULL  |   NULL  |   NULL  |   NULL  |
         * |   NULL  | MISSING | MISSING | MISSING |
         * |         |         |         |         |
         * | MISSING |   TRUE  | MISSING |   TRUE  |
         * | MISSING |  FALSE  |  FALSE  | MISSING |
         * | MISSING |   NULL  | MISSING | MISSING |
         * | MISSING | MISSING | MISSING | MISSING |
         *
         * Notes:
         *
         * - For [TypingMode.LEGACY], replace all `MISSING` with `NULL` in the third and fourth columns.
         * - For AND: short-circuits to FALSE if one of its operands is FALSE, otherwise unknown propagation
         * works according to the current [TypingMode].
         * - For OR: short-circuits to TRUE if one of its operands is TRUE, otherwise unknown propagation
         * works according to the current [TypingMode].
         */
        private fun createLogicalAndOrTestCases() =
            listOf(
                createCommonLogicalTestCases("i.x AND i.y", "true", "false", "false"),
                createCommonLogicalTestCases("i.x OR i.y", "true", "true", "false"),
                createUnknownLogicalTestCases()
            ).flatten()

        /** These tests cases either do not have nulls in them or have all null or all misisng operands. */
        private fun createCommonLogicalTestCases(
            expression: String,
            trueTrueResult: String,
            trueFalseResult: String,
            falseFalseResult: String
        ): List<EvaluatorTestCase> {
            val tt = "<< { 'result': $trueTrueResult } >>"
            val tf = "<< { 'result': $trueFalseResult } >>"
            val ff = "<< { 'result': $falseFalseResult } >>"

            fun testCases(expression: String, input: String, expectedResult: String) =
                createCasesForTypingModes("logical (basic)", expression, input, expectedResult)

            return listOf(
                // No null or missing values
                testCases(expression, "{'x': true, 'y': true}", tt),
                testCases(expression, "{'x': true, 'y': false}", tf),
                testCases(expression, "{'x': false, 'y': false}", ff),
                testCases(expression, "{'x': false, 'y': true}", tf),

                // both operands are null
                testCases(expression, "{'x': null, 'y': null}", nullResult),
                // both operands are missing
                testCases(expression, "{}", missingResult)
            ).flatten()
        }

        private val trueResult = "<< { 'result': true} >>"
        private val falseResult = "<< { 'result': false } >>"

        /**
         * We need to create these cases separately because unknown propagation works a little bit differently
         * with AND and OR operators compared to the other binary operators.  Namely:
         * - AND short-circuits at the first FALSE operand, regardless of if the other operands are unknown
         * - OR short-circuits at the first TRUE operand, regardless of if the other operands are unknown
         */
        private fun createUnknownLogicalTestCases(): List<EvaluatorTestCase> {
            fun testCases(input: String, expectedAndResult: String, expectedOrResult: String) = listOf(
                createCasesForTypingModes("logical short-circuiting", "i.x AND i.y", input, expectedAndResult),
                createCasesForTypingModes("logical short-circuiting", "i.x OR i.y", input, expectedOrResult)
            ).flatten()

            return listOf(
                testCases("{'x': true, 'y': null}", nullResult, trueResult),
                testCases("{'x': false, 'y': null}", falseResult, nullResult),
                testCases("{'x': null, 'y': false}", falseResult, nullResult),
                testCases("{'x': null, 'y': true}", nullResult, trueResult),
                testCases("{'x': true}", missingResult, trueResult),
                testCases("{'x': false}", falseResult, missingResult),
                testCases("{'y': true}", missingResult, trueResult),
                testCases("{'y': false}", falseResult, missingResult),
                testCases("{'x': null}", missingResult, missingResult),
                testCases("{'y': null}", missingResult, missingResult)
            ).flatten()
        }

        /** Creates test cases for the logical NOT operator. */
        private fun createLogicalNotCases(): List<EvaluatorTestCase> {
            fun testCases(input: String, expectedResult: String) = listOf(
                createCasesForTypingModes("logical not", "NOT i.b", input, expectedResult)
            ).flatten()

            return listOf(
                testCases("{ 'b': true }", falseResult),
                testCases("{ 'b': false }", trueResult),
                testCases("{ 'b': NULL }", nullResult),
                testCases("{  }", missingResult)
            ).flatten()
        }

        /** Creates test cases for unknown propagation and the || operator. */
        private fun createConcatTestCases(): List<EvaluatorTestCase> {
            fun testCases(input: String, expectedResult: String): List<EvaluatorTestCase> =
                createCasesForTypingModes(
                    testCaseGroup = "string concatenation",
                    expression = "i.x || i.y",
                    input = input,
                    expectedResult = "<< { 'result': $expectedResult } >>")

            return listOf(
                testCases("""{'x': 'a', 'y': 'b'}""", "'ab'"),
                testCases("""{'x': 'a', 'y': null}""", "null"),
                testCases("""{'x': null, 'y': 'b'}""", "null"),
                testCases("""{'x': null, 'y': null}""", "null"),
                testCases("""{'x': 'a'}""", "missing"),
                testCases("""{'y': 'b'}""", "missing"),
                testCases("""{'x': null}""", "missing"),
                testCases("""{'y': null}""", "missing"),
                testCases("""{}""", "missing")
            ).flatten()
        }

        private fun createBetweenTestCases(): List<EvaluatorTestCase> {
            fun testCases(input: String, expectedResult: String) = listOf(
                createCasesForTypingModes("BETWEEN", "i.x BETWEEN i.y AND i.z", input, expectedResult)
            ).flatten()

            return listOf(
                testCases("{ 'x': 2, 'y': 1, 'z': 3 }", trueResult),
                testCases("{ 'x': 5, 'y': 1, 'z': 3 }", falseResult),

                // Combinations of nulls
                testCases("{ 'x': NULL, 'y': 1, 'z': 3 }", nullResult),
                testCases("{ 'x': 2, 'y': NULL, 'z': 3 }", nullResult),
                testCases("{ 'x': 2, 'y': 1, 'z': NULL }", nullResult),
                testCases("{ 'x': NULL, 'y': NULL, 'z': 3 }", nullResult),
                testCases("{ 'x': 2, 'y': NULL, 'z': NULL }", nullResult),
                testCases("{ 'x': NULL, 'y': NULL, 'z': NULL }", nullResult),

                // Combinations of missing
                testCases("{ 'x': MISSING, 'y': 1, 'z': 3 }", missingResult),
                testCases("{ 'x': 2, 'y': MISSING, 'z': 3 }", missingResult),
                testCases("{ 'x': 2, 'y': 1, 'z': MISSING }", missingResult),
                testCases("{ 'x': MISSING, 'y': MISSING, 'z': 3 }", missingResult),
                testCases("{ 'x': 2, 'y': MISSING, 'z': MISSING }", missingResult),
                testCases("{ 'x': MISSING, 'y': MISSING, 'z': MISSING }", missingResult),

                // Combinations of both
                testCases("{ 'x': NULL, 'y': MISSING, 'z': 3 }", missingResult),
                testCases("{ 'x': MISSING, 'y': NULL, 'z': 3 }", missingResult),
                testCases("{ 'x': 2, 'y': NULL, 'z': MISSING }", missingResult),
                testCases("{ 'x': 2, 'y': MISSING, 'z': NULL }", missingResult),
                testCases("{ 'x': NULL, 'y': MISSING, 'z': MISSING }", missingResult),
                testCases("{ 'x': MISSING, 'y': NULL, 'z': MISSING }", missingResult),
                testCases("{ 'x': MISSING, 'y': MISSING, 'z': NULL }", missingResult),
                testCases("{ 'x': NULL, 'y': NULL, 'z': MISSING }", missingResult),
                testCases("{ 'x': MISSING, 'y': MISSING, 'z': NULL }", missingResult)

            ).flatten()
        }

        private fun createUnaryTestCases(): List<EvaluatorTestCase> {
            fun testCases(input: String, expectedPosResult: String, expectedNegResult: String) = listOf(
                createCasesForTypingModes("unary +", "+i.n", input, expectedPosResult),
                createCasesForTypingModes("unary -", "-i.n", input, expectedNegResult)
            ).flatten()

            val oneResult = "<< { 'result': 1 } >>"
            val negOneResult = "<< { 'result': -1 } >>"

            return listOf(
                // note: unary + is effectively just a type check
                testCases("{ 'n': -1 }", negOneResult, oneResult),
                testCases("{ 'n': 1 }", oneResult, negOneResult),
                testCases("{ 'n': NULL }", nullResult, nullResult),
                testCases("{ }", missingResult, missingResult)

            ).flatten()
        }

        private fun createInTestCases(): List<EvaluatorTestCase> {
            fun testCases(input: String, expectedResult: String) = listOf(
                createCasesForTypingModes("IN (not optimized)", "i.x IN i.y", input, expectedResult)
            ).flatten()

            // Note that EvaluatingCompiler has an optimization which causes it to create a different thunk when
            // the right operand is a sequence constructor consisting entirely of non-null literals.  In this case,
            // unknown propagation only applies to the left-side of IN
            fun testCasesForOptimized(input: String, expectedResult: String) = listOf(
                createCasesForTypingModes("IN (optimized)", "i.x IN (1, 2, 3) ", input, expectedResult)
            ).flatten()

            return listOf(
                // known operands should not result in an unknown
                testCases("{ 'x': 2, 'y': [1, 2, 3] }", trueResult),
                testCases("{ 'x': 4, 'y': [1, 2, 3] }", falseResult),

                // unknowns within `y` are not propagated if the value of `x` is within `y`.
                testCases("{ 'x': 2, 'y': [1, null, 2, 3] }", trueResult),
                testCases("{ 'x': 2, 'y': [1, missing, 2, 3] }", trueResult),

                // unknowns within `y` *are* propagated if the value of `x` is *not* within `y`.
                testCases("{ 'x': 4, 'y': [1, null, 2, 3] }", nullResult),
                testCases("{ 'x': 4, 'y': [1, missing, 2, 3] }", missingResult),

                // one operand is null
                testCases("{ 'x': NULL, 'y': [1, 2, 3] }", nullResult),
                testCases("{ 'x': 2, 'y': null }", nullResult),

                // one operand is missing
                testCases("{ 'x': 2 }", missingResult),
                testCases("{ 'y': [1, 2, 3] }", missingResult),

                // Both sides are null.
                testCases("{ 'x': NULL, 'y': NULL }", nullResult),

                // One side is null and the other missing
                testCases("{ 'x': NULL, 'y': MISSING }", missingResult),
                testCases("{ 'x': MISSING, 'y': NULL }", missingResult),

                // Both sides are missing.
                testCases("{ }", missingResult),

                // cases for the optimized IN thunk.
                testCasesForOptimized("{ 'x': 1 }", trueResult),
                testCasesForOptimized("{ 'x': 4 }", falseResult),
                testCasesForOptimized("{ 'x': NULL }", nullResult),
                testCasesForOptimized("{ }", missingResult)

            ).flatten()
        }

        /**
         * Creates a few categories of LIKE tests to cover the different thunks that can be returned
         * under various usages, namely:
         *
         * - Binary or ternary `LIKE` where the pattern is a literal, wherein the pattern is compiled once and
         * re-used
         * - Binary `LIKE`, where both operands are non-literal expressions, wherein the pattern is recompiled
         * with every evaluation.
         * - Ternary `LIKE` (with `ESCAPE`), where all operands are non-literal expressions, also recompiles
         * the pattern with every evaluation.
         */
        private fun createLikeTestCases(): List<EvaluatorTestCase> {

            fun testCasesForPrecompilingBinaryLike(input: String, expectedResult: String) = listOf(
                createCasesForTypingModes("LIKE (binary)", "i.a LIKE 'a%'", input, expectedResult)
            ).flatten()

            fun testCasesForRecompilingBinaryLike(input: String, expectedResult: String) = listOf(
                createCasesForTypingModes("LIKE (binary)", "i.a LIKE i.b", input, expectedResult)
            ).flatten()

            fun testCasesForRecompilingTernaryLike(input: String, expectedResult: String) = listOf(
                createCasesForTypingModes("LIKE (ternary)", "i.a LIKE i.b ESCAPE i.c", input, expectedResult)
            ).flatten()

            return listOf(
                // pre-compiling binary LIKE (no need to test ternary here since the same thunk is used for ternary)
                testCasesForPrecompilingBinaryLike("{ 'a': 'ab' }", trueResult),
                testCasesForPrecompilingBinaryLike("{ 'a': 'ba' }", falseResult),
                testCasesForPrecompilingBinaryLike("{ 'a': NULL }", nullResult),
                testCasesForPrecompilingBinaryLike("{  }", missingResult),

                // re-compiling binary LIKE (w/known values)
                testCasesForRecompilingBinaryLike("{ 'a': 'ab', 'b': 'ab' }", trueResult),
                testCasesForRecompilingBinaryLike("{ 'a': 'ab', 'b': 'ba' }", falseResult),

                // re-compiling binary LIKE (w/unknown values)
                testCasesForRecompilingBinaryLike("{ 'a': NULL, 'b': 'ba' }", nullResult),
                testCasesForRecompilingBinaryLike("{ 'a': 'ab', 'b': NULL }", nullResult),
                testCasesForRecompilingBinaryLike("{ 'a': NULL, 'b': NULL }", nullResult),
                testCasesForRecompilingBinaryLike("{ 'b': 'ab' }", missingResult),
                testCasesForRecompilingBinaryLike("{ 'a': 'ab' }", missingResult),
                testCasesForRecompilingBinaryLike("{ 'a': NULL }", missingResult),
                testCasesForRecompilingBinaryLike("{ 'b': NULL }", missingResult),

                // re-compiling ternary LIKE (w/known values)
                testCasesForRecompilingTernaryLike("{ 'a': 'ab', 'b': 'ab', 'c': 'n' }", trueResult),
                testCasesForRecompilingTernaryLike("{ 'a': 'ab', 'b': 'ba', 'c': 'n' }", falseResult),

                // re-compiling ternary LIKE (w/null values)
                testCasesForRecompilingTernaryLike("{ 'a': NULL, 'b': 'ab', 'c': 'n' }", nullResult),
                testCasesForRecompilingTernaryLike("{ 'a': 'ab', 'b': NULL, 'c': 'n' }", nullResult),
                testCasesForRecompilingTernaryLike("{ 'a': 'ab', 'b': 'ab', 'c': NULL }", nullResult),
                testCasesForRecompilingTernaryLike("{ 'a': NULL, 'b': NULL, 'c': 'n' }", nullResult),
                testCasesForRecompilingTernaryLike("{ 'a': 'ab', 'b': NULL, 'c': NULL }", nullResult),
                testCasesForRecompilingTernaryLike("{ 'a': NULL, 'b': NULL, 'c': NULL }", nullResult),

                // re-compiling ternary LIKE (w/missing values)
                testCasesForRecompilingTernaryLike("{ 'b': 'ab', 'c': 'n' }", missingResult),
                testCasesForRecompilingTernaryLike("{ 'a': 'ab',  'c': 'n' }", missingResult),
                testCasesForRecompilingTernaryLike("{ 'a': 'ab', 'b': 'ab' }", missingResult),
                testCasesForRecompilingTernaryLike("{ 'c': 'n' }", missingResult),
                testCasesForRecompilingTernaryLike("{ 'a': 'ab' }", missingResult),
                testCasesForRecompilingTernaryLike("{ }", missingResult),

                // re-compiling ternary LIKE (w/mixed missing and null values)
                testCasesForRecompilingTernaryLike("{ 'a': NULL }", missingResult),
                testCasesForRecompilingTernaryLike("{ 'b': NULL }", missingResult),
                testCasesForRecompilingTernaryLike("{ 'c': NULL }", missingResult),
                testCasesForRecompilingTernaryLike("{ 'a': NULL, 'b': NULL }", missingResult),
                testCasesForRecompilingTernaryLike("{ 'b': NULL, 'c': NULL }", missingResult),
                testCasesForRecompilingTernaryLike("{ 'a': NULL, 'c': NULL }", missingResult)
            ).flatten()
        }

        private fun createFunctionCallTestCases(): List<EvaluatorTestCase> {
            fun testCases(input: String, expectedResult: String) = listOf(
                createCasesForTypingModes("function call", "simple_sum(i.x, i.y, i.z)", input, expectedResult)
            ).flatten()

            // Generates 54 test cases
            val cases =
                crossMap(
                    listOf("2", "missing", "null"),
                    listOf("3", "missing", "null"),
                    listOf("4", "missing", "null")
                ) { x, y, z ->
                    val input = "{ 'x': $x, 'y': $y, 'z': $z }"
                    testCases(
                        input = input,
                        expectedResult = when {
                            input.contains("missing") -> missingResult
                            input.contains("null") -> nullResult
                            else -> "<< { 'result': 9 } >>"
                        }
                    )
                }.flatten()

            return cases
        }

    } // end NAryUnknownPropagationCases



    private val nullSample = mapOf(
        "nullSample" to """
        [
            {val: "A", control: true, n: 1},
            {val: "B", control: false, n: null},
            {val: "C", control: null, n: 3},
        ]
        """).toSession()

    private val missingSample = mapOf(
        "missingSample" to """
        [
            {val: "A", control: true, n: 1},
            {val: "B", control: false, n: 2},
            {val: "C" ,},
        ]
        """).toSession()



    private val missingAndNullSample = mapOf(
        "missingAndNullSample" to """
        [
            {val: "A", control: true, n:2},
            {val: "B", control: false, n: 2},
            {val: "C", int:3},
            {val: "D", control: null, n:5},
        ]
        """).toSession()

    private val boolsWithUnknowns = mapOf(
        "boolsWithUnknowns" to """
        [
            {x: true, y: true},
            {x: true, y: false},
            {x: false, y: false},
            {x: false, y: true},
            {x: true, y: null},
            {x: false, y: null},
            {x: null, y: false},
            {x: null, y: true},
            {x: null, y: null},
            {x: true},
            {x: false},
            {y: true},
            {y: false},
            {x: null},
            {y: null},
            {}
        ]
    """).toSession()

    @Test
    fun andShortCircuits() = assertEvalExprValue(
        "SELECT s.x FROM [{'x': '1.1'},{'x': '2'},{'x': '3'},{'x': '4'},{'x': '5'}] as s WHERE FALSE AND CAST(s.x as INT)",
        "<<>>",
        boolsWithUnknowns)

    @Test
    fun andWithNullDoesNotShortCircuits() = assertThrows(
        "SELECT s.x FROM [{'x': '1.1'},{'x': '2'},{'x': '3'},{'x': '4'},{'x': '5'}] as s WHERE NULL AND CAST(s.x as INT)",
        "can't convert string value to INT",
        NodeMetadata(1, 96),
        "<<>>")

    @Test
    fun andWithMissingDoesNotShortCircuits() = assertThrows(
        "SELECT s.x FROM [{'x': '1.1'},{'x': '2'},{'x': '3'},{'x': '4'},{'x': '5'}] as s WHERE MISSING AND CAST(s.x as INT)",
        "can't convert string value to INT",
        NodeMetadata(1, 99),
        "<<>>")

    //////////////////////////////////////////////////
    // Where-clause
    //////////////////////////////////////////////////

    @Test
    fun whereClauseExprEvalsToNull() = assertEvalExprValue(
        "SELECT VALUE D.val from nullSample as D WHERE D.control",
        "<<'A'>>",
        nullSample)

    @Test
    fun whereClauseExprEvalsToMissing() = assertEvalExprValue(
        "SELECT VALUE D.val from missingSample as D WHERE D.control",
        "<<'A'>>",
        missingSample)

    @Test
    fun whereClauseExprEvalsToNullAndMissing() = assertEvalExprValue(
        "SELECT VALUE D.val from missingAndNullSample as D WHERE D.control",
        "<<'A'>>",
        missingAndNullSample)

    //////////////////////////////////////////////////
    // Aggregates
    //////////////////////////////////////////////////

    @Test
    @Disabled("PHYS_ALGEBRA_REFACTOR_CALL_AGG")
    fun aggregateSumWithNull() = assertEval("SELECT sum(x.n) from nullSample as x", "[{_1: 4}]", nullSample)

    @Test
    @Disabled("PHYS_ALGEBRA_REFACTOR_CALL_AGG")
    fun aggregateSumWithMissing() = assertEval("SELECT sum(x.n) from missingSample as x",
        "[{_1: 3}]",
        missingSample)

    @Test
    @Disabled("PHYS_ALGEBRA_REFACTOR_CALL_AGG")
    fun aggregateSumWithMissingAndNull() = assertEval("SELECT sum(x.n) from missingAndNullSample as x",
        "[{_1: 9}]",
        missingAndNullSample)


    @Test
    @Disabled("PHYS_ALGEBRA_REFACTOR_CALL_AGG")
    fun aggregateMinWithNull() = assertEval("SELECT min(x.n) from nullSample as x", "[{_1: 1}]", nullSample)

    @Test
    @Disabled("PHYS_ALGEBRA_REFACTOR_CALL_AGG")
    fun aggregateMinWithMissing() = assertEval("SELECT min(x.n) from missingSample as x",
        "[{_1: 1}]",
        missingSample)

    @Test
    @Disabled("PHYS_ALGEBRA_REFACTOR_CALL_AGG")
    fun aggregateMinWithMissingAndNull() = assertEval("SELECT min(x.n) from missingAndNullSample as x",
        "[{_1: 2}]",
        missingAndNullSample)


    @Test
    @Disabled("PHYS_ALGEBRA_REFACTOR_CALL_AGG")
    fun aggregateAvgWithNull() = assertEval("SELECT avg(x.n) from nullSample as x", "[{_1: 2.}]", nullSample)

    @Test
    @Disabled("PHYS_ALGEBRA_REFACTOR_CALL_AGG")
    fun aggregateAvgWithMissing() = assertEval("SELECT avg(x.n) from missingSample as x",
        "[{_1: 1.5}]",
        missingSample)

    @Test
    @Disabled("PHYS_ALGEBRA_REFACTOR_CALL_AGG")
    fun aggregateAvgWithMissingAndNull() = assertEval("SELECT avg(x.n) from missingAndNullSample as x",
        "[{_1: 3.}]",
        missingAndNullSample)


    @Test
    @Disabled("PHYS_ALGEBRA_REFACTOR_CALL_AGG")
    fun aggregateCountWithNull() = assertEval("SELECT count(x.n) from nullSample as x",
        "[{_1: 2}]",
        nullSample)

    @Test
    @Disabled("PHYS_ALGEBRA_REFACTOR_CALL_AGG")
    fun aggregateCountWithMissing() = assertEval("SELECT count(x.n) from missingSample as x",
        "[{_1: 2}]",
        missingSample)

    @Test
    @Disabled("PHYS_ALGEBRA_REFACTOR_CALL_AGG")
    fun aggregateCountWithMissingAndNull() = assertEval("SELECT count(x.n) from missingAndNullSample as x",
        "[{_1: 3}]",
        missingAndNullSample)

    @Test
    @Disabled("PHYS_ALGEBRA_REFACTOR_CALL_AGG")
    fun countEmpty() = assertEval("SELECT count(*) from `[]`", "[{_1: 0}]")

    @Test
    @Disabled("PHYS_ALGEBRA_REFACTOR_CALL_AGG")
    fun countEmptyTuple() = assertEval("SELECT count(*) from `[{}]`", "[{_1: 1}]")


    @Test
    @Disabled("PHYS_ALGEBRA_REFACTOR_CALL_AGG")
    fun sumEmpty() = assertEval("SELECT sum(x.i) from `[]` as x", "[{_1: null}]")

    @Test
    @Disabled("PHYS_ALGEBRA_REFACTOR_CALL_AGG")
    fun sumEmptyTuple() = assertEval("SELECT sum(x.i) from `[{}]` as x", "[{_1: null}]")

    @Test
    @Disabled("PHYS_ALGEBRA_REFACTOR_CALL_AGG")
    fun avgEmpty() = assertEval("SELECT avg(x.i) from `[]` as x", "[{_1: null}]")

    @Test
    @Disabled("PHYS_ALGEBRA_REFACTOR_CALL_AGG")
    fun avgEmptyTuple() = assertEval("SELECT avg(x.i) from `[{}]` as x", "[{_1: null}]")

    @Test
    @Disabled("PHYS_ALGEBRA_REFACTOR_CALL_AGG")
    fun avgSomeEmptyTuples() = assertEval("SELECT avg(x.i) from `[{i: 1}, {}, {i:3}]` as x",
        "[{_1: 2.}]")

    @Test
    @Disabled("PHYS_ALGEBRA_REFACTOR_CALL_AGG")
    fun avgSomeEmptyAndNullTuples() = assertEval("SELECT avg(x.i) from `[{i: 1}, {}, {i:null}, {i:3}]` as x",
        "[{_1: 2.}]")

    @Test
    @Disabled("PHYS_ALGEBRA_REFACTOR_CALL_AGG")
    fun minSomeEmptyTuples() = assertEval("SELECT min(x.i) from `[{i: null}, {}, {i:3}]` as x",
        "[{_1: 3}]")

    @Test
    @Disabled("PHYS_ALGEBRA_REFACTOR_CALL_AGG")
    fun maxSomeEmptyTuples() = assertEval("SELECT max(x.i) from `[{i: null}, {}, {i:3}, {i:10}]` as x",
        "[{_1: 10}]")
    @Test
    @Disabled("PHYS_ALGEBRA_REFACTOR_CALL_AGG")
    fun minEmpty() = assertEval("SELECT min(x.i) from `[]` as x", "[{_1: null}]")

    @Test
    @Disabled("PHYS_ALGEBRA_REFACTOR_CALL_AGG")
    fun minEmptyTuple() = assertEval("SELECT min(x.i) from `[{}]` as x", "[{_1: null}]")

    @Test
    @Disabled("PHYS_ALGEBRA_REFACTOR_CALL_AGG")
    fun maxEmpty() = assertEval("SELECT max(x.i) from `[]` as x", "[{_1: null}]")

    @Test
    @Disabled("PHYS_ALGEBRA_REFACTOR_CALL_AGG")
    fun maxEmptyTuple() = assertEval("SELECT max(x.i) from `[{}]` as x", "[{_1: null}]")

    @Test
    @Disabled("PHYS_ALGEBRA_REFACTOR_CALL_AGG")
    fun maxSomeEmptyTuple() = assertEval("SELECT max(x.i) from `[{}, {i:1}, {}, {i:2}]` as x",
        "[{_1: 2}]")

    @Test
    @Disabled("PHYS_ALGEBRA_REFACTOR_CALL_AGG")
    fun minSomeEmptyTuple() = assertEval("SELECT min(x.i) from `[{}, {i:1}, {}, {i:2}]` as x",
        "[{_1: 1}]")

    @Test
    @Disabled("PHYS_ALGEBRA_REFACTOR_CALL_AGG")
    fun sumSomeEmptyTuple() = assertEval("SELECT sum(x.i) from `[{}, {i:1}, {}, {i:2}]` as x",
        "[{_1: 3}]")

    @Test
    @Disabled("PHYS_ALGEBRA_REFACTOR_CALL_AGG")
    fun countSomeEmptyTuple() = assertEval("SELECT count(x.i) from `[{}, {i:1}, {}, {i:2}]` as x",
        "[{_1: 2}]")

    @Test
    @Disabled("PHYS_ALGEBRA_REFACTOR_CALL_AGG")
    fun countStar() = assertEval("SELECT count(*) from `[{}, {i:1}, {}, {i:2}]` as x",
        "[{_1: 4}]")

    @Test
    @Disabled("PHYS_ALGEBRA_REFACTOR_CALL_AGG")
    fun countLiteral() = assertEval("SELECT count(1) from `[{}, {}, {}, {}]` as x", "[{_1: 4}]")
}





