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

import org.junit.Test
import org.partiql.errors.ErrorCode
import org.partiql.errors.Property
import org.partiql.lang.eval.evaluatortestframework.EvaluatorTestTarget
import org.partiql.lang.util.propertyValueMapOf

class SimpleEvaluatingCompilerTests : EvaluatorTestBase() {
    val session = mapOf(
        "someScalar" to "1",
        "listOfInts" to "[1, 2, 3, 4, 5]"
    ).toSession()

    @Test
    fun selectValue() {
        runEvaluatorTestCase("SELECT VALUE someScalar FROM someScalar", session, "$BAG_ANNOTATION::[1]")
    }

    @Test
    fun selectValueWithAsAlias() {
        runEvaluatorTestCase("SELECT VALUE s FROM someScalar as s", session, "$BAG_ANNOTATION::[1]")
    }

    @Test
    fun selectStar() {
        runEvaluatorTestCase("SELECT * FROM `[{a: 100, b: 101}]`", expectedResult = "$BAG_ANNOTATION::[{a: 100, b: 101}]")
    }

    @Test
    fun selectStarWhere() {
        runEvaluatorTestCase(
            "SELECT * FROM `[{a: 100, b: 1000}, {a: 101, b: 1001}]` WHERE a > 100",
            expectedResult = "$BAG_ANNOTATION::[{a: 101, b: 1001}]"
        )
    }

    @Test
    fun selectList() {
        runEvaluatorTestCase("SELECT a, b FROM `[{a: 100, b: 101}]`", expectedResult = "$BAG_ANNOTATION::[{a: 100, b: 101}]")
    }

    @Test
    fun selectListWithBinaryExpr() {
        runEvaluatorTestCase("SELECT a + b FROM `[{a: 100, b: 101}]`", expectedResult = "$BAG_ANNOTATION::[{_1: 201}]")
    }

    @Test
    fun selectListWithBinaryExprAndAlias() {
        runEvaluatorTestCase("SELECT a + b AS c FROM `[{a: 100, b: 101}]`", expectedResult = "$BAG_ANNOTATION::[{c: 201}]")
    }

    @Test
    fun unpivot() = runEvaluatorTestCase(
        "SELECT name, val FROM UNPIVOT `{a:1, b:2, c:3, d:4, e:5, f:6}` AS val AT name",
        expectedResult = """$BAG_ANNOTATION::[
                    {name:"a",val:1},
                    {name:"b",val:2},
                    {name:"c",val:3},
                    {name:"d",val:4},
                    {name:"e",val:5},
                    {name:"f",val:6}
                ]""",
        // planner & physical plan have no support for UNPIVOT (yet)
        target = EvaluatorTestTarget.COMPILER_PIPELINE,
    )

    @Test
    fun simpleJoin() = runEvaluatorTestCase(
        """SELECT * FROM `[{a: 1}, {a: 2}]` AS t1 INNER CROSS JOIN `[{b: 1, c: "one" }, {b: 2, c: "two" }]`""",
        expectedResult = """$BAG_ANNOTATION::[
                    {a:1,b:1,c:"one"},
                    {a:1,b:2,c:"two"},
                    {a:2,b:1,c:"one"},
                    {a:2,b:2,c:"two"}
                ]"""
    )

    private val sessionWithG = mapOf(
        "table_1" to "[{a:[{b: 1}, {b:2}]}]",
        "g" to "{a: \"from global variable g\"}"
    ).toSession()

    /** Demonstrates that without the scope qualifier ('@'), the `g` in `g.a' refers to global `g`. */
    @Test
    fun joinWithoutScopeQualifier() = runEvaluatorTestCase(
        """SELECT g2 FROM table_1 AS g, g.a AS g2""",
        session = sessionWithG,
        expectedResult = "$BAG_ANNOTATION::[{g2:\"from global variable g\"}]"
    )

