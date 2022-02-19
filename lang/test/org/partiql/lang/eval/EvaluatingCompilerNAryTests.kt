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
import org.partiql.lang.domains.PartiqlAst

/**
 * This test class is needed to test operator types of [PartiqlAst.Expr] with an arity > 2.
 *
 * Currently, the parser does not ever instantiate these with an arity > 2 so this is the only way to test this.
 */
@RunWith(JUnitParamsRunner::class)
class EvaluatingCompilerNAryTests: EvaluatorTestBase() {
    private val session = EvaluationSession.standard()

    private fun Boolean?.toIonValue(): IonValue =
        this?.let { ion.newBool(it) } ?: ion.newNull()

    private fun Long?.toIonValue(): IonValue =
        this?.let { ion.newInt(it) } ?: ion.newNull()

    /**
     * A test case for integer arithmetic.  Should be sufficient to test [NAry] arithmetic with arity > 2.
     *
     * A null value of any argument is converted to PartiQL `NULL`.
     */
    data class ArithmeticTestCase(val op: String, val arg1: Long?, val arg2: Long?, val arg3: Long?, val expectedResult: Long?)


    fun parametersForTernaryArithmeticTest() = listOf(
        //Null propagation for ADD
        ArithmeticTestCase("plus", null, 2, 3, null),
        ArithmeticTestCase("plus", 1, null, 3, null),
        ArithmeticTestCase("plus", 1, 2, null, null),

        //ADD is commutative
        ArithmeticTestCase("plus", 1, 2, 3, 6),
        ArithmeticTestCase("plus", 3, 1, 2, 6),

        //Null propagation for SUB
        ArithmeticTestCase("minus", null, 1, 2, null),
        ArithmeticTestCase("minus", 10, null, 2, null),
        ArithmeticTestCase("minus", 10, 1, null, null),

        //SUB is noncommutative
        ArithmeticTestCase("minus", 10, 1, 2, 7),
        ArithmeticTestCase("minus", 1, 2, 10, -11),

        //Null propagation for MUL
        ArithmeticTestCase("times", null, 2, 3, null),
        ArithmeticTestCase("times", 10, null, 3, null),
        ArithmeticTestCase("times", 10, 2, null, null),

        //MUL is commutative
        ArithmeticTestCase("times", 10, 2, 3, 60),
        ArithmeticTestCase("times", 2, 3, 10, 60),

        //Null propagation for DIV
        ArithmeticTestCase("divide", null, 2, 3, null),
        ArithmeticTestCase("divide", 10, null, 3, null),
        ArithmeticTestCase("divide", 10, 2, null, null),

        //DIV is noncommutative
        ArithmeticTestCase("divide", 60, 2, 3, 10),
        ArithmeticTestCase("divide", 2, 3, 10, 0),

        //Null propagation for MOD
        ArithmeticTestCase("modulo", null, 2, 3, null),
        ArithmeticTestCase("modulo", 10, null, 3, null),
        ArithmeticTestCase("modulo", 10, 2, null, null),

        //MOD is noncommutative
        ArithmeticTestCase("modulo", 19, 5, 3, 1),
        ArithmeticTestCase("modulo", 5, 3, 19, 2)
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
                    "plus" -> plus(buildExprList(tc.arg1, tc.arg2, tc.arg3))
                    "minus" -> minus(buildExprList(tc.arg1, tc.arg2, tc.arg3))
                    "times" -> times(buildExprList(tc.arg1, tc.arg2, tc.arg3))
                    "divide" -> divide(buildExprList(tc.arg1, tc.arg2, tc.arg3))
                    "modulo" -> modulo(buildExprList(tc.arg1, tc.arg2, tc.arg3))
                    else -> error("Internal error: Unrecognized operator name: ${tc.op}")
                }
            )
        }

        val expectedExprValue = tc.expectedResult?.let { valueFactory.newInt(it) } ?: valueFactory.nullValue

        assertEvalStatement(query, expectedExprValue)
    }

    private fun assertEvalStatement(
        astExpr: PartiqlAst.Statement,
        expectedExprValue: ExprValue) {
        val pipeline = CompilerPipeline.standard(ion)
        val expr = pipeline.compile(astExpr)
        val result = expr.eval(session)
        assertEquals(expectedExprValue.ionValue, result.ionValue)
    }


    /**
     * A test case for comparison operators.
     *
     * A null value of any property is converted to PartiQL `NULL`.
     */
    data class ComparisonTestCase(val op: String, val args: List<Long?>, val expectedResult: Boolean?)

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

        class FuncDef(val op: String, val block: (Long?, Long?) -> Boolean?)
        val possibleFuncs = listOf(
            FuncDef("eq") { v1, v2 -> if (v1 == null || v2 == null) { null } else { v1 == v2 } },
            FuncDef("gt") { v1, v2 -> if (v1 == null || v2 == null) { null } else { v1 > v2 } },
            FuncDef("gte") { v1, v2 -> if (v1 == null || v2 == null) { null } else { v1 >= v2 } },
            FuncDef("lt") { v1, v2 -> if (v1 == null || v2 == null) { null } else { v1 < v2 } },
            FuncDef("lte") { v1, v2 -> if (v1 == null || v2 == null) { null } else { v1 <= v2 } }
        )

        val testCases = ArrayList<ComparisonTestCase>()
        possibleFuncs.forEach { func ->
            for (arity in 2..4) {
                val argumentPermutationCount = possibleArgumentValues.size.pow(arity)
                for(i in 0..argumentPermutationCount) {
                    val baseN = i.toStringZeroPadded(arity, possibleArgumentValues.size)
                    val args = baseN.map { possibleArgumentValues[it] }.toList()

                    //determine the expected value
                    var current = args.first()
                    val rest = args.drop(1)

                    var expected: Boolean? = true
                    loop@for(it in rest) {
                        when (func.block(current, it)) {
                            null  -> {
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
                    "eq" -> eq(tc.args.map { lit(it.toIonValue().toIonElement()) })
                    "gt" -> gt(tc.args.map { lit(it.toIonValue().toIonElement()) })
                    "gte" -> gte(tc.args.map { lit(it.toIonValue().toIonElement()) })
                    "lt" -> lt(tc.args.map { lit(it.toIonValue().toIonElement()) })
                    "lte" -> lte(tc.args.map { lit(it.toIonValue().toIonElement()) })
                    else -> error("Internal error: Unrecognized operator name: ${tc.op}")
                }
            )
        }

        val expectedExprValue = tc.expectedResult?.let { valueFactory.newBoolean(it) } ?: valueFactory.nullValue

        assertEvalStatement(query, expectedExprValue)
    }

    data class LogicalOperatorsTestCase(val op: String, val b1: Boolean?, val b2: Boolean?, val b3: Boolean?, val expectedResult: Boolean?)


    fun parametersForLogicalOperatorsTest() = listOf(
        // AND tests
        // true, false arguments
        LogicalOperatorsTestCase("and", true, true, true, true),
        LogicalOperatorsTestCase("and", true, true, false, false),
        LogicalOperatorsTestCase("and", true, false, true, false),
        LogicalOperatorsTestCase("and", false, true, true, false),
        // Null only propagates when none of the terms are false.  
        // If any one of them is false, the entire expression evaluates to false.
        // true, null arguments
        LogicalOperatorsTestCase("and", true, true, null, null),
        LogicalOperatorsTestCase("and", true, null, true, null),
        LogicalOperatorsTestCase("and", null, true, true, null),

        // true, false, null arguments.
        LogicalOperatorsTestCase("and", true, false, null, false),
        LogicalOperatorsTestCase("and", true, null, false, false),
        LogicalOperatorsTestCase("and", null, true, false, false),

        // OR tests
        // true, false arguments
        LogicalOperatorsTestCase("or", false, false, false, false),
        LogicalOperatorsTestCase("or", false, false, true, true),
        LogicalOperatorsTestCase("or", false, true, false, true),
        LogicalOperatorsTestCase("or", true, false, false, true),

        // Null only propagates when none of the terms are true.
        // If any one of them is true the entire expression evaluates to true.
        LogicalOperatorsTestCase("or", false, false, null, null),
        LogicalOperatorsTestCase("or", false, null, false, null),
        LogicalOperatorsTestCase("or", null, false, false, null),
        
        // true, false, null arguments
        LogicalOperatorsTestCase("or", true, false, null, true),
        LogicalOperatorsTestCase("or", true, null, false, true),
        LogicalOperatorsTestCase("or", null, true, false, true)
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
                    "and" -> and(buildExprList(tc.b1, tc.b2, tc.b3))
                    "or" -> or(buildExprList(tc.b1, tc.b2, tc.b3))
                    else -> error("Internal error: Unrecognized operator name: ${tc.op}")
                }
            )
        }

        val expectedExprValue = tc.expectedResult?.let { valueFactory.newBoolean(it)} ?: valueFactory.nullValue

        assertEvalStatement(query, expectedExprValue)
    }
}