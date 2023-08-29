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

import com.amazon.ionelement.api.toIonElement
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.junit.Test
import org.partiql.lang.domains.PartiqlAst

class PartiQLParserPrecedenceTest : PartiQLParserTestBase() {

    override val targets: Array<ParserTarget> = arrayOf(ParserTarget.DEFAULT, ParserTarget.EXPERIMENTAL)

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun intersectPrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForIntersectPrecedence(): List<Pair<String, String>> = listOf(
        // two by two binary operators
        /* (intersect, intersect_all)   */ "a intersect b intersect all c" to "(bag_op (intersect) (all) (bag_op (intersect) (distinct) (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (intersect, except)          */ "a intersect b except c" to "(bag_op (except) (distinct) (bag_op (intersect) (distinct) (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (intersect, except_all)      */ "a intersect b except all c" to "(bag_op (except) (all) (bag_op (intersect) (distinct) (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (intersect, union)           */ "a intersect b union c" to "(bag_op (union) (distinct) (bag_op (intersect) (distinct) (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (intersect, union_all)       */ "a intersect b union all c" to "(bag_op (union) (all) (bag_op (intersect) (distinct) (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (intersect, and)             */ "a intersect b and c" to "(bag_op (intersect) (distinct) (vr (id a (case_insensitive)) (unqualified)) (and (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (intersect, or)              */ "a intersect b or c" to "(bag_op (intersect) (distinct) (vr (id a (case_insensitive)) (unqualified)) (or (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (intersect, =)               */ "a intersect b = c" to "(bag_op (intersect) (distinct) (vr (id a (case_insensitive)) (unqualified)) (eq (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (intersect, <>)              */ "a intersect b <> c" to "(bag_op (intersect) (distinct) (vr (id a (case_insensitive)) (unqualified)) (ne (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (intersect, in)              */ "a intersect b in c" to "(bag_op (intersect) (distinct) (vr (id a (case_insensitive)) (unqualified)) (in_collection (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (intersect, not_in)          */ "a intersect b not in c" to "(bag_op (intersect) (distinct) (vr (id a (case_insensitive)) (unqualified)) (not (in_collection (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified)))))",
        /* (intersect, <)               */ "a intersect b < c" to "(bag_op (intersect) (distinct) (vr (id a (case_insensitive)) (unqualified)) (lt (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (intersect, <=)              */ "a intersect b <= c" to "(bag_op (intersect) (distinct) (vr (id a (case_insensitive)) (unqualified)) (lte (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (intersect, >)               */ "a intersect b > c" to "(bag_op (intersect) (distinct) (vr (id a (case_insensitive)) (unqualified)) (gt (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (intersect, >=)              */ "a intersect b >= c" to "(bag_op (intersect) (distinct) (vr (id a (case_insensitive)) (unqualified)) (gte (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (intersect, between)         */ "a intersect b between w and c" to "(bag_op (intersect) (distinct) (vr (id a (case_insensitive)) (unqualified)) (between (vr (id b (case_insensitive)) (unqualified)) (vr (id w (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (intersect, not_between)     */ "a intersect b not between y and c" to "(bag_op (intersect) (distinct) (vr (id a (case_insensitive)) (unqualified)) (not (between (vr (id b (case_insensitive)) (unqualified)) (vr (id y (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified)))))",
        /* (intersect, like)            */ "a intersect b like c" to "(bag_op (intersect) (distinct) (vr (id a (case_insensitive)) (unqualified)) (like (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified)) null))",
        /* (intersect, not_like)        */ "a intersect b not like c" to "(bag_op (intersect) (distinct) (vr (id a (case_insensitive)) (unqualified)) (not (like (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified)) null)))",
        /* (intersect, +)               */ "a intersect b + c" to "(bag_op (intersect) (distinct) (vr (id a (case_insensitive)) (unqualified)) (plus (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (intersect, -)               */ "a intersect b - c" to "(bag_op (intersect) (distinct) (vr (id a (case_insensitive)) (unqualified)) (minus (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (intersect, ||)              */ "a intersect b || c" to "(bag_op (intersect) (distinct) (vr (id a (case_insensitive)) (unqualified)) (concat (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (intersect, *)               */ "a intersect b * c" to "(bag_op (intersect) (distinct) (vr (id a (case_insensitive)) (unqualified)) (times (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (intersect, /)               */ "a intersect b / c" to "(bag_op (intersect) (distinct) (vr (id a (case_insensitive)) (unqualified)) (divide (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (intersect, %)               */ "a intersect b % c" to "(bag_op (intersect) (distinct) (vr (id a (case_insensitive)) (unqualified)) (modulo (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (intersect, is)              */ "a intersect b is boolean" to "(bag_op (intersect) (distinct) (vr (id a (case_insensitive)) (unqualified)) (is_type (vr (id b (case_insensitive)) (unqualified)) (boolean_type)))",
        /* (intersect, is_not)          */ "a intersect b is not boolean" to "(bag_op (intersect) (distinct) (vr (id a (case_insensitive)) (unqualified)) (not (is_type (vr (id b (case_insensitive)) (unqualified)) (boolean_type))))"
    )

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun intersectAllPrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForIntersectAllPrecedence() = listOf(
        /* (intersect_all, intersect)   */ "a intersect all b intersect c" to "(bag_op (intersect) (distinct) (bag_op (intersect) (all) (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (intersect_all, except)      */ "a intersect all b except c" to "(bag_op (except) (distinct) (bag_op (intersect) (all) (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (intersect_all, except_all)  */ "a intersect all b except all c" to "(bag_op (except) (all) (bag_op (intersect) (all) (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (intersect_all, union)       */ "a intersect all b union c" to "(bag_op (union) (distinct) (bag_op (intersect) (all) (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (intersect_all, union_all)   */ "a intersect all b union all c" to "(bag_op (union) (all) (bag_op (intersect) (all) (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (intersect_all, and)         */ "a intersect all b and c" to "(bag_op (intersect) (all) (vr (id a (case_insensitive)) (unqualified)) (and (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (intersect_all, or)          */ "a intersect all b or c" to "(bag_op (intersect) (all) (vr (id a (case_insensitive)) (unqualified)) (or (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (intersect_all, =)           */ "a intersect all b = c" to "(bag_op (intersect) (all) (vr (id a (case_insensitive)) (unqualified)) (eq (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (intersect_all, <>)          */ "a intersect all b <> c" to "(bag_op (intersect) (all) (vr (id a (case_insensitive)) (unqualified)) (ne (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (intersect_all, in)          */ "a intersect all b in c" to "(bag_op (intersect) (all) (vr (id a (case_insensitive)) (unqualified)) (in_collection (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (intersect_all, not_in)      */ "a intersect all b not in c" to "(bag_op (intersect) (all) (vr (id a (case_insensitive)) (unqualified)) (not (in_collection (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified)))))",
        /* (intersect_all, <)           */ "a intersect all b < c" to "(bag_op (intersect) (all) (vr (id a (case_insensitive)) (unqualified)) (lt (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (intersect_all, <=)          */ "a intersect all b <= c" to "(bag_op (intersect) (all) (vr (id a (case_insensitive)) (unqualified)) (lte (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (intersect_all, >)           */ "a intersect all b > c" to "(bag_op (intersect) (all) (vr (id a (case_insensitive)) (unqualified)) (gt (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (intersect_all, >=)          */ "a intersect all b >= c" to "(bag_op (intersect) (all) (vr (id a (case_insensitive)) (unqualified)) (gte (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (intersect_all, between)     */ "a intersect all b between w and c" to "(bag_op (intersect) (all) (vr (id a (case_insensitive)) (unqualified)) (between (vr (id b (case_insensitive)) (unqualified)) (vr (id w (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (intersect_all, not_between) */ "a intersect all b not between y and c" to "(bag_op (intersect) (all) (vr (id a (case_insensitive)) (unqualified)) (not (between (vr (id b (case_insensitive)) (unqualified)) (vr (id y (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified)))))",
        /* (intersect_all, like)        */ "a intersect all b like c" to "(bag_op (intersect) (all) (vr (id a (case_insensitive)) (unqualified)) (like (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified)) null))",
        /* (intersect_all, not_like)    */ "a intersect all b not like c" to "(bag_op (intersect) (all) (vr (id a (case_insensitive)) (unqualified)) (not (like (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified)) null)))",
        /* (intersect_all, +)           */ "a intersect all b + c" to "(bag_op (intersect) (all) (vr (id a (case_insensitive)) (unqualified)) (plus (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (intersect_all, -)           */ "a intersect all b - c" to "(bag_op (intersect) (all) (vr (id a (case_insensitive)) (unqualified)) (minus (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (intersect_all, ||)          */ "a intersect all b || c" to "(bag_op (intersect) (all) (vr (id a (case_insensitive)) (unqualified)) (concat (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (intersect_all, *)           */ "a intersect all b * c" to "(bag_op (intersect) (all) (vr (id a (case_insensitive)) (unqualified)) (times (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (intersect_all, /)           */ "a intersect all b / c" to "(bag_op (intersect) (all) (vr (id a (case_insensitive)) (unqualified)) (divide (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (intersect_all, %)           */ "a intersect all b % c" to "(bag_op (intersect) (all) (vr (id a (case_insensitive)) (unqualified)) (modulo (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (intersect_all, is)          */ "a intersect all b is boolean" to "(bag_op (intersect) (all) (vr (id a (case_insensitive)) (unqualified)) (is_type (vr (id b (case_insensitive)) (unqualified)) (boolean_type)))",
        /* (intersect_all, is_not)      */ "a intersect all b is not boolean" to "(bag_op (intersect) (all) (vr (id a (case_insensitive)) (unqualified)) (not (is_type (vr (id b (case_insensitive)) (unqualified)) (boolean_type))))"
    )

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun exceptPrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForExceptPrecedence() = listOf(
        /* (except, intersect)          */ "a except b intersect c" to "(bag_op (intersect) (distinct) (bag_op (except) (distinct) (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (except, intersect_all)      */ "a except b intersect all c" to "(bag_op (intersect) (all) (bag_op (except) (distinct) (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (except, except_all)         */ "a except b except all c" to "(bag_op (except) (all) (bag_op (except) (distinct) (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (except, union)              */ "a except b union c" to "(bag_op (union) (distinct) (bag_op (except) (distinct) (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (except, union_all)          */ "a except b union all c" to "(bag_op (union) (all) (bag_op (except) (distinct) (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (except, and)                */ "a except b and c" to "(bag_op (except) (distinct) (vr (id a (case_insensitive)) (unqualified)) (and (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (except, or)                 */ "a except b or c" to "(bag_op (except) (distinct) (vr (id a (case_insensitive)) (unqualified)) (or (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (except, =)                  */ "a except b = c" to "(bag_op (except) (distinct) (vr (id a (case_insensitive)) (unqualified)) (eq (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (except, <>)                 */ "a except b <> c" to "(bag_op (except) (distinct) (vr (id a (case_insensitive)) (unqualified)) (ne (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (except, in)                 */ "a except b in c" to "(bag_op (except) (distinct) (vr (id a (case_insensitive)) (unqualified)) (in_collection (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (except, not_in)             */ "a except b not in c" to "(bag_op (except) (distinct) (vr (id a (case_insensitive)) (unqualified)) (not (in_collection (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified)))))",
        /* (except, <)                  */ "a except b < c" to "(bag_op (except) (distinct) (vr (id a (case_insensitive)) (unqualified)) (lt (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (except, <=)                 */ "a except b <= c" to "(bag_op (except) (distinct) (vr (id a (case_insensitive)) (unqualified)) (lte (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (except, >)                  */ "a except b > c" to "(bag_op (except) (distinct) (vr (id a (case_insensitive)) (unqualified)) (gt (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (except, >=)                 */ "a except b >= c" to "(bag_op (except) (distinct) (vr (id a (case_insensitive)) (unqualified)) (gte (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (except, between)            */ "a except b between w and c" to "(bag_op (except) (distinct) (vr (id a (case_insensitive)) (unqualified)) (between (vr (id b (case_insensitive)) (unqualified)) (vr (id w (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (except, not_between)        */ "a except b not between y and c" to "(bag_op (except) (distinct) (vr (id a (case_insensitive)) (unqualified)) (not (between (vr (id b (case_insensitive)) (unqualified)) (vr (id y (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified)))))",
        /* (except, like)               */ "a except b like c" to "(bag_op (except) (distinct) (vr (id a (case_insensitive)) (unqualified)) (like (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified)) null))",
        /* (except, not_like)           */ "a except b not like c" to "(bag_op (except) (distinct) (vr (id a (case_insensitive)) (unqualified)) (not (like (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified)) null)))",
        /* (except, +)                  */ "a except b + c" to "(bag_op (except) (distinct) (vr (id a (case_insensitive)) (unqualified)) (plus (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (except, -)                  */ "a except b - c" to "(bag_op (except) (distinct) (vr (id a (case_insensitive)) (unqualified)) (minus (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (except, ||)                 */ "a except b || c" to "(bag_op (except) (distinct) (vr (id a (case_insensitive)) (unqualified)) (concat (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (except, *)                  */ "a except b * c" to "(bag_op (except) (distinct) (vr (id a (case_insensitive)) (unqualified)) (times (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (except, /)                  */ "a except b / c" to "(bag_op (except) (distinct) (vr (id a (case_insensitive)) (unqualified)) (divide (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (except, %)                  */ "a except b % c" to "(bag_op (except) (distinct) (vr (id a (case_insensitive)) (unqualified)) (modulo (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (except, is)                 */ "a except b is boolean" to "(bag_op (except) (distinct) (vr (id a (case_insensitive)) (unqualified)) (is_type (vr (id b (case_insensitive)) (unqualified)) (boolean_type)))",
        /* (except, is_not)             */ "a except b is not boolean" to "(bag_op (except) (distinct) (vr (id a (case_insensitive)) (unqualified)) (not (is_type (vr (id b (case_insensitive)) (unqualified)) (boolean_type))))"
    )

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun exceptAllPrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForExceptAllPrecedence() = listOf(
        /* (except_all, intersect)      */ "a except all b intersect c" to "(bag_op (intersect) (distinct) (bag_op (except) (all) (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (except_all, intersect_all)  */ "a except all b intersect all c" to "(bag_op (intersect) (all) (bag_op (except) (all) (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (except_all, except)         */ "a except all b except c" to "(bag_op (except) (distinct) (bag_op (except) (all) (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (except_all, union)          */ "a except all b union c" to "(bag_op (union) (distinct) (bag_op (except) (all) (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (except_all, union_all)      */ "a except all b union all c" to "(bag_op (union) (all) (bag_op (except) (all) (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (except_all, and)            */ "a except all b and c" to "(bag_op (except) (all) (vr (id a (case_insensitive)) (unqualified)) (and (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (except_all, or)             */ "a except all b or c" to "(bag_op (except) (all) (vr (id a (case_insensitive)) (unqualified)) (or (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (except_all, =)              */ "a except all b = c" to "(bag_op (except) (all) (vr (id a (case_insensitive)) (unqualified)) (eq (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (except_all, <>)             */ "a except all b <> c" to "(bag_op (except) (all) (vr (id a (case_insensitive)) (unqualified)) (ne (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (except_all, in)             */ "a except all b in c" to "(bag_op (except) (all) (vr (id a (case_insensitive)) (unqualified)) (in_collection (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (except_all, not_in)         */ "a except all b not in c" to "(bag_op (except) (all) (vr (id a (case_insensitive)) (unqualified)) (not (in_collection (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified)))))",
        /* (except_all, <)              */ "a except all b < c" to "(bag_op (except) (all) (vr (id a (case_insensitive)) (unqualified)) (lt (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (except_all, <=)             */ "a except all b <= c" to "(bag_op (except) (all) (vr (id a (case_insensitive)) (unqualified)) (lte (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (except_all, >)              */ "a except all b > c" to "(bag_op (except) (all) (vr (id a (case_insensitive)) (unqualified)) (gt (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (except_all, >=)             */ "a except all b >= c" to "(bag_op (except) (all) (vr (id a (case_insensitive)) (unqualified)) (gte (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (except_all, between)        */ "a except all b between w and c" to "(bag_op (except) (all) (vr (id a (case_insensitive)) (unqualified)) (between (vr (id b (case_insensitive)) (unqualified)) (vr (id w (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (except_all, not_between)    */ "a except all b not between y and c" to "(bag_op (except) (all) (vr (id a (case_insensitive)) (unqualified)) (not (between (vr (id b (case_insensitive)) (unqualified)) (vr (id y (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified)))))",
        /* (except_all, like)           */ "a except all b like c" to "(bag_op (except) (all) (vr (id a (case_insensitive)) (unqualified)) (like (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified)) null))",
        /* (except_all, not_like)       */ "a except all b not like c" to "(bag_op (except) (all) (vr (id a (case_insensitive)) (unqualified)) (not (like (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified)) null)))",
        /* (except_all, +)              */ "a except all b + c" to "(bag_op (except) (all) (vr (id a (case_insensitive)) (unqualified)) (plus (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (except_all, -)              */ "a except all b - c" to "(bag_op (except) (all) (vr (id a (case_insensitive)) (unqualified)) (minus (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (except_all, ||)             */ "a except all b || c" to "(bag_op (except) (all) (vr (id a (case_insensitive)) (unqualified)) (concat (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (except_all, *)              */ "a except all b * c" to "(bag_op (except) (all) (vr (id a (case_insensitive)) (unqualified)) (times (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (except_all, /)              */ "a except all b / c" to "(bag_op (except) (all) (vr (id a (case_insensitive)) (unqualified)) (divide (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (except_all, %)              */ "a except all b % c" to "(bag_op (except) (all) (vr (id a (case_insensitive)) (unqualified)) (modulo (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (except_all, is)             */ "a except all b is boolean" to "(bag_op (except) (all) (vr (id a (case_insensitive)) (unqualified)) (is_type (vr (id b (case_insensitive)) (unqualified)) (boolean_type)))",
        /* (except_all, is_not)         */ "a except all b is not boolean" to "(bag_op (except) (all) (vr (id a (case_insensitive)) (unqualified)) (not (is_type (vr (id b (case_insensitive)) (unqualified)) (boolean_type))))"
    )

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun unionPrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForUnionPrecedence() = listOf(
        /* (union, intersect)           */ "a union b intersect c" to "(bag_op (intersect) (distinct) (bag_op (union) (distinct) (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (union, intersect_all)       */ "a union b intersect all c" to "(bag_op (intersect) (all) (bag_op (union) (distinct) (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (union, except)              */ "a union b except c" to "(bag_op (except) (distinct) (bag_op (union) (distinct) (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (union, except_all)          */ "a union b except all c" to "(bag_op (except) (all) (bag_op (union) (distinct) (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (union, union_all)           */ "a union b union all c" to "(bag_op (union) (all) (bag_op (union) (distinct) (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (union, and)                 */ "a union b and c" to "(bag_op (union) (distinct) (vr (id a (case_insensitive)) (unqualified)) (and (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (union, or)                  */ "a union b or c" to "(bag_op (union) (distinct) (vr (id a (case_insensitive)) (unqualified)) (or (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (union, =)                   */ "a union b = c" to "(bag_op (union) (distinct) (vr (id a (case_insensitive)) (unqualified)) (eq (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (union, <>)                  */ "a union b <> c" to "(bag_op (union) (distinct) (vr (id a (case_insensitive)) (unqualified)) (ne (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (union, in)                  */ "a union b in c" to "(bag_op (union) (distinct) (vr (id a (case_insensitive)) (unqualified)) (in_collection (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (union, not_in)              */ "a union b not in c" to "(bag_op (union) (distinct) (vr (id a (case_insensitive)) (unqualified)) (not (in_collection (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified)))))",
        /* (union, <)                   */ "a union b < c" to "(bag_op (union) (distinct) (vr (id a (case_insensitive)) (unqualified)) (lt (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (union, <=)                  */ "a union b <= c" to "(bag_op (union) (distinct) (vr (id a (case_insensitive)) (unqualified)) (lte (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (union, >)                   */ "a union b > c" to "(bag_op (union) (distinct) (vr (id a (case_insensitive)) (unqualified)) (gt (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (union, >=)                  */ "a union b >= c" to "(bag_op (union) (distinct) (vr (id a (case_insensitive)) (unqualified)) (gte (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (union, between)             */ "a union b between w and c" to "(bag_op (union) (distinct) (vr (id a (case_insensitive)) (unqualified)) (between (vr (id b (case_insensitive)) (unqualified)) (vr (id w (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (union, not_between)         */ "a union b not between y and c" to "(bag_op (union) (distinct) (vr (id a (case_insensitive)) (unqualified)) (not (between (vr (id b (case_insensitive)) (unqualified)) (vr (id y (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified)))))",
        /* (union, like)                */ "a union b like c" to "(bag_op (union) (distinct) (vr (id a (case_insensitive)) (unqualified)) (like (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified)) null))",
        /* (union, not_like)            */ "a union b not like c" to "(bag_op (union) (distinct) (vr (id a (case_insensitive)) (unqualified)) (not (like (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified)) null)))",
        /* (union, +)                   */ "a union b + c" to "(bag_op (union) (distinct) (vr (id a (case_insensitive)) (unqualified)) (plus (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (union, -)                   */ "a union b - c" to "(bag_op (union) (distinct) (vr (id a (case_insensitive)) (unqualified)) (minus (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (union, ||)                  */ "a union b || c" to "(bag_op (union) (distinct) (vr (id a (case_insensitive)) (unqualified)) (concat (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (union, *)                   */ "a union b * c" to "(bag_op (union) (distinct) (vr (id a (case_insensitive)) (unqualified)) (times (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (union, /)                   */ "a union b / c" to "(bag_op (union) (distinct) (vr (id a (case_insensitive)) (unqualified)) (divide (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (union, %)                   */ "a union b % c" to "(bag_op (union) (distinct) (vr (id a (case_insensitive)) (unqualified)) (modulo (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (union, is)                  */ "a union b is boolean" to "(bag_op (union) (distinct) (vr (id a (case_insensitive)) (unqualified)) (is_type (vr (id b (case_insensitive)) (unqualified)) (boolean_type)))",
        /* (union, is_not)              */ "a union b is not boolean" to "(bag_op (union) (distinct) (vr (id a (case_insensitive)) (unqualified)) (not (is_type (vr (id b (case_insensitive)) (unqualified)) (boolean_type))))"
    )

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun unionAllPrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForUnionAllPrecedence() = listOf(
        /* (union_all, intersect)       */ "a union all b intersect c" to "(bag_op (intersect) (distinct) (bag_op (union) (all) (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (union_all, intersect_all)   */ "a union all b intersect all c" to "(bag_op (intersect) (all) (bag_op (union) (all) (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (union_all, except)          */ "a union all b except c" to "(bag_op (except) (distinct) (bag_op (union) (all) (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (union_all, except_all)      */ "a union all b except all c" to "(bag_op (except) (all) (bag_op (union) (all) (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (union_all, union)           */ "a union all b union c" to "(bag_op (union) (distinct) (bag_op (union) (all) (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (union_all, and)             */ "a union all b and c" to "(bag_op (union) (all) (vr (id a (case_insensitive)) (unqualified)) (and (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (union_all, or)              */ "a union all b or c" to "(bag_op (union) (all) (vr (id a (case_insensitive)) (unqualified)) (or (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (union_all, =)               */ "a union all b = c" to "(bag_op (union) (all) (vr (id a (case_insensitive)) (unqualified)) (eq (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (union_all, <>)              */ "a union all b <> c" to "(bag_op (union) (all) (vr (id a (case_insensitive)) (unqualified)) (ne (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (union_all, in)              */ "a union all b in c" to "(bag_op (union) (all) (vr (id a (case_insensitive)) (unqualified)) (in_collection (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (union_all, not_in)          */ "a union all b not in c" to "(bag_op (union) (all) (vr (id a (case_insensitive)) (unqualified)) (not (in_collection (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified)))))",
        /* (union_all, <)               */ "a union all b < c" to "(bag_op (union) (all) (vr (id a (case_insensitive)) (unqualified)) (lt (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (union_all, <=)              */ "a union all b <= c" to "(bag_op (union) (all) (vr (id a (case_insensitive)) (unqualified)) (lte (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (union_all, >)               */ "a union all b > c" to "(bag_op (union) (all) (vr (id a (case_insensitive)) (unqualified)) (gt (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (union_all, >=)              */ "a union all b >= c" to "(bag_op (union) (all) (vr (id a (case_insensitive)) (unqualified)) (gte (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (union_all, between)         */ "a union all b between w and c" to "(bag_op (union) (all) (vr (id a (case_insensitive)) (unqualified)) (between (vr (id b (case_insensitive)) (unqualified)) (vr (id w (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (union_all, not_between)     */ "a union all b not between y and c" to "(bag_op (union) (all) (vr (id a (case_insensitive)) (unqualified)) (not (between (vr (id b (case_insensitive)) (unqualified)) (vr (id y (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified)))))",
        /* (union_all, like)            */ "a union all b like c" to "(bag_op (union) (all) (vr (id a (case_insensitive)) (unqualified)) (like (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified)) null))",
        /* (union_all, not_like)        */ "a union all b not like c" to "(bag_op (union) (all) (vr (id a (case_insensitive)) (unqualified)) (not (like (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified)) null)))",
        /* (union_all, +)               */ "a union all b + c" to "(bag_op (union) (all) (vr (id a (case_insensitive)) (unqualified)) (plus (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (union_all, -)               */ "a union all b - c" to "(bag_op (union) (all) (vr (id a (case_insensitive)) (unqualified)) (minus (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (union_all, ||)              */ "a union all b || c" to "(bag_op (union) (all) (vr (id a (case_insensitive)) (unqualified)) (concat (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (union_all, *)               */ "a union all b * c" to "(bag_op (union) (all) (vr (id a (case_insensitive)) (unqualified)) (times (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (union_all, /)               */ "a union all b / c" to "(bag_op (union) (all) (vr (id a (case_insensitive)) (unqualified)) (divide (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (union_all, %)               */ "a union all b % c" to "(bag_op (union) (all) (vr (id a (case_insensitive)) (unqualified)) (modulo (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (union_all, is)              */ "a union all b is boolean" to "(bag_op (union) (all) (vr (id a (case_insensitive)) (unqualified)) (is_type (vr (id b (case_insensitive)) (unqualified)) (boolean_type)))",
        /* (union_all, is_not)          */ "a union all b is not boolean" to "(bag_op (union) (all) (vr (id a (case_insensitive)) (unqualified)) (not (is_type (vr (id b (case_insensitive)) (unqualified)) (boolean_type))))"
    )

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun andPrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForAndPrecedence() = listOf(
        /* (and, intersect)             */ "a and b intersect c" to "(bag_op (intersect) (distinct) (and (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (and, intersect_all)         */ "a and b intersect all c" to "(bag_op (intersect) (all) (and (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (and, except)                */ "a and b except c" to "(bag_op (except) (distinct) (and (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (and, except_all)            */ "a and b except all c" to "(bag_op (except) (all) (and (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (and, union)                 */ "a and b union c" to "(bag_op (union) (distinct) (and (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (and, union_all)             */ "a and b union all c" to "(bag_op (union) (all) (and (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (and, or)                    */ "a and b or c" to "(or (and (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (and, =)                     */ "a and b = c" to "(and (vr (id a (case_insensitive)) (unqualified)) (eq (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (and, <>)                    */ "a and b <> c" to "(and (vr (id a (case_insensitive)) (unqualified)) (ne (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (and, in)                    */ "a and b in c" to "(and (vr (id a (case_insensitive)) (unqualified)) (in_collection (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (and, not_in)                */ "a and b not in c" to "(and (vr (id a (case_insensitive)) (unqualified)) (not (in_collection (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified)))))",
        /* (and, <)                     */ "a and b < c" to "(and (vr (id a (case_insensitive)) (unqualified)) (lt (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (and, <=)                    */ "a and b <= c" to "(and (vr (id a (case_insensitive)) (unqualified)) (lte (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (and, >)                     */ "a and b > c" to "(and (vr (id a (case_insensitive)) (unqualified)) (gt (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (and, >=)                    */ "a and b >= c" to "(and (vr (id a (case_insensitive)) (unqualified)) (gte (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (and, between)               */ "a and b between w and c" to "(and (vr (id a (case_insensitive)) (unqualified)) (between (vr (id b (case_insensitive)) (unqualified)) (vr (id w (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (and, not_between)           */ "a and b not between y and c" to "(and (vr (id a (case_insensitive)) (unqualified)) (not (between (vr (id b (case_insensitive)) (unqualified)) (vr (id y (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified)))))",
        /* (and, like)                  */ "a and b like c" to "(and (vr (id a (case_insensitive)) (unqualified)) (like (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified)) null))",
        /* (and, not_like)              */ "a and b not like c" to "(and (vr (id a (case_insensitive)) (unqualified)) (not (like (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified)) null)))",
        /* (and, +)                     */ "a and b + c" to "(and (vr (id a (case_insensitive)) (unqualified)) (plus (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (and, -)                     */ "a and b - c" to "(and (vr (id a (case_insensitive)) (unqualified)) (minus (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (and, ||)                    */ "a and b || c" to "(and (vr (id a (case_insensitive)) (unqualified)) (concat (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (and, *)                     */ "a and b * c" to "(and (vr (id a (case_insensitive)) (unqualified)) (times (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (and, /)                     */ "a and b / c" to "(and (vr (id a (case_insensitive)) (unqualified)) (divide (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (and, %)                     */ "a and b % c" to "(and (vr (id a (case_insensitive)) (unqualified)) (modulo (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (and, is)                    */ "a and b is boolean" to "(and (vr (id a (case_insensitive)) (unqualified)) (is_type (vr (id b (case_insensitive)) (unqualified)) (boolean_type)))",
        /* (and, is_not)                */ "a and b is not boolean" to "(and (vr (id a (case_insensitive)) (unqualified)) (not (is_type (vr (id b (case_insensitive)) (unqualified)) (boolean_type))))"
    )

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun orPrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForOrPrecedence() = listOf(
        /* (or, intersect)              */ "a or b intersect c" to "(bag_op (intersect) (distinct) (or (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (or, intersect_all)          */ "a or b intersect all c " to "(bag_op (intersect) (all) (or (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (or, except)                 */ "a or b except c" to "(bag_op (except) (distinct) (or (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (or, except_all)             */ "a or b except all c " to "(bag_op (except) (all) (or (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (or, union)                  */ "a or b union c" to "(bag_op (union) (distinct) (or (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (or, union_all)              */ "a or b union all c " to "(bag_op (union) (all) (or (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (or, and)                    */ "a or b and c" to "(or (vr (id a (case_insensitive)) (unqualified)) (and (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (or, =)                      */ "a or b = c" to "(or (vr (id a (case_insensitive)) (unqualified)) (eq (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (or, <>)                     */ "a or b <> c" to "(or (vr (id a (case_insensitive)) (unqualified)) (ne (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (or, in)                     */ "a or b in c" to "(or (vr (id a (case_insensitive)) (unqualified)) (in_collection (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (or, not_in)                 */ "a or b not in c" to "(or (vr (id a (case_insensitive)) (unqualified)) (not (in_collection (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified)))))",
        /* (or, <)                      */ "a or b < c" to "(or (vr (id a (case_insensitive)) (unqualified)) (lt (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (or, <=)                     */ "a or b <= c" to "(or (vr (id a (case_insensitive)) (unqualified)) (lte (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (or, >)                      */ "a or b > c" to "(or (vr (id a (case_insensitive)) (unqualified)) (gt (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (or, >=)                     */ "a or b >= c" to "(or (vr (id a (case_insensitive)) (unqualified)) (gte (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (or, between)                */ "a or b between w and c" to "(or (vr (id a (case_insensitive)) (unqualified)) (between (vr (id b (case_insensitive)) (unqualified)) (vr (id w (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (or, not_between)            */ "a or b not between y and c" to "(or (vr (id a (case_insensitive)) (unqualified)) (not (between (vr (id b (case_insensitive)) (unqualified)) (vr (id y (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified)))))",
        /* (or, like)                   */ "a or b like c" to "(or (vr (id a (case_insensitive)) (unqualified)) (like (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified)) null))",
        /* (or, not_like)               */ "a or b not like c" to "(or (vr (id a (case_insensitive)) (unqualified)) (not (like (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified)) null)))",
        /* (or, +)                      */ "a or b + c" to "(or (vr (id a (case_insensitive)) (unqualified)) (plus (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (or, -)                      */ "a or b - c" to "(or (vr (id a (case_insensitive)) (unqualified)) (minus (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (or, ||)                     */ "a or b || c" to "(or (vr (id a (case_insensitive)) (unqualified)) (concat (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (or, *)                      */ "a or b * c" to "(or (vr (id a (case_insensitive)) (unqualified)) (times (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (or, /)                      */ "a or b / c" to "(or (vr (id a (case_insensitive)) (unqualified)) (divide (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (or, %)                      */ "a or b % c" to "(or (vr (id a (case_insensitive)) (unqualified)) (modulo (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (or, is)                     */ "a or b is boolean" to "(or (vr (id a (case_insensitive)) (unqualified)) (is_type (vr (id b (case_insensitive)) (unqualified)) (boolean_type)))",
        /* (or, is_not)                 */ "a or b is not boolean" to "(or (vr (id a (case_insensitive)) (unqualified)) (not (is_type (vr (id b (case_insensitive)) (unqualified)) (boolean_type))))"
    )

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun equalsPrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForEqualsPrecedence() = listOf(
        /* (=, intersect)               */ "a = b intersect c" to "(bag_op (intersect) (distinct) (eq (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (=, intersect_all)           */ "a = b intersect all c  " to "(bag_op (intersect) (all) (eq (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (=, except)                  */ "a = b except c" to "(bag_op (except) (distinct) (eq (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (=, except_all)              */ "a = b except all c  " to "(bag_op (except) (all) (eq (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (=, union)                   */ "a = b union c" to "(bag_op (union) (distinct) (eq (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (=, union_all)               */ "a = b union all c  " to "(bag_op (union) (all) (eq (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (=, or)                      */ "a = b or c" to "(or (eq (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (=, and)                     */ "a = b and c" to "(and (eq (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (=, <>)                      */ "a = b <> c" to "(ne (eq (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (=, in)                      */ "a = b in c" to "(in_collection (eq (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (=, not_in)                  */ "a = b not in c" to "(not (in_collection (eq (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified))))",
        /* (=, +)                       */ "a = b + c" to "(eq (vr (id a (case_insensitive)) (unqualified)) (plus (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (=, -)                       */ "a = b - c" to "(eq (vr (id a (case_insensitive)) (unqualified)) (minus (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (=, ||)                      */ "a = b || c" to "(eq (vr (id a (case_insensitive)) (unqualified)) (concat (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (=, *)                       */ "a = b * c" to "(eq (vr (id a (case_insensitive)) (unqualified)) (times (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (=, /)                       */ "a = b / c" to "(eq (vr (id a (case_insensitive)) (unqualified)) (divide (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (=, %)                       */ "a = b % c" to "(eq (vr (id a (case_insensitive)) (unqualified)) (modulo (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (=, is)                      */ "a = b is boolean" to "(is_type (eq (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (boolean_type))",
        /* (=, is_not)                  */ "a = b is not boolean" to "(not (is_type (eq (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (boolean_type)))"
    )

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun notEqualPrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForNotEqualPrecedence() = listOf(
        /* (<>, intersect)              */ "a <> b intersect c" to "(bag_op (intersect) (distinct) (ne (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (<>, intersect_all)          */ "a <> b intersect all c" to "(bag_op (intersect) (all) (ne (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (<>, except)                 */ "a <> b except c" to "(bag_op (except) (distinct) (ne (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (<>, except_all)             */ "a <> b except all c" to "(bag_op (except) (all) (ne (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (<>, union)                  */ "a <> b union c" to "(bag_op (union) (distinct) (ne (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (<>, union_all)              */ "a <> b union all c" to "(bag_op (union) (all) (ne (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (<>, or)                     */ "a <> b or c" to "(or (ne (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (<>, and)                    */ "a <> b and c" to "(and (ne (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (<>, =)                      */ "a <> b = c" to "(eq (ne (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (<>, in)                     */ "a <> b in c" to "(in_collection (ne (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (<>, not_in)                 */ "a <> b not in c" to "(not (in_collection (ne (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified))))",
        /* (<>, +)                      */ "a <> b + c" to "(ne (vr (id a (case_insensitive)) (unqualified)) (plus (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (<>, -)                      */ "a <> b - c" to "(ne (vr (id a (case_insensitive)) (unqualified)) (minus (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (<>, ||)                     */ "a <> b || c" to "(ne (vr (id a (case_insensitive)) (unqualified)) (concat (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (<>, *)                      */ "a <> b * c" to "(ne (vr (id a (case_insensitive)) (unqualified)) (times (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (<>, /)                      */ "a <> b / c" to "(ne (vr (id a (case_insensitive)) (unqualified)) (divide (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (<>, %)                      */ "a <> b % c" to "(ne (vr (id a (case_insensitive)) (unqualified)) (modulo (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (<>, is)                     */ "a <> b is boolean" to "(is_type (ne (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (boolean_type))",
        /* (<>, is_not)                 */ "a <> b is not boolean" to "(not (is_type (ne (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (boolean_type)))"
    )

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun isPrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForIsPrecedence() = listOf(
        /* (is, intersect)              */ "a is boolean intersect c" to "(bag_op (intersect) (distinct) (is_type (vr (id a (case_insensitive)) (unqualified)) (boolean_type)) (vr (id c (case_insensitive)) (unqualified)))",
        /* (is, intersect_all)          */ "a is boolean intersect all c" to "(bag_op (intersect) (all) (is_type (vr (id a (case_insensitive)) (unqualified)) (boolean_type)) (vr (id c (case_insensitive)) (unqualified)))",
        /* (is, except)                 */ "a is boolean except c" to "(bag_op (except) (distinct) (is_type (vr (id a (case_insensitive)) (unqualified)) (boolean_type)) (vr (id c (case_insensitive)) (unqualified)))",
        /* (is, except_all)             */ "a is boolean except all c" to "(bag_op (except) (all) (is_type (vr (id a (case_insensitive)) (unqualified)) (boolean_type)) (vr (id c (case_insensitive)) (unqualified)))",
        /* (is, union)                  */ "a is boolean union c" to "(bag_op (union) (distinct) (is_type (vr (id a (case_insensitive)) (unqualified)) (boolean_type)) (vr (id c (case_insensitive)) (unqualified)))",
        /* (is, union_all)              */ "a is boolean union all c" to "(bag_op (union) (all) (is_type (vr (id a (case_insensitive)) (unqualified)) (boolean_type)) (vr (id c (case_insensitive)) (unqualified)))",
        /* (is, or)                     */ "a is boolean or c" to "(or (is_type (vr (id a (case_insensitive)) (unqualified)) (boolean_type)) (vr (id c (case_insensitive)) (unqualified)))",
        /* (is, and)                    */ "a is boolean and c" to "(and (is_type (vr (id a (case_insensitive)) (unqualified)) (boolean_type)) (vr (id c (case_insensitive)) (unqualified)))",
        /* (is, =)                      */ "a is boolean = c" to "(eq (is_type (vr (id a (case_insensitive)) (unqualified)) (boolean_type)) (vr (id c (case_insensitive)) (unqualified)))",
        /* (is, in)                     */ "a is boolean in c" to "(in_collection (is_type (vr (id a (case_insensitive)) (unqualified)) (boolean_type)) (vr (id c (case_insensitive)) (unqualified)))",
        /* (is, not_in)                 */ "a is boolean not in c" to "(not (in_collection (is_type (vr (id a (case_insensitive)) (unqualified)) (boolean_type)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (is, <)                      */ "a is boolean < c" to "(lt (is_type (vr (id a (case_insensitive)) (unqualified)) (boolean_type)) (vr (id c (case_insensitive)) (unqualified)))",
        /* (is, <=)                     */ "a is boolean <= c" to "(lte (is_type (vr (id a (case_insensitive)) (unqualified)) (boolean_type)) (vr (id c (case_insensitive)) (unqualified)))",
        /* (is, >)                      */ "a is boolean > c" to "(gt (is_type (vr (id a (case_insensitive)) (unqualified)) (boolean_type)) (vr (id c (case_insensitive)) (unqualified)))",
        /* (is, >=)                     */ "a is boolean >= c" to "(gte (is_type (vr (id a (case_insensitive)) (unqualified)) (boolean_type)) (vr (id c (case_insensitive)) (unqualified)))",
        /* (is, between)                */ "a is boolean between w and c" to "(between (is_type (vr (id a (case_insensitive)) (unqualified)) (boolean_type)) (vr (id w (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified)))",
        /* (is, not_between)            */ "a is boolean not between y and c" to "(not (between (is_type (vr (id a (case_insensitive)) (unqualified)) (boolean_type)) (vr (id y (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (is, like)                   */ "a is boolean like c" to "(like (is_type (vr (id a (case_insensitive)) (unqualified)) (boolean_type)) (vr (id c (case_insensitive)) (unqualified)) null)",
        /* (is, not_like)               */ "a is boolean not like c" to "(not (like (is_type (vr (id a (case_insensitive)) (unqualified)) (boolean_type)) (vr (id c (case_insensitive)) (unqualified)) null))",
        /* (is, is_not)                 */ "a is boolean is not boolean" to "(not (is_type (is_type (vr (id a (case_insensitive)) (unqualified)) (boolean_type)) (boolean_type)))"
    )

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun isNotPrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForIsNotPrecedence() = listOf(
        /* (not (is, intersect)          */ "a is not boolean intersect c" to "(bag_op (intersect) (distinct) (not (is_type (vr (id a (case_insensitive)) (unqualified)) (boolean_type))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (not (is, intersect_all)      */ "a is not boolean intersect all c" to "(bag_op (intersect) (all) (not (is_type (vr (id a (case_insensitive)) (unqualified)) (boolean_type))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (not (is, except)             */ "a is not boolean except c" to "(bag_op (except) (distinct) (not (is_type (vr (id a (case_insensitive)) (unqualified)) (boolean_type))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (not (is, union)              */ "a is not boolean union c" to "(bag_op (union) (distinct) (not (is_type (vr (id a (case_insensitive)) (unqualified)) (boolean_type))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (not (is, union_all)          */ "a is not boolean union all c" to "(bag_op (union) (all) (not (is_type (vr (id a (case_insensitive)) (unqualified)) (boolean_type))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (not (is, or)                 */ "a is not boolean or c" to "(or (not (is_type (vr (id a (case_insensitive)) (unqualified)) (boolean_type))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (not (is, and)                */ "a is not boolean and c" to "(and (not (is_type (vr (id a (case_insensitive)) (unqualified)) (boolean_type))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (not (is, =)                  */ "a is not boolean = c" to "(eq (not (is_type (vr (id a (case_insensitive)) (unqualified)) (boolean_type))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (not (is, <>)                 */ "a is not boolean <> c" to "(ne (not (is_type (vr (id a (case_insensitive)) (unqualified)) (boolean_type))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (not (is, in)                 */ "a is not boolean in c" to "(in_collection (not (is_type (vr (id a (case_insensitive)) (unqualified)) (boolean_type))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (not (is, not_in)             */ "a is not boolean not in c" to "(not (in_collection (not (is_type (vr (id a (case_insensitive)) (unqualified)) (boolean_type))) (vr (id c (case_insensitive)) (unqualified))))",
        /* (not (is, <)                  */ "a is not boolean < c" to "(lt (not (is_type (vr (id a (case_insensitive)) (unqualified)) (boolean_type))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (not (is, <=)                 */ "a is not boolean <= c" to "(lte (not (is_type (vr (id a (case_insensitive)) (unqualified)) (boolean_type))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (not (is, >)                  */ "a is not boolean > c" to "(gt (not (is_type (vr (id a (case_insensitive)) (unqualified)) (boolean_type))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (not (is, >=)                 */ "a is not boolean >= c" to "(gte (not (is_type (vr (id a (case_insensitive)) (unqualified)) (boolean_type))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (not (is, between)            */ "a is not boolean between w and c" to "(between (not (is_type (vr (id a (case_insensitive)) (unqualified)) (boolean_type))) (vr (id w (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified)))",
        /* (not (is, not_between)        */ "a is not boolean not between y and c" to "(not (between (not (is_type (vr (id a (case_insensitive)) (unqualified)) (boolean_type))) (vr (id y (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (not (is, like)               */ "a is not boolean like c" to "(like (not (is_type (vr (id a (case_insensitive)) (unqualified)) (boolean_type))) (vr (id c (case_insensitive)) (unqualified)) null)",
        /* (not (is, not_like)           */ "a is not boolean not like c" to "(not (like (not (is_type (vr (id a (case_insensitive)) (unqualified)) (boolean_type))) (vr (id c (case_insensitive)) (unqualified)) null))",
        /* (not (is, is)                 */ "a is not boolean is boolean" to "(is_type (not (is_type (vr (id a (case_insensitive)) (unqualified)) (boolean_type))) (boolean_type))"
    )

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun inPrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForInPrecedence() = listOf(
        /* (in, intersect)              */ "a in b intersect c" to "(bag_op (intersect) (distinct) (in_collection (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (in, intersect_all)          */ "a in b intersect all c" to "(bag_op (intersect) (all) (in_collection (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (in, except)                 */ "a in b except c" to "(bag_op (except) (distinct) (in_collection (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (in, except_all)             */ "a in b except all c" to "(bag_op (except) (all) (in_collection (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (in, union)                  */ "a in b union c" to "(bag_op (union) (distinct) (in_collection (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (in, union_all)              */ "a in b union all c" to "(bag_op (union) (all) (in_collection (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (in, or)                     */ "a in b or c" to "(or (in_collection (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (in, and)                    */ "a in b and c" to "(and (in_collection (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (in, =)                      */ "a in b = c" to "(eq (in_collection (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (in, <>)                     */ "a in b <> c" to "(ne (in_collection (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (in, not_in)                 */ "a in b not in c" to "(not (in_collection (in_collection (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified))))",
        /* (in, +)                      */ "a in b + c" to "(in_collection (vr (id a (case_insensitive)) (unqualified)) (plus (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (in, -)                      */ "a in b - c" to "(in_collection (vr (id a (case_insensitive)) (unqualified)) (minus (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (in, ||)                     */ "a in b || c" to "(in_collection (vr (id a (case_insensitive)) (unqualified)) (concat (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (in, *)                      */ "a in b * c" to "(in_collection (vr (id a (case_insensitive)) (unqualified)) (times (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (in, /)                      */ "a in b / c" to "(in_collection (vr (id a (case_insensitive)) (unqualified)) (divide (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (in, %)                      */ "a in b % c" to "(in_collection (vr (id a (case_insensitive)) (unqualified)) (modulo (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (in, is)                     */ "a in b is boolean" to "(is_type (in_collection (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (boolean_type))",
        /* (in, is_not)                 */ "a in b is not boolean" to "(not (is_type (in_collection (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (boolean_type)))"
    )

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun notInPrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForNotInPrecedence() = listOf(
        /* (not (in, intersect)          */ "a not in b intersect c" to "(bag_op (intersect) (distinct) (not (in_collection (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (not (in, intersect_all)      */ "a not in b intersect all c" to "(bag_op (intersect) (all) (not (in_collection (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (not (in, except)             */ "a not in b except c" to "(bag_op (except) (distinct) (not (in_collection (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (not (in, except_all)         */ "a not in b except all c" to "(bag_op (except) (all) (not (in_collection (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (not (in, union)              */ "a not in b union c" to "(bag_op (union) (distinct) (not (in_collection (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (not (in, union_all)          */ "a not in b union all c" to "(bag_op (union) (all) (not (in_collection (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (not (in, or)                 */ "a not in b or c" to "(or (not (in_collection (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (not (in, and)                */ "a not in b and c" to "(and (not (in_collection (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (not (in, =)                  */ "a not in b = c" to "(eq (not (in_collection (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (not (in, <>)                 */ "a not in b <> c" to "(ne (not (in_collection (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (not (in, in)                 */ "a not in b in c" to "(in_collection (not (in_collection (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (not (in, +)                  */ "a not in b + c" to "(not (in_collection (vr (id a (case_insensitive)) (unqualified)) (plus (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified)))))",
        /* (not (in, -)                  */ "a not in b - c" to "(not (in_collection (vr (id a (case_insensitive)) (unqualified)) (minus (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified)))))",
        /* (not (in, ||)                 */ "a not in b || c" to "(not (in_collection (vr (id a (case_insensitive)) (unqualified)) (concat (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified)))))",
        /* (not (in, *)                  */ "a not in b * c" to "(not (in_collection (vr (id a (case_insensitive)) (unqualified)) (times (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified)))))",
        /* (not (in, /)                  */ "a not in b / c" to "(not (in_collection (vr (id a (case_insensitive)) (unqualified)) (divide (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified)))))",
        /* (not (in, %)                  */ "a not in b % c" to "(not (in_collection (vr (id a (case_insensitive)) (unqualified)) (modulo (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified)))))",
        /* (not (in, is)                 */ "a not in b is boolean" to "(is_type (not (in_collection (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)))) (boolean_type))",
        /* (not (in, is_not)             */ "a not in b is not boolean" to "(not (is_type (not (in_collection (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)))) (boolean_type)))"
    )

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun ltPrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForLtPrecedence() = listOf(
        /* (<, intersect)               */ "a < b intersect c" to "(bag_op (intersect) (distinct) (lt (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (<, intersect_all)           */ "a < b intersect all c" to "(bag_op (intersect) (all) (lt (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (<, except)                  */ "a < b except c" to "(bag_op (except) (distinct) (lt (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (<, except_all)              */ "a < b except all c" to "(bag_op (except) (all) (lt (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (<, union)                   */ "a < b union c" to "(bag_op (union) (distinct) (lt (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (<, union_all)               */ "a < b union all c" to "(bag_op (union) (all) (lt (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (<, or)                      */ "a < b or c" to "(or (lt (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (<, and)                     */ "a < b and c" to "(and (lt (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (<, =)                       */ "a < b = c" to "(eq (lt (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (<, <>)                      */ "a < b <> c" to "(ne (lt (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (<, in)                      */ "a < b in c" to "(in_collection (lt (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (<, not_in)                  */ "a < b not in c" to "(not (in_collection (lt (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified))))",
        /* (<, <=)                      */ "a < b <= c" to "(lte (lt (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (<, >)                       */ "a < b > c" to "(gt (lt (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (<, >=)                      */ "a < b >= c" to "(gte (lt (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (<, between)                 */ "a < b between w and c" to "(between (lt (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id w (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified)))",
        /* (<, not_between)             */ "a < b not between y and c" to "(not (between (lt (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id y (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (<, like)                    */ "a < b like c" to "(like (lt (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)) null)",
        /* (<, not_like)                */ "a < b not like c" to "(not (like (lt (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)) null))",
        /* (<, +)                       */ "a < b + c" to "(lt (vr (id a (case_insensitive)) (unqualified)) (plus (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (<, -)                       */ "a < b - c" to "(lt (vr (id a (case_insensitive)) (unqualified)) (minus (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (<, ||)                      */ "a < b || c" to "(lt (vr (id a (case_insensitive)) (unqualified)) (concat (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (<, *)                       */ "a < b * c" to "(lt (vr (id a (case_insensitive)) (unqualified)) (times (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (<, /)                       */ "a < b / c" to "(lt (vr (id a (case_insensitive)) (unqualified)) (divide (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (<, %)                       */ "a < b % c" to "(lt (vr (id a (case_insensitive)) (unqualified)) (modulo (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (<, is)                      */ "a < b is boolean" to "(is_type (lt (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (boolean_type))",
        /* (<, is_not)                  */ "a < b is not boolean" to "(not (is_type (lt (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (boolean_type)))"
    )

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun ltePrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForLtePrecedence() = listOf(
        /* (<=, intersect)              */ "a <= b intersect c" to "(bag_op (intersect) (distinct) (lte (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (<=, intersect_all)          */ "a <= b intersect all c" to "(bag_op (intersect) (all) (lte (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (<=, except)                 */ "a <= b except c" to "(bag_op (except) (distinct) (lte (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (<=, except_all)             */ "a <= b except all c" to "(bag_op (except) (all) (lte (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (<=, union)                  */ "a <= b union c" to "(bag_op (union) (distinct) (lte (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (<=, union_all)              */ "a <= b union all c" to "(bag_op (union) (all) (lte (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (<=, or)                     */ "a <= b or c" to "(or (lte (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (<=, and)                    */ "a <= b and c" to "(and (lte (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (<=, =)                      */ "a <= b = c" to "(eq (lte (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (<=, <>)                     */ "a <= b <> c" to "(ne (lte (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (<=, in)                     */ "a <= b in c" to "(in_collection (lte (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (<=, not_in)                 */ "a <= b not in c" to "(not (in_collection (lte (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified))))",
        /* (<=, <)                      */ "a <= b < c" to "(lt (lte (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (<=, >)                      */ "a <= b > c" to "(gt (lte (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (<=, >=)                     */ "a <= b >= c" to "(gte (lte (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (<=, between)                */ "a <= b between w and c" to "(between (lte (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id w (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified)))",
        /* (<=, not_between)            */ "a <= b not between y and c" to "(not (between (lte (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id y (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (<=, like)                   */ "a <= b like c" to "(like (lte (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)) null)",
        /* (<=, not_like)               */ "a <= b not like c" to "(not (like (lte (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)) null))",
        /* (<=, +)                      */ "a <= b + c" to "(lte (vr (id a (case_insensitive)) (unqualified)) (plus (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (<=, -)                      */ "a <= b - c" to "(lte (vr (id a (case_insensitive)) (unqualified)) (minus (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (<=, ||)                     */ "a <= b || c" to "(lte (vr (id a (case_insensitive)) (unqualified)) (concat (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (<=, *)                      */ "a <= b * c" to "(lte (vr (id a (case_insensitive)) (unqualified)) (times (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (<=, /)                      */ "a <= b / c" to "(lte (vr (id a (case_insensitive)) (unqualified)) (divide (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (<=, %)                      */ "a <= b % c" to "(lte (vr (id a (case_insensitive)) (unqualified)) (modulo (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (<=, is)                     */ "a <= b is boolean" to "(is_type (lte (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (boolean_type))",
        /* (<=, is_not)                 */ "a <= b is not boolean" to "(not (is_type (lte (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (boolean_type)))"
    )

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun gtPrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForGtPrecedence() = listOf(
        /* (>, intersect)               */ "a > b intersect c" to "(bag_op (intersect) (distinct) (gt (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (>, intersect_all)           */ "a > b intersect all c" to "(bag_op (intersect) (all) (gt (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (>, except)                  */ "a > b except c" to "(bag_op (except) (distinct) (gt (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (>, except_all)              */ "a > b except all c" to "(bag_op (except) (all) (gt (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (>, union)                   */ "a > b union c" to "(bag_op (union) (distinct) (gt (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (>, union_all)               */ "a > b union all c" to "(bag_op (union) (all) (gt (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (>, or)                      */ "a > b or c" to "(or (gt (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (>, and)                     */ "a > b and c" to "(and (gt (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (>, =)                       */ "a > b = c" to "(eq (gt (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (>, <>)                      */ "a > b <> c" to "(ne (gt (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (>, in)                      */ "a > b in c" to "(in_collection (gt (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (>, not_in)                  */ "a > b not in c" to "(not (in_collection (gt (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified))))",
        /* (>, <)                       */ "a > b < c" to "(lt (gt (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (>, <=)                      */ "a > b <= c" to "(lte (gt (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (>, >=)                      */ "a > b >= c" to "(gte (gt (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (>, between)                 */ "a > b between w and c" to "(between (gt (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id w (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified)))",
        /* (>, not_between)             */ "a > b not between y and c" to "(not (between (gt (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id y (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (>, like)                    */ "a > b like c" to "(like (gt (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)) null)",
        /* (>, not_like)                */ "a > b not like c" to "(not (like (gt (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)) null))",
        /* (>, +)                       */ "a > b + c" to "(gt (vr (id a (case_insensitive)) (unqualified)) (plus (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (>, -)                       */ "a > b - c" to "(gt (vr (id a (case_insensitive)) (unqualified)) (minus (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (>, ||)                      */ "a > b || c" to "(gt (vr (id a (case_insensitive)) (unqualified)) (concat (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (>, *)                       */ "a > b * c" to "(gt (vr (id a (case_insensitive)) (unqualified)) (times (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (>, /)                       */ "a > b / c" to "(gt (vr (id a (case_insensitive)) (unqualified)) (divide (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (>, %)                       */ "a > b % c" to "(gt (vr (id a (case_insensitive)) (unqualified)) (modulo (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (>, is)                      */ "a > b is boolean" to "(is_type (gt (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (boolean_type))",
        /* (>, is_not)                  */ "a > b is not boolean" to "(not (is_type (gt (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (boolean_type)))"
    )

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun gtePrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForGtePrecedence() = listOf(
        /* (>=, intersect)              */ "a >= b intersect c" to "(bag_op (intersect) (distinct) (gte (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (>=, intersect_all)          */ "a >= b intersect all c" to "(bag_op (intersect) (all) (gte (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (>=, except)                 */ "a >= b except c" to "(bag_op (except) (distinct) (gte (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (>=, except_all)             */ "a >= b except all c" to "(bag_op (except) (all) (gte (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (>=, union)                  */ "a >= b union c" to "(bag_op (union) (distinct) (gte (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (>=, union_all)              */ "a >= b union all c" to "(bag_op (union) (all) (gte (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (>=, or)                     */ "a >= b or c" to "(or (gte (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (>=, and)                    */ "a >= b and c" to "(and (gte (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (>=, =)                      */ "a >= b = c" to "(eq (gte (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (>=, <>)                     */ "a >= b <> c" to "(ne (gte (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (>=, in)                     */ "a >= b in c" to "(in_collection (gte (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (>=, not_in)                 */ "a >= b not in c" to "(not (in_collection (gte (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified))))",
        /* (>=, <)                      */ "a >= b < c" to "(lt (gte (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (>=, <=)                     */ "a >= b <= c" to "(lte (gte (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (>=, >)                      */ "a >= b > c" to "(gt (gte (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (>=, between)                */ "a >= b between w and c" to "(between (gte (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id w (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified)))",
        /* (>=, not_between)            */ "a >= b not between y and c" to "(not (between (gte (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id y (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (>=, like)                   */ "a >= b like c" to "(like (gte (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)) null)",
        /* (>=, not_like)               */ "a >= b not like c" to "(not (like (gte (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)) null))",
        /* (>=, +)                      */ "a >= b + c" to "(gte (vr (id a (case_insensitive)) (unqualified)) (plus (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (>=, -)                      */ "a >= b - c" to "(gte (vr (id a (case_insensitive)) (unqualified)) (minus (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (>=, ||)                     */ "a >= b || c" to "(gte (vr (id a (case_insensitive)) (unqualified)) (concat (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (>=, *)                      */ "a >= b * c" to "(gte (vr (id a (case_insensitive)) (unqualified)) (times (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (>=, /)                      */ "a >= b / c" to "(gte (vr (id a (case_insensitive)) (unqualified)) (divide (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (>=, %)                      */ "a >= b % c" to "(gte (vr (id a (case_insensitive)) (unqualified)) (modulo (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (>=, is)                     */ "a >= b is boolean" to "(is_type (gte (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (boolean_type))",
        /* (>=, is_not)                 */ "a >= b is not boolean" to "(not (is_type (gte (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (boolean_type)))"
    )

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun betweenPrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForBetweenPrecedence() = listOf(
        /* (between, intersect)         */ "a between b and w intersect c" to "(bag_op (intersect) (distinct) (between (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)) (vr (id w (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (between, intersect_all)     */ "a between b and w intersect all c" to "(bag_op (intersect) (all) (between (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)) (vr (id w (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (between, except)            */ "a between b and w except c" to "(bag_op (except) (distinct) (between (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)) (vr (id w (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (between, except_all)        */ "a between b and w except all c" to "(bag_op (except) (all) (between (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)) (vr (id w (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (between, union)             */ "a between b and w union c" to "(bag_op (union) (distinct) (between (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)) (vr (id w (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (between, union_all)         */ "a between b and w union all c" to "(bag_op (union) (all) (between (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)) (vr (id w (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (between, or)                */ "a between w and b or c" to "(or (between (vr (id a (case_insensitive)) (unqualified)) (vr (id w (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (between, and)               */ "a between w and b and c" to "(and (between (vr (id a (case_insensitive)) (unqualified)) (vr (id w (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (between, =)                 */ "a between w and b = c" to "(eq (between (vr (id a (case_insensitive)) (unqualified)) (vr (id w (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (between, <>)                */ "a between w and b <> c" to "(ne (between (vr (id a (case_insensitive)) (unqualified)) (vr (id w (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (between, in)                */ "a between w and b in c" to "(in_collection (between (vr (id a (case_insensitive)) (unqualified)) (vr (id w (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (between, not_in)            */ "a between w and b not in c" to "(not (in_collection (between (vr (id a (case_insensitive)) (unqualified)) (vr (id w (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified))))",
        /* (between, <)                 */ "a between w and b < c" to "(lt (between (vr (id a (case_insensitive)) (unqualified)) (vr (id w (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (between, <=)                */ "a between w and b <= c" to "(lte (between (vr (id a (case_insensitive)) (unqualified)) (vr (id w (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (between, >)                 */ "a between w and b > c" to "(gt (between (vr (id a (case_insensitive)) (unqualified)) (vr (id w (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (between, >=)                */ "a between w and b >= c" to "(gte (between (vr (id a (case_insensitive)) (unqualified)) (vr (id w (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (between, not_between)       */ "a between w and b not between y and c" to "(not (between (between (vr (id a (case_insensitive)) (unqualified)) (vr (id w (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id y (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (between, like)              */ "a between w and b like c" to "(like (between (vr (id a (case_insensitive)) (unqualified)) (vr (id w (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)) null)",
        /* (between, not_like)          */ "a between w and b not like c" to "(not (like (between (vr (id a (case_insensitive)) (unqualified)) (vr (id w (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)) null))",
        /* (between, +)                 */ "a between w and b + c" to "(between (vr (id a (case_insensitive)) (unqualified)) (vr (id w (case_insensitive)) (unqualified))  (plus (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (between, -)                 */ "a between w and b - c" to "(between (vr (id a (case_insensitive)) (unqualified)) (vr (id w (case_insensitive)) (unqualified))  (minus (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (between, ||)                */ "a between w and b || c" to "(between (vr (id a (case_insensitive)) (unqualified)) (vr (id w (case_insensitive)) (unqualified))  (concat (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (between, *)                 */ "a between w and b * c" to "(between (vr (id a (case_insensitive)) (unqualified)) (vr (id w (case_insensitive)) (unqualified))  (times (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (between, /)                 */ "a between w and b / c" to "(between (vr (id a (case_insensitive)) (unqualified)) (vr (id w (case_insensitive)) (unqualified))  (divide (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (between, %)                 */ "a between w and b % c" to "(between (vr (id a (case_insensitive)) (unqualified)) (vr (id w (case_insensitive)) (unqualified))  (modulo (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (between, is)                */ "a between w and b is boolean" to "(is_type (between (vr (id a (case_insensitive)) (unqualified)) (vr (id w (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (boolean_type))",
        /* (between, is_not)            */ "a between w and b is not boolean" to "(not (is_type (between (vr (id a (case_insensitive)) (unqualified)) (vr (id w (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (boolean_type)))"
    )

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun notBetweenPrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForNotBetweenPrecedence() = listOf(
        /* (not (between, intersect)     */ "a not between w and b intersect c" to "(bag_op (intersect) (distinct) (not (between (vr (id a (case_insensitive)) (unqualified)) (vr (id w (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (not (between, intersect_all) */ "a not between w and b intersect all c" to "(bag_op (intersect) (all) (not (between (vr (id a (case_insensitive)) (unqualified)) (vr (id w (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (not (between, except)        */ "a not between w and b except c" to "(bag_op (except) (distinct) (not (between (vr (id a (case_insensitive)) (unqualified)) (vr (id w (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (not (between, except_all)    */ "a not between w and b except all c" to "(bag_op (except) (all) (not (between (vr (id a (case_insensitive)) (unqualified)) (vr (id w (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (not (between, union)         */ "a not between w and b union c" to "(bag_op (union) (distinct) (not (between (vr (id a (case_insensitive)) (unqualified)) (vr (id w (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (not (between, union_all)     */ "a not between w and b union all c" to "(bag_op (union) (all) (not (between (vr (id a (case_insensitive)) (unqualified)) (vr (id w (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (not (between, or)            */ "a not between y and b or c" to "(or (not (between (vr (id a (case_insensitive)) (unqualified)) (vr (id y (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (not (between, and)           */ "a not between y and b and c" to "(and (not (between (vr (id a (case_insensitive)) (unqualified)) (vr (id y (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (not (between, =)             */ "a not between y and b = c" to "(eq (not (between (vr (id a (case_insensitive)) (unqualified)) (vr (id y (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (not (between, <>)            */ "a not between y and b <> c" to "(ne (not (between (vr (id a (case_insensitive)) (unqualified)) (vr (id y (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (not (between, in)            */ "a not between y and b in c" to "(in_collection (not (between (vr (id a (case_insensitive)) (unqualified)) (vr (id y (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (not (between, not_in)        */ "a not between y and b not in c" to "(not (in_collection (not (between (vr (id a (case_insensitive)) (unqualified)) (vr (id y (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)))) (vr (id c (case_insensitive)) (unqualified))))",
        /* (not (between, <)             */ "a not between y and b < c" to "(lt (not (between (vr (id a (case_insensitive)) (unqualified)) (vr (id y (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (not (between, <=)            */ "a not between y and b <= c" to "(lte (not (between (vr (id a (case_insensitive)) (unqualified)) (vr (id y (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (not (between, >)             */ "a not between y and b > c" to "(gt (not (between (vr (id a (case_insensitive)) (unqualified)) (vr (id y (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (not (between, >=)            */ "a not between y and b >= c" to "(gte (not (between (vr (id a (case_insensitive)) (unqualified)) (vr (id y (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (not (between, between)       */ "a not between y and b between w and c" to "(between (not (between (vr (id a (case_insensitive)) (unqualified)) (vr (id y (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)))) (vr (id w (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified)))",
        /* (not (between, like)          */ "a not between y and b like c" to "(like (not (between (vr (id a (case_insensitive)) (unqualified)) (vr (id y (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)))) (vr (id c (case_insensitive)) (unqualified)) null)",
        /* (not (between, not_like)      */ "a not between y and b not like c" to "(not (like (not (between (vr (id a (case_insensitive)) (unqualified)) (vr (id y (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)))) (vr (id c (case_insensitive)) (unqualified)) null))",
        /* (not (between, +)             */ "a not between y and b + c" to "(not (between (vr (id a (case_insensitive)) (unqualified)) (vr (id y (case_insensitive)) (unqualified)) (plus (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified)))))",
        /* (not (between, -)             */ "a not between y and b - c" to "(not (between (vr (id a (case_insensitive)) (unqualified)) (vr (id y (case_insensitive)) (unqualified)) (minus (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified)))))",
        /* (not (between, ||)            */ "a not between y and b || c" to "(not (between (vr (id a (case_insensitive)) (unqualified)) (vr (id y (case_insensitive)) (unqualified)) (concat (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified)))))",
        /* (not (between, *)             */ "a not between y and b * c" to "(not (between (vr (id a (case_insensitive)) (unqualified)) (vr (id y (case_insensitive)) (unqualified)) (times (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified)))))",
        /* (not (between, /)             */ "a not between y and b / c" to "(not (between (vr (id a (case_insensitive)) (unqualified)) (vr (id y (case_insensitive)) (unqualified)) (divide (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified)))))",
        /* (not (between, %)             */ "a not between y and b % c" to "(not (between (vr (id a (case_insensitive)) (unqualified)) (vr (id y (case_insensitive)) (unqualified)) (modulo (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified)))))",
        /* (not (between, is)            */ "a not between y and b is boolean" to "(is_type (not (between (vr (id a (case_insensitive)) (unqualified)) (vr (id y (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)))) (boolean_type))",
        /* (not (between, is_not)        */ "a not between y and b is not boolean" to "(not (is_type (not (between (vr (id a (case_insensitive)) (unqualified)) (vr (id y (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)))) (boolean_type)))"
    )

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun likePrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForLikePrecedence() = listOf(
        /* (like, intersect)            */ "a like b intersect c" to "(bag_op (intersect) (distinct) (like (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)) null) (vr (id c (case_insensitive)) (unqualified)))",
        /* (like, intersect_all)        */ "a like b intersect all c" to "(bag_op (intersect) (all) (like (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)) null) (vr (id c (case_insensitive)) (unqualified)))",
        /* (like, except)               */ "a like b except c" to "(bag_op (except) (distinct) (like (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)) null) (vr (id c (case_insensitive)) (unqualified)))",
        /* (like, except_all)           */ "a like b except all c" to "(bag_op (except) (all) (like (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)) null) (vr (id c (case_insensitive)) (unqualified)))",
        /* (like, union)                */ "a like b union c" to "(bag_op (union) (distinct) (like (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)) null) (vr (id c (case_insensitive)) (unqualified)))",
        /* (like, union_all)            */ "a like b union all c" to "(bag_op (union) (all) (like (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)) null) (vr (id c (case_insensitive)) (unqualified)))",
        /* (like, or)                   */ "a like b or c" to "(or (like (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)) null) (vr (id c (case_insensitive)) (unqualified)))",
        /* (like, and)                  */ "a like b and c" to "(and (like (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)) null) (vr (id c (case_insensitive)) (unqualified)))",
        /* (like, =)                    */ "a like b = c" to "(eq (like (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)) null) (vr (id c (case_insensitive)) (unqualified)))",
        /* (like, <>)                   */ "a like b <> c" to "(ne (like (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)) null) (vr (id c (case_insensitive)) (unqualified)))",
        /* (like, in)                   */ "a like b in c" to "(in_collection (like (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)) null) (vr (id c (case_insensitive)) (unqualified)))",
        /* (like, not_in)               */ "a like b not in c" to "(not (in_collection (like (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)) null) (vr (id c (case_insensitive)) (unqualified))))",
        /* (like, <)                    */ "a like b < c" to "(lt (like (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)) null) (vr (id c (case_insensitive)) (unqualified)))",
        /* (like, <=)                   */ "a like b <= c" to "(lte (like (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)) null) (vr (id c (case_insensitive)) (unqualified)))",
        /* (like, >)                    */ "a like b > c" to "(gt (like (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)) null) (vr (id c (case_insensitive)) (unqualified)))",
        /* (like, >=)                   */ "a like b >= c" to "(gte (like (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)) null) (vr (id c (case_insensitive)) (unqualified)))",
        /* (like, between)              */ "a like b between w and c" to "(between (like (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)) null) (vr (id w (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified)))",
        /* (like, not_between)          */ "a like b not between y and c" to "(not (between (like (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)) null) (vr (id y (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (like, not_like)             */ "a like b not like c" to "(not (like (like (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)) null) (vr (id c (case_insensitive)) (unqualified)) null))",
        /* (like, +)                    */ "a like b + c" to "(like (vr (id a (case_insensitive)) (unqualified)) (plus (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))) null)",
        /* (like, -)                    */ "a like b - c" to "(like (vr (id a (case_insensitive)) (unqualified)) (minus (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))) null)",
        /* (like, ||)                   */ "a like b || c" to "(like (vr (id a (case_insensitive)) (unqualified)) (concat (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))) null)",
        /* (like, *)                    */ "a like b * c" to "(like (vr (id a (case_insensitive)) (unqualified)) (times (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))) null)",
        /* (like, /)                    */ "a like b / c" to "(like (vr (id a (case_insensitive)) (unqualified)) (divide (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))) null)",
        /* (like, %)                    */ "a like b % c" to "(like (vr (id a (case_insensitive)) (unqualified)) (modulo (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))) null)",
        /* (like, is)                   */ "a like b is boolean" to "(is_type (like (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)) null) (boolean_type))",
        /* (like, is_not)               */ "a like b is not boolean" to "(not (is_type (like (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)) null) (boolean_type)))"
    )

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun notLikePrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForNotLikePrecedence() = listOf(
        /* (not (like, intersect)        */ "a not like b intersect c" to "(bag_op (intersect) (distinct) (not (like (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)) null)) (vr (id c (case_insensitive)) (unqualified)))",
        /* (not (like, intersect_all)    */ "a not like b intersect all c" to "(bag_op (intersect) (all) (not (like (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)) null)) (vr (id c (case_insensitive)) (unqualified)))",
        /* (not (like, except)           */ "a not like b except c" to "(bag_op (except) (distinct) (not (like (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)) null)) (vr (id c (case_insensitive)) (unqualified)))",
        /* (not (like, except_all)       */ "a not like b except all c" to "(bag_op (except) (all) (not (like (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)) null)) (vr (id c (case_insensitive)) (unqualified)))",
        /* (not (like, union)            */ "a not like b union c" to "(bag_op (union) (distinct) (not (like (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)) null)) (vr (id c (case_insensitive)) (unqualified)))",
        /* (not (like, union_all)        */ "a not like b union all c" to "(bag_op (union) (all) (not (like (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)) null)) (vr (id c (case_insensitive)) (unqualified)))",
        /* (not (like, or)               */ "a not like b or c" to "(or (not (like (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)) null)) (vr (id c (case_insensitive)) (unqualified)))",
        /* (not (like, and)              */ "a not like b and c" to "(and (not (like (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)) null)) (vr (id c (case_insensitive)) (unqualified)))",
        /* (not (like, =)                */ "a not like b = c" to "(eq (not (like (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)) null)) (vr (id c (case_insensitive)) (unqualified)))",
        /* (not (like, <>)               */ "a not like b <> c" to "(ne (not (like (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)) null)) (vr (id c (case_insensitive)) (unqualified)))",
        /* (not (like, in)               */ "a not like b in c" to "(in_collection (not (like (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)) null)) (vr (id c (case_insensitive)) (unqualified)))",
        /* (not (like, not_in)           */ "a not like b not in c" to "(not (in_collection (not (like (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)) null)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (not (like, <)                */ "a not like b < c" to "(lt (not (like (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)) null)) (vr (id c (case_insensitive)) (unqualified)))",
        /* (not (like, <=)               */ "a not like b <= c" to "(lte (not (like (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)) null)) (vr (id c (case_insensitive)) (unqualified)))",
        /* (not (like, >)                */ "a not like b > c" to "(gt (not (like (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)) null)) (vr (id c (case_insensitive)) (unqualified)))",
        /* (not (like, >=)               */ "a not like b >= c" to "(gte (not (like (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)) null)) (vr (id c (case_insensitive)) (unqualified)))",
        /* (not (like, between)          */ "a not like b between w and c" to "(between (not (like (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)) null)) (vr (id w (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified)))",
        /* (not (like, not_between)      */ "a not like b not between y and c" to "(not (between (not (like (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)) null)) (vr (id y (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (not (like, like)             */ "a not like b like c" to "(like (not (like (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)) null)) (vr (id c (case_insensitive)) (unqualified)) null)",
        /* (not (like, +)                */ "a not like b + c" to "(not (like (vr (id a (case_insensitive)) (unqualified)) (plus (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))) null))",
        /* (not (like, -)                */ "a not like b - c" to "(not (like (vr (id a (case_insensitive)) (unqualified)) (minus (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))) null))",
        /* (not (like, ||)               */ "a not like b || c" to "(not (like (vr (id a (case_insensitive)) (unqualified)) (concat (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))) null))",
        /* (not (like, *)                */ "a not like b * c" to "(not (like (vr (id a (case_insensitive)) (unqualified)) (times (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))) null))",
        /* (not (like, /)                */ "a not like b / c" to "(not (like (vr (id a (case_insensitive)) (unqualified)) (divide (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))) null))",
        /* (not (like, %)                */ "a not like b % c" to "(not (like (vr (id a (case_insensitive)) (unqualified)) (modulo (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))) null))",
        /* (not (like, is)               */ "a not like b is boolean" to "(is_type (not (like (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)) null)) (boolean_type))",
        /* (not (like, is_not)           */ "a not like b is not boolean" to "(not (is_type (not (like (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)) null)) (boolean_type)))"
    )

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun subtractPrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForSubtractPrecedence() = listOf(
        /* (+, intersect)               */ "a + b intersect c" to "(bag_op (intersect) (distinct) (plus (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (+, intersect_all)           */ "a + b intersect all c" to "(bag_op (intersect) (all) (plus (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (+, except)                  */ "a + b except c" to "(bag_op (except) (distinct) (plus (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (+, except_all)              */ "a + b except all c" to "(bag_op (except) (all) (plus (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (+, union)                   */ "a + b union c" to "(bag_op (union) (distinct) (plus (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (+, union_all)               */ "a + b union all c" to "(bag_op (union) (all) (plus (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (+, or)                      */ "a + b or c" to "(or (plus (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (+, and)                     */ "a + b and c" to "(and (plus (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (+, =)                       */ "a + b = c" to "(eq (plus (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (+, <>)                      */ "a + b <> c" to "(ne (plus (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (+, in)                      */ "a + b in c" to "(in_collection (plus (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (+, not_in)                  */ "a + b not in c" to "(not (in_collection (plus (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified))))",
        /* (+, <)                       */ "a + b < c" to "(lt (plus (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (+, <=)                      */ "a + b <= c" to "(lte (plus (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (+, >)                       */ "a + b > c" to "(gt (plus (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (+, >=)                      */ "a + b >= c" to "(gte (plus (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (+, between)                 */ "a + b between w and c" to "(between (plus (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id w (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified)))",
        /* (+, not_between)             */ "a + b not between y and c" to "(not (between (plus (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id y (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (+, like)                    */ "a + b like c" to "(like (plus (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)) null)",
        /* (+, not_like)                */ "a + b not like c" to "(not (like (plus (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)) null))",
        /* (+, -)                       */ "a + b - c" to "(minus (plus (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (+, ||)                      */ "a + b || c" to "(concat (plus (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (+, *)                       */ "a + b * c" to "(plus (vr (id a (case_insensitive)) (unqualified)) (times (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (+, /)                       */ "a + b / c" to "(plus (vr (id a (case_insensitive)) (unqualified)) (divide (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (+, %)                       */ "a + b % c" to "(plus (vr (id a (case_insensitive)) (unqualified)) (modulo (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (+, is)                      */ "a + b is boolean" to "(is_type (plus (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (boolean_type))",
        /* (+, is_not)                  */ "a + b is not boolean" to "(not (is_type (plus (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (boolean_type)))"
    )

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun minusPrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForMinusPrecedence() = listOf(
        /* (-, intersect)               */ "a - b intersect c" to "(bag_op (intersect) (distinct) (minus (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (-, intersect_all)           */ "a - b intersect all c" to "(bag_op (intersect) (all) (minus (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (-, except)                  */ "a - b except c" to "(bag_op (except) (distinct) (minus (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (-, except_all)              */ "a - b except all c" to "(bag_op (except) (all) (minus (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (-, union)                   */ "a - b union c" to "(bag_op (union) (distinct) (minus (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (-, union_all)               */ "a - b union all c" to "(bag_op (union) (all) (minus (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (-, or)                      */ "a - b or c" to "(or (minus (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (-, and)                     */ "a - b and c" to "(and (minus (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (-, =)                       */ "a - b = c" to "(eq (minus (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (-, <>)                      */ "a - b <> c" to "(ne (minus (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (-, in)                      */ "a - b in c" to "(in_collection (minus (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (-, not_in)                  */ "a - b not in c" to "(not (in_collection (minus (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified))))",
        /* (-, <)                       */ "a - b < c" to "(lt (minus (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (-, <=)                      */ "a - b <= c" to "(lte (minus (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (-, >)                       */ "a - b > c" to "(gt (minus (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (-, >=)                      */ "a - b >= c" to "(gte (minus (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (-, between)                 */ "a - b between w and c" to "(between (minus (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id w (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified)))",
        /* (-, not_between)             */ "a - b not between y and c" to "(not (between (minus (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id y (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (-, like)                    */ "a - b like c" to "(like (minus (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)) null)",
        /* (-, not_like)                */ "a - b not like c" to "(not (like (minus (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)) null))",
        /* (-, +)                       */ "a - b + c" to "(plus (minus (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (-, ||)                      */ "a - b || c" to "(concat (minus (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (-, *)                       */ "a - b * c" to "(minus (vr (id a (case_insensitive)) (unqualified)) (times (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (-, /)                       */ "a - b / c" to "(minus (vr (id a (case_insensitive)) (unqualified)) (divide (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (-, %)                       */ "a - b % c" to "(minus (vr (id a (case_insensitive)) (unqualified)) (modulo (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (-, is)                      */ "a - b is boolean" to "(is_type (minus (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (boolean_type))",
        /* (-, is_not)                  */ "a - b is not boolean" to "(not (is_type (minus (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (boolean_type)))"
    )

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun concatPrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForConcatPrecedence() = listOf(
        /* (||, intersect)              */ "a || b intersect c" to "(bag_op (intersect) (distinct) (concat (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (||, intersect_all)          */ "a || b intersect all c" to "(bag_op (intersect) (all) (concat (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (||, except)                 */ "a || b except c" to "(bag_op (except) (distinct) (concat (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (||, except_all)             */ "a || b except all c" to "(bag_op (except) (all) (concat (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (||, union)                  */ "a || b union c" to "(bag_op (union) (distinct) (concat (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (||, union_all)              */ "a || b union all c" to "(bag_op (union) (all) (concat (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (||, or)                     */ "a || b or c" to "(or (concat (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (||, and)                    */ "a || b and c" to "(and (concat (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (||, =)                      */ "a || b = c" to "(eq (concat (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (||, <>)                     */ "a || b <> c" to "(ne (concat (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (||, in)                     */ "a || b in c" to "(in_collection (concat (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (||, not_in)                 */ "a || b not in c" to "(not (in_collection (concat (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified))))",
        /* (||, <)                      */ "a || b < c" to "(lt (concat (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (||, <=)                     */ "a || b <= c" to "(lte (concat (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (||, >)                      */ "a || b > c" to "(gt (concat (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (||, >=)                     */ "a || b >= c" to "(gte (concat (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (||, between)                */ "a || b between w and c" to "(between (concat (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id w (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified)))",
        /* (||, not_between)            */ "a || b not between y and c" to "(not (between (concat (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id y (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (||, like)                   */ "a || b like c" to "(like (concat (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)) null)",
        /* (||, not_like)               */ "a || b not like c" to "(not (like (concat (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)) null))",
        /* (||, *)                      */ "a || b * c" to "(concat (vr (id a (case_insensitive)) (unqualified)) (times (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (||, /)                      */ "a || b / c" to "(concat (vr (id a (case_insensitive)) (unqualified)) (divide (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (||, %)                      */ "a || b % c" to "(concat (vr (id a (case_insensitive)) (unqualified)) (modulo (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (||, is)                     */ "a || b is boolean" to "(is_type (concat (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (boolean_type))",
        /* (||, is_not)                 */ "a || b is not boolean" to "(not (is_type (concat (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (boolean_type)))"
    )

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun mulPrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForMulPrecedence() = listOf(
        /* (*, intersect)               */ "a * b intersect c" to "(bag_op (intersect) (distinct) (times (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (*, intersect_all)           */ "a * b intersect all c" to "(bag_op (intersect) (all) (times (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (*, except)                  */ "a * b except c" to "(bag_op (except) (distinct) (times (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (*, except_all)              */ "a * b except all c" to "(bag_op (except) (all) (times (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (*, union)                   */ "a * b union c" to "(bag_op (union) (distinct) (times (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (*, union_all)               */ "a * b union all c" to "(bag_op (union) (all) (times (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (*, or)                      */ "a * b or c" to "(or (times (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (*, and)                     */ "a * b and c" to "(and (times (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (*, =)                       */ "a * b = c" to "(eq (times (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (*, <>)                      */ "a * b <> c" to "(ne (times (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (*, in)                      */ "a * b in c" to "(in_collection (times (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (*, not_in)                  */ "a * b not in c" to "(not (in_collection (times (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified))))",
        /* (*, <)                       */ "a * b < c" to "(lt (times (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (*, <=)                      */ "a * b <= c" to "(lte (times (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (*, >)                       */ "a * b > c" to "(gt (times (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (*, >=)                      */ "a * b >= c" to "(gte (times (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (*, between)                 */ "a * b between w and c" to "(between (times (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id w (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified)))",
        /* (*, not_between)             */ "a * b not between y and c" to "(not (between (times (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id y (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (*, like)                    */ "a * b like c" to "(like (times (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)) null)",
        /* (*, not_like)                */ "a * b not like c" to "(not (like (times (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)) null))",
        /* (*, +)                       */ "a * b + c" to "(plus (times (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (*, -)                       */ "a * b - c" to "(minus (times (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (*, ||)                      */ "a * b || c" to "(concat (times (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (*, /)                       */ "a * b / c" to "(divide (times (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (*, %)                       */ "a * b % c" to "(modulo (times (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (*, is)                      */ "a * b is boolean" to "(is_type (times (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (boolean_type))",
        /* (*, is_not)                  */ "a * b is not boolean" to "(not (is_type (times (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (boolean_type)))"
    )

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun divPrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForDivPrecedence() = listOf(
        /* (/, intersect)               */ "a / b intersect c" to "(bag_op (intersect) (distinct) (divide (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (/, intersect_all)           */ "a / b intersect all c" to "(bag_op (intersect) (all) (divide (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (/, except)                  */ "a / b except c" to "(bag_op (except) (distinct) (divide (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (/, except_all)              */ "a / b except all c" to "(bag_op (except) (all) (divide (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (/, union)                   */ "a / b union c" to "(bag_op (union) (distinct) (divide (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (/, union_all)               */ "a / b union all c" to "(bag_op (union) (all) (divide (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (/, or)                      */ "a / b or c" to "(or (divide (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (/, and)                     */ "a / b and c" to "(and (divide (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (/, =)                       */ "a / b = c" to "(eq (divide (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (/, <>)                      */ "a / b <> c" to "(ne (divide (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (/, in)                      */ "a / b in c" to "(in_collection (divide (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (/, not_in)                  */ "a / b not in c" to "(not (in_collection (divide (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified))))",
        /* (/, <)                       */ "a / b < c" to "(lt (divide (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (/, <=)                      */ "a / b <= c" to "(lte (divide (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (/, >)                       */ "a / b > c" to "(gt (divide (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (/, >=)                      */ "a / b >= c" to "(gte (divide (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (/, between)                 */ "a / b between w and c" to "(between (divide (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id w (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified)))",
        /* (/, not_between)             */ "a / b not between y and c" to "(not (between (divide (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id y (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (/, like)                    */ "a / b like c" to "(like (divide (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)) null)",
        /* (/, not_like)                */ "a / b not like c" to "(not (like (divide (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)) null))",
        /* (/, +)                       */ "a / b + c" to "(plus (divide (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (/, -)                       */ "a / b - c" to "(minus (divide (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (/, ||)                      */ "a / b || c" to "(concat (divide (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (/, *)                       */ "a / b * c" to "(times (divide (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (/, %)                       */ "a / b % c" to "(modulo (divide (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (/, is)                      */ "a / b is boolean" to "(is_type (divide (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (boolean_type))",
        /* (/, is_not)                  */ "a / b is not boolean" to "(not (is_type (divide (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (boolean_type)))"
    )

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun modPrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForModPrecedence() = listOf(
        /* (%, intersect)               */ "a % b intersect c" to "(bag_op (intersect) (distinct) (modulo (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (%, intersect_all)           */ "a % b intersect all c" to "(bag_op (intersect) (all) (modulo (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (%, except)                  */ "a % b except c" to "(bag_op (except) (distinct) (modulo (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (%, except_all)              */ "a % b except all c" to "(bag_op (except) (all) (modulo (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (%, union)                   */ "a % b union c" to "(bag_op (union) (distinct) (modulo (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (%, union_all)               */ "a % b union all c" to "(bag_op (union) (all) (modulo (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (%, or)                      */ "a % b or c" to "(or (modulo (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (%, and)                     */ "a % b and c" to "(and (modulo (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (%, =)                       */ "a % b = c" to "(eq (modulo (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (%, <>)                      */ "a % b <> c" to "(ne (modulo (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (%, in)                      */ "a % b in c" to "(in_collection (modulo (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (%, not_in)                  */ "a % b not in c" to "(not (in_collection (modulo (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified))))",
        /* (%, <)                       */ "a % b < c" to "(lt (modulo (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (%, <=)                      */ "a % b <= c" to "(lte (modulo (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (%, >)                       */ "a % b > c" to "(gt (modulo (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (%, >=)                      */ "a % b >= c" to "(gte (modulo (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (%, between)                 */ "a % b between w and c" to "(between (modulo (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id w (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified)))",
        /* (%, not_between)             */ "a % b not between y and c" to "(not (between (modulo (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id y (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (%, like)                    */ "a % b like c" to "(like (modulo (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)) null)",
        /* (%, not_like)                */ "a % b not like c" to "(not (like (modulo (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)) null))",
        /* (%, +)                       */ "a % b + c" to "(plus (modulo (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (%, -)                       */ "a % b - c" to "(minus (modulo (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (%, ||)                      */ "a % b || c" to "(concat (modulo (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (%, *)                       */ "a % b * c" to "(times (modulo (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (%, /)                       */ "a % b / c" to "(divide (modulo (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (%, is)                      */ "a % b is boolean" to "(is_type (modulo (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (boolean_type))",
        /* (%, is_not)                  */ "a % b is not boolean" to "(not (is_type (modulo (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (boolean_type)))"
    )

    @Test
    fun combinationOfBinaryOperators() = runTest(
        "a + b AND c / d * e - f || g OR h" to """
            (or
                (and
                    (plus (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)) )
                    (concat
                        (minus
                            (times
                                (divide (vr (id c (case_insensitive)) (unqualified)) (vr (id d (case_insensitive)) (unqualified)))
                                (vr (id e (case_insensitive)) (unqualified))
                            )
                            (vr (id f (case_insensitive)) (unqualified))
                        )
                        (vr (id g (case_insensitive)) (unqualified))
                    )
                )
                (vr (id h (case_insensitive)) (unqualified))
            )"""
    )

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun notUnaryPrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForNotUnaryPrecedence() = listOf(
        /* (not, intersect)     */ "not a intersect b" to "(bag_op (intersect) (distinct) (not (vr (id a (case_insensitive)) (unqualified))) (vr (id b (case_insensitive)) (unqualified)))",
        /* (not, intersect_all) */ "not a intersect all b" to "(bag_op (intersect) (all) (not (vr (id a (case_insensitive)) (unqualified))) (vr (id b (case_insensitive)) (unqualified)))",
        /* (not, except)        */ "not a except b" to "(bag_op (except) (distinct) (not (vr (id a (case_insensitive)) (unqualified))) (vr (id b (case_insensitive)) (unqualified)))",
        /* (not, except_all)    */ "not a except all b" to "(bag_op (except) (all) (not (vr (id a (case_insensitive)) (unqualified))) (vr (id b (case_insensitive)) (unqualified)))",
        /* (not, union)         */ "not a union b" to "(bag_op (union) (distinct) (not (vr (id a (case_insensitive)) (unqualified))) (vr (id b (case_insensitive)) (unqualified)))",
        /* (not, union_all)     */ "not a union all b" to "(bag_op (union) (all) (not (vr (id a (case_insensitive)) (unqualified))) (vr (id b (case_insensitive)) (unqualified)))",
        /* (not, or)            */ "not a or b" to "(or (not (vr (id a (case_insensitive)) (unqualified))) (vr (id b (case_insensitive)) (unqualified)))",
        /* (not, and)           */ "not a and b" to "(and (not (vr (id a (case_insensitive)) (unqualified))) (vr (id b (case_insensitive)) (unqualified)))",
        /* (not, =)             */ "not a = b" to "(not (eq (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))))",
        /* (not, <>)            */ "not a <> b" to "(not (ne (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))))",
        /* (not, in)            */ "not a in b" to "(not (in_collection (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))))",
        /* (not, not_in)        */ "not a not in b" to "(not (not (in_collection (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)))))",
        /* (not, <)             */ "not a < b" to "(not (lt (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))))",
        /* (not, <=)            */ "not a <= b" to "(not (lte (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))))",
        /* (not, >)             */ "not a > b" to "(not (gt (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))))",
        /* (not, >=)            */ "not a >= b" to "(not (gte (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))))",
        /* (not, between)       */ "not a between b and c" to "(not (between (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (not, not_between)   */ "not a not between b and c" to "(not (not (between (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified)))))",
        /* (not, like)          */ "not a like b" to "(not (like (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)) null))",
        /* (not, not_like)      */ "not a not like b" to "(not (not (like (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)) null)))",
        /* (not, +)             */ "not a + b" to "(not (plus (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))))",
        /* (not, -)             */ "not a - b" to "(not (minus (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))))",
        /* (not, ||)            */ "not a || b" to "(not (concat (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))))",
        /* (not, *)             */ "not a * b" to "(not (times (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))))",
        /* (not, /)             */ "not a / b" to "(not (divide (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))))",
        /* (not, %)             */ "not a % b" to "(not (modulo (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))))",
        /* (not, is)            */ "not a is boolean" to "(not (is_type (vr (id a (case_insensitive)) (unqualified)) (boolean_type)))",
        /* (not, is_not)        */ "not a is not boolean" to "(not (not (is_type (vr (id a (case_insensitive)) (unqualified)) (boolean_type))))"
    )

    @Parameters
    @TestCaseName("{0}")
    fun notComboPrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForNotComboPrecedence() = listOf(
        // not combination
        "not a and b or c and not d or not e" to """
            (or
                (or
                    (and
                        (not (vr (id a (case_insensitive)) (unqualified)))
                        (vr (id b (case_insensitive)) (unqualified))
                    )
                    (and
                        (vr (id c (case_insensitive)) (unqualified))
                        (not (vr (id d (case_insensitive)) (unqualified)))
                    )
                )
                (not (vr (id e (case_insensitive)) (unqualified)))
            )""",

        // pos and neg
        "- a + b" to "(plus (neg (vr (id a (case_insensitive)) (unqualified))) (vr (id b (case_insensitive)) (unqualified)) )",

        "(a+-5e0) and (c-+7.0)" to """
            (and
                (plus (vr (id a (case_insensitive)) (unqualified)) (neg (lit 5.) ) )
                (minus (vr (id c (case_insensitive)) (unqualified)) (pos (lit 7.0) ) )
            )""",

        "d*-+-9 and e>=+-+foo" to """
            (and
                (times (vr (id d (case_insensitive)) (unqualified)) (neg (pos (neg (lit 9) ))) )
                (gte
                    (vr (id e (case_insensitive)) (unqualified))
                    (pos (neg (pos (vr (id foo (case_insensitive)) (unqualified)))))
                )
            )"""
    )

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun partiQLOnly(pair: Pair<String, String>) = runTest(pair)
    fun parametersForPartiQLOnly() = listOf(
        /* (||, +)                      */ "a || b + c" to "(concat (vr (id a (case_insensitive)) (unqualified)) (plus (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (||, -)                      */ "a || b - c" to "(concat (vr (id a (case_insensitive)) (unqualified)) (minus (vr (id b (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (not (in, <)                  */ "a not in b < c" to "(lt (not (in_collection (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (not (in, <=)                 */ "a not in b <= c" to "(lte (not (in_collection (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (not (in, >)                  */ "a not in b > c" to "(gt (not (in_collection (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (not (in, >=)                 */ "a not in b >= c" to "(gte (not (in_collection (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (not (in, between)            */ "a not in b between w and c" to "(between (not (in_collection (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)))) (vr (id w (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified)))",
        /* (not (in, not_between)        */ "a not in b not between y and c" to "(not (between (not (in_collection (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)))) (vr (id y (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (not (in, like)               */ "a not in b like c" to "(like (not (in_collection (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)))) (vr (id c (case_insensitive)) (unqualified)) null)",
        /* (not (in, not_like)           */ "a not in b not like c" to "(not (like (not (in_collection (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified)))) (vr (id c (case_insensitive)) (unqualified)) null))",
        /* (in, <)                      */ "a in b < c" to "(lt (in_collection (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (in, <=)                     */ "a in b <= c" to "(lte (in_collection (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (in, >)                      */ "a in b > c" to "(gt (in_collection (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (in, >=)                     */ "a in b >= c" to "(gte (in_collection (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (in, between)                */ "a in b between w and c" to "(between (in_collection (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id w (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified)))",
        /* (in, not_between)            */ "a in b not between y and c" to "(not (between (in_collection (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id y (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (in, like)                   */ "a in b like c" to "(like (in_collection (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)) null)",
        /* (in, not_like)               */ "a in b not like c" to "(not (like (in_collection (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)) null))",
        /* (<>, <)                      */ "a <> b < c" to "(lt (ne (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (<>, <=)                     */ "a <> b <= c" to "(lte (ne (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (<>, >)                      */ "a <> b > c" to "(gt (ne (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (<>, >=)                     */ "a <> b >= c" to "(gte (ne (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (<>, between)                */ "a <> b between w and c" to "(between (ne (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id w (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified)))",
        /* (<>, not_between)            */ "a <> b not between y and c" to "(not (between (ne (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id y (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (<>, like)                   */ "a <> b like c" to "(like (ne (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)) null)",
        /* (<>, not_like)               */ "a <> b not like c" to "(not (like (ne (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)) null))",
        /* (=, <)                       */ "a = b < c" to "(lt (eq (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (=, <=)                      */ "a = b <= c" to "(lte (eq (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (=, >)                       */ "a = b > c" to "(gt (eq (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (=, >=)                      */ "a = b >= c" to "(gte (eq (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)))",
        /* (=, between)                 */ "a = b between w and c" to "(between (eq (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id w (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified)))",
        /* (=, not_between)             */ "a = b not between y and c" to "(not (between (eq (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id y (case_insensitive)) (unqualified)) (vr (id c (case_insensitive)) (unqualified))))",
        /* (=, like)                    */ "a = b like c" to "(like (eq (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)) null)",
        /* (=, not_like)                */ "a = b not like c" to "(not (like (eq (vr (id a (case_insensitive)) (unqualified)) (vr (id b (case_insensitive)) (unqualified))) (vr (id c (case_insensitive)) (unqualified)) null))",
    )

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun failureCases(case: String) = assertFailure(case)
    fun parametersForFailureCases() = listOf(
        /* (is, +)                      */ "a is boolean + c",
        /* (is, -)                      */ "a is boolean - c",
        /* (is, ||)                     */ "a is boolean || c",
        /* (is, *)                      */ "a is boolean * c",
        /* (is, /)                      */ "a is boolean / c",
        /* (is, %)                      */ "a is boolean % c",
        /* (not (is, +)                  */ "a is not boolean + c",
        /* (not (is, -)                  */ "a is not boolean - c",
        /* (not (is, ||)                 */ "a is not boolean || c",
        /* (not (is, *)                  */ "a is not boolean * c",
        /* (not (is, /)                  */ "a is not boolean / c",
        /* (not (is, %)                  */ "a is not boolean % c",
    )

    private fun runTest(pair: Pair<String, String>) {
        targets.forEach { target ->
            val (source, expectedAst) = pair

            val expectedExpr = PartiqlAst.transform(ion.singleValue(expectedAst).toIonElement()) as PartiqlAst.Expr
            val expectedStatement = PartiqlAst.build { query(expectedExpr) }

            val actualStatement = target.parser.parseAstStatement(source)
            assertEquals(expectedStatement, actualStatement)
        }
    }

    private fun assertFailure(case: String) {
        targets.forEach { target ->
            assertThrows(ParserException::class.java) {
                target.parser.parseAstStatement(case)
            }
        }
    }
}