    /** Demonstrates that with the scope qualifier ('@'), the `g` in `@g.a' refers to local `g`. */
    @Test
    fun joinWithScopeQualifier() = runEvaluatorTestCase(
        """SELECT g2 FROM table_1 AS g, @g.a AS g2""",
        session = sessionWithG,
        expectedResult = "$BAG_ANNOTATION::[{g2:{b:1}},{g2:{b:2}}]"
    )

    @Test
    fun simpleJoinWithCondition() = runEvaluatorTestCase(
        """
            SELECT *
            FROM `[{a: 1}, {a: 2}]` AS t1
                INNER JOIN `[{b: 1, c: "one" }, {b: 2, c:"two" }]` AS t2
                    ON t1.a = t2.b""",
        expectedResult = """$BAG_ANNOTATION::[
                    {a:1,b:1,c:"one"},
                    {a:2,b:2,c:"two"}
                ]"""
    )

    @Test
    fun tableAliases() = runEvaluatorTestCase(
        "SELECT _2 FROM `[{_1: a, _2: 1}, {_1: a, _2: 'a'}, {_1: a, _2: 3}]` WHERE _2 = 21",
        expectedResult = "$BAG_ANNOTATION::[]"
    )

    @Test
    fun castStringToIntFailed() = runEvaluatorErrorTestCase(
        "CAST(`'a'` as INT)",
        ErrorCode.EVALUATOR_CAST_FAILED,
        propertyValueMapOf(1, 1, Property.CAST_FROM to "SYMBOL", Property.CAST_TO to "INT"),
        expectedPermissiveModeResult = "MISSING"
    )

    @Test
    fun sum() {
        // Note: planner & phys. alg. have no support for aggregates (yet)
        runEvaluatorTestCase("SUM(`[1, 2, 3]`)", expectedResult = "6", target = EvaluatorTestTarget.COMPILER_PIPELINE)
        runEvaluatorTestCase(
            "SUM(`[1, 2e0, 3e0]`)",
            expectedResult = "6e0",
            target = EvaluatorTestTarget.COMPILER_PIPELINE
        )
        runEvaluatorTestCase(
            "SUM(`[1, 2d0, 3d0]`)",
            expectedResult = "6d0",
            target = EvaluatorTestTarget.COMPILER_PIPELINE
        )
        runEvaluatorTestCase(
            "SUM(`[1, 2e0, 3d0]`)",
            expectedResult = "6d0",
            target = EvaluatorTestTarget.COMPILER_PIPELINE
        )
        runEvaluatorTestCase(
            "SUM(`[1, 2d0, 3e0]`)",
            expectedResult = "6d0",
            target = EvaluatorTestTarget.COMPILER_PIPELINE
        )
    }

    @Test
    fun max() {
        // Note: planner & phys. alg. have no support for aggregates (yet)
        runEvaluatorTestCase("max(`[1, 2, 3]`)", expectedResult = "3", target = EvaluatorTestTarget.COMPILER_PIPELINE)
        runEvaluatorTestCase("max(`[1, 2.0, 3]`)", expectedResult = "3", target = EvaluatorTestTarget.COMPILER_PIPELINE)
        runEvaluatorTestCase(
            "max(`[1, 2e0, 3e0]`)",
            expectedResult = "3e0",
            target = EvaluatorTestTarget.COMPILER_PIPELINE
        )
        runEvaluatorTestCase(
            "max(`[1, 2d0, 3d0]`)",
            expectedResult = "3d0",
            target = EvaluatorTestTarget.COMPILER_PIPELINE
        )
        runEvaluatorTestCase(
            "max(`[1, 2e0, 3d0]`)",
            expectedResult = "3d0",
            target = EvaluatorTestTarget.COMPILER_PIPELINE
        )
        runEvaluatorTestCase(
            "max(`[1, 2d0, 3e0]`)",
            expectedResult = "3e0",
            target = EvaluatorTestTarget.COMPILER_PIPELINE
        )
    }
}
