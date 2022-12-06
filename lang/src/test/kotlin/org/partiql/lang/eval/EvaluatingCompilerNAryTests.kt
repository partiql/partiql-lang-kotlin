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

import com.amazon.ion.IonValue
import com.amazon.ionelement.api.toIonElement
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import org.junit.Test
import org.junit.runner.RunWith
import org.partiql.lang.CompilerPipeline
import org.partiql.lang.ION
import org.partiql.lang.domains.PartiqlAst

/**
 * This test class is needed to test operator types of [PartiqlAst.Expr] with an arity > 2.
 *
 * Currently, the parser does not ever instantiate these with an arity > 2 so this is the only way to test this.
 */
@RunWith(JUnitParamsRunner::class)
class EvaluatingCompilerNAryTests : EvaluatorTestBase() {
    private val session = EvaluationSession.standard()

    private fun Boolean?.toIonValue(): IonValue =
        this?.let { ion.newBool(it) } ?: ion.newNull()

    private fun Long?.toIonValue(): IonValue =
        this?.let { ion.newInt(it) } ?: ion.newNull()

    enum class ArithmeticOp {
        Plus,
        Minus,
        Times,
        Divide,
        Modulo
    }

    enum class ComparisonOp {
        Eq,
        Gt,
        Gte,
        Lt,
        Lte
    }

    enum class LogicalOp {
        And,
        Or
    }

    /**
     * A test case for integer arithmetic.  Should be sufficient to test arithmetic operators with arity > 2.
     *
     * A null value of any argument is converted to PartiQL `NULL`.
     */
    data class ArithmeticTestCase(val op: ArithmeticOp, val arg1: Long?, val arg2: Long?, val arg3: Long?, val expectedResult: Long?)

    fun parametersForTernaryArithmeticTest() = listOf(
        // Null propagation for ADD
        ArithmeticTestCase(ArithmeticOp.Plus, null, 2, 3, null),
        ArithmeticTestCase(ArithmeticOp.Plus, 1, null, 3, null),
        ArithmeticTestCase(ArithmeticOp.Plus, 1, 2, null, null),

        // ADD is commutative
        ArithmeticTestCase(ArithmeticOp.Plus, 1, 2, 3, 6),
        ArithmeticTestCase(ArithmeticOp.Plus, 3, 1, 2, 6),

        // Null propagation for SUB
        ArithmeticTestCase(ArithmeticOp.Minus, null, 1, 2, null),
        ArithmeticTestCase(ArithmeticOp.Minus, 10, null, 2, null),
        ArithmeticTestCase(ArithmeticOp.Minus, 10, 1, null, null),

        // SUB is noncommutative
        ArithmeticTestCase(ArithmeticOp.Minus, 10, 1, 2, 7),
        ArithmeticTestCase(ArithmeticOp.Minus, 1, 2, 10, -11),

        // Null propagation for MUL
        ArithmeticTestCase(ArithmeticOp.Times, null, 2, 3, null),
        ArithmeticTestCase(ArithmeticOp.Times, 10, null, 3, null),
        ArithmeticTestCase(ArithmeticOp.Times, 10, 2, null, null),

        // MUL is commutative
        ArithmeticTestCase(ArithmeticOp.Times, 10, 2, 3, 60),
        ArithmeticTestCase(ArithmeticOp.Times, 2, 3, 10, 60),

        // Null propagation for DIV
        ArithmeticTestCase(ArithmeticOp.Divide, null, 2, 3, null),
        ArithmeticTestCase(ArithmeticOp.Divide, 10, null, 3, null),
        ArithmeticTestCase(ArithmeticOp.Divide, 10, 2, null, null),

        // DIV is noncommutative
        ArithmeticTestCase(ArithmeticOp.Divide, 60, 2, 3, 10),
        ArithmeticTestCase(ArithmeticOp.Divide, 2, 3, 10, 0),

        // Null propagation for MOD
        ArithmeticTestCase(ArithmeticOp.Modulo, null, 2, 3, null),
        ArithmeticTestCase(ArithmeticOp.Modulo, 10, null, 3, null),
        ArithmeticTestCase(ArithmeticOp.Modulo, 10, 2, null, null),

        // MOD is noncommutative
        ArithmeticTestCase(ArithmeticOp.Modulo, 19, 5, 3, 1),
        ArithmeticTestCase(ArithmeticOp.Modulo, 5, 3, 19, 2)
    )

