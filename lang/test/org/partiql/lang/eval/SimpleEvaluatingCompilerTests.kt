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

import org.partiql.lang.errors.*
import org.partiql.lang.util.*
import org.junit.*

class SimpleEvaluatingCompilerTests : EvaluatorTestBase() {
    val session = mapOf(
        "someScalar" to "1",
        "listOfInts" to "[1, 2, 3, 4, 5]"
    ).toSession()


    @Test
    fun selectValue() {
        assertEval("SELECT VALUE someScalar FROM someScalar", "[1]", session)
    }

    @Test
    fun selectValueWithAsAlias() {
        assertEval("SELECT VALUE s FROM someScalar as s", "[1]", session)
    }

    @Test
    fun selectStar() {
        assertEval("SELECT * FROM `[{a: 100, b: 101}]`", "[{a: 100, b: 101}]")
    }

    @Test
    fun selectStarWhere() {
        assertEval(
            "SELECT * FROM `[{a: 100, b: 1000}, {a: 101, b: 1001}]` WHERE a > 100",
            "[{a: 101, b: 1001}]")
    }

    @Test
    fun selectList() {
        assertEval("SELECT a, b FROM `[{a: 100, b: 101}]`", "[{a: 100, b: 101}]")
    }

    @Test
    fun selectListWithBinaryExpr() {
        assertEval("SELECT a + b FROM `[{a: 100, b: 101}]`", "[{_1: 201}]")
    }

    @Test
    fun selectListWithBinaryExprAndAlias() {
        assertEval("SELECT a + b AS c FROM `[{a: 100, b: 101}]`", "[{c: 201}]")
    }

    @Test
    fun unpivot() = assertEval(
        "SELECT name, val FROM UNPIVOT `{a:1, b:2, c:3, d:4, e:5, f:6}` AS val AT name",
        """[
            {name:"a",val:1},
            {name:"b",val:2},
            {name:"c",val:3},
            {name:"d",val:4},
            {name:"e",val:5},
            {name:"f",val:6}
        ]"""
    )

    @Test
    fun simpleJoin() = assertEval(
        """SELECT * FROM `[{a: 1}, {a: 2}]` AS t1 INNER CROSS JOIN `[{b: 1, c: "one" }, {b: 2, c: "two" }]`""",
        """[
            {a:1,b:1,c:"one"},
            {a:1,b:2,c:"two"},
            {a:2,b:1,c:"one"},
            {a:2,b:2,c:"two"}
        ]"""
    )

    @Test
    fun simpleJoinWithCondition() = assertEval(
        """
            SELECT *
            FROM `[{a: 1}, {a: 2}]` AS t1
                INNER JOIN `[{b: 1, c: "one" }, {b: 2, c:"two" }]` AS t2
                    ON t1.a = t2.b""",
        """[
            {a:1,b:1,c:"one"},
            {a:2,b:2,c:"two"}
        ]"""
    )

    @Test
    fun tableAliases() = assertEval("SELECT _2 FROM `[{_1: a, _2: 1}, {_1: a, _2: 'a'}, {_1: a, _2: 3}]` WHERE _2 = 21", "[]")

    @Test
    fun castStringToIntFailed() = checkInputThrowingEvaluationException(
        "CAST(`'a'` as INT)",
        ErrorCode.EVALUATOR_CAST_FAILED,
        sourceLocationProperties(1, 5) + mapOf(Property.CAST_FROM to "SYMBOL", Property.CAST_TO to "INTEGER"))

    @Test
    fun sum() {
        assertEval("SUM(`[1, 2, 3]`)", "6")
        assertEval("SUM(`[1, 2e0, 3e0]`)", "6e0")
        assertEval("SUM(`[1, 2d0, 3d0]`)", "6d0")
        assertEval("SUM(`[1, 2e0, 3d0]`)", "6d0")
        assertEval("SUM(`[1, 2d0, 3e0]`)", "6d0")
    }

    @Test
    fun max() {
        assertEval("max(`[1, 2, 3]`)", "3")
        assertEval("max(`[1, 2.0, 3]`)", "3")
        assertEval("max(`[1, 2e0, 3e0]`)", "3e0")
        assertEval("max(`[1, 2d0, 3d0]`)", "3d0")
        assertEval("max(`[1, 2e0, 3d0]`)", "3d0")
        assertEval("max(`[1, 2d0, 3e0]`)", "3e0")
    }
}
