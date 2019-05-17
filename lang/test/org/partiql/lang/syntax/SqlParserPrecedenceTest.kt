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

package org.partiql.lang.syntax

import junitparams.*
import junitparams.naming.*
import org.junit.*
import com.amazon.ion.*
import org.partiql.lang.ast.*
import org.partiql.lang.ast.passes.*


class SqlParserPrecedenceTest : SqlParserTestBase() {

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun intersectPrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForIntersectPrecedence(): List<Pair<String, String>> = listOf(
        // two by two binary operators
        /* (intersect, intersect_all)   */ "a intersect b intersect all c"         to "(intersect_all (intersect (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (intersect, except)          */ "a intersect b except c"                to "(except (intersect (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (intersect, except_all)      */ "a intersect b except all c"            to "(except_all (intersect (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (intersect, union)           */ "a intersect b union c"                 to "(union (intersect (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (intersect, union_all)       */ "a intersect b union all c"             to "(union_all (intersect (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (intersect, and)             */ "a intersect b and c"                   to "(intersect (id a case_insensitive) (and (id b case_insensitive) (id c case_insensitive)))",
        /* (intersect, or)              */ "a intersect b or c"                    to "(intersect (id a case_insensitive) (or (id b case_insensitive) (id c case_insensitive)))",
        /* (intersect, =)               */ "a intersect b = c"                     to "(intersect (id a case_insensitive) (= (id b case_insensitive) (id c case_insensitive)))",
        /* (intersect, <>)              */ "a intersect b <> c"                    to "(intersect (id a case_insensitive) (<> (id b case_insensitive) (id c case_insensitive)))",
        /* (intersect, in)              */ "a intersect b in c"                    to "(intersect (id a case_insensitive) (in (id b case_insensitive) (id c case_insensitive)))",
        /* (intersect, not_in)          */ "a intersect b not in c"                to "(intersect (id a case_insensitive) (not (in (id b case_insensitive) (id c case_insensitive))))",
        /* (intersect, <)               */ "a intersect b < c"                     to "(intersect (id a case_insensitive) (< (id b case_insensitive) (id c case_insensitive)))",
        /* (intersect, <=)              */ "a intersect b <= c"                    to "(intersect (id a case_insensitive) (<= (id b case_insensitive) (id c case_insensitive)))",
        /* (intersect, >)               */ "a intersect b > c"                     to "(intersect (id a case_insensitive) (> (id b case_insensitive) (id c case_insensitive)))",
        /* (intersect, >=)              */ "a intersect b >= c"                    to "(intersect (id a case_insensitive) (>= (id b case_insensitive) (id c case_insensitive)))",
        /* (intersect, between)         */ "a intersect b between w and c"         to "(intersect (id a case_insensitive) (between (id b case_insensitive) (id w case_insensitive) (id c case_insensitive)))",
        /* (intersect, not_between)     */ "a intersect b not between y and c"     to "(intersect (id a case_insensitive) (not (between (id b case_insensitive) (id y case_insensitive) (id c case_insensitive))))",
        /* (intersect, like)            */ "a intersect b like c"                  to "(intersect (id a case_insensitive) (like (id b case_insensitive) (id c case_insensitive)))",
        /* (intersect, not_like)        */ "a intersect b not like c"              to "(intersect (id a case_insensitive) (not (like (id b case_insensitive) (id c case_insensitive))))",
        /* (intersect, +)               */ "a intersect b + c"                     to "(intersect (id a case_insensitive) (+ (id b case_insensitive) (id c case_insensitive)))",
        /* (intersect, -)               */ "a intersect b - c"                     to "(intersect (id a case_insensitive) (- (id b case_insensitive) (id c case_insensitive)))",
        /* (intersect, ||)              */ "a intersect b || c"                    to "(intersect (id a case_insensitive) (|| (id b case_insensitive) (id c case_insensitive)))",
        /* (intersect, *)               */ "a intersect b * c"                     to "(intersect (id a case_insensitive) (* (id b case_insensitive) (id c case_insensitive)))",
        /* (intersect, /)               */ "a intersect b / c"                     to "(intersect (id a case_insensitive) (/ (id b case_insensitive) (id c case_insensitive)))",
        /* (intersect, %)               */ "a intersect b % c"                     to "(intersect (id a case_insensitive) (% (id b case_insensitive) (id c case_insensitive)))",
        /* (intersect, is)              */ "a intersect b is boolean"              to "(intersect (id a case_insensitive) (is (id b case_insensitive) (type boolean)))",
        /* (intersect, is_not)          */ "a intersect b is not boolean"          to "(intersect (id a case_insensitive) (not (is (id b case_insensitive) (type boolean))))")

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun intersectAllPrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForIntersectAllPrecedence() = listOf(
        /* (intersect_all, intersect)   */ "a intersect all b intersect c"         to "(intersect (intersect_all (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (intersect_all, except)      */ "a intersect all b except c"            to "(except (intersect_all (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (intersect_all, except_all)  */ "a intersect all b except all c"        to "(except_all (intersect_all (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (intersect_all, union)       */ "a intersect all b union c"             to "(union (intersect_all (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (intersect_all, union_all)   */ "a intersect all b union all c"         to "(union_all (intersect_all (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (intersect_all, and)         */ "a intersect all b and c"               to "(intersect_all (id a case_insensitive) (and (id b case_insensitive) (id c case_insensitive)))",
        /* (intersect_all, or)          */ "a intersect all b or c"                to "(intersect_all (id a case_insensitive) (or (id b case_insensitive) (id c case_insensitive)))",
        /* (intersect_all, =)           */ "a intersect all b = c"                 to "(intersect_all (id a case_insensitive) (= (id b case_insensitive) (id c case_insensitive)))",
        /* (intersect_all, <>)          */ "a intersect all b <> c"                to "(intersect_all (id a case_insensitive) (<> (id b case_insensitive) (id c case_insensitive)))",
        /* (intersect_all, in)          */ "a intersect all b in c"                to "(intersect_all (id a case_insensitive) (in (id b case_insensitive) (id c case_insensitive)))",
        /* (intersect_all, not_in)      */ "a intersect all b not in c"            to "(intersect_all (id a case_insensitive) (not (in (id b case_insensitive) (id c case_insensitive))))",
        /* (intersect_all, <)           */ "a intersect all b < c"                 to "(intersect_all (id a case_insensitive) (< (id b case_insensitive) (id c case_insensitive)))",
        /* (intersect_all, <=)          */ "a intersect all b <= c"                to "(intersect_all (id a case_insensitive) (<= (id b case_insensitive) (id c case_insensitive)))",
        /* (intersect_all, >)           */ "a intersect all b > c"                 to "(intersect_all (id a case_insensitive) (> (id b case_insensitive) (id c case_insensitive)))",
        /* (intersect_all, >=)          */ "a intersect all b >= c"                to "(intersect_all (id a case_insensitive) (>= (id b case_insensitive) (id c case_insensitive)))",
        /* (intersect_all, between)     */ "a intersect all b between w and c"     to "(intersect_all (id a case_insensitive) (between (id b case_insensitive) (id w case_insensitive) (id c case_insensitive)))",
        /* (intersect_all, not_between) */ "a intersect all b not between y and c" to "(intersect_all (id a case_insensitive) (not (between (id b case_insensitive) (id y case_insensitive) (id c case_insensitive))))",
        /* (intersect_all, like)        */ "a intersect all b like c"              to "(intersect_all (id a case_insensitive) (like (id b case_insensitive) (id c case_insensitive)))",
        /* (intersect_all, not_like)    */ "a intersect all b not like c"          to "(intersect_all (id a case_insensitive) (not (like (id b case_insensitive) (id c case_insensitive))))",
        /* (intersect_all, +)           */ "a intersect all b + c"                 to "(intersect_all (id a case_insensitive) (+ (id b case_insensitive) (id c case_insensitive)))",
        /* (intersect_all, -)           */ "a intersect all b - c"                 to "(intersect_all (id a case_insensitive) (- (id b case_insensitive) (id c case_insensitive)))",
        /* (intersect_all, ||)          */ "a intersect all b || c"                to "(intersect_all (id a case_insensitive) (|| (id b case_insensitive) (id c case_insensitive)))",
        /* (intersect_all, *)           */ "a intersect all b * c"                 to "(intersect_all (id a case_insensitive) (* (id b case_insensitive) (id c case_insensitive)))",
        /* (intersect_all, /)           */ "a intersect all b / c"                 to "(intersect_all (id a case_insensitive) (/ (id b case_insensitive) (id c case_insensitive)))",
        /* (intersect_all, %)           */ "a intersect all b % c"                 to "(intersect_all (id a case_insensitive) (% (id b case_insensitive) (id c case_insensitive)))",
        /* (intersect_all, is)          */ "a intersect all b is boolean"          to "(intersect_all (id a case_insensitive) (is (id b case_insensitive) (type boolean)))",
        /* (intersect_all, is_not)      */ "a intersect all b is not boolean"      to "(intersect_all (id a case_insensitive) (not (is (id b case_insensitive) (type boolean))))"
    )

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun exceptPrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForExceptPrecedence() = listOf(
        /* (except, intersect)          */ "a except b intersect c"                to "(intersect (except (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (except, intersect_all)      */ "a except b intersect all c"            to "(intersect_all (except (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (except, except_all)         */ "a except b except all c"               to "(except_all (except (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (except, union)              */ "a except b union c"                    to "(union (except (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (except, union_all)          */ "a except b union all c"                to "(union_all (except (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (except, and)                */ "a except b and c"                      to "(except (id a case_insensitive) (and (id b case_insensitive) (id c case_insensitive)))",
        /* (except, or)                 */ "a except b or c"                       to "(except (id a case_insensitive) (or (id b case_insensitive) (id c case_insensitive)))",
        /* (except, =)                  */ "a except b = c"                        to "(except (id a case_insensitive) (= (id b case_insensitive) (id c case_insensitive)))",
        /* (except, <>)                 */ "a except b <> c"                       to "(except (id a case_insensitive) (<> (id b case_insensitive) (id c case_insensitive)))",
        /* (except, in)                 */ "a except b in c"                       to "(except (id a case_insensitive) (in (id b case_insensitive) (id c case_insensitive)))",
        /* (except, not_in)             */ "a except b not in c"                   to "(except (id a case_insensitive) (not (in (id b case_insensitive) (id c case_insensitive))))",
        /* (except, <)                  */ "a except b < c"                        to "(except (id a case_insensitive) (< (id b case_insensitive) (id c case_insensitive)))",
        /* (except, <=)                 */ "a except b <= c"                       to "(except (id a case_insensitive) (<= (id b case_insensitive) (id c case_insensitive)))",
        /* (except, >)                  */ "a except b > c"                        to "(except (id a case_insensitive) (> (id b case_insensitive) (id c case_insensitive)))",
        /* (except, >=)                 */ "a except b >= c"                       to "(except (id a case_insensitive) (>= (id b case_insensitive) (id c case_insensitive)))",
        /* (except, between)            */ "a except b between w and c"            to "(except (id a case_insensitive) (between (id b case_insensitive) (id w case_insensitive) (id c case_insensitive)))",
        /* (except, not_between)        */ "a except b not between y and c"        to "(except (id a case_insensitive) (not (between (id b case_insensitive) (id y case_insensitive) (id c case_insensitive))))",
        /* (except, like)               */ "a except b like c"                     to "(except (id a case_insensitive) (like (id b case_insensitive) (id c case_insensitive)))",
        /* (except, not_like)           */ "a except b not like c"                 to "(except (id a case_insensitive) (not (like (id b case_insensitive) (id c case_insensitive))))",
        /* (except, +)                  */ "a except b + c"                        to "(except (id a case_insensitive) (+ (id b case_insensitive) (id c case_insensitive)))",
        /* (except, -)                  */ "a except b - c"                        to "(except (id a case_insensitive) (- (id b case_insensitive) (id c case_insensitive)))",
        /* (except, ||)                 */ "a except b || c"                       to "(except (id a case_insensitive) (|| (id b case_insensitive) (id c case_insensitive)))",
        /* (except, *)                  */ "a except b * c"                        to "(except (id a case_insensitive) (* (id b case_insensitive) (id c case_insensitive)))",
        /* (except, /)                  */ "a except b / c"                        to "(except (id a case_insensitive) (/ (id b case_insensitive) (id c case_insensitive)))",
        /* (except, %)                  */ "a except b % c"                        to "(except (id a case_insensitive) (% (id b case_insensitive) (id c case_insensitive)))",
        /* (except, is)                 */ "a except b is boolean"                 to "(except (id a case_insensitive) (is (id b case_insensitive) (type boolean)))",
        /* (except, is_not)             */ "a except b is not boolean"             to "(except (id a case_insensitive) (not (is (id b case_insensitive) (type boolean))))")

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun exceptAllPrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForExceptAllPrecedence() = listOf(
        /* (except_all, intersect)      */ "a except all b intersect c"            to "(intersect (except_all (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (except_all, intersect_all)  */ "a except all b intersect all c"        to "(intersect_all (except_all (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (except_all, except)         */ "a except all b except c"               to "(except (except_all (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (except_all, union)          */ "a except all b union c"                to "(union (except_all (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (except_all, union_all)      */ "a except all b union all c"            to "(union_all (except_all (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (except_all, and)            */ "a except all b and c"                  to "(except_all (id a case_insensitive) (and (id b case_insensitive) (id c case_insensitive)))",
        /* (except_all, or)             */ "a except all b or c"                   to "(except_all (id a case_insensitive) (or (id b case_insensitive) (id c case_insensitive)))",
        /* (except_all, =)              */ "a except all b = c"                    to "(except_all (id a case_insensitive) (= (id b case_insensitive) (id c case_insensitive)))",
        /* (except_all, <>)             */ "a except all b <> c"                   to "(except_all (id a case_insensitive) (<> (id b case_insensitive) (id c case_insensitive)))",
        /* (except_all, in)             */ "a except all b in c"                   to "(except_all (id a case_insensitive) (in (id b case_insensitive) (id c case_insensitive)))",
        /* (except_all, not_in)         */ "a except all b not in c"               to "(except_all (id a case_insensitive) (not (in (id b case_insensitive) (id c case_insensitive))))",
        /* (except_all, <)              */ "a except all b < c"                    to "(except_all (id a case_insensitive) (< (id b case_insensitive) (id c case_insensitive)))",
        /* (except_all, <=)             */ "a except all b <= c"                   to "(except_all (id a case_insensitive) (<= (id b case_insensitive) (id c case_insensitive)))",
        /* (except_all, >)              */ "a except all b > c"                    to "(except_all (id a case_insensitive) (> (id b case_insensitive) (id c case_insensitive)))",
        /* (except_all, >=)             */ "a except all b >= c"                   to "(except_all (id a case_insensitive) (>= (id b case_insensitive) (id c case_insensitive)))",
        /* (except_all, between)        */ "a except all b between w and c"        to "(except_all (id a case_insensitive) (between (id b case_insensitive) (id w case_insensitive) (id c case_insensitive)))",
        /* (except_all, not_between)    */ "a except all b not between y and c"    to "(except_all (id a case_insensitive) (not (between (id b case_insensitive) (id y case_insensitive) (id c case_insensitive))))",
        /* (except_all, like)           */ "a except all b like c"                 to "(except_all (id a case_insensitive) (like (id b case_insensitive) (id c case_insensitive)))",
        /* (except_all, not_like)       */ "a except all b not like c"             to "(except_all (id a case_insensitive) (not (like (id b case_insensitive) (id c case_insensitive))))",
        /* (except_all, +)              */ "a except all b + c"                    to "(except_all (id a case_insensitive) (+ (id b case_insensitive) (id c case_insensitive)))",
        /* (except_all, -)              */ "a except all b - c"                    to "(except_all (id a case_insensitive) (- (id b case_insensitive) (id c case_insensitive)))",
        /* (except_all, ||)             */ "a except all b || c"                   to "(except_all (id a case_insensitive) (|| (id b case_insensitive) (id c case_insensitive)))",
        /* (except_all, *)              */ "a except all b * c"                    to "(except_all (id a case_insensitive) (* (id b case_insensitive) (id c case_insensitive)))",
        /* (except_all, /)              */ "a except all b / c"                    to "(except_all (id a case_insensitive) (/ (id b case_insensitive) (id c case_insensitive)))",
        /* (except_all, %)              */ "a except all b % c"                    to "(except_all (id a case_insensitive) (% (id b case_insensitive) (id c case_insensitive)))",
        /* (except_all, is)             */ "a except all b is boolean"             to "(except_all (id a case_insensitive) (is (id b case_insensitive) (type boolean)))",
        /* (except_all, is_not)         */ "a except all b is not boolean"         to "(except_all (id a case_insensitive) (not (is (id b case_insensitive) (type boolean))))")

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun unionPrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForUnionPrecedence() = listOf(
        /* (union, intersect)           */ "a union b intersect c"                 to "(intersect (union (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (union, intersect_all)       */ "a union b intersect all c"             to "(intersect_all (union (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (union, except)              */ "a union b except c"                    to "(except (union (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (union, except_all)          */ "a union b except all c"                to "(except_all (union (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (union, union_all)           */ "a union b union all c"                 to "(union_all (union (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (union, and)                 */ "a union b and c"                       to "(union (id a case_insensitive) (and (id b case_insensitive) (id c case_insensitive)))",
        /* (union, or)                  */ "a union b or c"                        to "(union (id a case_insensitive) (or (id b case_insensitive) (id c case_insensitive)))",
        /* (union, =)                   */ "a union b = c"                         to "(union (id a case_insensitive) (= (id b case_insensitive) (id c case_insensitive)))",
        /* (union, <>)                  */ "a union b <> c"                        to "(union (id a case_insensitive) (<> (id b case_insensitive) (id c case_insensitive)))",
        /* (union, in)                  */ "a union b in c"                        to "(union (id a case_insensitive) (in (id b case_insensitive) (id c case_insensitive)))",
        /* (union, not_in)              */ "a union b not in c"                    to "(union (id a case_insensitive) (not (in (id b case_insensitive) (id c case_insensitive))))",
        /* (union, <)                   */ "a union b < c"                         to "(union (id a case_insensitive) (< (id b case_insensitive) (id c case_insensitive)))",
        /* (union, <=)                  */ "a union b <= c"                        to "(union (id a case_insensitive) (<= (id b case_insensitive) (id c case_insensitive)))",
        /* (union, >)                   */ "a union b > c"                         to "(union (id a case_insensitive) (> (id b case_insensitive) (id c case_insensitive)))",
        /* (union, >=)                  */ "a union b >= c"                        to "(union (id a case_insensitive) (>= (id b case_insensitive) (id c case_insensitive)))",
        /* (union, between)             */ "a union b between w and c"             to "(union (id a case_insensitive) (between (id b case_insensitive) (id w case_insensitive) (id c case_insensitive)))",
        /* (union, not_between)         */ "a union b not between y and c"         to "(union (id a case_insensitive) (not (between (id b case_insensitive) (id y case_insensitive) (id c case_insensitive))))",
        /* (union, like)                */ "a union b like c"                      to "(union (id a case_insensitive) (like (id b case_insensitive) (id c case_insensitive)))",
        /* (union, not_like)            */ "a union b not like c"                  to "(union (id a case_insensitive) (not (like (id b case_insensitive) (id c case_insensitive))))",
        /* (union, +)                   */ "a union b + c"                         to "(union (id a case_insensitive) (+ (id b case_insensitive) (id c case_insensitive)))",
        /* (union, -)                   */ "a union b - c"                         to "(union (id a case_insensitive) (- (id b case_insensitive) (id c case_insensitive)))",
        /* (union, ||)                  */ "a union b || c"                        to "(union (id a case_insensitive) (|| (id b case_insensitive) (id c case_insensitive)))",
        /* (union, *)                   */ "a union b * c"                         to "(union (id a case_insensitive) (* (id b case_insensitive) (id c case_insensitive)))",
        /* (union, /)                   */ "a union b / c"                         to "(union (id a case_insensitive) (/ (id b case_insensitive) (id c case_insensitive)))",
        /* (union, %)                   */ "a union b % c"                         to "(union (id a case_insensitive) (% (id b case_insensitive) (id c case_insensitive)))",
        /* (union, is)                  */ "a union b is boolean"                  to "(union (id a case_insensitive) (is (id b case_insensitive) (type boolean)))",
        /* (union, is_not)              */ "a union b is not boolean"              to "(union (id a case_insensitive) (not (is (id b case_insensitive) (type boolean))))")

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun unionAllPrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForUnionAllPrecedence() = listOf(
        /* (union_all, intersect)       */ "a union all b intersect c"             to "(intersect (union_all (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (union_all, intersect_all)   */ "a union all b intersect all c"         to "(intersect_all (union_all (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (union_all, except)          */ "a union all b except c"                to "(except (union_all (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (union_all, except_all)      */ "a union all b except all c"            to "(except_all (union_all (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (union_all, union)           */ "a union all b union c"                 to "(union (union_all (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (union_all, and)             */ "a union all b and c"                   to "(union_all (id a case_insensitive) (and (id b case_insensitive) (id c case_insensitive)))",
        /* (union_all, or)              */ "a union all b or c"                    to "(union_all (id a case_insensitive) (or (id b case_insensitive) (id c case_insensitive)))",
        /* (union_all, =)               */ "a union all b = c"                     to "(union_all (id a case_insensitive) (= (id b case_insensitive) (id c case_insensitive)))",
        /* (union_all, <>)              */ "a union all b <> c"                    to "(union_all (id a case_insensitive) (<> (id b case_insensitive) (id c case_insensitive)))",
        /* (union_all, in)              */ "a union all b in c"                    to "(union_all (id a case_insensitive) (in (id b case_insensitive) (id c case_insensitive)))",
        /* (union_all, not_in)          */ "a union all b not in c"                to "(union_all (id a case_insensitive) (not (in (id b case_insensitive) (id c case_insensitive))))",
        /* (union_all, <)               */ "a union all b < c"                     to "(union_all (id a case_insensitive) (< (id b case_insensitive) (id c case_insensitive)))",
        /* (union_all, <=)              */ "a union all b <= c"                    to "(union_all (id a case_insensitive) (<= (id b case_insensitive) (id c case_insensitive)))",
        /* (union_all, >)               */ "a union all b > c"                     to "(union_all (id a case_insensitive) (> (id b case_insensitive) (id c case_insensitive)))",
        /* (union_all, >=)              */ "a union all b >= c"                    to "(union_all (id a case_insensitive) (>= (id b case_insensitive) (id c case_insensitive)))",
        /* (union_all, between)         */ "a union all b between w and c"         to "(union_all (id a case_insensitive) (between (id b case_insensitive) (id w case_insensitive) (id c case_insensitive)))",
        /* (union_all, not_between)     */ "a union all b not between y and c"     to "(union_all (id a case_insensitive) (not (between (id b case_insensitive) (id y case_insensitive) (id c case_insensitive))))",
        /* (union_all, like)            */ "a union all b like c"                  to "(union_all (id a case_insensitive) (like (id b case_insensitive) (id c case_insensitive)))",
        /* (union_all, not_like)        */ "a union all b not like c"              to "(union_all (id a case_insensitive) (not (like (id b case_insensitive) (id c case_insensitive))))",
        /* (union_all, +)               */ "a union all b + c"                     to "(union_all (id a case_insensitive) (+ (id b case_insensitive) (id c case_insensitive)))",
        /* (union_all, -)               */ "a union all b - c"                     to "(union_all (id a case_insensitive) (- (id b case_insensitive) (id c case_insensitive)))",
        /* (union_all, ||)              */ "a union all b || c"                    to "(union_all (id a case_insensitive) (|| (id b case_insensitive) (id c case_insensitive)))",
        /* (union_all, *)               */ "a union all b * c"                     to "(union_all (id a case_insensitive) (* (id b case_insensitive) (id c case_insensitive)))",
        /* (union_all, /)               */ "a union all b / c"                     to "(union_all (id a case_insensitive) (/ (id b case_insensitive) (id c case_insensitive)))",
        /* (union_all, %)               */ "a union all b % c"                     to "(union_all (id a case_insensitive) (% (id b case_insensitive) (id c case_insensitive)))",
        /* (union_all, is)              */ "a union all b is boolean"              to "(union_all (id a case_insensitive) (is (id b case_insensitive) (type boolean)))",
        /* (union_all, is_not)          */ "a union all b is not boolean"          to "(union_all (id a case_insensitive) (not (is (id b case_insensitive) (type boolean))))")

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun andPrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForAndPrecedence() = listOf(
        /* (and, intersect)             */ "a and b intersect c"                   to "(intersect (and (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (and, intersect_all)         */ "a and b intersect all c"               to "(intersect_all (and (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (and, except)                */ "a and b except c"                      to "(except (and (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (and, except_all)            */ "a and b except all c"                  to "(except_all (and (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (and, union)                 */ "a and b union c"                       to "(union (and (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (and, union_all)             */ "a and b union all c"                   to "(union_all (and (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (and, or)                    */ "a and b or c"                          to "(or (and (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (and, =)                     */ "a and b = c"                           to "(and (id a case_insensitive) (= (id b case_insensitive) (id c case_insensitive)))",
        /* (and, <>)                    */ "a and b <> c"                          to "(and (id a case_insensitive) (<> (id b case_insensitive) (id c case_insensitive)))",
        /* (and, in)                    */ "a and b in c"                          to "(and (id a case_insensitive) (in (id b case_insensitive) (id c case_insensitive)))",
        /* (and, not_in)                */ "a and b not in c"                      to "(and (id a case_insensitive) (not (in (id b case_insensitive) (id c case_insensitive))))",
        /* (and, <)                     */ "a and b < c"                           to "(and (id a case_insensitive) (< (id b case_insensitive) (id c case_insensitive)))",
        /* (and, <=)                    */ "a and b <= c"                          to "(and (id a case_insensitive) (<= (id b case_insensitive) (id c case_insensitive)))",
        /* (and, >)                     */ "a and b > c"                           to "(and (id a case_insensitive) (> (id b case_insensitive) (id c case_insensitive)))",
        /* (and, >=)                    */ "a and b >= c"                          to "(and (id a case_insensitive) (>= (id b case_insensitive) (id c case_insensitive)))",
        /* (and, between)               */ "a and b between w and c"               to "(and (id a case_insensitive) (between (id b case_insensitive) (id w case_insensitive) (id c case_insensitive)))",
        /* (and, not_between)           */ "a and b not between y and c"           to "(and (id a case_insensitive) (not (between (id b case_insensitive) (id y case_insensitive) (id c case_insensitive))))",
        /* (and, like)                  */ "a and b like c"                        to "(and (id a case_insensitive) (like (id b case_insensitive) (id c case_insensitive)))",
        /* (and, not_like)              */ "a and b not like c"                    to "(and (id a case_insensitive) (not (like (id b case_insensitive) (id c case_insensitive))))",
        /* (and, +)                     */ "a and b + c"                           to "(and (id a case_insensitive) (+ (id b case_insensitive) (id c case_insensitive)))",
        /* (and, -)                     */ "a and b - c"                           to "(and (id a case_insensitive) (- (id b case_insensitive) (id c case_insensitive)))",
        /* (and, ||)                    */ "a and b || c"                          to "(and (id a case_insensitive) (|| (id b case_insensitive) (id c case_insensitive)))",
        /* (and, *)                     */ "a and b * c"                           to "(and (id a case_insensitive) (* (id b case_insensitive) (id c case_insensitive)))",
        /* (and, /)                     */ "a and b / c"                           to "(and (id a case_insensitive) (/ (id b case_insensitive) (id c case_insensitive)))",
        /* (and, %)                     */ "a and b % c"                           to "(and (id a case_insensitive) (% (id b case_insensitive) (id c case_insensitive)))",
        /* (and, is)                    */ "a and b is boolean"                    to "(and (id a case_insensitive) (is (id b case_insensitive) (type boolean)))",
        /* (and, is_not)                */ "a and b is not boolean"                to "(and (id a case_insensitive) (not (is (id b case_insensitive) (type boolean))))")

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun orPrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForOrPrecedence() = listOf(
        /* (or, intersect)              */ "a or b intersect c"                    to "(intersect (or (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (or, intersect_all)          */ "a or b intersect all c "               to "(intersect_all (or (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (or, except)                 */ "a or b except c"                       to "(except (or (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (or, except_all)             */ "a or b except all c "                  to "(except_all (or (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (or, union)                  */ "a or b union c"                        to "(union (or (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (or, union_all)              */ "a or b union all c "                   to "(union_all (or (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (or, and)                    */ "a or b and c"                          to "(or (id a case_insensitive) (and (id b case_insensitive) (id c case_insensitive)))",
        /* (or, =)                      */ "a or b = c"                            to "(or (id a case_insensitive) (= (id b case_insensitive) (id c case_insensitive)))",
        /* (or, <>)                     */ "a or b <> c"                           to "(or (id a case_insensitive) (<> (id b case_insensitive) (id c case_insensitive)))",
        /* (or, in)                     */ "a or b in c"                           to "(or (id a case_insensitive) (in (id b case_insensitive) (id c case_insensitive)))",
        /* (or, not_in)                 */ "a or b not in c"                       to "(or (id a case_insensitive) (not (in (id b case_insensitive) (id c case_insensitive))))",
        /* (or, <)                      */ "a or b < c"                            to "(or (id a case_insensitive) (< (id b case_insensitive) (id c case_insensitive)))",
        /* (or, <=)                     */ "a or b <= c"                           to "(or (id a case_insensitive) (<= (id b case_insensitive) (id c case_insensitive)))",
        /* (or, >)                      */ "a or b > c"                            to "(or (id a case_insensitive) (> (id b case_insensitive) (id c case_insensitive)))",
        /* (or, >=)                     */ "a or b >= c"                           to "(or (id a case_insensitive) (>= (id b case_insensitive) (id c case_insensitive)))",
        /* (or, between)                */ "a or b between w and c"                to "(or (id a case_insensitive) (between (id b case_insensitive) (id w case_insensitive) (id c case_insensitive)))",
        /* (or, not_between)            */ "a or b not between y and c"            to "(or (id a case_insensitive) (not (between (id b case_insensitive) (id y case_insensitive) (id c case_insensitive))))",
        /* (or, like)                   */ "a or b like c"                         to "(or (id a case_insensitive) (like (id b case_insensitive) (id c case_insensitive)))",
        /* (or, not_like)               */ "a or b not like c"                     to "(or (id a case_insensitive) (not (like (id b case_insensitive) (id c case_insensitive))))",
        /* (or, +)                      */ "a or b + c"                            to "(or (id a case_insensitive) (+ (id b case_insensitive) (id c case_insensitive)))",
        /* (or, -)                      */ "a or b - c"                            to "(or (id a case_insensitive) (- (id b case_insensitive) (id c case_insensitive)))",
        /* (or, ||)                     */ "a or b || c"                           to "(or (id a case_insensitive) (|| (id b case_insensitive) (id c case_insensitive)))",
        /* (or, *)                      */ "a or b * c"                            to "(or (id a case_insensitive) (* (id b case_insensitive) (id c case_insensitive)))",
        /* (or, /)                      */ "a or b / c"                            to "(or (id a case_insensitive) (/ (id b case_insensitive) (id c case_insensitive)))",
        /* (or, %)                      */ "a or b % c"                            to "(or (id a case_insensitive) (% (id b case_insensitive) (id c case_insensitive)))",
        /* (or, is)                     */ "a or b is boolean"                     to "(or (id a case_insensitive) (is (id b case_insensitive) (type boolean)))",
        /* (or, is_not)                 */ "a or b is not boolean"                 to "(or (id a case_insensitive) (not (is (id b case_insensitive) (type boolean))))")

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun equalsPrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForEqualsPrecedence() = listOf(
        /* (=, intersect)               */ "a = b intersect c"                     to "(intersect (= (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (=, intersect_all)           */ "a = b intersect all c  "               to "(intersect_all (= (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (=, except)                  */ "a = b except c"                        to "(except (= (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (=, except_all)              */ "a = b except all c  "                  to "(except_all (= (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (=, union)                   */ "a = b union c"                         to "(union (= (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (=, union_all)               */ "a = b union all c  "                   to "(union_all (= (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (=, or)                      */ "a = b or c"                            to "(or (= (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (=, and)                     */ "a = b and c"                           to "(and (= (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (=, <>)                      */ "a = b <> c"                            to "(<> (= (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (=, in)                      */ "a = b in c"                            to "(in (= (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (=, not_in)                  */ "a = b not in c"                        to "(not (in (= (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive)))",
        /* (=, <)                       */ "a = b < c"                             to "(= (id a case_insensitive) (< (id b case_insensitive) (id c case_insensitive)))",
        /* (=, <=)                      */ "a = b <= c"                            to "(= (id a case_insensitive) (<= (id b case_insensitive) (id c case_insensitive)))",
        /* (=, >)                       */ "a = b > c"                             to "(= (id a case_insensitive) (> (id b case_insensitive) (id c case_insensitive)))",
        /* (=, >=)                      */ "a = b >= c"                            to "(= (id a case_insensitive) (>= (id b case_insensitive) (id c case_insensitive)))",
        /* (=, between)                 */ "a = b between w and c"                 to "(= (id a case_insensitive) (between (id b case_insensitive) (id w case_insensitive) (id c case_insensitive)))",
        /* (=, not_between)             */ "a = b not between y and c"             to "(= (id a case_insensitive) (not (between (id b case_insensitive) (id y case_insensitive) (id c case_insensitive))))",
        /* (=, like)                    */ "a = b like c"                          to "(= (id a case_insensitive) (like (id b case_insensitive) (id c case_insensitive)))",
        /* (=, not_like)                */ "a = b not like c"                      to "(= (id a case_insensitive) (not (like (id b case_insensitive) (id c case_insensitive))))",
        /* (=, +)                       */ "a = b + c"                             to "(= (id a case_insensitive) (+ (id b case_insensitive) (id c case_insensitive)))",
        /* (=, -)                       */ "a = b - c"                             to "(= (id a case_insensitive) (- (id b case_insensitive) (id c case_insensitive)))",
        /* (=, ||)                      */ "a = b || c"                            to "(= (id a case_insensitive) (|| (id b case_insensitive) (id c case_insensitive)))",
        /* (=, *)                       */ "a = b * c"                             to "(= (id a case_insensitive) (* (id b case_insensitive) (id c case_insensitive)))",
        /* (=, /)                       */ "a = b / c"                             to "(= (id a case_insensitive) (/ (id b case_insensitive) (id c case_insensitive)))",
        /* (=, %)                       */ "a = b % c"                             to "(= (id a case_insensitive) (% (id b case_insensitive) (id c case_insensitive)))",
        /* (=, is)                      */ "a = b is boolean"                      to "(is (= (id a case_insensitive) (id b case_insensitive)) (type boolean))",
        /* (=, is_not)                  */ "a = b is not boolean"                  to "(not (is (= (id a case_insensitive) (id b case_insensitive)) (type boolean)))")

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun notEqualPrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForNotEqualPrecedence() = listOf(
        /* (<>, intersect)              */ "a <> b intersect c"                    to "(intersect (<> (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (<>, intersect_all)          */ "a <> b intersect all c"                to "(intersect_all (<> (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (<>, except)                 */ "a <> b except c"                       to "(except (<> (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (<>, except_all)             */ "a <> b except all c"                   to "(except_all (<> (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (<>, union)                  */ "a <> b union c"                        to "(union (<> (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (<>, union_all)              */ "a <> b union all c"                    to "(union_all (<> (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (<>, or)                     */ "a <> b or c"                           to "(or (<> (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (<>, and)                    */ "a <> b and c"                          to "(and (<> (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (<>, =)                      */ "a <> b = c"                            to "(= (<> (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (<>, in)                     */ "a <> b in c"                           to "(in (<> (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (<>, not_in)                 */ "a <> b not in c"                       to "(not (in (<> (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive)))",
        /* (<>, <)                      */ "a <> b < c"                            to "(<> (id a case_insensitive) (< (id b case_insensitive) (id c case_insensitive)))",
        /* (<>, <=)                     */ "a <> b <= c"                           to "(<> (id a case_insensitive) (<= (id b case_insensitive) (id c case_insensitive)))",
        /* (<>, >)                      */ "a <> b > c"                            to "(<> (id a case_insensitive) (> (id b case_insensitive) (id c case_insensitive)))",
        /* (<>, >=)                     */ "a <> b >= c"                           to "(<> (id a case_insensitive) (>= (id b case_insensitive) (id c case_insensitive)))",
        /* (<>, between)                */ "a <> b between w and c"                to "(<> (id a case_insensitive) (between (id b case_insensitive) (id w case_insensitive) (id c case_insensitive)))",
        /* (<>, not_between)            */ "a <> b not between y and c"            to "(<> (id a case_insensitive) (not (between (id b case_insensitive) (id y case_insensitive) (id c case_insensitive))))",
        /* (<>, like)                   */ "a <> b like c"                         to "(<> (id a case_insensitive) (like (id b case_insensitive) (id c case_insensitive)))",
        /* (<>, not_like)               */ "a <> b not like c"                     to "(<> (id a case_insensitive) (not (like (id b case_insensitive) (id c case_insensitive))))",
        /* (<>, +)                      */ "a <> b + c"                            to "(<> (id a case_insensitive) (+ (id b case_insensitive) (id c case_insensitive)))",
        /* (<>, -)                      */ "a <> b - c"                            to "(<> (id a case_insensitive) (- (id b case_insensitive) (id c case_insensitive)))",
        /* (<>, ||)                     */ "a <> b || c"                           to "(<> (id a case_insensitive) (|| (id b case_insensitive) (id c case_insensitive)))",
        /* (<>, *)                      */ "a <> b * c"                            to "(<> (id a case_insensitive) (* (id b case_insensitive) (id c case_insensitive)))",
        /* (<>, /)                      */ "a <> b / c"                            to "(<> (id a case_insensitive) (/ (id b case_insensitive) (id c case_insensitive)))",
        /* (<>, %)                      */ "a <> b % c"                            to "(<> (id a case_insensitive) (% (id b case_insensitive) (id c case_insensitive)))",
        /* (<>, is)                     */ "a <> b is boolean"                     to "(is (<> (id a case_insensitive) (id b case_insensitive)) (type boolean))",
        /* (<>, is_not)                 */ "a <> b is not boolean"                 to "(not (is (<> (id a case_insensitive) (id b case_insensitive)) (type boolean)))")

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun isPrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForIsPrecedence() = listOf(
        /* (is, intersect)              */ "a is boolean intersect c"              to "(intersect (is (id a case_insensitive) (type boolean)) (id c case_insensitive))",
        /* (is, intersect_all)          */ "a is boolean intersect all c"          to "(intersect_all (is (id a case_insensitive) (type boolean)) (id c case_insensitive))",
        /* (is, except)                 */ "a is boolean except c"                 to "(except (is (id a case_insensitive) (type boolean)) (id c case_insensitive))",
        /* (is, except_all)             */ "a is boolean except all c"             to "(except_all (is (id a case_insensitive) (type boolean)) (id c case_insensitive))",
        /* (is, union)                  */ "a is boolean union c"                  to "(union (is (id a case_insensitive) (type boolean)) (id c case_insensitive))",
        /* (is, union_all)              */ "a is boolean union all c"              to "(union_all (is (id a case_insensitive) (type boolean)) (id c case_insensitive))",
        /* (is, or)                     */ "a is boolean or c"                     to "(or (is (id a case_insensitive) (type boolean)) (id c case_insensitive))",
        /* (is, and)                    */ "a is boolean and c"                    to "(and (is (id a case_insensitive) (type boolean)) (id c case_insensitive))",
        /* (is, =)                      */ "a is boolean = c"                      to "(= (is (id a case_insensitive) (type boolean)) (id c case_insensitive))",
        /* (is, in)                     */ "a is boolean in c"                     to "(in (is (id a case_insensitive) (type boolean)) (id c case_insensitive))",
        /* (is, not_in)                 */ "a is boolean not in c"                 to "(not (in (is (id a case_insensitive) (type boolean)) (id c case_insensitive)))",
        /* (is, <)                      */ "a is boolean < c"                      to "(< (is (id a case_insensitive) (type boolean)) (id c case_insensitive))",
        /* (is, <=)                     */ "a is boolean <= c"                     to "(<= (is (id a case_insensitive) (type boolean)) (id c case_insensitive))",
        /* (is, >)                      */ "a is boolean > c"                      to "(> (is (id a case_insensitive) (type boolean)) (id c case_insensitive))",
        /* (is, >=)                     */ "a is boolean >= c"                     to "(>= (is (id a case_insensitive) (type boolean)) (id c case_insensitive))",
        /* (is, between)                */ "a is boolean between w and c"          to "(between (is (id a case_insensitive) (type boolean)) (id w case_insensitive) (id c case_insensitive))",
        /* (is, not_between)            */ "a is boolean not between y and c"      to "(not (between (is (id a case_insensitive) (type boolean)) (id y case_insensitive) (id c case_insensitive)))",
        /* (is, like)                   */ "a is boolean like c"                   to "(like (is (id a case_insensitive) (type boolean)) (id c case_insensitive))",
        /* (is, not_like)               */ "a is boolean not like c"               to "(not (like (is (id a case_insensitive) (type boolean)) (id c case_insensitive)))",
        /* (is, +)                      */ "a is boolean + c"                      to "(+ (is (id a case_insensitive) (type boolean)) (id c case_insensitive))",
        /* (is, -)                      */ "a is boolean - c"                      to "(- (is (id a case_insensitive) (type boolean)) (id c case_insensitive))",
        /* (is, ||)                     */ "a is boolean || c"                     to "(|| (is (id a case_insensitive) (type boolean)) (id c case_insensitive))",
        /* (is, *)                      */ "a is boolean * c"                      to "(* (is (id a case_insensitive) (type boolean)) (id c case_insensitive))",
        /* (is, /)                      */ "a is boolean / c"                      to "(/ (is (id a case_insensitive) (type boolean)) (id c case_insensitive))",
        /* (is, %)                      */ "a is boolean % c"                      to "(% (is (id a case_insensitive) (type boolean)) (id c case_insensitive))",
        /* (is, is_not)                 */ "a is boolean is not boolean"           to "(not (is (is (id a case_insensitive) (type boolean)) (type boolean)))")

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun isNotPrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForIsNotPrecedence() = listOf(
        /* (not (is, intersect)          */ "a is not boolean intersect c"          to "(intersect (not (is (id a case_insensitive) (type boolean))) (id c case_insensitive))",
        /* (not (is, intersect_all)      */ "a is not boolean intersect all c"      to "(intersect_all (not (is (id a case_insensitive) (type boolean))) (id c case_insensitive))",
        /* (not (is, except)             */ "a is not boolean except c"             to "(except (not (is (id a case_insensitive) (type boolean))) (id c case_insensitive))",
        /* (not (is, union)              */ "a is not boolean union c"              to "(union (not (is (id a case_insensitive) (type boolean))) (id c case_insensitive))",
        /* (not (is, union_all)          */ "a is not boolean union all c"          to "(union_all (not (is (id a case_insensitive) (type boolean))) (id c case_insensitive))",
        /* (not (is, or)                 */ "a is not boolean or c"                 to "(or (not (is (id a case_insensitive) (type boolean))) (id c case_insensitive))",
        /* (not (is, and)                */ "a is not boolean and c"                to "(and (not (is (id a case_insensitive) (type boolean))) (id c case_insensitive))",
        /* (not (is, =)                  */ "a is not boolean = c"                  to "(= (not (is (id a case_insensitive) (type boolean))) (id c case_insensitive))",
        /* (not (is, <>)                 */ "a is not boolean <> c"                 to "(<> (not (is (id a case_insensitive) (type boolean))) (id c case_insensitive))",
        /* (not (is, in)                 */ "a is not boolean in c"                 to "(in (not (is (id a case_insensitive) (type boolean))) (id c case_insensitive))",
        /* (not (is, not_in)             */ "a is not boolean not in c"             to "(not (in (not (is (id a case_insensitive) (type boolean))) (id c case_insensitive)))",
        /* (not (is, <)                  */ "a is not boolean < c"                  to "(< (not (is (id a case_insensitive) (type boolean))) (id c case_insensitive))",
        /* (not (is, <=)                 */ "a is not boolean <= c"                 to "(<= (not (is (id a case_insensitive) (type boolean))) (id c case_insensitive))",
        /* (not (is, >)                  */ "a is not boolean > c"                  to "(> (not (is (id a case_insensitive) (type boolean))) (id c case_insensitive))",
        /* (not (is, >=)                 */ "a is not boolean >= c"                 to "(>= (not (is (id a case_insensitive) (type boolean))) (id c case_insensitive))",
        /* (not (is, between)            */ "a is not boolean between w and c"      to "(between (not (is (id a case_insensitive) (type boolean))) (id w case_insensitive) (id c case_insensitive))",
        /* (not (is, not_between)        */ "a is not boolean not between y and c"  to "(not (between (not (is (id a case_insensitive) (type boolean))) (id y case_insensitive) (id c case_insensitive)))",
        /* (not (is, like)               */ "a is not boolean like c"               to "(like (not (is (id a case_insensitive) (type boolean))) (id c case_insensitive))",
        /* (not (is, not_like)           */ "a is not boolean not like c"           to "(not (like (not (is (id a case_insensitive) (type boolean))) (id c case_insensitive)))",
        /* (not (is, +)                  */ "a is not boolean + c"                  to "(+ (not (is (id a case_insensitive) (type boolean))) (id c case_insensitive))",
        /* (not (is, -)                  */ "a is not boolean - c"                  to "(- (not (is (id a case_insensitive) (type boolean))) (id c case_insensitive))",
        /* (not (is, ||)                 */ "a is not boolean || c"                 to "(|| (not (is (id a case_insensitive) (type boolean))) (id c case_insensitive))",
        /* (not (is, *)                  */ "a is not boolean * c"                  to "(* (not (is (id a case_insensitive) (type boolean))) (id c case_insensitive))",
        /* (not (is, /)                  */ "a is not boolean / c"                  to "(/ (not (is (id a case_insensitive) (type boolean))) (id c case_insensitive))",
        /* (not (is, %)                  */ "a is not boolean % c"                  to "(% (not (is (id a case_insensitive) (type boolean))) (id c case_insensitive))",
        /* (not (is, is)                 */ "a is not boolean is boolean"           to "(is (not (is (id a case_insensitive) (type boolean))) (type boolean))")

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun inPrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForInPrecedence() = listOf(
        /* (in, intersect)              */ "a in b intersect c"                    to "(intersect (in (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (in, intersect_all)          */ "a in b intersect all c"                to "(intersect_all (in (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (in, except)                 */ "a in b except c"                       to "(except (in (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (in, except_all)             */ "a in b except all c"                   to "(except_all (in (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (in, union)                  */ "a in b union c"                        to "(union (in (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (in, union_all)              */ "a in b union all c"                    to "(union_all (in (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (in, or)                     */ "a in b or c"                           to "(or (in (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (in, and)                    */ "a in b and c"                          to "(and (in (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (in, =)                      */ "a in b = c"                            to "(= (in (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (in, <>)                     */ "a in b <> c"                           to "(<> (in (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (in, not_in)                 */ "a in b not in c"                       to "(not (in (in (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive)))",
        /* (in, <)                      */ "a in b < c"                            to "(in (id a case_insensitive) (< (id b case_insensitive) (id c case_insensitive)))",
        /* (in, <=)                     */ "a in b <= c"                           to "(in (id a case_insensitive) (<= (id b case_insensitive) (id c case_insensitive)))",
        /* (in, >)                      */ "a in b > c"                            to "(in (id a case_insensitive) (> (id b case_insensitive) (id c case_insensitive)))",
        /* (in, >=)                     */ "a in b >= c"                           to "(in (id a case_insensitive) (>= (id b case_insensitive) (id c case_insensitive)))",
        /* (in, between)                */ "a in b between w and c"                to "(in (id a case_insensitive) (between (id b case_insensitive) (id w case_insensitive) (id c case_insensitive)))",
        /* (in, not_between)            */ "a in b not between y and c"            to "(in (id a case_insensitive) (not (between (id b case_insensitive) (id y case_insensitive) (id c case_insensitive))))",
        /* (in, like)                   */ "a in b like c"                         to "(in (id a case_insensitive) (like (id b case_insensitive) (id c case_insensitive)))",
        /* (in, not_like)               */ "a in b not like c"                     to "(in (id a case_insensitive) (not (like (id b case_insensitive) (id c case_insensitive))))",
        /* (in, +)                      */ "a in b + c"                            to "(in (id a case_insensitive) (+ (id b case_insensitive) (id c case_insensitive)))",
        /* (in, -)                      */ "a in b - c"                            to "(in (id a case_insensitive) (- (id b case_insensitive) (id c case_insensitive)))",
        /* (in, ||)                     */ "a in b || c"                           to "(in (id a case_insensitive) (|| (id b case_insensitive) (id c case_insensitive)))",
        /* (in, *)                      */ "a in b * c"                            to "(in (id a case_insensitive) (* (id b case_insensitive) (id c case_insensitive)))",
        /* (in, /)                      */ "a in b / c"                            to "(in (id a case_insensitive) (/ (id b case_insensitive) (id c case_insensitive)))",
        /* (in, %)                      */ "a in b % c"                            to "(in (id a case_insensitive) (% (id b case_insensitive) (id c case_insensitive)))",
        /* (in, is)                     */ "a in b is boolean"                     to "(is (in (id a case_insensitive) (id b case_insensitive)) (type boolean))",
        /* (in, is_not)                 */ "a in b is not boolean"                 to "(not (is (in (id a case_insensitive) (id b case_insensitive)) (type boolean)))")

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun notInPrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForNotInPrecedence() = listOf(
        /* (not (in, intersect)          */ "a not in b intersect c"                to "(intersect (not (in (id a case_insensitive) (id b case_insensitive))) (id c case_insensitive))",
        /* (not (in, intersect_all)      */ "a not in b intersect all c"            to "(intersect_all (not (in (id a case_insensitive) (id b case_insensitive))) (id c case_insensitive))",
        /* (not (in, except)             */ "a not in b except c"                   to "(except (not (in (id a case_insensitive) (id b case_insensitive))) (id c case_insensitive))",
        /* (not (in, except_all)         */ "a not in b except all c"               to "(except_all (not (in (id a case_insensitive) (id b case_insensitive))) (id c case_insensitive))",
        /* (not (in, union)              */ "a not in b union c"                    to "(union (not (in (id a case_insensitive) (id b case_insensitive))) (id c case_insensitive))",
        /* (not (in, union_all)          */ "a not in b union all c"                to "(union_all (not (in (id a case_insensitive) (id b case_insensitive))) (id c case_insensitive))",
        /* (not (in, or)                 */ "a not in b or c"                       to "(or (not (in (id a case_insensitive) (id b case_insensitive))) (id c case_insensitive))",
        /* (not (in, and)                */ "a not in b and c"                      to "(and (not (in (id a case_insensitive) (id b case_insensitive))) (id c case_insensitive))",
        /* (not (in, =)                  */ "a not in b = c"                        to "(= (not (in (id a case_insensitive) (id b case_insensitive))) (id c case_insensitive))",
        /* (not (in, <>)                 */ "a not in b <> c"                       to "(<> (not (in (id a case_insensitive) (id b case_insensitive))) (id c case_insensitive))",
        /* (not (in, in)                 */ "a not in b in c"                       to "(in (not (in (id a case_insensitive) (id b case_insensitive))) (id c case_insensitive))",
        /* (not (in, <)                  */ "a not in b < c"                        to "(not (in (id a case_insensitive) (< (id b case_insensitive) (id c case_insensitive))))",
        /* (not (in, <=)                 */ "a not in b <= c"                       to "(not (in (id a case_insensitive) (<= (id b case_insensitive) (id c case_insensitive))))",
        /* (not (in, >)                  */ "a not in b > c"                        to "(not (in (id a case_insensitive) (> (id b case_insensitive) (id c case_insensitive))))",
        /* (not (in, >=)                 */ "a not in b >= c"                       to "(not (in (id a case_insensitive) (>= (id b case_insensitive) (id c case_insensitive))))",
        /* (not (in, between)            */ "a not in b between w and c"            to "(not (in (id a case_insensitive) (between (id b case_insensitive) (id w case_insensitive) (id c case_insensitive))))",
        /* (not (in, not_between)        */ "a not in b not between y and c"        to "(not (in (id a case_insensitive) (not (between (id b case_insensitive) (id y case_insensitive) (id c case_insensitive)))))",
        /* (not (in, like)               */ "a not in b like c"                     to "(not (in (id a case_insensitive) (like (id b case_insensitive) (id c case_insensitive))))",
        /* (not (in, not_like)           */ "a not in b not like c"                 to "(not (in (id a case_insensitive) (not (like (id b case_insensitive) (id c case_insensitive)))))",
        /* (not (in, +)                  */ "a not in b + c"                        to "(not (in (id a case_insensitive) (+ (id b case_insensitive) (id c case_insensitive))))",
        /* (not (in, -)                  */ "a not in b - c"                        to "(not (in (id a case_insensitive) (- (id b case_insensitive) (id c case_insensitive))))",
        /* (not (in, ||)                 */ "a not in b || c"                       to "(not (in (id a case_insensitive) (|| (id b case_insensitive) (id c case_insensitive))))",
        /* (not (in, *)                  */ "a not in b * c"                        to "(not (in (id a case_insensitive) (* (id b case_insensitive) (id c case_insensitive))))",
        /* (not (in, /)                  */ "a not in b / c"                        to "(not (in (id a case_insensitive) (/ (id b case_insensitive) (id c case_insensitive))))",
        /* (not (in, %)                  */ "a not in b % c"                        to "(not (in (id a case_insensitive) (% (id b case_insensitive) (id c case_insensitive))))",
        /* (not (in, is)                 */ "a not in b is boolean"                 to "(is (not (in (id a case_insensitive) (id b case_insensitive))) (type boolean))",
        /* (not (in, is_not)             */ "a not in b is not boolean"             to "(not (is (not (in (id a case_insensitive) (id b case_insensitive))) (type boolean)))")

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun ltPrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForLtPrecedence() = listOf(
        /* (<, intersect)               */ "a < b intersect c"                     to "(intersect (< (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (<, intersect_all)           */ "a < b intersect all c"                 to "(intersect_all (< (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (<, except)                  */ "a < b except c"                        to "(except (< (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (<, except_all)              */ "a < b except all c"                    to "(except_all (< (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (<, union)                   */ "a < b union c"                         to "(union (< (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (<, union_all)               */ "a < b union all c"                     to "(union_all (< (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (<, or)                      */ "a < b or c"                            to "(or (< (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (<, and)                     */ "a < b and c"                           to "(and (< (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (<, =)                       */ "a < b = c"                             to "(= (< (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (<, <>)                      */ "a < b <> c"                            to "(<> (< (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (<, in)                      */ "a < b in c"                            to "(in (< (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (<, not_in)                  */ "a < b not in c"                        to "(not (in (< (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive)))",
        /* (<, <=)                      */ "a < b <= c"                            to "(<= (< (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (<, >)                       */ "a < b > c"                             to "(> (< (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (<, >=)                      */ "a < b >= c"                            to "(>= (< (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (<, between)                 */ "a < b between w and c"                 to "(between (< (id a case_insensitive) (id b case_insensitive)) (id w case_insensitive) (id c case_insensitive))",
        /* (<, not_between)             */ "a < b not between y and c"             to "(not (between (< (id a case_insensitive) (id b case_insensitive)) (id y case_insensitive) (id c case_insensitive)))",
        /* (<, like)                    */ "a < b like c"                          to "(like (< (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (<, not_like)                */ "a < b not like c"                      to "(not (like (< (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive)))",
        /* (<, +)                       */ "a < b + c"                             to "(< (id a case_insensitive) (+ (id b case_insensitive) (id c case_insensitive)))",
        /* (<, -)                       */ "a < b - c"                             to "(< (id a case_insensitive) (- (id b case_insensitive) (id c case_insensitive)))",
        /* (<, ||)                      */ "a < b || c"                            to "(< (id a case_insensitive) (|| (id b case_insensitive) (id c case_insensitive)))",
        /* (<, *)                       */ "a < b * c"                             to "(< (id a case_insensitive) (* (id b case_insensitive) (id c case_insensitive)))",
        /* (<, /)                       */ "a < b / c"                             to "(< (id a case_insensitive) (/ (id b case_insensitive) (id c case_insensitive)))",
        /* (<, %)                       */ "a < b % c"                             to "(< (id a case_insensitive) (% (id b case_insensitive) (id c case_insensitive)))",
        /* (<, is)                      */ "a < b is boolean"                      to "(is (< (id a case_insensitive) (id b case_insensitive)) (type boolean))",
        /* (<, is_not)                  */ "a < b is not boolean"                  to "(not (is (< (id a case_insensitive) (id b case_insensitive)) (type boolean)))")

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun ltePrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForLtePrecedence() = listOf(
        /* (<=, intersect)              */ "a <= b intersect c"                    to "(intersect (<= (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (<=, intersect_all)          */ "a <= b intersect all c"                to "(intersect_all (<= (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (<=, except)                 */ "a <= b except c"                       to "(except (<= (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (<=, except_all)             */ "a <= b except all c"                   to "(except_all (<= (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (<=, union)                  */ "a <= b union c"                        to "(union (<= (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (<=, union_all)              */ "a <= b union all c"                    to "(union_all (<= (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (<=, or)                     */ "a <= b or c"                           to "(or (<= (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (<=, and)                    */ "a <= b and c"                          to "(and (<= (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (<=, =)                      */ "a <= b = c"                            to "(= (<= (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (<=, <>)                     */ "a <= b <> c"                           to "(<> (<= (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (<=, in)                     */ "a <= b in c"                           to "(in (<= (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (<=, not_in)                 */ "a <= b not in c"                       to "(not (in (<= (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive)))",
        /* (<=, <)                      */ "a <= b < c"                            to "(< (<= (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (<=, >)                      */ "a <= b > c"                            to "(> (<= (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (<=, >=)                     */ "a <= b >= c"                           to "(>= (<= (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (<=, between)                */ "a <= b between w and c"                to "(between (<= (id a case_insensitive) (id b case_insensitive)) (id w case_insensitive) (id c case_insensitive))",
        /* (<=, not_between)            */ "a <= b not between y and c"            to "(not (between (<= (id a case_insensitive) (id b case_insensitive)) (id y case_insensitive) (id c case_insensitive)))",
        /* (<=, like)                   */ "a <= b like c"                         to "(like (<= (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (<=, not_like)               */ "a <= b not like c"                     to "(not (like (<= (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive)))",
        /* (<=, +)                      */ "a <= b + c"                            to "(<= (id a case_insensitive) (+ (id b case_insensitive) (id c case_insensitive)))",
        /* (<=, -)                      */ "a <= b - c"                            to "(<= (id a case_insensitive) (- (id b case_insensitive) (id c case_insensitive)))",
        /* (<=, ||)                     */ "a <= b || c"                           to "(<= (id a case_insensitive) (|| (id b case_insensitive) (id c case_insensitive)))",
        /* (<=, *)                      */ "a <= b * c"                            to "(<= (id a case_insensitive) (* (id b case_insensitive) (id c case_insensitive)))",
        /* (<=, /)                      */ "a <= b / c"                            to "(<= (id a case_insensitive) (/ (id b case_insensitive) (id c case_insensitive)))",
        /* (<=, %)                      */ "a <= b % c"                            to "(<= (id a case_insensitive) (% (id b case_insensitive) (id c case_insensitive)))",
        /* (<=, is)                     */ "a <= b is boolean"                     to "(is (<= (id a case_insensitive) (id b case_insensitive)) (type boolean))",
        /* (<=, is_not)                 */ "a <= b is not boolean"                 to "(not (is (<= (id a case_insensitive) (id b case_insensitive)) (type boolean)))")

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun gtPrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForGtPrecedence() = listOf(
        /* (>, intersect)               */ "a > b intersect c"                     to "(intersect (> (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (>, intersect_all)           */ "a > b intersect all c"                 to "(intersect_all (> (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (>, except)                  */ "a > b except c"                        to "(except (> (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (>, except_all)              */ "a > b except all c"                    to "(except_all (> (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (>, union)                   */ "a > b union c"                         to "(union (> (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (>, union_all)               */ "a > b union all c"                     to "(union_all (> (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (>, or)                      */ "a > b or c"                            to "(or (> (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (>, and)                     */ "a > b and c"                           to "(and (> (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (>, =)                       */ "a > b = c"                             to "(= (> (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (>, <>)                      */ "a > b <> c"                            to "(<> (> (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (>, in)                      */ "a > b in c"                            to "(in (> (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (>, not_in)                  */ "a > b not in c"                        to "(not (in (> (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive)))",
        /* (>, <)                       */ "a > b < c"                             to "(< (> (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (>, <=)                      */ "a > b <= c"                            to "(<= (> (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (>, >=)                      */ "a > b >= c"                            to "(>= (> (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (>, between)                 */ "a > b between w and c"                 to "(between (> (id a case_insensitive) (id b case_insensitive)) (id w case_insensitive) (id c case_insensitive))",
        /* (>, not_between)             */ "a > b not between y and c"             to "(not (between (> (id a case_insensitive) (id b case_insensitive)) (id y case_insensitive) (id c case_insensitive)))",
        /* (>, like)                    */ "a > b like c"                          to "(like (> (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (>, not_like)                */ "a > b not like c"                      to "(not (like (> (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive)))",
        /* (>, +)                       */ "a > b + c"                             to "(> (id a case_insensitive) (+ (id b case_insensitive) (id c case_insensitive)))",
        /* (>, -)                       */ "a > b - c"                             to "(> (id a case_insensitive) (- (id b case_insensitive) (id c case_insensitive)))",
        /* (>, ||)                      */ "a > b || c"                            to "(> (id a case_insensitive) (|| (id b case_insensitive) (id c case_insensitive)))",
        /* (>, *)                       */ "a > b * c"                             to "(> (id a case_insensitive) (* (id b case_insensitive) (id c case_insensitive)))",
        /* (>, /)                       */ "a > b / c"                             to "(> (id a case_insensitive) (/ (id b case_insensitive) (id c case_insensitive)))",
        /* (>, %)                       */ "a > b % c"                             to "(> (id a case_insensitive) (% (id b case_insensitive) (id c case_insensitive)))",
        /* (>, is)                      */ "a > b is boolean"                      to "(is (> (id a case_insensitive) (id b case_insensitive)) (type boolean))",
        /* (>, is_not)                  */ "a > b is not boolean"                  to "(not (is (> (id a case_insensitive) (id b case_insensitive)) (type boolean)))")

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun gtePrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForGtePrecedence() = listOf(
        /* (>=, intersect)              */ "a >= b intersect c"                    to "(intersect (>= (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (>=, intersect_all)          */ "a >= b intersect all c"                to "(intersect_all (>= (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (>=, except)                 */ "a >= b except c"                       to "(except (>= (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (>=, except_all)             */ "a >= b except all c"                   to "(except_all (>= (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (>=, union)                  */ "a >= b union c"                        to "(union (>= (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (>=, union_all)              */ "a >= b union all c"                    to "(union_all (>= (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (>=, or)                     */ "a >= b or c"                           to "(or (>= (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (>=, and)                    */ "a >= b and c"                          to "(and (>= (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (>=, =)                      */ "a >= b = c"                            to "(= (>= (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (>=, <>)                     */ "a >= b <> c"                           to "(<> (>= (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (>=, in)                     */ "a >= b in c"                           to "(in (>= (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (>=, not_in)                 */ "a >= b not in c"                       to "(not (in (>= (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive)))",
        /* (>=, <)                      */ "a >= b < c"                            to "(< (>= (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (>=, <=)                     */ "a >= b <= c"                           to "(<= (>= (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (>=, >)                      */ "a >= b > c"                            to "(> (>= (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (>=, between)                */ "a >= b between w and c"                to "(between (>= (id a case_insensitive) (id b case_insensitive)) (id w case_insensitive) (id c case_insensitive))",
        /* (>=, not_between)            */ "a >= b not between y and c"            to "(not (between (>= (id a case_insensitive) (id b case_insensitive)) (id y case_insensitive) (id c case_insensitive)))",
        /* (>=, like)                   */ "a >= b like c"                         to "(like (>= (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (>=, not_like)               */ "a >= b not like c"                     to "(not (like (>= (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive)))",
        /* (>=, +)                      */ "a >= b + c"                            to "(>= (id a case_insensitive) (+ (id b case_insensitive) (id c case_insensitive)))",
        /* (>=, -)                      */ "a >= b - c"                            to "(>= (id a case_insensitive) (- (id b case_insensitive) (id c case_insensitive)))",
        /* (>=, ||)                     */ "a >= b || c"                           to "(>= (id a case_insensitive) (|| (id b case_insensitive) (id c case_insensitive)))",
        /* (>=, *)                      */ "a >= b * c"                            to "(>= (id a case_insensitive) (* (id b case_insensitive) (id c case_insensitive)))",
        /* (>=, /)                      */ "a >= b / c"                            to "(>= (id a case_insensitive) (/ (id b case_insensitive) (id c case_insensitive)))",
        /* (>=, %)                      */ "a >= b % c"                            to "(>= (id a case_insensitive) (% (id b case_insensitive) (id c case_insensitive)))",
        /* (>=, is)                     */ "a >= b is boolean"                     to "(is (>= (id a case_insensitive) (id b case_insensitive)) (type boolean))",
        /* (>=, is_not)                 */ "a >= b is not boolean"                 to "(not (is (>= (id a case_insensitive) (id b case_insensitive)) (type boolean)))")

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun betweenPrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForBetweenPrecedence() = listOf(
        /* (between, intersect)         */ "a between b and w intersect c"         to "(intersect (between (id a case_insensitive) (id b case_insensitive) (id w case_insensitive)) (id c case_insensitive))",
        /* (between, intersect_all)     */ "a between b and w intersect all c"     to "(intersect_all (between (id a case_insensitive) (id b case_insensitive) (id w case_insensitive)) (id c case_insensitive))",
        /* (between, except)            */ "a between b and w except c"            to "(except (between (id a case_insensitive) (id b case_insensitive) (id w case_insensitive)) (id c case_insensitive))",
        /* (between, except_all)        */ "a between b and w except all c"        to "(except_all (between (id a case_insensitive) (id b case_insensitive) (id w case_insensitive)) (id c case_insensitive))",
        /* (between, union)             */ "a between b and w union c"             to "(union (between (id a case_insensitive) (id b case_insensitive) (id w case_insensitive)) (id c case_insensitive))",
        /* (between, union_all)         */ "a between b and w union all c"         to "(union_all (between (id a case_insensitive) (id b case_insensitive) (id w case_insensitive)) (id c case_insensitive))",
        /* (between, or)                */ "a between w and b or c"                to "(or (between (id a case_insensitive) (id w case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (between, and)               */ "a between w and b and c"               to "(and (between (id a case_insensitive) (id w case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (between, =)                 */ "a between w and b = c"                 to "(= (between (id a case_insensitive) (id w case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (between, <>)                */ "a between w and b <> c"                to "(<> (between (id a case_insensitive) (id w case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (between, in)                */ "a between w and b in c"                to "(in (between (id a case_insensitive) (id w case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (between, not_in)            */ "a between w and b not in c"            to "(not (in (between (id a case_insensitive) (id w case_insensitive) (id b case_insensitive)) (id c case_insensitive)))",
        /* (between, <)                 */ "a between w and b < c"                 to "(< (between (id a case_insensitive) (id w case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (between, <=)                */ "a between w and b <= c"                to "(<= (between (id a case_insensitive) (id w case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (between, >)                 */ "a between w and b > c"                 to "(> (between (id a case_insensitive) (id w case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (between, >=)                */ "a between w and b >= c"                to "(>= (between (id a case_insensitive) (id w case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (between, not_between)       */ "a between w and b not between y and c" to "(not (between (between (id a case_insensitive) (id w case_insensitive) (id b case_insensitive)) (id y case_insensitive) (id c case_insensitive)))",
        /* (between, like)              */ "a between w and b like c"              to "(like (between (id a case_insensitive) (id w case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (between, not_like)          */ "a between w and b not like c"          to "(not (like (between (id a case_insensitive) (id w case_insensitive) (id b case_insensitive)) (id c case_insensitive)))",
        /* (between, +)                 */ "a between w and b + c"                 to "(between (id a case_insensitive) (id w case_insensitive)  (+ (id b case_insensitive) (id c case_insensitive)))",
        /* (between, -)                 */ "a between w and b - c"                 to "(between (id a case_insensitive) (id w case_insensitive)  (- (id b case_insensitive) (id c case_insensitive)))",
        /* (between, ||)                */ "a between w and b || c"                to "(between (id a case_insensitive) (id w case_insensitive)  (|| (id b case_insensitive) (id c case_insensitive)))",
        /* (between, *)                 */ "a between w and b * c"                 to "(between (id a case_insensitive) (id w case_insensitive)  (* (id b case_insensitive) (id c case_insensitive)))",
        /* (between, /)                 */ "a between w and b / c"                 to "(between (id a case_insensitive) (id w case_insensitive)  (/ (id b case_insensitive) (id c case_insensitive)))",
        /* (between, %)                 */ "a between w and b % c"                 to "(between (id a case_insensitive) (id w case_insensitive)  (% (id b case_insensitive) (id c case_insensitive)))",
        /* (between, is)                */ "a between w and b is boolean"          to "(is (between (id a case_insensitive) (id w case_insensitive) (id b case_insensitive)) (type boolean))",
        /* (between, is_not)            */ "a between w and b is not boolean"      to "(not (is (between (id a case_insensitive) (id w case_insensitive) (id b case_insensitive)) (type boolean)))")

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun notBetweenPrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForNotBetweenPrecedence() = listOf(
        /* (not (between, intersect)     */ "a not between w and b intersect c"     to "(intersect (not (between (id a case_insensitive) (id w case_insensitive) (id b case_insensitive))) (id c case_insensitive))",
        /* (not (between, intersect_all) */ "a not between w and b intersect all c" to "(intersect_all (not (between (id a case_insensitive) (id w case_insensitive) (id b case_insensitive))) (id c case_insensitive))",
        /* (not (between, except)        */ "a not between w and b except c"        to "(except (not (between (id a case_insensitive) (id w case_insensitive) (id b case_insensitive))) (id c case_insensitive))",
        /* (not (between, except_all)    */ "a not between w and b except all c"    to "(except_all (not (between (id a case_insensitive) (id w case_insensitive) (id b case_insensitive))) (id c case_insensitive))",
        /* (not (between, union)         */ "a not between w and b union c"         to "(union (not (between (id a case_insensitive) (id w case_insensitive) (id b case_insensitive))) (id c case_insensitive))",
        /* (not (between, union_all)     */ "a not between w and b union all c"     to "(union_all (not (between (id a case_insensitive) (id w case_insensitive) (id b case_insensitive))) (id c case_insensitive))",
        /* (not (between, or)            */ "a not between y and b or c"            to "(or (not (between (id a case_insensitive) (id y case_insensitive) (id b case_insensitive))) (id c case_insensitive))",
        /* (not (between, and)           */ "a not between y and b and c"           to "(and (not (between (id a case_insensitive) (id y case_insensitive) (id b case_insensitive))) (id c case_insensitive))",
        /* (not (between, =)             */ "a not between y and b = c"             to "(= (not (between (id a case_insensitive) (id y case_insensitive) (id b case_insensitive))) (id c case_insensitive))",
        /* (not (between, <>)            */ "a not between y and b <> c"            to "(<> (not (between (id a case_insensitive) (id y case_insensitive) (id b case_insensitive))) (id c case_insensitive))",
        /* (not (between, in)            */ "a not between y and b in c"            to "(in (not (between (id a case_insensitive) (id y case_insensitive) (id b case_insensitive))) (id c case_insensitive))",
        /* (not (between, not_in)        */ "a not between y and b not in c"        to "(not (in (not (between (id a case_insensitive) (id y case_insensitive) (id b case_insensitive))) (id c case_insensitive)))",
        /* (not (between, <)             */ "a not between y and b < c"             to "(< (not (between (id a case_insensitive) (id y case_insensitive) (id b case_insensitive))) (id c case_insensitive))",
        /* (not (between, <=)            */ "a not between y and b <= c"            to "(<= (not (between (id a case_insensitive) (id y case_insensitive) (id b case_insensitive))) (id c case_insensitive))",
        /* (not (between, >)             */ "a not between y and b > c"             to "(> (not (between (id a case_insensitive) (id y case_insensitive) (id b case_insensitive))) (id c case_insensitive))",
        /* (not (between, >=)            */ "a not between y and b >= c"            to "(>= (not (between (id a case_insensitive) (id y case_insensitive) (id b case_insensitive))) (id c case_insensitive))",
        /* (not (between, between)       */ "a not between y and b between w and c" to "(between (not (between (id a case_insensitive) (id y case_insensitive) (id b case_insensitive))) (id w case_insensitive) (id c case_insensitive))",
        /* (not (between, like)          */ "a not between y and b like c"          to "(like (not (between (id a case_insensitive) (id y case_insensitive) (id b case_insensitive))) (id c case_insensitive))",
        /* (not (between, not_like)      */ "a not between y and b not like c"      to "(not (like (not (between (id a case_insensitive) (id y case_insensitive) (id b case_insensitive))) (id c case_insensitive)))",
        /* (not (between, +)             */ "a not between y and b + c"             to "(not (between (id a case_insensitive) (id y case_insensitive) (+ (id b case_insensitive) (id c case_insensitive))))",
        /* (not (between, -)             */ "a not between y and b - c"             to "(not (between (id a case_insensitive) (id y case_insensitive) (- (id b case_insensitive) (id c case_insensitive))))",
        /* (not (between, ||)            */ "a not between y and b || c"            to "(not (between (id a case_insensitive) (id y case_insensitive) (|| (id b case_insensitive) (id c case_insensitive))))",
        /* (not (between, *)             */ "a not between y and b * c"             to "(not (between (id a case_insensitive) (id y case_insensitive) (* (id b case_insensitive) (id c case_insensitive))))",
        /* (not (between, /)             */ "a not between y and b / c"             to "(not (between (id a case_insensitive) (id y case_insensitive) (/ (id b case_insensitive) (id c case_insensitive))))",
        /* (not (between, %)             */ "a not between y and b % c"             to "(not (between (id a case_insensitive) (id y case_insensitive) (% (id b case_insensitive) (id c case_insensitive))))",
        /* (not (between, is)            */ "a not between y and b is boolean"      to "(is (not (between (id a case_insensitive) (id y case_insensitive) (id b case_insensitive))) (type boolean))",
        /* (not (between, is_not)        */ "a not between y and b is not boolean"  to "(not (is (not (between (id a case_insensitive) (id y case_insensitive) (id b case_insensitive))) (type boolean)))")

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun likePrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForLikePrecedence() = listOf(
        /* (like, intersect)            */ "a like b intersect c"                  to "(intersect (like (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (like, intersect_all)        */ "a like b intersect all c"              to "(intersect_all (like (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (like, except)               */ "a like b except c"                     to "(except (like (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (like, except_all)           */ "a like b except all c"                 to "(except_all (like (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (like, union)                */ "a like b union c"                      to "(union (like (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (like, union_all)            */ "a like b union all c"                  to "(union_all (like (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (like, or)                   */ "a like b or c"                         to "(or (like (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (like, and)                  */ "a like b and c"                        to "(and (like (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (like, =)                    */ "a like b = c"                          to "(= (like (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (like, <>)                   */ "a like b <> c"                         to "(<> (like (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (like, in)                   */ "a like b in c"                         to "(in (like (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (like, not_in)               */ "a like b not in c"                     to "(not (in (like (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive)))",
        /* (like, <)                    */ "a like b < c"                          to "(< (like (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (like, <=)                   */ "a like b <= c"                         to "(<= (like (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (like, >)                    */ "a like b > c"                          to "(> (like (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (like, >=)                   */ "a like b >= c"                         to "(>= (like (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (like, between)              */ "a like b between w and c"              to "(between (like (id a case_insensitive) (id b case_insensitive)) (id w case_insensitive) (id c case_insensitive))",
        /* (like, not_between)          */ "a like b not between y and c"          to "(not (between (like (id a case_insensitive) (id b case_insensitive)) (id y case_insensitive) (id c case_insensitive)))",
        /* (like, not_like)             */ "a like b not like c"                   to "(not (like (like (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive)))",
        /* (like, +)                    */ "a like b + c"                          to "(like (id a case_insensitive) (+ (id b case_insensitive) (id c case_insensitive)))",
        /* (like, -)                    */ "a like b - c"                          to "(like (id a case_insensitive) (- (id b case_insensitive) (id c case_insensitive)))",
        /* (like, ||)                   */ "a like b || c"                         to "(like (id a case_insensitive) (|| (id b case_insensitive) (id c case_insensitive)))",
        /* (like, *)                    */ "a like b * c"                          to "(like (id a case_insensitive) (* (id b case_insensitive) (id c case_insensitive)))",
        /* (like, /)                    */ "a like b / c"                          to "(like (id a case_insensitive) (/ (id b case_insensitive) (id c case_insensitive)))",
        /* (like, %)                    */ "a like b % c"                          to "(like (id a case_insensitive) (% (id b case_insensitive) (id c case_insensitive)))",
        /* (like, is)                   */ "a like b is boolean"                   to "(is (like (id a case_insensitive) (id b case_insensitive)) (type boolean))",
        /* (like, is_not)               */ "a like b is not boolean"               to "(not (is (like (id a case_insensitive) (id b case_insensitive)) (type boolean)))")

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun notLikePrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForNotLikePrecedence() = listOf(
        /* (not (like, intersect)        */ "a not like b intersect c"              to "(intersect (not (like (id a case_insensitive) (id b case_insensitive))) (id c case_insensitive))",
        /* (not (like, intersect_all)    */ "a not like b intersect all c"          to "(intersect_all (not (like (id a case_insensitive) (id b case_insensitive))) (id c case_insensitive))",
        /* (not (like, except)           */ "a not like b except c"                 to "(except (not (like (id a case_insensitive) (id b case_insensitive))) (id c case_insensitive))",
        /* (not (like, except_all)       */ "a not like b except all c"             to "(except_all (not (like (id a case_insensitive) (id b case_insensitive))) (id c case_insensitive))",
        /* (not (like, union)            */ "a not like b union c"                  to "(union (not (like (id a case_insensitive) (id b case_insensitive))) (id c case_insensitive))",
        /* (not (like, union_all)        */ "a not like b union all c"              to "(union_all (not (like (id a case_insensitive) (id b case_insensitive))) (id c case_insensitive))",
        /* (not (like, or)               */ "a not like b or c"                     to "(or (not (like (id a case_insensitive) (id b case_insensitive))) (id c case_insensitive))",
        /* (not (like, and)              */ "a not like b and c"                    to "(and (not (like (id a case_insensitive) (id b case_insensitive))) (id c case_insensitive))",
        /* (not (like, =)                */ "a not like b = c"                      to "(= (not (like (id a case_insensitive) (id b case_insensitive))) (id c case_insensitive))",
        /* (not (like, <>)               */ "a not like b <> c"                     to "(<> (not (like (id a case_insensitive) (id b case_insensitive))) (id c case_insensitive))",
        /* (not (like, in)               */ "a not like b in c"                     to "(in (not (like (id a case_insensitive) (id b case_insensitive))) (id c case_insensitive))",
        /* (not (like, not_in)           */ "a not like b not in c"                 to "(not (in (not (like (id a case_insensitive) (id b case_insensitive))) (id c case_insensitive)))",
        /* (not (like, <)                */ "a not like b < c"                      to "(< (not (like (id a case_insensitive) (id b case_insensitive))) (id c case_insensitive))",
        /* (not (like, <=)               */ "a not like b <= c"                     to "(<= (not (like (id a case_insensitive) (id b case_insensitive))) (id c case_insensitive))",
        /* (not (like, >)                */ "a not like b > c"                      to "(> (not (like (id a case_insensitive) (id b case_insensitive))) (id c case_insensitive))",
        /* (not (like, >=)               */ "a not like b >= c"                     to "(>= (not (like (id a case_insensitive) (id b case_insensitive))) (id c case_insensitive))",
        /* (not (like, between)          */ "a not like b between w and c"          to "(between (not (like (id a case_insensitive) (id b case_insensitive))) (id w case_insensitive) (id c case_insensitive))",
        /* (not (like, not_between)      */ "a not like b not between y and c"      to "(not (between (not (like (id a case_insensitive) (id b case_insensitive))) (id y case_insensitive) (id c case_insensitive)))",
        /* (not (like, like)             */ "a not like b like c"                   to "(like (not (like (id a case_insensitive) (id b case_insensitive))) (id c case_insensitive))",
        /* (not (like, +)                */ "a not like b + c"                      to "(not (like (id a case_insensitive) (+ (id b case_insensitive) (id c case_insensitive))))",
        /* (not (like, -)                */ "a not like b - c"                      to "(not (like (id a case_insensitive) (- (id b case_insensitive) (id c case_insensitive))))",
        /* (not (like, ||)               */ "a not like b || c"                     to "(not (like (id a case_insensitive) (|| (id b case_insensitive) (id c case_insensitive))))",
        /* (not (like, *)                */ "a not like b * c"                      to "(not (like (id a case_insensitive) (* (id b case_insensitive) (id c case_insensitive))))",
        /* (not (like, /)                */ "a not like b / c"                      to "(not (like (id a case_insensitive) (/ (id b case_insensitive) (id c case_insensitive))))",
        /* (not (like, %)                */ "a not like b % c"                      to "(not (like (id a case_insensitive) (% (id b case_insensitive) (id c case_insensitive))))",
        /* (not (like, is)               */ "a not like b is boolean"               to "(is (not (like (id a case_insensitive) (id b case_insensitive))) (type boolean))",
        /* (not (like, is_not)           */ "a not like b is not boolean"           to "(not (is (not (like (id a case_insensitive) (id b case_insensitive))) (type boolean)))")

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun subtractPrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForSubtractPrecedence() = listOf(
        /* (+, intersect)               */ "a + b intersect c"                     to "(intersect (+ (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (+, intersect_all)           */ "a + b intersect all c"                 to "(intersect_all (+ (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (+, except)                  */ "a + b except c"                        to "(except (+ (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (+, except_all)              */ "a + b except all c"                    to "(except_all (+ (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (+, union)                   */ "a + b union c"                         to "(union (+ (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (+, union_all)               */ "a + b union all c"                     to "(union_all (+ (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (+, or)                      */ "a + b or c"                            to "(or (+ (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (+, and)                     */ "a + b and c"                           to "(and (+ (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (+, =)                       */ "a + b = c"                             to "(= (+ (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (+, <>)                      */ "a + b <> c"                            to "(<> (+ (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (+, in)                      */ "a + b in c"                            to "(in (+ (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (+, not_in)                  */ "a + b not in c"                        to "(not (in (+ (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive)))",
        /* (+, <)                       */ "a + b < c"                             to "(< (+ (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (+, <=)                      */ "a + b <= c"                            to "(<= (+ (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (+, >)                       */ "a + b > c"                             to "(> (+ (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (+, >=)                      */ "a + b >= c"                            to "(>= (+ (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (+, between)                 */ "a + b between w and c"                 to "(between (+ (id a case_insensitive) (id b case_insensitive)) (id w case_insensitive) (id c case_insensitive))",
        /* (+, not_between)             */ "a + b not between y and c"             to "(not (between (+ (id a case_insensitive) (id b case_insensitive)) (id y case_insensitive) (id c case_insensitive)))",
        /* (+, like)                    */ "a + b like c"                          to "(like (+ (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (+, not_like)                */ "a + b not like c"                      to "(not (like (+ (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive)))",
        /* (+, -)                       */ "a + b - c"                             to "(- (+ (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (+, ||)                      */ "a + b || c"                            to "(|| (+ (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (+, *)                       */ "a + b * c"                             to "(+ (id a case_insensitive) (* (id b case_insensitive) (id c case_insensitive)))",
        /* (+, /)                       */ "a + b / c"                             to "(+ (id a case_insensitive) (/ (id b case_insensitive) (id c case_insensitive)))",
        /* (+, %)                       */ "a + b % c"                             to "(+ (id a case_insensitive) (% (id b case_insensitive) (id c case_insensitive)))",
        /* (+, is)                      */ "a + b is boolean"                      to "(is (+ (id a case_insensitive) (id b case_insensitive)) (type boolean))",
        /* (+, is_not)                  */ "a + b is not boolean"                  to "(not (is (+ (id a case_insensitive) (id b case_insensitive)) (type boolean)))")

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun minusPrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForMinusPrecedence() = listOf(
        /* (-, intersect)               */ "a - b intersect c"                     to "(intersect (- (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (-, intersect_all)           */ "a - b intersect all c"                 to "(intersect_all (- (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (-, except)                  */ "a - b except c"                        to "(except (- (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (-, except_all)              */ "a - b except all c"                    to "(except_all (- (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (-, union)                   */ "a - b union c"                         to "(union (- (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (-, union_all)               */ "a - b union all c"                     to "(union_all (- (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (-, or)                      */ "a - b or c"                            to "(or (- (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (-, and)                     */ "a - b and c"                           to "(and (- (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (-, =)                       */ "a - b = c"                             to "(= (- (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (-, <>)                      */ "a - b <> c"                            to "(<> (- (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (-, in)                      */ "a - b in c"                            to "(in (- (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (-, not_in)                  */ "a - b not in c"                        to "(not (in (- (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive)))",
        /* (-, <)                       */ "a - b < c"                             to "(< (- (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (-, <=)                      */ "a - b <= c"                            to "(<= (- (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (-, >)                       */ "a - b > c"                             to "(> (- (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (-, >=)                      */ "a - b >= c"                            to "(>= (- (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (-, between)                 */ "a - b between w and c"                 to "(between (- (id a case_insensitive) (id b case_insensitive)) (id w case_insensitive) (id c case_insensitive))",
        /* (-, not_between)             */ "a - b not between y and c"             to "(not (between (- (id a case_insensitive) (id b case_insensitive)) (id y case_insensitive) (id c case_insensitive)))",
        /* (-, like)                    */ "a - b like c"                          to "(like (- (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (-, not_like)                */ "a - b not like c"                      to "(not (like (- (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive)))",
        /* (-, +)                       */ "a - b + c"                             to "(+ (- (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (-, ||)                      */ "a - b || c"                            to "(|| (- (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (-, *)                       */ "a - b * c"                             to "(- (id a case_insensitive) (* (id b case_insensitive) (id c case_insensitive)))",
        /* (-, /)                       */ "a - b / c"                             to "(- (id a case_insensitive) (/ (id b case_insensitive) (id c case_insensitive)))",
        /* (-, %)                       */ "a - b % c"                             to "(- (id a case_insensitive) (% (id b case_insensitive) (id c case_insensitive)))",
        /* (-, is)                      */ "a - b is boolean"                      to "(is (- (id a case_insensitive) (id b case_insensitive)) (type boolean))",
        /* (-, is_not)                  */ "a - b is not boolean"                  to "(not (is (- (id a case_insensitive) (id b case_insensitive)) (type boolean)))")

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun concatPrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForConcatPrecedence() = listOf(
        /* (||, intersect)              */ "a || b intersect c"                    to "(intersect (|| (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (||, intersect_all)          */ "a || b intersect all c"                to "(intersect_all (|| (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (||, except)                 */ "a || b except c"                       to "(except (|| (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (||, except_all)             */ "a || b except all c"                   to "(except_all (|| (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (||, union)                  */ "a || b union c"                        to "(union (|| (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (||, union_all)              */ "a || b union all c"                    to "(union_all (|| (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (||, or)                     */ "a || b or c"                           to "(or (|| (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (||, and)                    */ "a || b and c"                          to "(and (|| (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (||, =)                      */ "a || b = c"                            to "(= (|| (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (||, <>)                     */ "a || b <> c"                           to "(<> (|| (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (||, in)                     */ "a || b in c"                           to "(in (|| (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (||, not_in)                 */ "a || b not in c"                       to "(not (in (|| (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive)))",
        /* (||, <)                      */ "a || b < c"                            to "(< (|| (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (||, <=)                     */ "a || b <= c"                           to "(<= (|| (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (||, >)                      */ "a || b > c"                            to "(> (|| (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (||, >=)                     */ "a || b >= c"                           to "(>= (|| (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (||, between)                */ "a || b between w and c"                to "(between (|| (id a case_insensitive) (id b case_insensitive)) (id w case_insensitive) (id c case_insensitive))",
        /* (||, not_between)            */ "a || b not between y and c"            to "(not (between (|| (id a case_insensitive) (id b case_insensitive)) (id y case_insensitive) (id c case_insensitive)))",
        /* (||, like)                   */ "a || b like c"                         to "(like (|| (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (||, not_like)               */ "a || b not like c"                     to "(not (like (|| (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive)))",
        /* (||, +)                      */ "a || b + c"                            to "(+ (|| (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (||, -)                      */ "a || b - c"                            to "(- (|| (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (||, *)                      */ "a || b * c"                            to "(|| (id a case_insensitive) (* (id b case_insensitive) (id c case_insensitive)))",
        /* (||, /)                      */ "a || b / c"                            to "(|| (id a case_insensitive) (/ (id b case_insensitive) (id c case_insensitive)))",
        /* (||, %)                      */ "a || b % c"                            to "(|| (id a case_insensitive) (% (id b case_insensitive) (id c case_insensitive)))",
        /* (||, is)                     */ "a || b is boolean"                     to "(is (|| (id a case_insensitive) (id b case_insensitive)) (type boolean))",
        /* (||, is_not)                 */ "a || b is not boolean"                 to "(not (is (|| (id a case_insensitive) (id b case_insensitive)) (type boolean)))")

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun mulPrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForMulPrecedence() = listOf(
        /* (*, intersect)               */ "a * b intersect c"                     to "(intersect (* (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (*, intersect_all)           */ "a * b intersect all c"                 to "(intersect_all (* (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (*, except)                  */ "a * b except c"                        to "(except (* (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (*, except_all)              */ "a * b except all c"                    to "(except_all (* (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (*, union)                   */ "a * b union c"                         to "(union (* (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (*, union_all)               */ "a * b union all c"                     to "(union_all (* (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (*, or)                      */ "a * b or c"                            to "(or (* (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (*, and)                     */ "a * b and c"                           to "(and (* (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (*, =)                       */ "a * b = c"                             to "(= (* (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (*, <>)                      */ "a * b <> c"                            to "(<> (* (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (*, in)                      */ "a * b in c"                            to "(in (* (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (*, not_in)                  */ "a * b not in c"                        to "(not (in (* (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive)))",
        /* (*, <)                       */ "a * b < c"                             to "(< (* (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (*, <=)                      */ "a * b <= c"                            to "(<= (* (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (*, >)                       */ "a * b > c"                             to "(> (* (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (*, >=)                      */ "a * b >= c"                            to "(>= (* (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (*, between)                 */ "a * b between w and c"                 to "(between (* (id a case_insensitive) (id b case_insensitive)) (id w case_insensitive) (id c case_insensitive))",
        /* (*, not_between)             */ "a * b not between y and c"             to "(not (between (* (id a case_insensitive) (id b case_insensitive)) (id y case_insensitive) (id c case_insensitive)))",
        /* (*, like)                    */ "a * b like c"                          to "(like (* (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (*, not_like)                */ "a * b not like c"                      to "(not (like (* (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive)))",
        /* (*, +)                       */ "a * b + c"                             to "(+ (* (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (*, -)                       */ "a * b - c"                             to "(- (* (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (*, ||)                      */ "a * b || c"                            to "(|| (* (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (*, /)                       */ "a * b / c"                             to "(/ (* (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (*, %)                       */ "a * b % c"                             to "(% (* (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (*, is)                      */ "a * b is boolean"                      to "(is (* (id a case_insensitive) (id b case_insensitive)) (type boolean))",
        /* (*, is_not)                  */ "a * b is not boolean"                  to "(not (is (* (id a case_insensitive) (id b case_insensitive)) (type boolean)))")

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun divPrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForDivPrecedence() = listOf(
        /* (/, intersect)               */ "a / b intersect c"                     to "(intersect (/ (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (/, intersect_all)           */ "a / b intersect all c"                 to "(intersect_all (/ (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (/, except)                  */ "a / b except c"                        to "(except (/ (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (/, except_all)              */ "a / b except all c"                    to "(except_all (/ (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (/, union)                   */ "a / b union c"                         to "(union (/ (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (/, union_all)               */ "a / b union all c"                     to "(union_all (/ (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (/, or)                      */ "a / b or c"                            to "(or (/ (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (/, and)                     */ "a / b and c"                           to "(and (/ (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (/, =)                       */ "a / b = c"                             to "(= (/ (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (/, <>)                      */ "a / b <> c"                            to "(<> (/ (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (/, in)                      */ "a / b in c"                            to "(in (/ (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (/, not_in)                  */ "a / b not in c"                        to "(not (in (/ (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive)))",
        /* (/, <)                       */ "a / b < c"                             to "(< (/ (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (/, <=)                      */ "a / b <= c"                            to "(<= (/ (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (/, >)                       */ "a / b > c"                             to "(> (/ (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (/, >=)                      */ "a / b >= c"                            to "(>= (/ (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (/, between)                 */ "a / b between w and c"                 to "(between (/ (id a case_insensitive) (id b case_insensitive)) (id w case_insensitive) (id c case_insensitive))",
        /* (/, not_between)             */ "a / b not between y and c"             to "(not (between (/ (id a case_insensitive) (id b case_insensitive)) (id y case_insensitive) (id c case_insensitive)))",
        /* (/, like)                    */ "a / b like c"                          to "(like (/ (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (/, not_like)                */ "a / b not like c"                      to "(not (like (/ (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive)))",
        /* (/, +)                       */ "a / b + c"                             to "(+ (/ (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (/, -)                       */ "a / b - c"                             to "(- (/ (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (/, ||)                      */ "a / b || c"                            to "(|| (/ (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (/, *)                       */ "a / b * c"                             to "(* (/ (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (/, %)                       */ "a / b % c"                             to "(% (/ (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (/, is)                      */ "a / b is boolean"                      to "(is (/ (id a case_insensitive) (id b case_insensitive)) (type boolean))",
        /* (/, is_not)                  */ "a / b is not boolean"                  to "(not (is (/ (id a case_insensitive) (id b case_insensitive)) (type boolean)))")

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun modPrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForModPrecedence() = listOf(
        /* (%, intersect)               */ "a % b intersect c"                     to "(intersect (% (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (%, intersect_all)           */ "a % b intersect all c"                 to "(intersect_all (% (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (%, except)                  */ "a % b except c"                        to "(except (% (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (%, except_all)              */ "a % b except all c"                    to "(except_all (% (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (%, union)                   */ "a % b union c"                         to "(union (% (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (%, union_all)               */ "a % b union all c"                     to "(union_all (% (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (%, or)                      */ "a % b or c"                            to "(or (% (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (%, and)                     */ "a % b and c"                           to "(and (% (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (%, =)                       */ "a % b = c"                             to "(= (% (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (%, <>)                      */ "a % b <> c"                            to "(<> (% (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (%, in)                      */ "a % b in c"                            to "(in (% (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (%, not_in)                  */ "a % b not in c"                        to "(not (in (% (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive)))",
        /* (%, <)                       */ "a % b < c"                             to "(< (% (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (%, <=)                      */ "a % b <= c"                            to "(<= (% (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (%, >)                       */ "a % b > c"                             to "(> (% (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (%, >=)                      */ "a % b >= c"                            to "(>= (% (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (%, between)                 */ "a % b between w and c"                 to "(between (% (id a case_insensitive) (id b case_insensitive)) (id w case_insensitive) (id c case_insensitive))",
        /* (%, not_between)             */ "a % b not between y and c"             to "(not (between (% (id a case_insensitive) (id b case_insensitive)) (id y case_insensitive) (id c case_insensitive)))",
        /* (%, like)                    */ "a % b like c"                          to "(like (% (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (%, not_like)                */ "a % b not like c"                      to "(not (like (% (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive)))",
        /* (%, +)                       */ "a % b + c"                             to "(+ (% (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (%, -)                       */ "a % b - c"                             to "(- (% (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (%, ||)                      */ "a % b || c"                            to "(|| (% (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (%, *)                       */ "a % b * c"                             to "(* (% (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (%, /)                       */ "a % b / c"                             to "(/ (% (id a case_insensitive) (id b case_insensitive)) (id c case_insensitive))",
        /* (%, is)                      */ "a % b is boolean"                      to "(is (% (id a case_insensitive) (id b case_insensitive)) (type boolean))",
        /* (%, is_not)                  */ "a % b is not boolean"                  to "(not (is (% (id a case_insensitive) (id b case_insensitive)) (type boolean)))")

    @Test
    fun combinationOfBinaryOperators() = runTest(
        "a + b AND c / d * e - f || g OR h" to """
            (or
                (and
                    (+ (id a case_insensitive) (id b case_insensitive) )
                    (||
                        (-
                            (*
                                (/ (id c case_insensitive) (id d case_insensitive))
                                (id e case_insensitive)
                            )
                            (id f case_insensitive)
                        )
                        (id g case_insensitive)
                    )
                )
                (id h case_insensitive)
            )""")

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun notUnaryPrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForNotUnaryPrecedence() = listOf(
        /* (not, intersect)     */ "not a intersect b"         to "(intersect (not (id a case_insensitive)) (id b case_insensitive))",
        /* (not, intersect_all) */ "not a intersect all b"     to "(intersect_all (not (id a case_insensitive)) (id b case_insensitive))",
        /* (not, except)        */ "not a except b"            to "(except (not (id a case_insensitive)) (id b case_insensitive))",
        /* (not, except_all)    */ "not a except all b"        to "(except_all (not (id a case_insensitive)) (id b case_insensitive))",
        /* (not, union)         */ "not a union b"             to "(union (not (id a case_insensitive)) (id b case_insensitive))",
        /* (not, union_all)     */ "not a union all b"         to "(union_all (not (id a case_insensitive)) (id b case_insensitive))",
        /* (not, or)            */ "not a or b"                to "(or (not (id a case_insensitive)) (id b case_insensitive))",
        /* (not, and)           */ "not a and b"               to "(and (not (id a case_insensitive)) (id b case_insensitive))",
        /* (not, =)             */ "not a = b"                 to "(not (= (id a case_insensitive) (id b case_insensitive)))",
        /* (not, <>)            */ "not a <> b"                to "(not (<> (id a case_insensitive) (id b case_insensitive)))",
        /* (not, in)            */ "not a in b"                to "(not (in (id a case_insensitive) (id b case_insensitive)))",
        /* (not, not_in)        */ "not a not in b"            to "(not (not (in (id a case_insensitive) (id b case_insensitive))))",
        /* (not, <)             */ "not a < b"                 to "(not (< (id a case_insensitive) (id b case_insensitive)))",
        /* (not, <=)            */ "not a <= b"                to "(not (<= (id a case_insensitive) (id b case_insensitive)))",
        /* (not, >)             */ "not a > b"                 to "(not (> (id a case_insensitive) (id b case_insensitive)))",
        /* (not, >=)            */ "not a >= b"                to "(not (>= (id a case_insensitive) (id b case_insensitive)))",
        /* (not, between)       */ "not a between b and c"     to "(not (between (id a case_insensitive) (id b case_insensitive) (id c case_insensitive)))",
        /* (not, not_between)   */ "not a not between b and c" to "(not (not (between (id a case_insensitive) (id b case_insensitive) (id c case_insensitive))))",
        /* (not, like)          */ "not a like b"              to "(not (like (id a case_insensitive) (id b case_insensitive)))",
        /* (not, not_like)      */ "not a not like b"          to "(not (not (like (id a case_insensitive) (id b case_insensitive))))",
        /* (not, +)             */ "not a + b"                 to "(not (+ (id a case_insensitive) (id b case_insensitive)))",
        /* (not, -)             */ "not a - b"                 to "(not (- (id a case_insensitive) (id b case_insensitive)))",
        /* (not, ||)            */ "not a || b"                to "(not (|| (id a case_insensitive) (id b case_insensitive)))",
        /* (not, *)             */ "not a * b"                 to "(not (* (id a case_insensitive) (id b case_insensitive)))",
        /* (not, /)             */ "not a / b"                 to "(not (/ (id a case_insensitive) (id b case_insensitive)))",
        /* (not, %)             */ "not a % b"                 to "(not (% (id a case_insensitive) (id b case_insensitive)))",
        /* (not, is)            */ "not a is boolean"          to "(not (is (id a case_insensitive) (type boolean)))",
        /* (not, is_not)        */ "not a is not boolean"      to "(not (not (is (id a case_insensitive) (type boolean))))"
    )

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun notComboPrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForNotComboPrecedence() = listOf(
        // not combination
       "not a and b or c and not d or not e" to """
            (or
                (or
                    (and
                        (not (id a case_insensitive))
                        (id b case_insensitive)
                    )
                    (and
                        (id c case_insensitive)
                        (not (id d case_insensitive))
                    )
                )
                (not (id e case_insensitive))
            )""",

        // minus and plus unary
        "- a + b" to "(+ (- (id a case_insensitive)) (id b case_insensitive) )",

        "(a+-5e0) and (c-+7.0)" to """
            (and
                (+ (id a case_insensitive) (lit -5.) )
                (- (id c case_insensitive) (lit 7.0) )
            )""",

        "d*-+-9 and e>=+-+foo" to """
            (and
                (* (id d case_insensitive) (lit 9) )
                (>=
                    (id e case_insensitive)
                    (+ (- (+ (id foo case_insensitive))))
                )
            )""",

        "NOT s.id = 5" to """
            (not
                (=
                    (path (id s case_insensitive) (path_element (lit "id") case_insensitive))
                    (lit 5)
                )
            )""")



    private fun runTest(pair: Pair<String, String>) {
        val (source, expectedAst) = pair

        val v1SexpAst = "(ast (version 1) (root $expectedAst))"
        val expectedExprNode = AstDeserializerBuilder(ion).build().deserialize(ion.singleValue(v1SexpAst) as IonSexp)
        val actualExprNode = MetaStrippingRewriter.stripMetas(SqlParser(ion).parseExprNode(source))



        assertEquals(expectedExprNode, actualExprNode)
    }
}