    @Test
    @Parameters
    fun ternaryArithmeticTest(tc: ArithmeticTestCase) {
        fun buildExprList(arg1: Long?, arg2: Long?, arg3: Long?) = listOf(
            PartiqlAst.build { lit(arg1.toIonValue().toIonElement()) },
            PartiqlAst.build { lit(arg2.toIonValue().toIonElement()) },
            PartiqlAst.build { lit(arg3.toIonValue().toIonElement()) },
        )

        val query = PartiqlAst.build {
            query(
                when (tc.op) {
                    ArithmeticOp.Plus -> plus(buildExprList(tc.arg1, tc.arg2, tc.arg3))
                    ArithmeticOp.Minus -> minus(buildExprList(tc.arg1, tc.arg2, tc.arg3))
                    ArithmeticOp.Times -> times(buildExprList(tc.arg1, tc.arg2, tc.arg3))
                    ArithmeticOp.Divide -> divide(buildExprList(tc.arg1, tc.arg2, tc.arg3))
                    ArithmeticOp.Modulo -> modulo(buildExprList(tc.arg1, tc.arg2, tc.arg3))
                }
            )
        }

        val expectedExprValue = tc.expectedResult?.let { exprInt(it) } ?: exprNull()

        assertEvalStatement(query, expectedExprValue)
    }

    private fun assertEvalStatement(
        astExpr: PartiqlAst.Statement,
        expectedExprValue: ExprValue
    ) {
        val pipeline = CompilerPipeline.standard()
        val expr = pipeline.compile(astExpr)
        val result = expr.eval(session)
        assertEquals(expectedExprValue.toIonValue(ION), result.toIonValue(ION))
    }

    /**
     * A test case for comparison operators.
     *
     * A null value of any property is converted to PartiQL `NULL`.
     */
    data class ComparisonTestCase(val op: ComparisonOp, val args: List<Long?>, val expectedResult: Boolean?)

    private fun Int.pow(n: Int): Int = when {
        n > 0 -> this * this.pow(n - 1)
        else -> 1
    }

    private fun Int.toStringZeroPadded(paddingLength: Int, radix: Int): String {
        val str = this.toString(radix)
        val sb = StringBuilder()
        repeat(paddingLength - str.length) {
            sb.append('0')
        }
        sb.append(str)
        return sb.toString()
    }

    fun parametersForTernaryComparisonTest(): List<ComparisonTestCase> {
        val possibleArgumentValues = mapOf('0' to -1L, '1' to 1L, '2' to null)

        class FuncDef(val op: ComparisonOp, val block: (Long?, Long?) -> Boolean?)
        val possibleFuncs = listOf(
            FuncDef(ComparisonOp.Eq) { v1, v2 -> if (v1 == null || v2 == null) { null } else { v1 == v2 } },
            FuncDef(ComparisonOp.Gt) { v1, v2 -> if (v1 == null || v2 == null) { null } else { v1 > v2 } },
            FuncDef(ComparisonOp.Gte) { v1, v2 -> if (v1 == null || v2 == null) { null } else { v1 >= v2 } },
            FuncDef(ComparisonOp.Lt) { v1, v2 -> if (v1 == null || v2 == null) { null } else { v1 < v2 } },
            FuncDef(ComparisonOp.Lte) { v1, v2 -> if (v1 == null || v2 == null) { null } else { v1 <= v2 } }
        )

        val testCases = ArrayList<ComparisonTestCase>()
        possibleFuncs.forEach { func ->
            for (arity in 2..4) {
                val argumentPermutationCount = possibleArgumentValues.size.pow(arity)
                for (i in 0..argumentPermutationCount) {
                    val baseN = i.toStringZeroPadded(arity, possibleArgumentValues.size)
                    val args = baseN.map { possibleArgumentValues[it] }.toList()

                    // determine the expected value
                    var current = args.first()
                    val rest = args.drop(1)

                    var expected: Boolean? = true
                    loop@for (it in rest) {
                        when (func.block(current, it)) {
                            null -> {
                                expected = null
                                break@loop
                            }
                            false -> {
                                expected = false
                                break@loop
                            }
                        }
                        current = it
                    }

                    testCases.add(ComparisonTestCase(func.op, args, expected))
                }
            }
        }
        return testCases
    }

