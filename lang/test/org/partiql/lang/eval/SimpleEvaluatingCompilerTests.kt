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
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.util.propertyValueMapOf

class SimpleEvaluatingCompilerTests : EvaluatorTestBase() {
    val session = mapOf(
        "someScalar" to "1",
        "listOfInts" to "[1, 2, 3, 4, 5]"
    ).toSession()

    @Test
    fun selectValue() {
        runEvaluatorTestCase("SELECT VALUE someScalar FROM someScalar", session, "[1]")
    }

    @Test
    fun selectValueWithAsAlias() {
        runEvaluatorTestCase("SELECT VALUE s FROM someScalar as s", session, "[1]")
    }

    @Test
    fun selectStar() {
        runEvaluatorTestCase("SELECT * FROM `[{a: 100, b: 101}]`", expectedResult = "[{a: 100, b: 101}]")
    }

    @Test
    fun selectStarWhere() {
        runEvaluatorTestCase(
            "SELECT * FROM `[{a: 100, b: 1000}, {a: 101, b: 1001}]` WHERE a > 100",
            expectedResult = "[{a: 101, b: 1001}]"
        )
    }

    @Test
    fun selectList() {
        runEvaluatorTestCase("SELECT a, b FROM `[{a: 100, b: 101}]`", expectedResult = "[{a: 100, b: 101}]")
    }

    @Test
    fun selectListWithBinaryExpr() {
        runEvaluatorTestCase("SELECT a + b FROM `[{a: 100, b: 101}]`", expectedResult = "[{_1: 201}]")
    }

    @Test
    fun selectListWithBinaryExprAndAlias() {
        runEvaluatorTestCase("SELECT a + b AS c FROM `[{a: 100, b: 101}]`", expectedResult = "[{c: 201}]")
    }

    @Test
    fun unpivot() = runEvaluatorTestCase(
        "SELECT name, val FROM UNPIVOT `{a:1, b:2, c:3, d:4, e:5, f:6}` AS val AT name",
        expectedResult = """[
                    {name:"a",val:1},
                    {name:"b",val:2},
                    {name:"c",val:3},
                    {name:"d",val:4},
                    {name:"e",val:5},
                    {name:"f",val:6}
                ]"""
    )

    @Test
    fun simpleJoin() = runEvaluatorTestCase(
        """SELECT * FROM `[{a: 1}, {a: 2}]` AS t1 INNER CROSS JOIN `[{b: 1, c: "one" }, {b: 2, c: "two" }]`""",
        expectedResult = """[
                    {a:1,b:1,c:"one"},
                    {a:1,b:2,c:"two"},
                    {a:2,b:1,c:"one"},
                    {a:2,b:2,c:"two"}
                ]"""
    )

    @Test
    fun simpleJoinWithCondition() = runEvaluatorTestCase(
        """
            SELECT *
            FROM `[{a: 1}, {a: 2}]` AS t1
                INNER JOIN `[{b: 1, c: "one" }, {b: 2, c:"two" }]` AS t2
                    ON t1.a = t2.b""",
        expectedResult = """[
                    {a:1,b:1,c:"one"},
                    {a:2,b:2,c:"two"}
                ]"""
    )

    @Test
    fun tableAliases() = runEvaluatorTestCase(
        "SELECT _2 FROM `[{_1: a, _2: 1}, {_1: a, _2: 'a'}, {_1: a, _2: 3}]` WHERE _2 = 21",
        expectedResult = "[]"
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
        runEvaluatorTestCase("SUM(`[1, 2, 3]`)", expectedResult = "6")
        runEvaluatorTestCase("SUM(`[1, 2e0, 3e0]`)", expectedResult = "6e0")
        runEvaluatorTestCase("SUM(`[1, 2d0, 3d0]`)", expectedResult = "6d0")
        runEvaluatorTestCase("SUM(`[1, 2e0, 3d0]`)", expectedResult = "6d0")
        runEvaluatorTestCase("SUM(`[1, 2d0, 3e0]`)", expectedResult = "6d0")
    }

