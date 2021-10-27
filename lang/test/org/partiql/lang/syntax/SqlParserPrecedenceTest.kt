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
import com.amazon.ionelement.api.toIonElement
import org.partiql.lang.ast.passes.MetaStrippingRewriter
import org.partiql.lang.ast.toExprNode
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.util.asIonSexp


class SqlParserPrecedenceTest : SqlParserTestBase() {

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun intersectPrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForIntersectPrecedence(): List<Pair<String, String>> = listOf(
        // two by two binary operators
        /* (intersect, intersect_all)   */ "a intersect b intersect all c"         to "(intersect (all) (intersect (distinct) (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (intersect, except)          */ "a intersect b except c"                to "(except (distinct) (intersect (distinct) (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (intersect, except_all)      */ "a intersect b except all c"            to "(except (all) (intersect (distinct) (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (intersect, union)           */ "a intersect b union c"                 to "(union (distinct) (intersect (distinct) (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (intersect, union_all)       */ "a intersect b union all c"             to "(union (all) (intersect (distinct) (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (intersect, and)             */ "a intersect b and c"                   to "(intersect (distinct) (id a (case_insensitive) (unqualified)) (and (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (intersect, or)              */ "a intersect b or c"                    to "(intersect (distinct) (id a (case_insensitive) (unqualified)) (or (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (intersect, =)               */ "a intersect b = c"                     to "(intersect (distinct) (id a (case_insensitive) (unqualified)) (eq (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (intersect, <>)              */ "a intersect b <> c"                    to "(intersect (distinct) (id a (case_insensitive) (unqualified)) (ne (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (intersect, in)              */ "a intersect b in c"                    to "(intersect (distinct) (id a (case_insensitive) (unqualified)) (in_collection (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (intersect, not_in)          */ "a intersect b not in c"                to "(intersect (distinct) (id a (case_insensitive) (unqualified)) (not (in_collection (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified)))))",
        /* (intersect, <)               */ "a intersect b < c"                     to "(intersect (distinct) (id a (case_insensitive) (unqualified)) (lt (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (intersect, <=)              */ "a intersect b <= c"                    to "(intersect (distinct) (id a (case_insensitive) (unqualified)) (lte (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (intersect, >)               */ "a intersect b > c"                     to "(intersect (distinct) (id a (case_insensitive) (unqualified)) (gt (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (intersect, >=)              */ "a intersect b >= c"                    to "(intersect (distinct) (id a (case_insensitive) (unqualified)) (gte (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (intersect, between)         */ "a intersect b between w and c"         to "(intersect (distinct) (id a (case_insensitive) (unqualified)) (between (id b (case_insensitive) (unqualified)) (id w (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (intersect, not_between)     */ "a intersect b not between y and c"     to "(intersect (distinct) (id a (case_insensitive) (unqualified)) (not (between (id b (case_insensitive) (unqualified)) (id y (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified)))))",
        /* (intersect, like)            */ "a intersect b like c"                  to "(intersect (distinct) (id a (case_insensitive) (unqualified)) (like (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified)) null))",
        /* (intersect, not_like)        */ "a intersect b not like c"              to "(intersect (distinct) (id a (case_insensitive) (unqualified)) (not (like (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified)) null)))",
        /* (intersect, +)               */ "a intersect b + c"                     to "(intersect (distinct) (id a (case_insensitive) (unqualified)) (plus (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (intersect, -)               */ "a intersect b - c"                     to "(intersect (distinct) (id a (case_insensitive) (unqualified)) (minus (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (intersect, ||)              */ "a intersect b || c"                    to "(intersect (distinct) (id a (case_insensitive) (unqualified)) (concat (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (intersect, *)               */ "a intersect b * c"                     to "(intersect (distinct) (id a (case_insensitive) (unqualified)) (times (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (intersect, /)               */ "a intersect b / c"                     to "(intersect (distinct) (id a (case_insensitive) (unqualified)) (divide (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (intersect, %)               */ "a intersect b % c"                     to "(intersect (distinct) (id a (case_insensitive) (unqualified)) (modulo (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (intersect, is)              */ "a intersect b is boolean"              to "(intersect (distinct) (id a (case_insensitive) (unqualified)) (is_type (id b (case_insensitive) (unqualified)) (boolean_type)))",
        /* (intersect, is_not)          */ "a intersect b is not boolean"          to "(intersect (distinct) (id a (case_insensitive) (unqualified)) (not (is_type (id b (case_insensitive) (unqualified)) (boolean_type))))")

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun intersectAllPrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForIntersectAllPrecedence() = listOf(
        /* (intersect_all, intersect)   */ "a intersect all b intersect c"         to "(intersect (distinct) (intersect (all) (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (intersect_all, except)      */ "a intersect all b except c"            to "(except (distinct) (intersect (all) (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (intersect_all, except_all)  */ "a intersect all b except all c"        to "(except (all) (intersect (all) (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (intersect_all, union)       */ "a intersect all b union c"             to "(union (distinct) (intersect (all) (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (intersect_all, union_all)   */ "a intersect all b union all c"         to "(union (all) (intersect (all) (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (intersect_all, and)         */ "a intersect all b and c"               to "(intersect (all) (id a (case_insensitive) (unqualified)) (and (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (intersect_all, or)          */ "a intersect all b or c"                to "(intersect (all) (id a (case_insensitive) (unqualified)) (or (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (intersect_all, =)           */ "a intersect all b = c"                 to "(intersect (all) (id a (case_insensitive) (unqualified)) (eq (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (intersect_all, <>)          */ "a intersect all b <> c"                to "(intersect (all) (id a (case_insensitive) (unqualified)) (ne (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (intersect_all, in)          */ "a intersect all b in c"                to "(intersect (all) (id a (case_insensitive) (unqualified)) (in_collection (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (intersect_all, not_in)      */ "a intersect all b not in c"            to "(intersect (all) (id a (case_insensitive) (unqualified)) (not (in_collection (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified)))))",
        /* (intersect_all, <)           */ "a intersect all b < c"                 to "(intersect (all) (id a (case_insensitive) (unqualified)) (lt (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (intersect_all, <=)          */ "a intersect all b <= c"                to "(intersect (all) (id a (case_insensitive) (unqualified)) (lte (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (intersect_all, >)           */ "a intersect all b > c"                 to "(intersect (all) (id a (case_insensitive) (unqualified)) (gt (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (intersect_all, >=)          */ "a intersect all b >= c"                to "(intersect (all) (id a (case_insensitive) (unqualified)) (gte (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (intersect_all, between)     */ "a intersect all b between w and c"     to "(intersect (all) (id a (case_insensitive) (unqualified)) (between (id b (case_insensitive) (unqualified)) (id w (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (intersect_all, not_between) */ "a intersect all b not between y and c" to "(intersect (all) (id a (case_insensitive) (unqualified)) (not (between (id b (case_insensitive) (unqualified)) (id y (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified)))))",
        /* (intersect_all, like)        */ "a intersect all b like c"              to "(intersect (all) (id a (case_insensitive) (unqualified)) (like (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified)) null))",
        /* (intersect_all, not_like)    */ "a intersect all b not like c"          to "(intersect (all) (id a (case_insensitive) (unqualified)) (not (like (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified)) null)))",
        /* (intersect_all, +)           */ "a intersect all b + c"                 to "(intersect (all) (id a (case_insensitive) (unqualified)) (plus (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (intersect_all, -)           */ "a intersect all b - c"                 to "(intersect (all) (id a (case_insensitive) (unqualified)) (minus (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (intersect_all, ||)          */ "a intersect all b || c"                to "(intersect (all) (id a (case_insensitive) (unqualified)) (concat (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (intersect_all, *)           */ "a intersect all b * c"                 to "(intersect (all) (id a (case_insensitive) (unqualified)) (times (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (intersect_all, /)           */ "a intersect all b / c"                 to "(intersect (all) (id a (case_insensitive) (unqualified)) (divide (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (intersect_all, %)           */ "a intersect all b % c"                 to "(intersect (all) (id a (case_insensitive) (unqualified)) (modulo (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (intersect_all, is)          */ "a intersect all b is boolean"          to "(intersect (all) (id a (case_insensitive) (unqualified)) (is_type (id b (case_insensitive) (unqualified)) (boolean_type)))",
        /* (intersect_all, is_not)      */ "a intersect all b is not boolean"      to "(intersect (all) (id a (case_insensitive) (unqualified)) (not (is_type (id b (case_insensitive) (unqualified)) (boolean_type))))"
    )

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun exceptPrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForExceptPrecedence() = listOf(
        /* (except, intersect)          */ "a except b intersect c"                to "(intersect (distinct) (except (distinct) (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (except, intersect_all)      */ "a except b intersect all c"            to "(intersect (all) (except (distinct) (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (except, except_all)         */ "a except b except all c"               to "(except (all) (except (distinct) (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (except, union)              */ "a except b union c"                    to "(union (distinct) (except (distinct) (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (except, union_all)          */ "a except b union all c"                to "(union (all) (except (distinct) (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (except, and)                */ "a except b and c"                      to "(except (distinct) (id a (case_insensitive) (unqualified)) (and (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (except, or)                 */ "a except b or c"                       to "(except (distinct) (id a (case_insensitive) (unqualified)) (or (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (except, =)                  */ "a except b = c"                        to "(except (distinct) (id a (case_insensitive) (unqualified)) (eq (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (except, <>)                 */ "a except b <> c"                       to "(except (distinct) (id a (case_insensitive) (unqualified)) (ne (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (except, in)                 */ "a except b in c"                       to "(except (distinct) (id a (case_insensitive) (unqualified)) (in_collection (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (except, not_in)             */ "a except b not in c"                   to "(except (distinct) (id a (case_insensitive) (unqualified)) (not (in_collection (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified)))))",
        /* (except, <)                  */ "a except b < c"                        to "(except (distinct) (id a (case_insensitive) (unqualified)) (lt (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (except, <=)                 */ "a except b <= c"                       to "(except (distinct) (id a (case_insensitive) (unqualified)) (lte (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (except, >)                  */ "a except b > c"                        to "(except (distinct) (id a (case_insensitive) (unqualified)) (gt (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (except, >=)                 */ "a except b >= c"                       to "(except (distinct) (id a (case_insensitive) (unqualified)) (gte (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (except, between)            */ "a except b between w and c"            to "(except (distinct) (id a (case_insensitive) (unqualified)) (between (id b (case_insensitive) (unqualified)) (id w (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (except, not_between)        */ "a except b not between y and c"        to "(except (distinct) (id a (case_insensitive) (unqualified)) (not (between (id b (case_insensitive) (unqualified)) (id y (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified)))))",
        /* (except, like)               */ "a except b like c"                     to "(except (distinct) (id a (case_insensitive) (unqualified)) (like (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified)) null))",
        /* (except, not_like)           */ "a except b not like c"                 to "(except (distinct) (id a (case_insensitive) (unqualified)) (not (like (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified)) null)))",
        /* (except, +)                  */ "a except b + c"                        to "(except (distinct) (id a (case_insensitive) (unqualified)) (plus (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (except, -)                  */ "a except b - c"                        to "(except (distinct) (id a (case_insensitive) (unqualified)) (minus (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (except, ||)                 */ "a except b || c"                       to "(except (distinct) (id a (case_insensitive) (unqualified)) (concat (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (except, *)                  */ "a except b * c"                        to "(except (distinct) (id a (case_insensitive) (unqualified)) (times (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (except, /)                  */ "a except b / c"                        to "(except (distinct) (id a (case_insensitive) (unqualified)) (divide (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (except, %)                  */ "a except b % c"                        to "(except (distinct) (id a (case_insensitive) (unqualified)) (modulo (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (except, is)                 */ "a except b is boolean"                 to "(except (distinct) (id a (case_insensitive) (unqualified)) (is_type (id b (case_insensitive) (unqualified)) (boolean_type)))",
        /* (except, is_not)             */ "a except b is not boolean"             to "(except (distinct) (id a (case_insensitive) (unqualified)) (not (is_type (id b (case_insensitive) (unqualified)) (boolean_type))))")

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun exceptAllPrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForExceptAllPrecedence() = listOf(
        /* (except_all, intersect)      */ "a except all b intersect c"            to "(intersect (distinct) (except (all) (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (except_all, intersect_all)  */ "a except all b intersect all c"        to "(intersect (all) (except (all) (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (except_all, except)         */ "a except all b except c"               to "(except (distinct) (except (all) (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (except_all, union)          */ "a except all b union c"                to "(union (distinct) (except (all) (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (except_all, union_all)      */ "a except all b union all c"            to "(union (all) (except (all) (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (except_all, and)            */ "a except all b and c"                  to "(except (all) (id a (case_insensitive) (unqualified)) (and (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (except_all, or)             */ "a except all b or c"                   to "(except (all) (id a (case_insensitive) (unqualified)) (or (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (except_all, =)              */ "a except all b = c"                    to "(except (all) (id a (case_insensitive) (unqualified)) (eq (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (except_all, <>)             */ "a except all b <> c"                   to "(except (all) (id a (case_insensitive) (unqualified)) (ne (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (except_all, in)             */ "a except all b in c"                   to "(except (all) (id a (case_insensitive) (unqualified)) (in_collection (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (except_all, not_in)         */ "a except all b not in c"               to "(except (all) (id a (case_insensitive) (unqualified)) (not (in_collection (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified)))))",
        /* (except_all, <)              */ "a except all b < c"                    to "(except (all) (id a (case_insensitive) (unqualified)) (lt (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (except_all, <=)             */ "a except all b <= c"                   to "(except (all) (id a (case_insensitive) (unqualified)) (lte (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (except_all, >)              */ "a except all b > c"                    to "(except (all) (id a (case_insensitive) (unqualified)) (gt (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (except_all, >=)             */ "a except all b >= c"                   to "(except (all) (id a (case_insensitive) (unqualified)) (gte (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (except_all, between)        */ "a except all b between w and c"        to "(except (all) (id a (case_insensitive) (unqualified)) (between (id b (case_insensitive) (unqualified)) (id w (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (except_all, not_between)    */ "a except all b not between y and c"    to "(except (all) (id a (case_insensitive) (unqualified)) (not (between (id b (case_insensitive) (unqualified)) (id y (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified)))))",
        /* (except_all, like)           */ "a except all b like c"                 to "(except (all) (id a (case_insensitive) (unqualified)) (like (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified)) null))",
        /* (except_all, not_like)       */ "a except all b not like c"             to "(except (all) (id a (case_insensitive) (unqualified)) (not (like (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified)) null)))",
        /* (except_all, +)              */ "a except all b + c"                    to "(except (all) (id a (case_insensitive) (unqualified)) (plus (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (except_all, -)              */ "a except all b - c"                    to "(except (all) (id a (case_insensitive) (unqualified)) (minus (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (except_all, ||)             */ "a except all b || c"                   to "(except (all) (id a (case_insensitive) (unqualified)) (concat (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (except_all, *)              */ "a except all b * c"                    to "(except (all) (id a (case_insensitive) (unqualified)) (times (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (except_all, /)              */ "a except all b / c"                    to "(except (all) (id a (case_insensitive) (unqualified)) (divide (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (except_all, %)              */ "a except all b % c"                    to "(except (all) (id a (case_insensitive) (unqualified)) (modulo (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (except_all, is)             */ "a except all b is boolean"             to "(except (all) (id a (case_insensitive) (unqualified)) (is_type (id b (case_insensitive) (unqualified)) (boolean_type)))",
        /* (except_all, is_not)         */ "a except all b is not boolean"         to "(except (all) (id a (case_insensitive) (unqualified)) (not (is_type (id b (case_insensitive) (unqualified)) (boolean_type))))")

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun unionPrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForUnionPrecedence() = listOf(
        /* (union, intersect)           */ "a union b intersect c"                 to "(intersect (distinct) (union (distinct) (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (union, intersect_all)       */ "a union b intersect all c"             to "(intersect (all) (union (distinct) (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (union, except)              */ "a union b except c"                    to "(except (distinct) (union (distinct) (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (union, except_all)          */ "a union b except all c"                to "(except (all) (union (distinct) (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (union, union_all)           */ "a union b union all c"                 to "(union (all) (union (distinct) (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (union, and)                 */ "a union b and c"                       to "(union (distinct) (id a (case_insensitive) (unqualified)) (and (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (union, or)                  */ "a union b or c"                        to "(union (distinct) (id a (case_insensitive) (unqualified)) (or (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (union, =)                   */ "a union b = c"                         to "(union (distinct) (id a (case_insensitive) (unqualified)) (eq (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (union, <>)                  */ "a union b <> c"                        to "(union (distinct) (id a (case_insensitive) (unqualified)) (ne (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (union, in)                  */ "a union b in c"                        to "(union (distinct) (id a (case_insensitive) (unqualified)) (in_collection (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (union, not_in)              */ "a union b not in c"                    to "(union (distinct) (id a (case_insensitive) (unqualified)) (not (in_collection (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified)))))",
        /* (union, <)                   */ "a union b < c"                         to "(union (distinct) (id a (case_insensitive) (unqualified)) (lt (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (union, <=)                  */ "a union b <= c"                        to "(union (distinct) (id a (case_insensitive) (unqualified)) (lte (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (union, >)                   */ "a union b > c"                         to "(union (distinct) (id a (case_insensitive) (unqualified)) (gt (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (union, >=)                  */ "a union b >= c"                        to "(union (distinct) (id a (case_insensitive) (unqualified)) (gte (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (union, between)             */ "a union b between w and c"             to "(union (distinct) (id a (case_insensitive) (unqualified)) (between (id b (case_insensitive) (unqualified)) (id w (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (union, not_between)         */ "a union b not between y and c"         to "(union (distinct) (id a (case_insensitive) (unqualified)) (not (between (id b (case_insensitive) (unqualified)) (id y (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified)))))",
        /* (union, like)                */ "a union b like c"                      to "(union (distinct) (id a (case_insensitive) (unqualified)) (like (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified)) null))",
        /* (union, not_like)            */ "a union b not like c"                  to "(union (distinct) (id a (case_insensitive) (unqualified)) (not (like (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified)) null)))",
        /* (union, +)                   */ "a union b + c"                         to "(union (distinct) (id a (case_insensitive) (unqualified)) (plus (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (union, -)                   */ "a union b - c"                         to "(union (distinct) (id a (case_insensitive) (unqualified)) (minus (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (union, ||)                  */ "a union b || c"                        to "(union (distinct) (id a (case_insensitive) (unqualified)) (concat (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (union, *)                   */ "a union b * c"                         to "(union (distinct) (id a (case_insensitive) (unqualified)) (times (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (union, /)                   */ "a union b / c"                         to "(union (distinct) (id a (case_insensitive) (unqualified)) (divide (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (union, %)                   */ "a union b % c"                         to "(union (distinct) (id a (case_insensitive) (unqualified)) (modulo (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (union, is)                  */ "a union b is boolean"                  to "(union (distinct) (id a (case_insensitive) (unqualified)) (is_type (id b (case_insensitive) (unqualified)) (boolean_type)))",
        /* (union, is_not)              */ "a union b is not boolean"              to "(union (distinct) (id a (case_insensitive) (unqualified)) (not (is_type (id b (case_insensitive) (unqualified)) (boolean_type))))")

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun unionAllPrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForUnionAllPrecedence() = listOf(
        /* (union_all, intersect)       */ "a union all b intersect c"             to "(intersect (distinct) (union (all) (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (union_all, intersect_all)   */ "a union all b intersect all c"         to "(intersect (all) (union (all) (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (union_all, except)          */ "a union all b except c"                to "(except (distinct) (union (all) (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (union_all, except_all)      */ "a union all b except all c"            to "(except (all) (union (all) (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (union_all, union)           */ "a union all b union c"                 to "(union (distinct) (union (all) (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (union_all, and)             */ "a union all b and c"                   to "(union (all) (id a (case_insensitive) (unqualified)) (and (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (union_all, or)              */ "a union all b or c"                    to "(union (all) (id a (case_insensitive) (unqualified)) (or (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (union_all, =)               */ "a union all b = c"                     to "(union (all) (id a (case_insensitive) (unqualified)) (eq (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (union_all, <>)              */ "a union all b <> c"                    to "(union (all) (id a (case_insensitive) (unqualified)) (ne (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (union_all, in)              */ "a union all b in c"                    to "(union (all) (id a (case_insensitive) (unqualified)) (in_collection (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (union_all, not_in)          */ "a union all b not in c"                to "(union (all) (id a (case_insensitive) (unqualified)) (not (in_collection (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified)))))",
        /* (union_all, <)               */ "a union all b < c"                     to "(union (all) (id a (case_insensitive) (unqualified)) (lt (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (union_all, <=)              */ "a union all b <= c"                    to "(union (all) (id a (case_insensitive) (unqualified)) (lte (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (union_all, >)               */ "a union all b > c"                     to "(union (all) (id a (case_insensitive) (unqualified)) (gt (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (union_all, >=)              */ "a union all b >= c"                    to "(union (all) (id a (case_insensitive) (unqualified)) (gte (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (union_all, between)         */ "a union all b between w and c"         to "(union (all) (id a (case_insensitive) (unqualified)) (between (id b (case_insensitive) (unqualified)) (id w (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (union_all, not_between)     */ "a union all b not between y and c"     to "(union (all) (id a (case_insensitive) (unqualified)) (not (between (id b (case_insensitive) (unqualified)) (id y (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified)))))",
        /* (union_all, like)            */ "a union all b like c"                  to "(union (all) (id a (case_insensitive) (unqualified)) (like (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified)) null))",
        /* (union_all, not_like)        */ "a union all b not like c"              to "(union (all) (id a (case_insensitive) (unqualified)) (not (like (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified)) null)))",
        /* (union_all, +)               */ "a union all b + c"                     to "(union (all) (id a (case_insensitive) (unqualified)) (plus (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (union_all, -)               */ "a union all b - c"                     to "(union (all) (id a (case_insensitive) (unqualified)) (minus (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (union_all, ||)              */ "a union all b || c"                    to "(union (all) (id a (case_insensitive) (unqualified)) (concat (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (union_all, *)               */ "a union all b * c"                     to "(union (all) (id a (case_insensitive) (unqualified)) (times (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (union_all, /)               */ "a union all b / c"                     to "(union (all) (id a (case_insensitive) (unqualified)) (divide (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (union_all, %)               */ "a union all b % c"                     to "(union (all) (id a (case_insensitive) (unqualified)) (modulo (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (union_all, is)              */ "a union all b is boolean"              to "(union (all) (id a (case_insensitive) (unqualified)) (is_type (id b (case_insensitive) (unqualified)) (boolean_type)))",
        /* (union_all, is_not)          */ "a union all b is not boolean"          to "(union (all) (id a (case_insensitive) (unqualified)) (not (is_type (id b (case_insensitive) (unqualified)) (boolean_type))))")

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun andPrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForAndPrecedence() = listOf(
        /* (and, intersect)             */ "a and b intersect c"                   to "(intersect (distinct) (and (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (and, intersect_all)         */ "a and b intersect all c"               to "(intersect (all) (and (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (and, except)                */ "a and b except c"                      to "(except (distinct) (and (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (and, except_all)            */ "a and b except all c"                  to "(except (all) (and (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (and, union)                 */ "a and b union c"                       to "(union (distinct) (and (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (and, union_all)             */ "a and b union all c"                   to "(union (all) (and (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (and, or)                    */ "a and b or c"                          to "(or (and (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (and, =)                     */ "a and b = c"                           to "(and (id a (case_insensitive) (unqualified)) (eq (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (and, <>)                    */ "a and b <> c"                          to "(and (id a (case_insensitive) (unqualified)) (ne (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (and, in)                    */ "a and b in c"                          to "(and (id a (case_insensitive) (unqualified)) (in_collection (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (and, not_in)                */ "a and b not in c"                      to "(and (id a (case_insensitive) (unqualified)) (not (in_collection (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified)))))",
        /* (and, <)                     */ "a and b < c"                           to "(and (id a (case_insensitive) (unqualified)) (lt (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (and, <=)                    */ "a and b <= c"                          to "(and (id a (case_insensitive) (unqualified)) (lte (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (and, >)                     */ "a and b > c"                           to "(and (id a (case_insensitive) (unqualified)) (gt (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (and, >=)                    */ "a and b >= c"                          to "(and (id a (case_insensitive) (unqualified)) (gte (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (and, between)               */ "a and b between w and c"               to "(and (id a (case_insensitive) (unqualified)) (between (id b (case_insensitive) (unqualified)) (id w (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (and, not_between)           */ "a and b not between y and c"           to "(and (id a (case_insensitive) (unqualified)) (not (between (id b (case_insensitive) (unqualified)) (id y (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified)))))",
        /* (and, like)                  */ "a and b like c"                        to "(and (id a (case_insensitive) (unqualified)) (like (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified)) null))",
        /* (and, not_like)              */ "a and b not like c"                    to "(and (id a (case_insensitive) (unqualified)) (not (like (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified)) null)))",
        /* (and, +)                     */ "a and b + c"                           to "(and (id a (case_insensitive) (unqualified)) (plus (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (and, -)                     */ "a and b - c"                           to "(and (id a (case_insensitive) (unqualified)) (minus (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (and, ||)                    */ "a and b || c"                          to "(and (id a (case_insensitive) (unqualified)) (concat (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (and, *)                     */ "a and b * c"                           to "(and (id a (case_insensitive) (unqualified)) (times (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (and, /)                     */ "a and b / c"                           to "(and (id a (case_insensitive) (unqualified)) (divide (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (and, %)                     */ "a and b % c"                           to "(and (id a (case_insensitive) (unqualified)) (modulo (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (and, is)                    */ "a and b is boolean"                    to "(and (id a (case_insensitive) (unqualified)) (is_type (id b (case_insensitive) (unqualified)) (boolean_type)))",
        /* (and, is_not)                */ "a and b is not boolean"                to "(and (id a (case_insensitive) (unqualified)) (not (is_type (id b (case_insensitive) (unqualified)) (boolean_type))))")

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun orPrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForOrPrecedence() = listOf(
        /* (or, intersect)              */ "a or b intersect c"                    to "(intersect (distinct) (or (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (or, intersect_all)          */ "a or b intersect all c "               to "(intersect (all) (or (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (or, except)                 */ "a or b except c"                       to "(except (distinct) (or (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (or, except_all)             */ "a or b except all c "                  to "(except (all) (or (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (or, union)                  */ "a or b union c"                        to "(union (distinct) (or (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (or, union_all)              */ "a or b union all c "                   to "(union (all) (or (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (or, and)                    */ "a or b and c"                          to "(or (id a (case_insensitive) (unqualified)) (and (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (or, =)                      */ "a or b = c"                            to "(or (id a (case_insensitive) (unqualified)) (eq (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (or, <>)                     */ "a or b <> c"                           to "(or (id a (case_insensitive) (unqualified)) (ne (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (or, in)                     */ "a or b in c"                           to "(or (id a (case_insensitive) (unqualified)) (in_collection (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (or, not_in)                 */ "a or b not in c"                       to "(or (id a (case_insensitive) (unqualified)) (not (in_collection (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified)))))",
        /* (or, <)                      */ "a or b < c"                            to "(or (id a (case_insensitive) (unqualified)) (lt (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (or, <=)                     */ "a or b <= c"                           to "(or (id a (case_insensitive) (unqualified)) (lte (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (or, >)                      */ "a or b > c"                            to "(or (id a (case_insensitive) (unqualified)) (gt (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (or, >=)                     */ "a or b >= c"                           to "(or (id a (case_insensitive) (unqualified)) (gte (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (or, between)                */ "a or b between w and c"                to "(or (id a (case_insensitive) (unqualified)) (between (id b (case_insensitive) (unqualified)) (id w (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (or, not_between)            */ "a or b not between y and c"            to "(or (id a (case_insensitive) (unqualified)) (not (between (id b (case_insensitive) (unqualified)) (id y (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified)))))",
        /* (or, like)                   */ "a or b like c"                         to "(or (id a (case_insensitive) (unqualified)) (like (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified)) null))",
        /* (or, not_like)               */ "a or b not like c"                     to "(or (id a (case_insensitive) (unqualified)) (not (like (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified)) null)))",
        /* (or, +)                      */ "a or b + c"                            to "(or (id a (case_insensitive) (unqualified)) (plus (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (or, -)                      */ "a or b - c"                            to "(or (id a (case_insensitive) (unqualified)) (minus (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (or, ||)                     */ "a or b || c"                           to "(or (id a (case_insensitive) (unqualified)) (concat (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (or, *)                      */ "a or b * c"                            to "(or (id a (case_insensitive) (unqualified)) (times (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (or, /)                      */ "a or b / c"                            to "(or (id a (case_insensitive) (unqualified)) (divide (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (or, %)                      */ "a or b % c"                            to "(or (id a (case_insensitive) (unqualified)) (modulo (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (or, is)                     */ "a or b is boolean"                     to "(or (id a (case_insensitive) (unqualified)) (is_type (id b (case_insensitive) (unqualified)) (boolean_type)))",
        /* (or, is_not)                 */ "a or b is not boolean"                 to "(or (id a (case_insensitive) (unqualified)) (not (is_type (id b (case_insensitive) (unqualified)) (boolean_type))))")

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun equalsPrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForEqualsPrecedence() = listOf(
        /* (=, intersect)               */ "a = b intersect c"                     to "(intersect (distinct) (eq (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (=, intersect_all)           */ "a = b intersect all c  "               to "(intersect (all) (eq (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (=, except)                  */ "a = b except c"                        to "(except (distinct) (eq (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (=, except_all)              */ "a = b except all c  "                  to "(except (all) (eq (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (=, union)                   */ "a = b union c"                         to "(union (distinct) (eq (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (=, union_all)               */ "a = b union all c  "                   to "(union (all) (eq (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (=, or)                      */ "a = b or c"                            to "(or (eq (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (=, and)                     */ "a = b and c"                           to "(and (eq (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (=, <>)                      */ "a = b <> c"                            to "(ne (eq (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (=, in)                      */ "a = b in c"                            to "(in_collection (eq (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (=, not_in)                  */ "a = b not in c"                        to "(not (in_collection (eq (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified))))",
        /* (=, <)                       */ "a = b < c"                             to "(eq (id a (case_insensitive) (unqualified)) (lt (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (=, <=)                      */ "a = b <= c"                            to "(eq (id a (case_insensitive) (unqualified)) (lte (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (=, >)                       */ "a = b > c"                             to "(eq (id a (case_insensitive) (unqualified)) (gt (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (=, >=)                      */ "a = b >= c"                            to "(eq (id a (case_insensitive) (unqualified)) (gte (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (=, between)                 */ "a = b between w and c"                 to "(eq (id a (case_insensitive) (unqualified)) (between (id b (case_insensitive) (unqualified)) (id w (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (=, not_between)             */ "a = b not between y and c"             to "(eq (id a (case_insensitive) (unqualified)) (not (between (id b (case_insensitive) (unqualified)) (id y (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified)))))",
        /* (=, like)                    */ "a = b like c"                          to "(eq (id a (case_insensitive) (unqualified)) (like (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified)) null))",
        /* (=, not_like)                */ "a = b not like c"                      to "(eq (id a (case_insensitive) (unqualified)) (not (like (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified)) null)))",
        /* (=, +)                       */ "a = b + c"                             to "(eq (id a (case_insensitive) (unqualified)) (plus (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (=, -)                       */ "a = b - c"                             to "(eq (id a (case_insensitive) (unqualified)) (minus (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (=, ||)                      */ "a = b || c"                            to "(eq (id a (case_insensitive) (unqualified)) (concat (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (=, *)                       */ "a = b * c"                             to "(eq (id a (case_insensitive) (unqualified)) (times (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (=, /)                       */ "a = b / c"                             to "(eq (id a (case_insensitive) (unqualified)) (divide (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (=, %)                       */ "a = b % c"                             to "(eq (id a (case_insensitive) (unqualified)) (modulo (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (=, is)                      */ "a = b is boolean"                      to "(is_type (eq (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (boolean_type))",
        /* (=, is_not)                  */ "a = b is not boolean"                  to "(not (is_type (eq (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (boolean_type)))")

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun notEqualPrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForNotEqualPrecedence() = listOf(
        /* (<>, intersect)              */ "a <> b intersect c"                    to "(intersect (distinct) (ne (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (<>, intersect_all)          */ "a <> b intersect all c"                to "(intersect (all) (ne (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (<>, except)                 */ "a <> b except c"                       to "(except (distinct) (ne (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (<>, except_all)             */ "a <> b except all c"                   to "(except (all) (ne (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (<>, union)                  */ "a <> b union c"                        to "(union (distinct) (ne (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (<>, union_all)              */ "a <> b union all c"                    to "(union (all) (ne (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (<>, or)                     */ "a <> b or c"                           to "(or (ne (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (<>, and)                    */ "a <> b and c"                          to "(and (ne (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (<>, =)                      */ "a <> b = c"                            to "(eq (ne (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (<>, in)                     */ "a <> b in c"                           to "(in_collection (ne (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (<>, not_in)                 */ "a <> b not in c"                       to "(not (in_collection (ne (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified))))",
        /* (<>, <)                      */ "a <> b < c"                            to "(ne (id a (case_insensitive) (unqualified)) (lt (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (<>, <=)                     */ "a <> b <= c"                           to "(ne (id a (case_insensitive) (unqualified)) (lte (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (<>, >)                      */ "a <> b > c"                            to "(ne (id a (case_insensitive) (unqualified)) (gt (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (<>, >=)                     */ "a <> b >= c"                           to "(ne (id a (case_insensitive) (unqualified)) (gte (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (<>, between)                */ "a <> b between w and c"                to "(ne (id a (case_insensitive) (unqualified)) (between (id b (case_insensitive) (unqualified)) (id w (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (<>, not_between)            */ "a <> b not between y and c"            to "(ne (id a (case_insensitive) (unqualified)) (not (between (id b (case_insensitive) (unqualified)) (id y (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified)))))",
        /* (<>, like)                   */ "a <> b like c"                         to "(ne (id a (case_insensitive) (unqualified)) (like (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified)) null))",
        /* (<>, not_like)               */ "a <> b not like c"                     to "(ne (id a (case_insensitive) (unqualified)) (not (like (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified)) null)))",
        /* (<>, +)                      */ "a <> b + c"                            to "(ne (id a (case_insensitive) (unqualified)) (plus (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (<>, -)                      */ "a <> b - c"                            to "(ne (id a (case_insensitive) (unqualified)) (minus (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (<>, ||)                     */ "a <> b || c"                           to "(ne (id a (case_insensitive) (unqualified)) (concat (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (<>, *)                      */ "a <> b * c"                            to "(ne (id a (case_insensitive) (unqualified)) (times (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (<>, /)                      */ "a <> b / c"                            to "(ne (id a (case_insensitive) (unqualified)) (divide (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (<>, %)                      */ "a <> b % c"                            to "(ne (id a (case_insensitive) (unqualified)) (modulo (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (<>, is)                     */ "a <> b is boolean"                     to "(is_type (ne (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (boolean_type))",
        /* (<>, is_not)                 */ "a <> b is not boolean"                 to "(not (is_type (ne (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (boolean_type)))")

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun isPrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForIsPrecedence() = listOf(
        /* (is, intersect)              */ "a is boolean intersect c"              to "(intersect (distinct) (is_type (id a (case_insensitive) (unqualified)) (boolean_type)) (id c (case_insensitive) (unqualified)))",
        /* (is, intersect_all)          */ "a is boolean intersect all c"          to "(intersect (all) (is_type (id a (case_insensitive) (unqualified)) (boolean_type)) (id c (case_insensitive) (unqualified)))",
        /* (is, except)                 */ "a is boolean except c"                 to "(except (distinct) (is_type (id a (case_insensitive) (unqualified)) (boolean_type)) (id c (case_insensitive) (unqualified)))",
        /* (is, except_all)             */ "a is boolean except all c"             to "(except (all) (is_type (id a (case_insensitive) (unqualified)) (boolean_type)) (id c (case_insensitive) (unqualified)))",
        /* (is, union)                  */ "a is boolean union c"                  to "(union (distinct) (is_type (id a (case_insensitive) (unqualified)) (boolean_type)) (id c (case_insensitive) (unqualified)))",
        /* (is, union_all)              */ "a is boolean union all c"              to "(union (all) (is_type (id a (case_insensitive) (unqualified)) (boolean_type)) (id c (case_insensitive) (unqualified)))",
        /* (is, or)                     */ "a is boolean or c"                     to "(or (is_type (id a (case_insensitive) (unqualified)) (boolean_type)) (id c (case_insensitive) (unqualified)))",
        /* (is, and)                    */ "a is boolean and c"                    to "(and (is_type (id a (case_insensitive) (unqualified)) (boolean_type)) (id c (case_insensitive) (unqualified)))",
        /* (is, =)                      */ "a is boolean = c"                      to "(eq (is_type (id a (case_insensitive) (unqualified)) (boolean_type)) (id c (case_insensitive) (unqualified)))",
        /* (is, in)                     */ "a is boolean in c"                     to "(in_collection (is_type (id a (case_insensitive) (unqualified)) (boolean_type)) (id c (case_insensitive) (unqualified)))",
        /* (is, not_in)                 */ "a is boolean not in c"                 to "(not (in_collection (is_type (id a (case_insensitive) (unqualified)) (boolean_type)) (id c (case_insensitive) (unqualified))))",
        /* (is, <)                      */ "a is boolean < c"                      to "(lt (is_type (id a (case_insensitive) (unqualified)) (boolean_type)) (id c (case_insensitive) (unqualified)))",
        /* (is, <=)                     */ "a is boolean <= c"                     to "(lte (is_type (id a (case_insensitive) (unqualified)) (boolean_type)) (id c (case_insensitive) (unqualified)))",
        /* (is, >)                      */ "a is boolean > c"                      to "(gt (is_type (id a (case_insensitive) (unqualified)) (boolean_type)) (id c (case_insensitive) (unqualified)))",
        /* (is, >=)                     */ "a is boolean >= c"                     to "(gte (is_type (id a (case_insensitive) (unqualified)) (boolean_type)) (id c (case_insensitive) (unqualified)))",
        /* (is, between)                */ "a is boolean between w and c"          to "(between (is_type (id a (case_insensitive) (unqualified)) (boolean_type)) (id w (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified)))",
        /* (is, not_between)            */ "a is boolean not between y and c"      to "(not (between (is_type (id a (case_insensitive) (unqualified)) (boolean_type)) (id y (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (is, like)                   */ "a is boolean like c"                   to "(like (is_type (id a (case_insensitive) (unqualified)) (boolean_type)) (id c (case_insensitive) (unqualified)) null)",
        /* (is, not_like)               */ "a is boolean not like c"               to "(not (like (is_type (id a (case_insensitive) (unqualified)) (boolean_type)) (id c (case_insensitive) (unqualified)) null))",
        /* (is, +)                      */ "a is boolean + c"                      to "(plus (is_type (id a (case_insensitive) (unqualified)) (boolean_type)) (id c (case_insensitive) (unqualified)))",
        /* (is, -)                      */ "a is boolean - c"                      to "(minus (is_type (id a (case_insensitive) (unqualified)) (boolean_type)) (id c (case_insensitive) (unqualified)))",
        /* (is, ||)                     */ "a is boolean || c"                     to "(concat (is_type (id a (case_insensitive) (unqualified)) (boolean_type)) (id c (case_insensitive) (unqualified)))",
        /* (is, *)                      */ "a is boolean * c"                      to "(times (is_type (id a (case_insensitive) (unqualified)) (boolean_type)) (id c (case_insensitive) (unqualified)))",
        /* (is, /)                      */ "a is boolean / c"                      to "(divide (is_type (id a (case_insensitive) (unqualified)) (boolean_type)) (id c (case_insensitive) (unqualified)))",
        /* (is, %)                      */ "a is boolean % c"                      to "(modulo (is_type (id a (case_insensitive) (unqualified)) (boolean_type)) (id c (case_insensitive) (unqualified)))",
        /* (is, is_not)                 */ "a is boolean is not boolean"           to "(not (is_type (is_type (id a (case_insensitive) (unqualified)) (boolean_type)) (boolean_type)))")

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun isNotPrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForIsNotPrecedence() = listOf(
        /* (not (is, intersect)          */ "a is not boolean intersect c"          to "(intersect (distinct) (not (is_type (id a (case_insensitive) (unqualified)) (boolean_type))) (id c (case_insensitive) (unqualified)))",
        /* (not (is, intersect_all)      */ "a is not boolean intersect all c"      to "(intersect (all) (not (is_type (id a (case_insensitive) (unqualified)) (boolean_type))) (id c (case_insensitive) (unqualified)))",
        /* (not (is, except)             */ "a is not boolean except c"             to "(except (distinct) (not (is_type (id a (case_insensitive) (unqualified)) (boolean_type))) (id c (case_insensitive) (unqualified)))",
        /* (not (is, union)              */ "a is not boolean union c"              to "(union (distinct) (not (is_type (id a (case_insensitive) (unqualified)) (boolean_type))) (id c (case_insensitive) (unqualified)))",
        /* (not (is, union_all)          */ "a is not boolean union all c"          to "(union (all) (not (is_type (id a (case_insensitive) (unqualified)) (boolean_type))) (id c (case_insensitive) (unqualified)))",
        /* (not (is, or)                 */ "a is not boolean or c"                 to "(or (not (is_type (id a (case_insensitive) (unqualified)) (boolean_type))) (id c (case_insensitive) (unqualified)))",
        /* (not (is, and)                */ "a is not boolean and c"                to "(and (not (is_type (id a (case_insensitive) (unqualified)) (boolean_type))) (id c (case_insensitive) (unqualified)))",
        /* (not (is, =)                  */ "a is not boolean = c"                  to "(eq (not (is_type (id a (case_insensitive) (unqualified)) (boolean_type))) (id c (case_insensitive) (unqualified)))",
        /* (not (is, <>)                 */ "a is not boolean <> c"                 to "(ne (not (is_type (id a (case_insensitive) (unqualified)) (boolean_type))) (id c (case_insensitive) (unqualified)))",
        /* (not (is, in)                 */ "a is not boolean in c"                 to "(in_collection (not (is_type (id a (case_insensitive) (unqualified)) (boolean_type))) (id c (case_insensitive) (unqualified)))",
        /* (not (is, not_in)             */ "a is not boolean not in c"             to "(not (in_collection (not (is_type (id a (case_insensitive) (unqualified)) (boolean_type))) (id c (case_insensitive) (unqualified))))",
        /* (not (is, <)                  */ "a is not boolean < c"                  to "(lt (not (is_type (id a (case_insensitive) (unqualified)) (boolean_type))) (id c (case_insensitive) (unqualified)))",
        /* (not (is, <=)                 */ "a is not boolean <= c"                 to "(lte (not (is_type (id a (case_insensitive) (unqualified)) (boolean_type))) (id c (case_insensitive) (unqualified)))",
        /* (not (is, >)                  */ "a is not boolean > c"                  to "(gt (not (is_type (id a (case_insensitive) (unqualified)) (boolean_type))) (id c (case_insensitive) (unqualified)))",
        /* (not (is, >=)                 */ "a is not boolean >= c"                 to "(gte (not (is_type (id a (case_insensitive) (unqualified)) (boolean_type))) (id c (case_insensitive) (unqualified)))",
        /* (not (is, between)            */ "a is not boolean between w and c"      to "(between (not (is_type (id a (case_insensitive) (unqualified)) (boolean_type))) (id w (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified)))",
        /* (not (is, not_between)        */ "a is not boolean not between y and c"  to "(not (between (not (is_type (id a (case_insensitive) (unqualified)) (boolean_type))) (id y (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (not (is, like)               */ "a is not boolean like c"               to "(like (not (is_type (id a (case_insensitive) (unqualified)) (boolean_type))) (id c (case_insensitive) (unqualified)) null)",
        /* (not (is, not_like)           */ "a is not boolean not like c"           to "(not (like (not (is_type (id a (case_insensitive) (unqualified)) (boolean_type))) (id c (case_insensitive) (unqualified)) null))",
        /* (not (is, +)                  */ "a is not boolean + c"                  to "(plus (not (is_type (id a (case_insensitive) (unqualified)) (boolean_type))) (id c (case_insensitive) (unqualified)))",
        /* (not (is, -)                  */ "a is not boolean - c"                  to "(minus (not (is_type (id a (case_insensitive) (unqualified)) (boolean_type))) (id c (case_insensitive) (unqualified)))",
        /* (not (is, ||)                 */ "a is not boolean || c"                 to "(concat (not (is_type (id a (case_insensitive) (unqualified)) (boolean_type))) (id c (case_insensitive) (unqualified)))",
        /* (not (is, *)                  */ "a is not boolean * c"                  to "(times (not (is_type (id a (case_insensitive) (unqualified)) (boolean_type))) (id c (case_insensitive) (unqualified)))",
        /* (not (is, /)                  */ "a is not boolean / c"                  to "(divide (not (is_type (id a (case_insensitive) (unqualified)) (boolean_type))) (id c (case_insensitive) (unqualified)))",
        /* (not (is, %)                  */ "a is not boolean % c"                  to "(modulo (not (is_type (id a (case_insensitive) (unqualified)) (boolean_type))) (id c (case_insensitive) (unqualified)))",
        /* (not (is, is)                 */ "a is not boolean is boolean"           to "(is_type (not (is_type (id a (case_insensitive) (unqualified)) (boolean_type))) (boolean_type))")

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun inPrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForInPrecedence() = listOf(
        /* (in, intersect)              */ "a in b intersect c"                    to "(intersect (distinct) (in_collection (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (in, intersect_all)          */ "a in b intersect all c"                to "(intersect (all) (in_collection (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (in, except)                 */ "a in b except c"                       to "(except (distinct) (in_collection (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (in, except_all)             */ "a in b except all c"                   to "(except (all) (in_collection (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (in, union)                  */ "a in b union c"                        to "(union (distinct) (in_collection (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (in, union_all)              */ "a in b union all c"                    to "(union (all) (in_collection (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (in, or)                     */ "a in b or c"                           to "(or (in_collection (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (in, and)                    */ "a in b and c"                          to "(and (in_collection (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (in, =)                      */ "a in b = c"                            to "(eq (in_collection (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (in, <>)                     */ "a in b <> c"                           to "(ne (in_collection (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (in, not_in)                 */ "a in b not in c"                       to "(not (in_collection (in_collection (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified))))",
        /* (in, <)                      */ "a in b < c"                            to "(in_collection (id a (case_insensitive) (unqualified)) (lt (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (in, <=)                     */ "a in b <= c"                           to "(in_collection (id a (case_insensitive) (unqualified)) (lte (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (in, >)                      */ "a in b > c"                            to "(in_collection (id a (case_insensitive) (unqualified)) (gt (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (in, >=)                     */ "a in b >= c"                           to "(in_collection (id a (case_insensitive) (unqualified)) (gte (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (in, between)                */ "a in b between w and c"                to "(in_collection (id a (case_insensitive) (unqualified)) (between (id b (case_insensitive) (unqualified)) (id w (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (in, not_between)            */ "a in b not between y and c"            to "(in_collection (id a (case_insensitive) (unqualified)) (not (between (id b (case_insensitive) (unqualified)) (id y (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified)))))",
        /* (in, like)                   */ "a in b like c"                         to "(in_collection (id a (case_insensitive) (unqualified)) (like (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified)) null))",
        /* (in, not_like)               */ "a in b not like c"                     to "(in_collection (id a (case_insensitive) (unqualified)) (not (like (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified)) null)))",
        /* (in, +)                      */ "a in b + c"                            to "(in_collection (id a (case_insensitive) (unqualified)) (plus (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (in, -)                      */ "a in b - c"                            to "(in_collection (id a (case_insensitive) (unqualified)) (minus (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (in, ||)                     */ "a in b || c"                           to "(in_collection (id a (case_insensitive) (unqualified)) (concat (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (in, *)                      */ "a in b * c"                            to "(in_collection (id a (case_insensitive) (unqualified)) (times (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (in, /)                      */ "a in b / c"                            to "(in_collection (id a (case_insensitive) (unqualified)) (divide (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (in, %)                      */ "a in b % c"                            to "(in_collection (id a (case_insensitive) (unqualified)) (modulo (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (in, is)                     */ "a in b is boolean"                     to "(is_type (in_collection (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (boolean_type))",
        /* (in, is_not)                 */ "a in b is not boolean"                 to "(not (is_type (in_collection (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (boolean_type)))")

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun notInPrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForNotInPrecedence() = listOf(
        /* (not (in, intersect)          */ "a not in b intersect c"                to "(intersect (distinct) (not (in_collection (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)))) (id c (case_insensitive) (unqualified)))",
        /* (not (in, intersect_all)      */ "a not in b intersect all c"            to "(intersect (all) (not (in_collection (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)))) (id c (case_insensitive) (unqualified)))",
        /* (not (in, except)             */ "a not in b except c"                   to "(except (distinct) (not (in_collection (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)))) (id c (case_insensitive) (unqualified)))",
        /* (not (in, except_all)         */ "a not in b except all c"               to "(except (all) (not (in_collection (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)))) (id c (case_insensitive) (unqualified)))",
        /* (not (in, union)              */ "a not in b union c"                    to "(union (distinct) (not (in_collection (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)))) (id c (case_insensitive) (unqualified)))",
        /* (not (in, union_all)          */ "a not in b union all c"                to "(union (all) (not (in_collection (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)))) (id c (case_insensitive) (unqualified)))",
        /* (not (in, or)                 */ "a not in b or c"                       to "(or (not (in_collection (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)))) (id c (case_insensitive) (unqualified)))",
        /* (not (in, and)                */ "a not in b and c"                      to "(and (not (in_collection (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)))) (id c (case_insensitive) (unqualified)))",
        /* (not (in, =)                  */ "a not in b = c"                        to "(eq (not (in_collection (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)))) (id c (case_insensitive) (unqualified)))",
        /* (not (in, <>)                 */ "a not in b <> c"                       to "(ne (not (in_collection (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)))) (id c (case_insensitive) (unqualified)))",
        /* (not (in, in)                 */ "a not in b in c"                       to "(in_collection (not (in_collection (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)))) (id c (case_insensitive) (unqualified)))",
        /* (not (in, <)                  */ "a not in b < c"                        to "(not (in_collection (id a (case_insensitive) (unqualified)) (lt (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified)))))",
        /* (not (in, <=)                 */ "a not in b <= c"                       to "(not (in_collection (id a (case_insensitive) (unqualified)) (lte (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified)))))",
        /* (not (in, >)                  */ "a not in b > c"                        to "(not (in_collection (id a (case_insensitive) (unqualified)) (gt (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified)))))",
        /* (not (in, >=)                 */ "a not in b >= c"                       to "(not (in_collection (id a (case_insensitive) (unqualified)) (gte (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified)))))",
        /* (not (in, between)            */ "a not in b between w and c"            to "(not (in_collection (id a (case_insensitive) (unqualified)) (between (id b (case_insensitive) (unqualified)) (id w (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified)))))",
        /* (not (in, not_between)        */ "a not in b not between y and c"        to "(not (in_collection (id a (case_insensitive) (unqualified)) (not (between (id b (case_insensitive) (unqualified)) (id y (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))))",
        /* (not (in, like)               */ "a not in b like c"                     to "(not (in_collection (id a (case_insensitive) (unqualified)) (like (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified)) null)))",
        /* (not (in, not_like)           */ "a not in b not like c"                 to "(not (in_collection (id a (case_insensitive) (unqualified)) (not (like (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified)) null))))",
        /* (not (in, +)                  */ "a not in b + c"                        to "(not (in_collection (id a (case_insensitive) (unqualified)) (plus (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified)))))",
        /* (not (in, -)                  */ "a not in b - c"                        to "(not (in_collection (id a (case_insensitive) (unqualified)) (minus (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified)))))",
        /* (not (in, ||)                 */ "a not in b || c"                       to "(not (in_collection (id a (case_insensitive) (unqualified)) (concat (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified)))))",
        /* (not (in, *)                  */ "a not in b * c"                        to "(not (in_collection (id a (case_insensitive) (unqualified)) (times (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified)))))",
        /* (not (in, /)                  */ "a not in b / c"                        to "(not (in_collection (id a (case_insensitive) (unqualified)) (divide (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified)))))",
        /* (not (in, %)                  */ "a not in b % c"                        to "(not (in_collection (id a (case_insensitive) (unqualified)) (modulo (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified)))))",
        /* (not (in, is)                 */ "a not in b is boolean"                 to "(is_type (not (in_collection (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)))) (boolean_type))",
        /* (not (in, is_not)             */ "a not in b is not boolean"             to "(not (is_type (not (in_collection (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)))) (boolean_type)))")

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun ltPrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForLtPrecedence() = listOf(
        /* (<, intersect)               */ "a < b intersect c"                     to "(intersect (distinct) (lt (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (<, intersect_all)           */ "a < b intersect all c"                 to "(intersect (all) (lt (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (<, except)                  */ "a < b except c"                        to "(except (distinct) (lt (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (<, except_all)              */ "a < b except all c"                    to "(except (all) (lt (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (<, union)                   */ "a < b union c"                         to "(union (distinct) (lt (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (<, union_all)               */ "a < b union all c"                     to "(union (all) (lt (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (<, or)                      */ "a < b or c"                            to "(or (lt (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (<, and)                     */ "a < b and c"                           to "(and (lt (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (<, =)                       */ "a < b = c"                             to "(eq (lt (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (<, <>)                      */ "a < b <> c"                            to "(ne (lt (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (<, in)                      */ "a < b in c"                            to "(in_collection (lt (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (<, not_in)                  */ "a < b not in c"                        to "(not (in_collection (lt (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified))))",
        /* (<, <=)                      */ "a < b <= c"                            to "(lte (lt (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (<, >)                       */ "a < b > c"                             to "(gt (lt (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (<, >=)                      */ "a < b >= c"                            to "(gte (lt (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (<, between)                 */ "a < b between w and c"                 to "(between (lt (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id w (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified)))",
        /* (<, not_between)             */ "a < b not between y and c"             to "(not (between (lt (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id y (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (<, like)                    */ "a < b like c"                          to "(like (lt (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)) null)",
        /* (<, not_like)                */ "a < b not like c"                      to "(not (like (lt (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)) null))",
        /* (<, +)                       */ "a < b + c"                             to "(lt (id a (case_insensitive) (unqualified)) (plus (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (<, -)                       */ "a < b - c"                             to "(lt (id a (case_insensitive) (unqualified)) (minus (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (<, ||)                      */ "a < b || c"                            to "(lt (id a (case_insensitive) (unqualified)) (concat (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (<, *)                       */ "a < b * c"                             to "(lt (id a (case_insensitive) (unqualified)) (times (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (<, /)                       */ "a < b / c"                             to "(lt (id a (case_insensitive) (unqualified)) (divide (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (<, %)                       */ "a < b % c"                             to "(lt (id a (case_insensitive) (unqualified)) (modulo (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (<, is)                      */ "a < b is boolean"                      to "(is_type (lt (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (boolean_type))",
        /* (<, is_not)                  */ "a < b is not boolean"                  to "(not (is_type (lt (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (boolean_type)))")

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun ltePrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForLtePrecedence() = listOf(
        /* (<=, intersect)              */ "a <= b intersect c"                    to "(intersect (distinct) (lte (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (<=, intersect_all)          */ "a <= b intersect all c"                to "(intersect (all) (lte (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (<=, except)                 */ "a <= b except c"                       to "(except (distinct) (lte (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (<=, except_all)             */ "a <= b except all c"                   to "(except (all) (lte (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (<=, union)                  */ "a <= b union c"                        to "(union (distinct) (lte (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (<=, union_all)              */ "a <= b union all c"                    to "(union (all) (lte (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (<=, or)                     */ "a <= b or c"                           to "(or (lte (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (<=, and)                    */ "a <= b and c"                          to "(and (lte (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (<=, =)                      */ "a <= b = c"                            to "(eq (lte (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (<=, <>)                     */ "a <= b <> c"                           to "(ne (lte (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (<=, in)                     */ "a <= b in c"                           to "(in_collection (lte (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (<=, not_in)                 */ "a <= b not in c"                       to "(not (in_collection (lte (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified))))",
        /* (<=, <)                      */ "a <= b < c"                            to "(lt (lte (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (<=, >)                      */ "a <= b > c"                            to "(gt (lte (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (<=, >=)                     */ "a <= b >= c"                           to "(gte (lte (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (<=, between)                */ "a <= b between w and c"                to "(between (lte (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id w (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified)))",
        /* (<=, not_between)            */ "a <= b not between y and c"            to "(not (between (lte (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id y (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (<=, like)                   */ "a <= b like c"                         to "(like (lte (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)) null)",
        /* (<=, not_like)               */ "a <= b not like c"                     to "(not (like (lte (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)) null))",
        /* (<=, +)                      */ "a <= b + c"                            to "(lte (id a (case_insensitive) (unqualified)) (plus (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (<=, -)                      */ "a <= b - c"                            to "(lte (id a (case_insensitive) (unqualified)) (minus (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (<=, ||)                     */ "a <= b || c"                           to "(lte (id a (case_insensitive) (unqualified)) (concat (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (<=, *)                      */ "a <= b * c"                            to "(lte (id a (case_insensitive) (unqualified)) (times (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (<=, /)                      */ "a <= b / c"                            to "(lte (id a (case_insensitive) (unqualified)) (divide (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (<=, %)                      */ "a <= b % c"                            to "(lte (id a (case_insensitive) (unqualified)) (modulo (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (<=, is)                     */ "a <= b is boolean"                     to "(is_type (lte (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (boolean_type))",
        /* (<=, is_not)                 */ "a <= b is not boolean"                 to "(not (is_type (lte (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (boolean_type)))")

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun gtPrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForGtPrecedence() = listOf(
        /* (>, intersect)               */ "a > b intersect c"                     to "(intersect (distinct) (gt (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (>, intersect_all)           */ "a > b intersect all c"                 to "(intersect (all) (gt (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (>, except)                  */ "a > b except c"                        to "(except (distinct) (gt (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (>, except_all)              */ "a > b except all c"                    to "(except (all) (gt (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (>, union)                   */ "a > b union c"                         to "(union (distinct) (gt (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (>, union_all)               */ "a > b union all c"                     to "(union (all) (gt (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (>, or)                      */ "a > b or c"                            to "(or (gt (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (>, and)                     */ "a > b and c"                           to "(and (gt (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (>, =)                       */ "a > b = c"                             to "(eq (gt (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (>, <>)                      */ "a > b <> c"                            to "(ne (gt (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (>, in)                      */ "a > b in c"                            to "(in_collection (gt (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (>, not_in)                  */ "a > b not in c"                        to "(not (in_collection (gt (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified))))",
        /* (>, <)                       */ "a > b < c"                             to "(lt (gt (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (>, <=)                      */ "a > b <= c"                            to "(lte (gt (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (>, >=)                      */ "a > b >= c"                            to "(gte (gt (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (>, between)                 */ "a > b between w and c"                 to "(between (gt (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id w (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified)))",
        /* (>, not_between)             */ "a > b not between y and c"             to "(not (between (gt (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id y (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (>, like)                    */ "a > b like c"                          to "(like (gt (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)) null)",
        /* (>, not_like)                */ "a > b not like c"                      to "(not (like (gt (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)) null))",
        /* (>, +)                       */ "a > b + c"                             to "(gt (id a (case_insensitive) (unqualified)) (plus (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (>, -)                       */ "a > b - c"                             to "(gt (id a (case_insensitive) (unqualified)) (minus (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (>, ||)                      */ "a > b || c"                            to "(gt (id a (case_insensitive) (unqualified)) (concat (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (>, *)                       */ "a > b * c"                             to "(gt (id a (case_insensitive) (unqualified)) (times (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (>, /)                       */ "a > b / c"                             to "(gt (id a (case_insensitive) (unqualified)) (divide (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (>, %)                       */ "a > b % c"                             to "(gt (id a (case_insensitive) (unqualified)) (modulo (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (>, is)                      */ "a > b is boolean"                      to "(is_type (gt (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (boolean_type))",
        /* (>, is_not)                  */ "a > b is not boolean"                  to "(not (is_type (gt (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (boolean_type)))")

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun gtePrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForGtePrecedence() = listOf(
        /* (>=, intersect)              */ "a >= b intersect c"                    to "(intersect (distinct) (gte (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (>=, intersect_all)          */ "a >= b intersect all c"                to "(intersect (all) (gte (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (>=, except)                 */ "a >= b except c"                       to "(except (distinct) (gte (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (>=, except_all)             */ "a >= b except all c"                   to "(except (all) (gte (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (>=, union)                  */ "a >= b union c"                        to "(union (distinct) (gte (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (>=, union_all)              */ "a >= b union all c"                    to "(union (all) (gte (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (>=, or)                     */ "a >= b or c"                           to "(or (gte (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (>=, and)                    */ "a >= b and c"                          to "(and (gte (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (>=, =)                      */ "a >= b = c"                            to "(eq (gte (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (>=, <>)                     */ "a >= b <> c"                           to "(ne (gte (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (>=, in)                     */ "a >= b in c"                           to "(in_collection (gte (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (>=, not_in)                 */ "a >= b not in c"                       to "(not (in_collection (gte (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified))))",
        /* (>=, <)                      */ "a >= b < c"                            to "(lt (gte (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (>=, <=)                     */ "a >= b <= c"                           to "(lte (gte (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (>=, >)                      */ "a >= b > c"                            to "(gt (gte (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (>=, between)                */ "a >= b between w and c"                to "(between (gte (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id w (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified)))",
        /* (>=, not_between)            */ "a >= b not between y and c"            to "(not (between (gte (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id y (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (>=, like)                   */ "a >= b like c"                         to "(like (gte (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)) null)",
        /* (>=, not_like)               */ "a >= b not like c"                     to "(not (like (gte (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)) null))",
        /* (>=, +)                      */ "a >= b + c"                            to "(gte (id a (case_insensitive) (unqualified)) (plus (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (>=, -)                      */ "a >= b - c"                            to "(gte (id a (case_insensitive) (unqualified)) (minus (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (>=, ||)                     */ "a >= b || c"                           to "(gte (id a (case_insensitive) (unqualified)) (concat (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (>=, *)                      */ "a >= b * c"                            to "(gte (id a (case_insensitive) (unqualified)) (times (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (>=, /)                      */ "a >= b / c"                            to "(gte (id a (case_insensitive) (unqualified)) (divide (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (>=, %)                      */ "a >= b % c"                            to "(gte (id a (case_insensitive) (unqualified)) (modulo (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (>=, is)                     */ "a >= b is boolean"                     to "(is_type (gte (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (boolean_type))",
        /* (>=, is_not)                 */ "a >= b is not boolean"                 to "(not (is_type (gte (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (boolean_type)))")

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun betweenPrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForBetweenPrecedence() = listOf(
        /* (between, intersect)         */ "a between b and w intersect c"         to "(intersect (distinct) (between (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)) (id w (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (between, intersect_all)     */ "a between b and w intersect all c"     to "(intersect (all) (between (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)) (id w (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (between, except)            */ "a between b and w except c"            to "(except (distinct) (between (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)) (id w (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (between, except_all)        */ "a between b and w except all c"        to "(except (all) (between (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)) (id w (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (between, union)             */ "a between b and w union c"             to "(union (distinct) (between (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)) (id w (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (between, union_all)         */ "a between b and w union all c"         to "(union (all) (between (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)) (id w (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (between, or)                */ "a between w and b or c"                to "(or (between (id a (case_insensitive) (unqualified)) (id w (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (between, and)               */ "a between w and b and c"               to "(and (between (id a (case_insensitive) (unqualified)) (id w (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (between, =)                 */ "a between w and b = c"                 to "(eq (between (id a (case_insensitive) (unqualified)) (id w (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (between, <>)                */ "a between w and b <> c"                to "(ne (between (id a (case_insensitive) (unqualified)) (id w (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (between, in)                */ "a between w and b in c"                to "(in_collection (between (id a (case_insensitive) (unqualified)) (id w (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (between, not_in)            */ "a between w and b not in c"            to "(not (in_collection (between (id a (case_insensitive) (unqualified)) (id w (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified))))",
        /* (between, <)                 */ "a between w and b < c"                 to "(lt (between (id a (case_insensitive) (unqualified)) (id w (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (between, <=)                */ "a between w and b <= c"                to "(lte (between (id a (case_insensitive) (unqualified)) (id w (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (between, >)                 */ "a between w and b > c"                 to "(gt (between (id a (case_insensitive) (unqualified)) (id w (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (between, >=)                */ "a between w and b >= c"                to "(gte (between (id a (case_insensitive) (unqualified)) (id w (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (between, not_between)       */ "a between w and b not between y and c" to "(not (between (between (id a (case_insensitive) (unqualified)) (id w (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id y (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (between, like)              */ "a between w and b like c"              to "(like (between (id a (case_insensitive) (unqualified)) (id w (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)) null)",
        /* (between, not_like)          */ "a between w and b not like c"          to "(not (like (between (id a (case_insensitive) (unqualified)) (id w (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)) null))",
        /* (between, +)                 */ "a between w and b + c"                 to "(between (id a (case_insensitive) (unqualified)) (id w (case_insensitive) (unqualified))  (plus (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (between, -)                 */ "a between w and b - c"                 to "(between (id a (case_insensitive) (unqualified)) (id w (case_insensitive) (unqualified))  (minus (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (between, ||)                */ "a between w and b || c"                to "(between (id a (case_insensitive) (unqualified)) (id w (case_insensitive) (unqualified))  (concat (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (between, *)                 */ "a between w and b * c"                 to "(between (id a (case_insensitive) (unqualified)) (id w (case_insensitive) (unqualified))  (times (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (between, /)                 */ "a between w and b / c"                 to "(between (id a (case_insensitive) (unqualified)) (id w (case_insensitive) (unqualified))  (divide (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (between, %)                 */ "a between w and b % c"                 to "(between (id a (case_insensitive) (unqualified)) (id w (case_insensitive) (unqualified))  (modulo (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (between, is)                */ "a between w and b is boolean"          to "(is_type (between (id a (case_insensitive) (unqualified)) (id w (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (boolean_type))",
        /* (between, is_not)            */ "a between w and b is not boolean"      to "(not (is_type (between (id a (case_insensitive) (unqualified)) (id w (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (boolean_type)))")

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun notBetweenPrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForNotBetweenPrecedence() = listOf(
        /* (not (between, intersect)     */ "a not between w and b intersect c"     to "(intersect (distinct) (not (between (id a (case_insensitive) (unqualified)) (id w (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)))) (id c (case_insensitive) (unqualified)))",
        /* (not (between, intersect_all) */ "a not between w and b intersect all c" to "(intersect (all) (not (between (id a (case_insensitive) (unqualified)) (id w (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)))) (id c (case_insensitive) (unqualified)))",
        /* (not (between, except)        */ "a not between w and b except c"        to "(except (distinct) (not (between (id a (case_insensitive) (unqualified)) (id w (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)))) (id c (case_insensitive) (unqualified)))",
        /* (not (between, except_all)    */ "a not between w and b except all c"    to "(except (all) (not (between (id a (case_insensitive) (unqualified)) (id w (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)))) (id c (case_insensitive) (unqualified)))",
        /* (not (between, union)         */ "a not between w and b union c"         to "(union (distinct) (not (between (id a (case_insensitive) (unqualified)) (id w (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)))) (id c (case_insensitive) (unqualified)))",
        /* (not (between, union_all)     */ "a not between w and b union all c"     to "(union (all) (not (between (id a (case_insensitive) (unqualified)) (id w (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)))) (id c (case_insensitive) (unqualified)))",
        /* (not (between, or)            */ "a not between y and b or c"            to "(or (not (between (id a (case_insensitive) (unqualified)) (id y (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)))) (id c (case_insensitive) (unqualified)))",
        /* (not (between, and)           */ "a not between y and b and c"           to "(and (not (between (id a (case_insensitive) (unqualified)) (id y (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)))) (id c (case_insensitive) (unqualified)))",
        /* (not (between, =)             */ "a not between y and b = c"             to "(eq (not (between (id a (case_insensitive) (unqualified)) (id y (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)))) (id c (case_insensitive) (unqualified)))",
        /* (not (between, <>)            */ "a not between y and b <> c"            to "(ne (not (between (id a (case_insensitive) (unqualified)) (id y (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)))) (id c (case_insensitive) (unqualified)))",
        /* (not (between, in)            */ "a not between y and b in c"            to "(in_collection (not (between (id a (case_insensitive) (unqualified)) (id y (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)))) (id c (case_insensitive) (unqualified)))",
        /* (not (between, not_in)        */ "a not between y and b not in c"        to "(not (in_collection (not (between (id a (case_insensitive) (unqualified)) (id y (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)))) (id c (case_insensitive) (unqualified))))",
        /* (not (between, <)             */ "a not between y and b < c"             to "(lt (not (between (id a (case_insensitive) (unqualified)) (id y (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)))) (id c (case_insensitive) (unqualified)))",
        /* (not (between, <=)            */ "a not between y and b <= c"            to "(lte (not (between (id a (case_insensitive) (unqualified)) (id y (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)))) (id c (case_insensitive) (unqualified)))",
        /* (not (between, >)             */ "a not between y and b > c"             to "(gt (not (between (id a (case_insensitive) (unqualified)) (id y (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)))) (id c (case_insensitive) (unqualified)))",
        /* (not (between, >=)            */ "a not between y and b >= c"            to "(gte (not (between (id a (case_insensitive) (unqualified)) (id y (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)))) (id c (case_insensitive) (unqualified)))",
        /* (not (between, between)       */ "a not between y and b between w and c" to "(between (not (between (id a (case_insensitive) (unqualified)) (id y (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)))) (id w (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified)))",
        /* (not (between, like)          */ "a not between y and b like c"          to "(like (not (between (id a (case_insensitive) (unqualified)) (id y (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)))) (id c (case_insensitive) (unqualified)) null)",
        /* (not (between, not_like)      */ "a not between y and b not like c"      to "(not (like (not (between (id a (case_insensitive) (unqualified)) (id y (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)))) (id c (case_insensitive) (unqualified)) null))",
        /* (not (between, +)             */ "a not between y and b + c"             to "(not (between (id a (case_insensitive) (unqualified)) (id y (case_insensitive) (unqualified)) (plus (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified)))))",
        /* (not (between, -)             */ "a not between y and b - c"             to "(not (between (id a (case_insensitive) (unqualified)) (id y (case_insensitive) (unqualified)) (minus (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified)))))",
        /* (not (between, ||)            */ "a not between y and b || c"            to "(not (between (id a (case_insensitive) (unqualified)) (id y (case_insensitive) (unqualified)) (concat (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified)))))",
        /* (not (between, *)             */ "a not between y and b * c"             to "(not (between (id a (case_insensitive) (unqualified)) (id y (case_insensitive) (unqualified)) (times (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified)))))",
        /* (not (between, /)             */ "a not between y and b / c"             to "(not (between (id a (case_insensitive) (unqualified)) (id y (case_insensitive) (unqualified)) (divide (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified)))))",
        /* (not (between, %)             */ "a not between y and b % c"             to "(not (between (id a (case_insensitive) (unqualified)) (id y (case_insensitive) (unqualified)) (modulo (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified)))))",
        /* (not (between, is)            */ "a not between y and b is boolean"      to "(is_type (not (between (id a (case_insensitive) (unqualified)) (id y (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)))) (boolean_type))",
        /* (not (between, is_not)        */ "a not between y and b is not boolean"  to "(not (is_type (not (between (id a (case_insensitive) (unqualified)) (id y (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)))) (boolean_type)))")

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun likePrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForLikePrecedence() = listOf(
        /* (like, intersect)            */ "a like b intersect c"                  to "(intersect (distinct) (like (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)) null) (id c (case_insensitive) (unqualified)))",
        /* (like, intersect_all)        */ "a like b intersect all c"              to "(intersect (all) (like (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)) null) (id c (case_insensitive) (unqualified)))",
        /* (like, except)               */ "a like b except c"                     to "(except (distinct) (like (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)) null) (id c (case_insensitive) (unqualified)))",
        /* (like, except_all)           */ "a like b except all c"                 to "(except (all) (like (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)) null) (id c (case_insensitive) (unqualified)))",
        /* (like, union)                */ "a like b union c"                      to "(union (distinct) (like (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)) null) (id c (case_insensitive) (unqualified)))",
        /* (like, union_all)            */ "a like b union all c"                  to "(union (all) (like (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)) null) (id c (case_insensitive) (unqualified)))",
        /* (like, or)                   */ "a like b or c"                         to "(or (like (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)) null) (id c (case_insensitive) (unqualified)))",
        /* (like, and)                  */ "a like b and c"                        to "(and (like (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)) null) (id c (case_insensitive) (unqualified)))",
        /* (like, =)                    */ "a like b = c"                          to "(eq (like (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)) null) (id c (case_insensitive) (unqualified)))",
        /* (like, <>)                   */ "a like b <> c"                         to "(ne (like (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)) null) (id c (case_insensitive) (unqualified)))",
        /* (like, in)                   */ "a like b in c"                         to "(in_collection (like (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)) null) (id c (case_insensitive) (unqualified)))",
        /* (like, not_in)               */ "a like b not in c"                     to "(not (in_collection (like (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)) null) (id c (case_insensitive) (unqualified))))",
        /* (like, <)                    */ "a like b < c"                          to "(lt (like (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)) null) (id c (case_insensitive) (unqualified)))",
        /* (like, <=)                   */ "a like b <= c"                         to "(lte (like (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)) null) (id c (case_insensitive) (unqualified)))",
        /* (like, >)                    */ "a like b > c"                          to "(gt (like (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)) null) (id c (case_insensitive) (unqualified)))",
        /* (like, >=)                   */ "a like b >= c"                         to "(gte (like (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)) null) (id c (case_insensitive) (unqualified)))",
        /* (like, between)              */ "a like b between w and c"              to "(between (like (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)) null) (id w (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified)))",
        /* (like, not_between)          */ "a like b not between y and c"          to "(not (between (like (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)) null) (id y (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (like, not_like)             */ "a like b not like c"                   to "(not (like (like (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)) null) (id c (case_insensitive) (unqualified)) null))",
        /* (like, +)                    */ "a like b + c"                          to "(like (id a (case_insensitive) (unqualified)) (plus (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))) null)",
        /* (like, -)                    */ "a like b - c"                          to "(like (id a (case_insensitive) (unqualified)) (minus (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))) null)",
        /* (like, ||)                   */ "a like b || c"                         to "(like (id a (case_insensitive) (unqualified)) (concat (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))) null)",
        /* (like, *)                    */ "a like b * c"                          to "(like (id a (case_insensitive) (unqualified)) (times (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))) null)",
        /* (like, /)                    */ "a like b / c"                          to "(like (id a (case_insensitive) (unqualified)) (divide (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))) null)",
        /* (like, %)                    */ "a like b % c"                          to "(like (id a (case_insensitive) (unqualified)) (modulo (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))) null)",
        /* (like, is)                   */ "a like b is boolean"                   to "(is_type (like (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)) null) (boolean_type))",
        /* (like, is_not)               */ "a like b is not boolean"               to "(not (is_type (like (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)) null) (boolean_type)))")

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun notLikePrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForNotLikePrecedence() = listOf(
        /* (not (like, intersect)        */ "a not like b intersect c"              to "(intersect (distinct) (not (like (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)) null)) (id c (case_insensitive) (unqualified)))",
        /* (not (like, intersect_all)    */ "a not like b intersect all c"          to "(intersect (all) (not (like (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)) null)) (id c (case_insensitive) (unqualified)))",
        /* (not (like, except)           */ "a not like b except c"                 to "(except (distinct) (not (like (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)) null)) (id c (case_insensitive) (unqualified)))",
        /* (not (like, except_all)       */ "a not like b except all c"             to "(except (all) (not (like (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)) null)) (id c (case_insensitive) (unqualified)))",
        /* (not (like, union)            */ "a not like b union c"                  to "(union (distinct) (not (like (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)) null)) (id c (case_insensitive) (unqualified)))",
        /* (not (like, union_all)        */ "a not like b union all c"              to "(union (all) (not (like (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)) null)) (id c (case_insensitive) (unqualified)))",
        /* (not (like, or)               */ "a not like b or c"                     to "(or (not (like (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)) null)) (id c (case_insensitive) (unqualified)))",
        /* (not (like, and)              */ "a not like b and c"                    to "(and (not (like (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)) null)) (id c (case_insensitive) (unqualified)))",
        /* (not (like, =)                */ "a not like b = c"                      to "(eq (not (like (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)) null)) (id c (case_insensitive) (unqualified)))",
        /* (not (like, <>)               */ "a not like b <> c"                     to "(ne (not (like (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)) null)) (id c (case_insensitive) (unqualified)))",
        /* (not (like, in)               */ "a not like b in c"                     to "(in_collection (not (like (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)) null)) (id c (case_insensitive) (unqualified)))",
        /* (not (like, not_in)           */ "a not like b not in c"                 to "(not (in_collection (not (like (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)) null)) (id c (case_insensitive) (unqualified))))",
        /* (not (like, <)                */ "a not like b < c"                      to "(lt (not (like (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)) null)) (id c (case_insensitive) (unqualified)))",
        /* (not (like, <=)               */ "a not like b <= c"                     to "(lte (not (like (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)) null)) (id c (case_insensitive) (unqualified)))",
        /* (not (like, >)                */ "a not like b > c"                      to "(gt (not (like (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)) null)) (id c (case_insensitive) (unqualified)))",
        /* (not (like, >=)               */ "a not like b >= c"                     to "(gte (not (like (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)) null)) (id c (case_insensitive) (unqualified)))",
        /* (not (like, between)          */ "a not like b between w and c"          to "(between (not (like (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)) null)) (id w (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified)))",
        /* (not (like, not_between)      */ "a not like b not between y and c"      to "(not (between (not (like (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)) null)) (id y (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (not (like, like)             */ "a not like b like c"                   to "(like (not (like (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)) null)) (id c (case_insensitive) (unqualified)) null)",
        /* (not (like, +)                */ "a not like b + c"                      to "(not (like (id a (case_insensitive) (unqualified)) (plus (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))) null))",
        /* (not (like, -)                */ "a not like b - c"                      to "(not (like (id a (case_insensitive) (unqualified)) (minus (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))) null))",
        /* (not (like, ||)               */ "a not like b || c"                     to "(not (like (id a (case_insensitive) (unqualified)) (concat (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))) null))",
        /* (not (like, *)                */ "a not like b * c"                      to "(not (like (id a (case_insensitive) (unqualified)) (times (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))) null))",
        /* (not (like, /)                */ "a not like b / c"                      to "(not (like (id a (case_insensitive) (unqualified)) (divide (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))) null))",
        /* (not (like, %)                */ "a not like b % c"                      to "(not (like (id a (case_insensitive) (unqualified)) (modulo (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))) null))",
        /* (not (like, is)               */ "a not like b is boolean"               to "(is_type (not (like (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)) null)) (boolean_type))",
        /* (not (like, is_not)           */ "a not like b is not boolean"           to "(not (is_type (not (like (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)) null)) (boolean_type)))")

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun subtractPrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForSubtractPrecedence() = listOf(
        /* (+, intersect)               */ "a + b intersect c"                     to "(intersect (distinct) (plus (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (+, intersect_all)           */ "a + b intersect all c"                 to "(intersect (all) (plus (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (+, except)                  */ "a + b except c"                        to "(except (distinct) (plus (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (+, except_all)              */ "a + b except all c"                    to "(except (all) (plus (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (+, union)                   */ "a + b union c"                         to "(union (distinct) (plus (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (+, union_all)               */ "a + b union all c"                     to "(union (all) (plus (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (+, or)                      */ "a + b or c"                            to "(or (plus (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (+, and)                     */ "a + b and c"                           to "(and (plus (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (+, =)                       */ "a + b = c"                             to "(eq (plus (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (+, <>)                      */ "a + b <> c"                            to "(ne (plus (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (+, in)                      */ "a + b in c"                            to "(in_collection (plus (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (+, not_in)                  */ "a + b not in c"                        to "(not (in_collection (plus (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified))))",
        /* (+, <)                       */ "a + b < c"                             to "(lt (plus (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (+, <=)                      */ "a + b <= c"                            to "(lte (plus (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (+, >)                       */ "a + b > c"                             to "(gt (plus (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (+, >=)                      */ "a + b >= c"                            to "(gte (plus (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (+, between)                 */ "a + b between w and c"                 to "(between (plus (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id w (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified)))",
        /* (+, not_between)             */ "a + b not between y and c"             to "(not (between (plus (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id y (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (+, like)                    */ "a + b like c"                          to "(like (plus (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)) null)",
        /* (+, not_like)                */ "a + b not like c"                      to "(not (like (plus (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)) null))",
        /* (+, -)                       */ "a + b - c"                             to "(minus (plus (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (+, ||)                      */ "a + b || c"                            to "(concat (plus (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (+, *)                       */ "a + b * c"                             to "(plus (id a (case_insensitive) (unqualified)) (times (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (+, /)                       */ "a + b / c"                             to "(plus (id a (case_insensitive) (unqualified)) (divide (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (+, %)                       */ "a + b % c"                             to "(plus (id a (case_insensitive) (unqualified)) (modulo (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (+, is)                      */ "a + b is boolean"                      to "(is_type (plus (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (boolean_type))",
        /* (+, is_not)                  */ "a + b is not boolean"                  to "(not (is_type (plus (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (boolean_type)))")

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun minusPrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForMinusPrecedence() = listOf(
        /* (-, intersect)               */ "a - b intersect c"                     to "(intersect (distinct) (minus (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (-, intersect_all)           */ "a - b intersect all c"                 to "(intersect (all) (minus (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (-, except)                  */ "a - b except c"                        to "(except (distinct) (minus (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (-, except_all)              */ "a - b except all c"                    to "(except (all) (minus (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (-, union)                   */ "a - b union c"                         to "(union (distinct) (minus (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (-, union_all)               */ "a - b union all c"                     to "(union (all) (minus (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (-, or)                      */ "a - b or c"                            to "(or (minus (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (-, and)                     */ "a - b and c"                           to "(and (minus (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (-, =)                       */ "a - b = c"                             to "(eq (minus (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (-, <>)                      */ "a - b <> c"                            to "(ne (minus (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (-, in)                      */ "a - b in c"                            to "(in_collection (minus (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (-, not_in)                  */ "a - b not in c"                        to "(not (in_collection (minus (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified))))",
        /* (-, <)                       */ "a - b < c"                             to "(lt (minus (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (-, <=)                      */ "a - b <= c"                            to "(lte (minus (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (-, >)                       */ "a - b > c"                             to "(gt (minus (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (-, >=)                      */ "a - b >= c"                            to "(gte (minus (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (-, between)                 */ "a - b between w and c"                 to "(between (minus (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id w (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified)))",
        /* (-, not_between)             */ "a - b not between y and c"             to "(not (between (minus (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id y (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (-, like)                    */ "a - b like c"                          to "(like (minus (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)) null)",
        /* (-, not_like)                */ "a - b not like c"                      to "(not (like (minus (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)) null))",
        /* (-, +)                       */ "a - b + c"                             to "(plus (minus (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (-, ||)                      */ "a - b || c"                            to "(concat (minus (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (-, *)                       */ "a - b * c"                             to "(minus (id a (case_insensitive) (unqualified)) (times (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (-, /)                       */ "a - b / c"                             to "(minus (id a (case_insensitive) (unqualified)) (divide (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (-, %)                       */ "a - b % c"                             to "(minus (id a (case_insensitive) (unqualified)) (modulo (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (-, is)                      */ "a - b is boolean"                      to "(is_type (minus (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (boolean_type))",
        /* (-, is_not)                  */ "a - b is not boolean"                  to "(not (is_type (minus (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (boolean_type)))")

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun concatPrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForConcatPrecedence() = listOf(
        /* (||, intersect)              */ "a || b intersect c"                    to "(intersect (distinct) (concat (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (||, intersect_all)          */ "a || b intersect all c"                to "(intersect (all) (concat (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (||, except)                 */ "a || b except c"                       to "(except (distinct) (concat (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (||, except_all)             */ "a || b except all c"                   to "(except (all) (concat (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (||, union)                  */ "a || b union c"                        to "(union (distinct) (concat (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (||, union_all)              */ "a || b union all c"                    to "(union (all) (concat (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (||, or)                     */ "a || b or c"                           to "(or (concat (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (||, and)                    */ "a || b and c"                          to "(and (concat (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (||, =)                      */ "a || b = c"                            to "(eq (concat (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (||, <>)                     */ "a || b <> c"                           to "(ne (concat (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (||, in)                     */ "a || b in c"                           to "(in_collection (concat (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (||, not_in)                 */ "a || b not in c"                       to "(not (in_collection (concat (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified))))",
        /* (||, <)                      */ "a || b < c"                            to "(lt (concat (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (||, <=)                     */ "a || b <= c"                           to "(lte (concat (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (||, >)                      */ "a || b > c"                            to "(gt (concat (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (||, >=)                     */ "a || b >= c"                           to "(gte (concat (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (||, between)                */ "a || b between w and c"                to "(between (concat (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id w (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified)))",
        /* (||, not_between)            */ "a || b not between y and c"            to "(not (between (concat (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id y (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (||, like)                   */ "a || b like c"                         to "(like (concat (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)) null)",
        /* (||, not_like)               */ "a || b not like c"                     to "(not (like (concat (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)) null))",
        /* (||, +)                      */ "a || b + c"                            to "(plus (concat (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (||, -)                      */ "a || b - c"                            to "(minus (concat (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (||, *)                      */ "a || b * c"                            to "(concat (id a (case_insensitive) (unqualified)) (times (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (||, /)                      */ "a || b / c"                            to "(concat (id a (case_insensitive) (unqualified)) (divide (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (||, %)                      */ "a || b % c"                            to "(concat (id a (case_insensitive) (unqualified)) (modulo (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (||, is)                     */ "a || b is boolean"                     to "(is_type (concat (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (boolean_type))",
        /* (||, is_not)                 */ "a || b is not boolean"                 to "(not (is_type (concat (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (boolean_type)))")

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun mulPrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForMulPrecedence() = listOf(
        /* (*, intersect)               */ "a * b intersect c"                     to "(intersect (distinct) (times (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (*, intersect_all)           */ "a * b intersect all c"                 to "(intersect (all) (times (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (*, except)                  */ "a * b except c"                        to "(except (distinct) (times (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (*, except_all)              */ "a * b except all c"                    to "(except (all) (times (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (*, union)                   */ "a * b union c"                         to "(union (distinct) (times (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (*, union_all)               */ "a * b union all c"                     to "(union (all) (times (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (*, or)                      */ "a * b or c"                            to "(or (times (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (*, and)                     */ "a * b and c"                           to "(and (times (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (*, =)                       */ "a * b = c"                             to "(eq (times (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (*, <>)                      */ "a * b <> c"                            to "(ne (times (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (*, in)                      */ "a * b in c"                            to "(in_collection (times (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (*, not_in)                  */ "a * b not in c"                        to "(not (in_collection (times (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified))))",
        /* (*, <)                       */ "a * b < c"                             to "(lt (times (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (*, <=)                      */ "a * b <= c"                            to "(lte (times (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (*, >)                       */ "a * b > c"                             to "(gt (times (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (*, >=)                      */ "a * b >= c"                            to "(gte (times (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (*, between)                 */ "a * b between w and c"                 to "(between (times (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id w (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified)))",
        /* (*, not_between)             */ "a * b not between y and c"             to "(not (between (times (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id y (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (*, like)                    */ "a * b like c"                          to "(like (times (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)) null)",
        /* (*, not_like)                */ "a * b not like c"                      to "(not (like (times (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)) null))",
        /* (*, +)                       */ "a * b + c"                             to "(plus (times (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (*, -)                       */ "a * b - c"                             to "(minus (times (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (*, ||)                      */ "a * b || c"                            to "(concat (times (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (*, /)                       */ "a * b / c"                             to "(divide (times (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (*, %)                       */ "a * b % c"                             to "(modulo (times (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (*, is)                      */ "a * b is boolean"                      to "(is_type (times (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (boolean_type))",
        /* (*, is_not)                  */ "a * b is not boolean"                  to "(not (is_type (times (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (boolean_type)))")

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun divPrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForDivPrecedence() = listOf(
        /* (/, intersect)               */ "a / b intersect c"                     to "(intersect (distinct) (divide (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (/, intersect_all)           */ "a / b intersect all c"                 to "(intersect (all) (divide (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (/, except)                  */ "a / b except c"                        to "(except (distinct) (divide (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (/, except_all)              */ "a / b except all c"                    to "(except (all) (divide (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (/, union)                   */ "a / b union c"                         to "(union (distinct) (divide (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (/, union_all)               */ "a / b union all c"                     to "(union (all) (divide (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (/, or)                      */ "a / b or c"                            to "(or (divide (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (/, and)                     */ "a / b and c"                           to "(and (divide (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (/, =)                       */ "a / b = c"                             to "(eq (divide (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (/, <>)                      */ "a / b <> c"                            to "(ne (divide (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (/, in)                      */ "a / b in c"                            to "(in_collection (divide (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (/, not_in)                  */ "a / b not in c"                        to "(not (in_collection (divide (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified))))",
        /* (/, <)                       */ "a / b < c"                             to "(lt (divide (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (/, <=)                      */ "a / b <= c"                            to "(lte (divide (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (/, >)                       */ "a / b > c"                             to "(gt (divide (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (/, >=)                      */ "a / b >= c"                            to "(gte (divide (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (/, between)                 */ "a / b between w and c"                 to "(between (divide (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id w (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified)))",
        /* (/, not_between)             */ "a / b not between y and c"             to "(not (between (divide (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id y (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (/, like)                    */ "a / b like c"                          to "(like (divide (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)) null)",
        /* (/, not_like)                */ "a / b not like c"                      to "(not (like (divide (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)) null))",
        /* (/, +)                       */ "a / b + c"                             to "(plus (divide (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (/, -)                       */ "a / b - c"                             to "(minus (divide (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (/, ||)                      */ "a / b || c"                            to "(concat (divide (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (/, *)                       */ "a / b * c"                             to "(times (divide (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (/, %)                       */ "a / b % c"                             to "(modulo (divide (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (/, is)                      */ "a / b is boolean"                      to "(is_type (divide (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (boolean_type))",
        /* (/, is_not)                  */ "a / b is not boolean"                  to "(not (is_type (divide (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (boolean_type)))")

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun modPrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForModPrecedence() = listOf(
        /* (%, intersect)               */ "a % b intersect c"                     to "(intersect (distinct) (modulo (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (%, intersect_all)           */ "a % b intersect all c"                 to "(intersect (all) (modulo (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (%, except)                  */ "a % b except c"                        to "(except (distinct) (modulo (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (%, except_all)              */ "a % b except all c"                    to "(except (all) (modulo (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (%, union)                   */ "a % b union c"                         to "(union (distinct) (modulo (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (%, union_all)               */ "a % b union all c"                     to "(union (all) (modulo (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (%, or)                      */ "a % b or c"                            to "(or (modulo (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (%, and)                     */ "a % b and c"                           to "(and (modulo (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (%, =)                       */ "a % b = c"                             to "(eq (modulo (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (%, <>)                      */ "a % b <> c"                            to "(ne (modulo (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (%, in)                      */ "a % b in c"                            to "(in_collection (modulo (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (%, not_in)                  */ "a % b not in c"                        to "(not (in_collection (modulo (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified))))",
        /* (%, <)                       */ "a % b < c"                             to "(lt (modulo (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (%, <=)                      */ "a % b <= c"                            to "(lte (modulo (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (%, >)                       */ "a % b > c"                             to "(gt (modulo (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (%, >=)                      */ "a % b >= c"                            to "(gte (modulo (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (%, between)                 */ "a % b between w and c"                 to "(between (modulo (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id w (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified)))",
        /* (%, not_between)             */ "a % b not between y and c"             to "(not (between (modulo (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id y (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (%, like)                    */ "a % b like c"                          to "(like (modulo (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)) null)",
        /* (%, not_like)                */ "a % b not like c"                      to "(not (like (modulo (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)) null))",
        /* (%, +)                       */ "a % b + c"                             to "(plus (modulo (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (%, -)                       */ "a % b - c"                             to "(minus (modulo (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (%, ||)                      */ "a % b || c"                            to "(concat (modulo (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (%, *)                       */ "a % b * c"                             to "(times (modulo (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (%, /)                       */ "a % b / c"                             to "(divide (modulo (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (id c (case_insensitive) (unqualified)))",
        /* (%, is)                      */ "a % b is boolean"                      to "(is_type (modulo (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (boolean_type))",
        /* (%, is_not)                  */ "a % b is not boolean"                  to "(not (is_type (modulo (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) (boolean_type)))")

    @Test
    fun combinationOfBinaryOperators() = runTest(
        "a + b AND c / d * e - f || g OR h" to """
            (or
                (and
                    (plus (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)) )
                    (concat
                        (minus
                            (times
                                (divide (id c (case_insensitive) (unqualified)) (id d (case_insensitive) (unqualified)))
                                (id e (case_insensitive) (unqualified))
                            )
                            (id f (case_insensitive) (unqualified))
                        )
                        (id g (case_insensitive) (unqualified))
                    )
                )
                (id h (case_insensitive) (unqualified))
            )""")

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun notUnaryPrecedence(pair: Pair<String, String>) = runTest(pair)
    fun parametersForNotUnaryPrecedence() = listOf(
        /* (not, intersect)     */ "not a intersect b"         to "(intersect (distinct) (not (id a (case_insensitive) (unqualified))) (id b (case_insensitive) (unqualified)))",
        /* (not, intersect_all) */ "not a intersect all b"     to "(intersect (all) (not (id a (case_insensitive) (unqualified))) (id b (case_insensitive) (unqualified)))",
        /* (not, except)        */ "not a except b"            to "(except (distinct) (not (id a (case_insensitive) (unqualified))) (id b (case_insensitive) (unqualified)))",
        /* (not, except_all)    */ "not a except all b"        to "(except (all) (not (id a (case_insensitive) (unqualified))) (id b (case_insensitive) (unqualified)))",
        /* (not, union)         */ "not a union b"             to "(union (distinct) (not (id a (case_insensitive) (unqualified))) (id b (case_insensitive) (unqualified)))",
        /* (not, union_all)     */ "not a union all b"         to "(union (all) (not (id a (case_insensitive) (unqualified))) (id b (case_insensitive) (unqualified)))",
        /* (not, or)            */ "not a or b"                to "(or (not (id a (case_insensitive) (unqualified))) (id b (case_insensitive) (unqualified)))",
        /* (not, and)           */ "not a and b"               to "(and (not (id a (case_insensitive) (unqualified))) (id b (case_insensitive) (unqualified)))",
        /* (not, =)             */ "not a = b"                 to "(not (eq (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))))",
        /* (not, <>)            */ "not a <> b"                to "(not (ne (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))))",
        /* (not, in)            */ "not a in b"                to "(not (in_collection (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))))",
        /* (not, not_in)        */ "not a not in b"            to "(not (not (in_collection (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)))))",
        /* (not, <)             */ "not a < b"                 to "(not (lt (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))))",
        /* (not, <=)            */ "not a <= b"                to "(not (lte (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))))",
        /* (not, >)             */ "not a > b"                 to "(not (gt (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))))",
        /* (not, >=)            */ "not a >= b"                to "(not (gte (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))))",
        /* (not, between)       */ "not a between b and c"     to "(not (between (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))",
        /* (not, not_between)   */ "not a not between b and c" to "(not (not (between (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified)))))",
        /* (not, like)          */ "not a like b"              to "(not (like (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)) null))",
        /* (not, not_like)      */ "not a not like b"          to "(not (not (like (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)) null)))",
        /* (not, +)             */ "not a + b"                 to "(not (plus (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))))",
        /* (not, -)             */ "not a - b"                 to "(not (minus (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))))",
        /* (not, ||)            */ "not a || b"                to "(not (concat (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))))",
        /* (not, *)             */ "not a * b"                 to "(not (times (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))))",
        /* (not, /)             */ "not a / b"                 to "(not (divide (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))))",
        /* (not, %)             */ "not a % b"                 to "(not (modulo (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))))",
        /* (not, is)            */ "not a is boolean"          to "(not (is_type (id a (case_insensitive) (unqualified)) (boolean_type)))",
        /* (not, is_not)        */ "not a is not boolean"      to "(not (not (is_type (id a (case_insensitive) (unqualified)) (boolean_type))))"
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
                        (not (id a (case_insensitive) (unqualified)))
                        (id b (case_insensitive) (unqualified))
                    )
                    (and
                        (id c (case_insensitive) (unqualified))
                        (not (id d (case_insensitive) (unqualified)))
                    )
                )
                (not (id e (case_insensitive) (unqualified)))
            )""",

        // minus and plus unary
        "- a + b" to "(plus (minus (id a (case_insensitive) (unqualified))) (id b (case_insensitive) (unqualified)) )",

        "(a+-5e0) and (c-+7.0)" to """
            (and
                (plus (id a (case_insensitive) (unqualified)) (lit -5.) )
                (minus (id c (case_insensitive) (unqualified)) (lit 7.0) )
            )""",

        "d*-+-9 and e>=+-+foo" to """
            (and
                (times (id d (case_insensitive) (unqualified)) (lit 9) )
                (gte
                    (id e (case_insensitive) (unqualified))
                    (plus (minus (plus (id foo (case_insensitive) (unqualified)))))
                )
            )""")



    private fun runTest(pair: Pair<String, String>) {
        val (source, expectedAst) = pair

        val expectedAstExpr = PartiqlAst.transform(ion.singleValue(expectedAst).toIonElement()) as PartiqlAst.Expr
        val expectedAstStatement = PartiqlAst.build { query(expectedAstExpr) }
        val expectedExprNode = MetaStrippingRewriter.stripMetas(expectedAstStatement.toExprNode(ion))

        val actualExprNode = MetaStrippingRewriter.stripMetas(SqlParser(ion).parseExprNode(source))

        assertEquals(expectedExprNode, actualExprNode)
    }
}