    @Test
    @Parameters
    fun ternaryComparisonTest(tc: ComparisonTestCase) {
        val query = PartiqlAst.build {
            query(
                when (tc.op) {
                    ComparisonOp.Eq -> eq(tc.args.map { lit(it.toIonValue().toIonElement()) })
                    ComparisonOp.Gt -> gt(tc.args.map { lit(it.toIonValue().toIonElement()) })
                    ComparisonOp.Gte -> gte(tc.args.map { lit(it.toIonValue().toIonElement()) })
                    ComparisonOp.Lt -> lt(tc.args.map { lit(it.toIonValue().toIonElement()) })
                    ComparisonOp.Lte -> lte(tc.args.map { lit(it.toIonValue().toIonElement()) })
                }
            )
        }

        val expectedExprValue = tc.expectedResult?.let { exprBoolean(it) } ?: exprNull()

        assertEvalStatement(query, expectedExprValue)
    }

    data class LogicalOperatorsTestCase(val op: LogicalOp, val b1: Boolean?, val b2: Boolean?, val b3: Boolean?, val expectedResult: Boolean?)

    fun parametersForLogicalOperatorsTest() = listOf(
        // AND tests
        // true, false arguments
        LogicalOperatorsTestCase(LogicalOp.And, true, true, true, true),
        LogicalOperatorsTestCase(LogicalOp.And, true, true, false, false),
        LogicalOperatorsTestCase(LogicalOp.And, true, false, true, false),
        LogicalOperatorsTestCase(LogicalOp.And, false, true, true, false),
        // Null only propagates when none of the terms are false.  
        // If any one of them is false, the entire expression evaluates to false.
        // true, null arguments
        LogicalOperatorsTestCase(LogicalOp.And, true, true, null, null),
        LogicalOperatorsTestCase(LogicalOp.And, true, null, true, null),
        LogicalOperatorsTestCase(LogicalOp.And, null, true, true, null),

        // true, false, null arguments.
        LogicalOperatorsTestCase(LogicalOp.And, true, false, null, false),
        LogicalOperatorsTestCase(LogicalOp.And, true, null, false, false),
        LogicalOperatorsTestCase(LogicalOp.And, null, true, false, false),

        // OR tests
        // true, false arguments
        LogicalOperatorsTestCase(LogicalOp.Or, false, false, false, false),
        LogicalOperatorsTestCase(LogicalOp.Or, false, false, true, true),
        LogicalOperatorsTestCase(LogicalOp.Or, false, true, false, true),
        LogicalOperatorsTestCase(LogicalOp.Or, true, false, false, true),

        // Null only propagates when none of the terms are true.
        // If any one of them is true the entire expression evaluates to true.
        LogicalOperatorsTestCase(LogicalOp.Or, false, false, null, null),
        LogicalOperatorsTestCase(LogicalOp.Or, false, null, false, null),
        LogicalOperatorsTestCase(LogicalOp.Or, null, false, false, null),

        // true, false, null arguments
        LogicalOperatorsTestCase(LogicalOp.Or, true, false, null, true),
        LogicalOperatorsTestCase(LogicalOp.Or, true, null, false, true),
        LogicalOperatorsTestCase(LogicalOp.Or, null, true, false, true)
    )

    @Test
    @Parameters
    fun logicalOperatorsTest(tc: LogicalOperatorsTestCase) {
        fun buildExprList(arg1: Boolean?, arg2: Boolean?, arg3: Boolean?) = listOf(
            PartiqlAst.build { lit(arg1.toIonValue().toIonElement()) },
            PartiqlAst.build { lit(arg2.toIonValue().toIonElement()) },
            PartiqlAst.build { lit(arg3.toIonValue().toIonElement()) },
        )

        val query = PartiqlAst.build {
            query(
                when (tc.op) {
                    LogicalOp.And -> and(buildExprList(tc.b1, tc.b2, tc.b3))
                    LogicalOp.Or -> or(buildExprList(tc.b1, tc.b2, tc.b3))
                }
            )
        }

        val expectedExprValue = tc.expectedResult?.let { exprBoolean(it) } ?: exprNull()

        assertEvalStatement(query, expectedExprValue)
    }
}