    @Test
    fun maxAndMin() {
        // Number
        runEvaluatorTestCase("max(`[1, 2, 3]`)", expectedResult = "3")
        runEvaluatorTestCase("max(`[1, 2.0, 3]`)", expectedResult = "3")
        runEvaluatorTestCase("max(`[1, 2e0, 3e0]`)", expectedResult = "3e0")
        runEvaluatorTestCase("max(`[1, 2d0, 3d0]`)", expectedResult = "3d0")
        runEvaluatorTestCase("max(`[1, 2e0, 3d0]`)", expectedResult = "3d0")
        runEvaluatorTestCase("max(`[1, 2d0, 3e0]`)", expectedResult = "3e0")
        runEvaluatorTestCase("min(`[1, 2, 3]`)", expectedResult = "1")
        runEvaluatorTestCase("min(`[1, 2.0, 3]`)", expectedResult = "1")
        runEvaluatorTestCase("min(`[1, 2e0, 3e0]`)", expectedResult = "1")
        runEvaluatorTestCase("min(`[1, 2d0, 3d0]`)", expectedResult = "1")
        runEvaluatorTestCase("min(`[1, 2e0, 3d0]`)", expectedResult = "1")
        runEvaluatorTestCase("min(`[1, 2d0, 3e0]`)", expectedResult = "1")

        // String
        runEvaluatorTestCase("max(['a', 'abc', '3'])", expectedResult = "\"abc\"")
        runEvaluatorTestCase("max(['1', '2', '3', null])", expectedResult = "\"3\"")
        runEvaluatorTestCase("min(['a', 'abc', '3'])", expectedResult = "\"3\"")
        runEvaluatorTestCase("min(['1', '2', '3', null])", expectedResult = "\"1\"")

        // Timestamp
        runEvaluatorTestCase("max([`2020-01-01T00:00:00Z`, `2020-01-01T00:00:01Z`, `2020-01-01T00:00:02Z`])", expectedResult = "2020-01-01T00:00:02Z")
        runEvaluatorTestCase("max([`2020-01-01T00:00:00Z`, `2020-01-01T00:01:00Z`, `2020-01-01T00:02:00Z`])", expectedResult = "2020-01-01T00:02:00Z")
        runEvaluatorTestCase("max([`2020-01-01T00:00:00Z`, `2020-01-01T01:00:00Z`, `2020-01-01T02:00:00Z`])", expectedResult = "2020-01-01T02:00:00Z")
        runEvaluatorTestCase("max([`2020-01-01T00:00:00Z`, `2020-01-02T00:00:00Z`, `2020-01-03T00:00:00Z`])", expectedResult = "2020-01-03T00:00:00Z")
        runEvaluatorTestCase("max([`2020-01-01T00:00:00Z`, `2020-02-01T00:00:00Z`, `2020-03-01T00:00:00Z`])", expectedResult = "2020-03-01T00:00:00Z")
        runEvaluatorTestCase("max([`2020-01-01T00:00:00Z`, `2021-01-01T00:00:00Z`, `2022-01-01T00:00:00Z`])", expectedResult = "2022-01-01T00:00:00Z")
        runEvaluatorTestCase("max([`2020-01-01T00:00:00Z`, `2020-01-01T00:00:01Z`, `2020-01-01T00:00:02Z`, null])", expectedResult = "2020-01-01T00:00:02Z")
        runEvaluatorTestCase("min([`2020-01-01T00:00:00Z`, `2020-01-01T00:00:01Z`, `2020-01-01T00:00:02Z`])", expectedResult = "2020-01-01T00:00:00Z")
        runEvaluatorTestCase("min([`2020-01-01T00:00:00Z`, `2020-01-01T00:01:00Z`, `2020-01-01T00:02:00Z`])", expectedResult = "2020-01-01T00:00:00Z")
        runEvaluatorTestCase("min([`2020-01-01T00:00:00Z`, `2020-01-01T01:00:00Z`, `2020-01-01T02:00:00Z`])", expectedResult = "2020-01-01T00:00:00Z")
        runEvaluatorTestCase("min([`2020-01-01T00:00:00Z`, `2020-01-02T00:00:00Z`, `2020-01-03T00:00:00Z`])", expectedResult = "2020-01-01T00:00:00Z")
        runEvaluatorTestCase("min([`2020-01-01T00:00:00Z`, `2020-02-01T00:00:00Z`, `2020-03-01T00:00:00Z`])", expectedResult = "2020-01-01T00:00:00Z")
        runEvaluatorTestCase("min([`2020-01-01T00:00:00Z`, `2021-01-01T00:00:00Z`, `2022-01-01T00:00:00Z`])", expectedResult = "2020-01-01T00:00:00Z")
        runEvaluatorTestCase("min([`2020-01-01T00:00:00Z`, `2020-01-01T00:00:01Z`, `2020-01-01T00:00:02Z`, null])", expectedResult = "2020-01-01T00:00:00Z")

        // Other data types
        runEvaluatorTestCase("max([NULL, NULL])", expectedResult = "null")
        runEvaluatorTestCase("max([MISSING, NULL])", expectedResult = "null")
        runEvaluatorTestCase("max([MISSING, MISSING])", expectedResult = "null")
        runEvaluatorTestCase("max([false, true])", expectedResult = "true")
        runEvaluatorTestCase("max([`{{ aaaa }}`, `{{ aaab }}`])", expectedResult = "{{aaab}}")
        runEvaluatorTestCase("max([`{{\"a\"}}`, `{{\"b\"}}`])", expectedResult = "{{\"b\"}}")
        runEvaluatorTestCase("min([NULL, NULL])", expectedResult = "null")
        runEvaluatorTestCase("min([MISSING, NULL])", expectedResult = "null")
        runEvaluatorTestCase("min([MISSING, MISSING])", expectedResult = "null")
        runEvaluatorTestCase("min([false, true])", expectedResult = "false")
        runEvaluatorTestCase("min([`{{ aaaa }}`, `{{ aaab }}`])", expectedResult = "{{aaaa}}")
        runEvaluatorTestCase("min([`{{\"a\"}}`, `{{\"b\"}}`])", expectedResult = "{{\"a\"}}")

        // Across data types
        runEvaluatorTestCase("max([false, 1])", expectedResult = "1")
        runEvaluatorTestCase("max([`2020-01-01T00:00:00Z`, 1])", expectedResult = "2020-01-01T00:00:00Z")
        runEvaluatorTestCase("max([`2020-01-01T00:00:00Z`, '1'])", expectedResult = "\"1\"")
        runEvaluatorTestCase("max([`{{\"abcd\"}}`, '1'])", expectedResult = "{{\"abcd\"}}")
        runEvaluatorTestCase("min([false, 1])", expectedResult = "false")
        runEvaluatorTestCase("min([`2020-01-01T00:00:00Z`, 1])", expectedResult = "1")
        runEvaluatorTestCase("min([`2020-01-01T00:00:00Z`, '1'])", expectedResult = "2020-01-01T00:00:00Z")
        runEvaluatorTestCase("min([`{{\"abcd\"}}`, '1'])", expectedResult = "\"1\"")
    }
}
