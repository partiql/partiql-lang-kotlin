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

import org.junit.*

/**
 * Test cases for PartiQL Unknown values `MISSING` and `NULL`.
 * See Unknowns.md for details.
 */

class UnknownValTest : EvaluatorTestBase() {

    private val nullSample = mapOf("nullSample" to """
            [
                {val: "A", control: true, n: 1},
                {val: "B", control: false, n: null},
                {val: "C", control: null, n: 3},
            ]
            """)

    private val missingSample = mapOf("missingSample" to """
            [
                {val: "A", control: true, n: 1},
                {val: "B", control: false, n: 2},
                {val: "C" ,},
            ]
            """)


    private val missingAndNullSample = mapOf("missingAndNullSample" to """
            [
                {val: "A", control: true, n:2},
                {val: "B", control: false, n: 2},
                {val: "C", int:3},
                {val: "D", control: null, n:5},
            ]
            """)

    private val stringConcatWithNulls = mapOf("stringConcatWithNulls" to """
            [
                {f: "a", s: "b"},
                {f: "a", s: null},
                {f: null, s: "b"},
                {f: null, s: null},
                {f: "a"},
                {s: "b"},
                {f: null},
                {s: null},
                {}
            ]
        """)

    private val arithWithUnkowns = mapOf("arithWithUnkowns" to """
            [
                {x: 4, y: 2},
                {x: 4, y: null},
                {x: null, y: 2},
                {x: null, y: null},
                {x: 4},
                {y: 2},
                {x: null},
                {y: null},
                {}
            ]
        """)


    private val boolsWithUnknowns = mapOf("boolsWithUnknowns" to """
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
        """)

    private val numsWithNull = mapOf("numsWithNull" to """[1,2,3,5,4,6]""")


    @Test
    fun whereClauseExprEvalsToNull() = assertEval("SELECT VALUE D.val from nullSample as D WHERE D.control",
                                                  """["A"]""",
                                                  nullSample.toSession())

    @Test
    fun whereClauseExprEvalsToMissing() = assertEval("SELECT VALUE D.val from missingSample as D WHERE D.control",
                                                     """["A"]""",
                                                     missingSample.toSession())

    @Test
    fun whereClauseExprEvalsToNullAndMissing() = assertEval("SELECT VALUE D.val from missingAndNullSample as D WHERE D.control",
                                                            """["A"]""",
                                                            missingAndNullSample.toSession())

    @Test
    fun aggregateSumWithNull() = assertEval("SELECT sum(x.n) from nullSample as x", "[{_1: 4}]", nullSample.toSession())

    @Test
    fun aggregateSumWithMissing() = assertEval("SELECT sum(x.n) from missingSample as x",
                                               "[{_1: 3}]",
                                               missingSample.toSession())

    @Test
    fun aggregateSumWithMissingAndNull() = assertEval("SELECT sum(x.n) from missingAndNullSample as x",
                                                      "[{_1: 9}]",
                                                      missingAndNullSample.toSession())


    @Test
    fun aggregateMinWithNull() = assertEval("SELECT min(x.n) from nullSample as x", "[{_1: 1}]", nullSample.toSession())

    @Test
    fun aggregateMinWithMissing() = assertEval("SELECT min(x.n) from missingSample as x",
                                               "[{_1: 1}]",
                                               missingSample.toSession())

    @Test
    fun aggregateMinWithMissingAndNull() = assertEval("SELECT min(x.n) from missingAndNullSample as x",
                                                      "[{_1: 2}]",
                                                      missingAndNullSample.toSession())


    @Test
    fun aggregateAvgWithNull() = assertEval("SELECT avg(x.n) from nullSample as x", "[{_1: 2.}]", nullSample.toSession())

    @Test
    fun aggregateAvgWithMissing() = assertEval("SELECT avg(x.n) from missingSample as x",
                                               "[{_1: 1.5}]",
                                               missingSample.toSession())

    @Test
    fun aggregateAvgWithMissingAndNull() = assertEval("SELECT avg(x.n) from missingAndNullSample as x",
                                                      "[{_1: 3.}]",
                                                      missingAndNullSample.toSession())


    @Test
    fun aggregateCountWithNull() = assertEval("SELECT count(x.n) from nullSample as x",
                                              "[{_1: 2}]",
                                              nullSample.toSession())

    @Test
    fun aggregateCountWithMissing() = assertEval("SELECT count(x.n) from missingSample as x",
                                                 "[{_1: 2}]",
                                                 missingSample.toSession())

    @Test
    fun aggregateCountWithMissingAndNull() = assertEval("SELECT count(x.n) from missingAndNullSample as x",
                                                        "[{_1: 3}]",
                                                        missingAndNullSample.toSession())

    @Test
    fun countEmpty() = assertEval("SELECT count(*) from `[]`", "[{_1: 0}]")

    @Test
    fun countEmptyTuple() = assertEval("SELECT count(*) from `[{}]`", "[{_1: 1}]")


