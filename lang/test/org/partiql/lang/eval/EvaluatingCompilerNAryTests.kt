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
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import org.junit.Test
import org.junit.runner.RunWith
import org.partiql.lang.CompilerPipeline
import org.partiql.lang.ast.Literal
import org.partiql.lang.ast.NAry
import org.partiql.lang.ast.NAryOp
import org.partiql.lang.ast.metaContainerOf

/**
 * This test class is needed to test certain types of [NAryOp] with an arity > 2.
 *
 * Currently, the parser does not ever instantiate these with an arity > 2 so this is the only way to test this.
 */
@RunWith(JUnitParamsRunner::class)
class EvaluatingCompilerNAryTests: EvaluatorTestBase() {
    private val dummyMetas = metaContainerOf()
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
    data class ArithmeticTestCase(val op: NAryOp, val arg1: Long?, val arg2: Long?, val arg3: Long?, val expectedResult: Long?)


    fun parametersForTernaryArithmeticTest() = listOf(
        //Null propagation for ADD
        ArithmeticTestCase(NAryOp.ADD, null, 2, 3, null),
        ArithmeticTestCase(NAryOp.ADD, 1, null, 3, null),
        ArithmeticTestCase(NAryOp.ADD, 1, 2, null, null),

        //ADD is commutative
        ArithmeticTestCase(NAryOp.ADD, 1, 2, 3, 6),
        ArithmeticTestCase(NAryOp.ADD, 3, 1, 2, 6),

        //Null propagation for SUB
        ArithmeticTestCase(NAryOp.SUB, null, 1, 2, null),
        ArithmeticTestCase(NAryOp.SUB, 10, null, 2, null),
        ArithmeticTestCase(NAryOp.SUB, 10, 1, null, null),

        //SUB is noncommutative
        ArithmeticTestCase(NAryOp.SUB, 10, 1, 2, 7),
        ArithmeticTestCase(NAryOp.SUB, 1, 2, 10, -11),

        //Null propagation for MUL
        ArithmeticTestCase(NAryOp.MUL, null, 2, 3, null),
        ArithmeticTestCase(NAryOp.MUL, 10, null, 3, null),
        ArithmeticTestCase(NAryOp.MUL, 10, 2, null, null),

        //MUL is commutative
        ArithmeticTestCase(NAryOp.MUL, 10, 2, 3, 60),
        ArithmeticTestCase(NAryOp.MUL, 2, 3, 10, 60),

        //Null propagation for DIV
        ArithmeticTestCase(NAryOp.DIV, null, 2, 3, null),
        ArithmeticTestCase(NAryOp.DIV, 10, null, 3, null),
        ArithmeticTestCase(NAryOp.DIV, 10, 2, null, null),

        //DIV is noncommutative
        ArithmeticTestCase(NAryOp.DIV, 60, 2, 3, 10),
        ArithmeticTestCase(NAryOp.DIV, 2, 3, 10, 0),

        //Null propagation for MOD
        ArithmeticTestCase(NAryOp.MOD, null, 2, 3, null),
        ArithmeticTestCase(NAryOp.MOD, 10, null, 3, null),
        ArithmeticTestCase(NAryOp.MOD, 10, 2, null, null),

        //MOD is noncommutative
        ArithmeticTestCase(NAryOp.MOD, 19, 5, 3, 1),
        ArithmeticTestCase(NAryOp.MOD, 5, 3, 19, 2)
    )

    @Test
    @Parameters
    fun ternaryArithmeticTest(tc: ArithmeticTestCase) {
        val exprNode = NAry(
            tc.op,
            listOf(
                Literal(tc.arg1.toIonValue(), dummyMetas),
                Literal(tc.arg2.toIonValue(), dummyMetas),
                Literal(tc.arg3.toIonValue(), dummyMetas)
            ), dummyMetas)


        val expectedExprValue = tc.expectedResult?.let { valueFactory.newInt(it) } ?: valueFactory.nullValue

        assertEvalExprNode(exprNode, expectedExprValue)
    }

    private fun assertEvalExprNode(
        exprNode: NAry,
        expectedExprValue: ExprValue) {
        val pipeline = CompilerPipeline.standard(ion)
        val expr = pipeline.compile(exprNode)
        val result = expr.eval(session)
        assertEquals(expectedExprValue.ionValue, result.ionValue)
    }