    @Test
    fun sumEmpty() = assertEval("SELECT sum(x.i) from `[]` as x", "[{_1: null}]")

    @Test
    fun sumEmptyTuple() = assertEval("SELECT sum(x.i) from `[{}]` as x", "[{_1: null}]")

    @Test
    fun avgEmpty() = assertEval("SELECT avg(x.i) from `[]` as x", "[{_1: null}]")

    @Test
    fun avgEmptyTuple() = assertEval("SELECT avg(x.i) from `[{}]` as x", "[{_1: null}]")

    @Test
    fun avgSomeEmptyTuples() = assertEval("SELECT avg(x.i) from `[{i: 1}, {}, {i:3}]` as x",
                                          "[{_1: 2.}]")

    @Test
    fun avgSomeEmptyAndNullTuples() = assertEval("SELECT avg(x.i) from `[{i: 1}, {}, {i:null}, {i:3}]` as x",
                                          "[{_1: 2.}]")

    @Test
    fun minSomeEmptyTuples() = assertEval("SELECT min(x.i) from `[{i: null}, {}, {i:3}]` as x",
                                          "[{_1: 3}]")

    @Test
    fun maxSomeEmptyTuples() = assertEval("SELECT max(x.i) from `[{i: null}, {}, {i:3}, {i:10}]` as x",
                                          "[{_1: 10}]")
    @Test
    fun minEmpty() = assertEval("SELECT min(x.i) from `[]` as x", "[{_1: null}]")

    @Test
    fun minEmptyTuple() = assertEval("SELECT min(x.i) from `[{}]` as x", "[{_1: null}]")

    @Test
    fun maxEmpty() = assertEval("SELECT max(x.i) from `[]` as x", "[{_1: null}]")

    @Test
    fun maxEmptyTuple() = assertEval("SELECT max(x.i) from `[{}]` as x", "[{_1: null}]")

    @Test
    fun maxSomeEmptyTuple() = assertEval("SELECT max(x.i) from `[{}, {i:1}, {}, {i:2}]` as x",
                                         "[{_1: 2}]")

    @Test
    fun minSomeEmptyTuple() = assertEval("SELECT min(x.i) from `[{}, {i:1}, {}, {i:2}]` as x",
                                         "[{_1: 1}]")

    @Test
    fun sumSomeEmptyTuple() = assertEval("SELECT sum(x.i) from `[{}, {i:1}, {}, {i:2}]` as x",
                                         "[{_1: 3}]")

    @Test
    fun countSomeEmptyTuple() = assertEval("SELECT count(x.i) from `[{}, {i:1}, {}, {i:2}]` as x",
                                           "[{_1: 2}]")

    @Test
    fun countStar() = assertEval("SELECT count(*) from `[{}, {i:1}, {}, {i:2}]` as x",
                                 "[{_1: 4}]")

    @Test
    fun countLiteral() = assertEval("SELECT count(1) from `[{}, {}, {}, {}]` as x", "[{_1: 4}]")


    @Test
    fun stringConcatOperatorNullOperand() = assertEval("SELECT  i.f || i.s FROM stringConcatWithNulls as i",
                                                       "[{_1:\"ab\"},{_1:null},{_1:null},{_1:null},{_1:null},{_1:null},{_1:null},{_1:null},{_1:null}]",
                                                       stringConcatWithNulls.toSession())


    @Test
    fun unaryPlus() = assertEval("SELECT +i.x from `[{x:null}, {}]` as i", "[{_1:null}, {_1:null}]")

    @Test
    fun plusWithUnknownOperands() = assertEval("SELECT  i.x + i.y FROM arithWithUnkowns as i",
                                              "[{_1:6},{_1:null},{_1:null},{_1:null},{_1:null},{_1:null},{_1:null},{_1:null},{_1:null}]",
                                              arithWithUnkowns.toSession())

    @Test
    fun unaryMinus() = assertEval("SELECT -i.x from `[{x:null}, {}]` as i", "[{_1:null}, {_1:null}]")

    @Test
    fun minusWithUnknownOperands() = assertEval("SELECT  i.x - i.y FROM arithWithUnkowns as i",
                                               "[{_1:2},{_1:null},{_1:null},{_1:null},{_1:null},{_1:null},{_1:null},{_1:null},{_1:null}]",
                                               arithWithUnkowns.toSession())

    @Test
    fun mulWithUnknownOperands() = assertEval("SELECT  i.x * i.y FROM arithWithUnkowns as i",
                                             "[{_1:8},{_1:null},{_1:null},{_1:null},{_1:null},{_1:null},{_1:null},{_1:null},{_1:null}]",
                                             arithWithUnkowns.toSession())

    @Test
    fun divWithUnknownOperands() = assertEval("SELECT  i.x / i.y FROM arithWithUnkowns as i",
                                             "[{_1:2},{_1:null},{_1:null},{_1:null},{_1:null},{_1:null},{_1:null},{_1:null},{_1:null}]",
                                             arithWithUnkowns.toSession())

    @Test
    fun modWithUnknownOperands() = assertEval("SELECT  i.x % i.y FROM arithWithUnkowns as i",
                                             "[{_1:0},{_1:null},{_1:null},{_1:null},{_1:null},{_1:null},{_1:null},{_1:null},{_1:null}]",
                                             arithWithUnkowns.toSession())

    @Test
    fun lessThanWithUnknownOperands() = assertEval("SELECT  i.x < i.y FROM arithWithUnkowns as i",
                                                  "[{_1:false},{_1:null},{_1:null},{_1:null},{_1:null},{_1:null},{_1:null},{_1:null},{_1:null}]",
                                                  arithWithUnkowns.toSession())

    @Test
    fun lessThanOrEqualWithUnknownOperands() = assertEval("SELECT  i.x <= i.y FROM arithWithUnkowns as i",
                                                         "[{_1:false},{_1:null},{_1:null},{_1:null},{_1:null},{_1:null},{_1:null},{_1:null},{_1:null}]",
                                                         arithWithUnkowns.toSession())


    @Test
    fun greaterThanWithUnknownOperands() = assertEval("SELECT  i.x > i.y FROM arithWithUnkowns as i",
                                                     "[{_1:true},{_1:null},{_1:null},{_1:null},{_1:null},{_1:null},{_1:null},{_1:null},{_1:null}]",
                                                     arithWithUnkowns.toSession())

    @Test
    fun greaterThanOrEqualWithUnknownOperands() = assertEval("SELECT  i.x >= i.y FROM arithWithUnkowns as i",
                                                            "[{_1:true},{_1:null},{_1:null},{_1:null},{_1:null},{_1:null},{_1:null},{_1:null},{_1:null}]",
                                                            arithWithUnkowns.toSession())

    @Test
    fun equalWithUnknownOperands() = assertEval("SELECT  i.x = i.y FROM arithWithUnkowns as i",
                                               "[{_1:false},{_1:null},{_1:null},{_1:null},{_1:null},{_1:null},{_1:null},{_1:null},{_1:null}]",
                                               arithWithUnkowns.toSession())

    @Test
    fun unEqualWithUnknownOperands() = assertEval("SELECT  i.x <> i.y FROM arithWithUnkowns as i",
                                                 "[{_1:true},{_1:null},{_1:null},{_1:null},{_1:null},{_1:null},{_1:null},{_1:null},{_1:null}]",
                                                 arithWithUnkowns.toSession())

    @Test
    fun notWithUnknownOperands() = assertEval("SELECT not i.x FROM boolsWithUnknowns as i",
                                             "[{_1:false},{_1:false},{_1:true},{_1:true},{_1:false},{_1:true},{_1:null},{_1:null},{_1:null},{_1:false},{_1:true},{_1:null},{_1:null},{_1:null},{_1:null},{_1:null}]",
                                             boolsWithUnknowns.toSession())

    @Test
    fun orWithUnknownOperands() = assertEval("SELECT  i.x or i.y FROM boolsWithUnknowns as i",
                                             "[{_1:true},{_1:true},{_1:false},{_1:true},{_1:true},{_1:null},{_1:null},{_1:true},{_1:null},{_1:true},{_1:null},{_1:true},{_1:null},{_1:null},{_1:null},{_1:null}]",
                                             boolsWithUnknowns.toSession())

    @Test
    fun andWithUnknownOperands() = assertEval("SELECT  i.x and i.y FROM boolsWithUnknowns as i",
                                            "[{_1:true},{_1:false},{_1:false},{_1:false},{_1:null},{_1:false},{_1:false},{_1:null},{_1:null},{_1:null},{_1:false},{_1:null},{_1:false},{_1:null},{_1:null},{_1:null}]",
                                            boolsWithUnknowns.toSession())

    @Test
    fun andShortCircuits() = assertEval("SELECT s.x FROM [{'x': '1.1'},{'x': '2'},{'x': '3'},{'x': '4'},{'x': '5'}] as s WHERE FALSE AND CAST(s.x as INT)",
                                              "[]",
                                              boolsWithUnknowns.toSession())

    @Test
    fun andWithNullDoesNotShortCircuits() = assertThrows("can't convert string value to INT", NodeMetadata(1,100)) {
        voidEval("SELECT s.x FROM [{'x': '1.1'},{'x': '2'},{'x': '3'},{'x': '4'},{'x': '5'}] as s WHERE NULL AND CAST(s.x as INT)")
    }

    @Test
    fun andWithMissingDoesNotShortCircuits() = assertThrows("can't convert string value to INT", NodeMetadata(1,103)) {
        voidEval("SELECT s.x FROM [{'x': '1.1'},{'x': '2'},{'x': '3'},{'x': '4'},{'x': '5'}] as s WHERE MISSING AND CAST(s.x as INT)")
    }

}