    /**
     * A test case for comparison operators.
     *
     * A null value of any property is converted to PartiQL `NULL`.
     */
    data class ComparisonTestCase(val op: NAryOp, val args: List<Long?>, val expectedResult: Boolean?)

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

        class FuncDef(val op: NAryOp, val block: (Long?, Long?) -> Boolean?)
        val possibleFuncs = listOf(
            FuncDef(NAryOp.EQ, { v1, v2 -> if(v1 == null || v2 == null) { null } else { v1 == v2 } }),
            FuncDef(NAryOp.GT, { v1, v2 -> if(v1 == null || v2 == null) { null } else { v1 >v2 } }),
            FuncDef(NAryOp.GTE, { v1, v2 -> if(v1 == null || v2 == null) { null } else { v1 >= v2 } }),
            FuncDef(NAryOp.LT, { v1, v2 -> if(v1 == null || v2 == null) { null } else { v1 < v2 } }),
            FuncDef(NAryOp.LTE, { v1, v2 -> if(v1 == null || v2 == null) { null } else { v1 <= v2 } })
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
                        val result = func.block(current, it)
                        when (result) {
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
        val exprNode = NAry(
            tc.op,
            tc.args.map { Literal(it.toIonValue(), dummyMetas) },
            dummyMetas)


        val expectedExprValue = tc.expectedResult?.let { valueFactory.newBoolean(it) } ?: valueFactory.nullValue

        assertEvalExprNode(exprNode, expectedExprValue)
    }

    data class LogicalOperatorsTestCase(val op: NAryOp, val b1: Boolean?, val b2: Boolean?, val b3: Boolean?, val expectedResult: Boolean?)


    fun parametersForLogicalOperatorsTest() = listOf(
        // AND tests
        // true, false arguments
        LogicalOperatorsTestCase(NAryOp.AND, true, true, true, true),
        LogicalOperatorsTestCase(NAryOp.AND, true, true, false, false),
        LogicalOperatorsTestCase(NAryOp.AND, true, false, true, false),
        LogicalOperatorsTestCase(NAryOp.AND, false, true, true, false),
        // Null only propagates when none of the terms are false.  
        // If any one of them is false, the entire expression evaluates to false.
        // true, null arguments
        LogicalOperatorsTestCase(NAryOp.AND, true, true, null, null),
        LogicalOperatorsTestCase(NAryOp.AND, true, null, true, null),
        LogicalOperatorsTestCase(NAryOp.AND, null, true, true, null),

        // true, false, null arguments.
        LogicalOperatorsTestCase(NAryOp.AND, true, false, null, false),
        LogicalOperatorsTestCase(NAryOp.AND, true, null, false, false),
        LogicalOperatorsTestCase(NAryOp.AND, null, true, false, false),

        // OR tests
        // true, false arguments
        LogicalOperatorsTestCase(NAryOp.OR, false, false, false, false),
        LogicalOperatorsTestCase(NAryOp.OR, false, false, true, true),
        LogicalOperatorsTestCase(NAryOp.OR, false, true, false, true),
        LogicalOperatorsTestCase(NAryOp.OR, true, false, false, true),

        // Null only propagates when none of the terms are true.
        // If any one of them is true the entire expression evaluates to true.
        LogicalOperatorsTestCase(NAryOp.OR, false, false, null, null),
        LogicalOperatorsTestCase(NAryOp.OR, false, null, false, null),
        LogicalOperatorsTestCase(NAryOp.OR, null, false, false, null),
        
        // true, false, null arguments
        LogicalOperatorsTestCase(NAryOp.OR, true, false, null, true),
        LogicalOperatorsTestCase(NAryOp.OR, true, null, false, true),
        LogicalOperatorsTestCase(NAryOp.OR, null, true, false, true)
    )

    @Test
    @Parameters
    fun logicalOperatorsTest(tc: LogicalOperatorsTestCase) {
        val exprNode = NAry(
            tc.op,
            listOf(
                Literal(tc.b1.toIonValue(), dummyMetas),
                Literal(tc.b2.toIonValue(), dummyMetas),
                Literal(tc.b3.toIonValue(), dummyMetas)
            ), dummyMetas)


        val expectedExprValue = tc.expectedResult?.let { valueFactory.newBoolean(it)} ?: valueFactory.nullValue

        assertEvalExprNode(exprNode, expectedExprValue)
    }
}