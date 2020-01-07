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

import org.junit.*
import org.partiql.lang.ast.*

/**
 * Originally just meant to test the parser, this class now tests several different things because
 * the same test cases can be used for all three:
 *
 * - Parsing of query to [ExprNode]s
 * - Conversion of [ExprNode]s to legacy and new s-exp ASTs.
 * - Conversion of both AST forms to [ExprNode]s.
 *
 */
class SqlParserTest : SqlParserTestBase() {

    //****************************************
    // literals
    //****************************************
    @Test
    fun litInt() = assertExpression(
        "(lit 5)",
        "5",
        "(term (exp (lit 5)))"
    )

    @Test
    fun litNull() = assertExpression(
        "(lit null)",
        "null",
        "(term (exp (lit null)))"
    )

    @Test
    fun litMissing() = assertExpression(
        "(missing)",
        "missing",
        "(term (exp (missing)))"
    )

    @Test
    fun listLiteral() = assertExpression(
        "(list (id a case_insensitive) (lit 5))",
        "[a, 5]",
        "(term (exp (list (term (exp (id a case_insensitive))) (term (exp (lit 5))))))"
    )

    @Test
    fun listLiteralWithBinary() = assertExpression(
        "(list (id a case_insensitive) (lit 5) (+ (id b case_insensitive) (lit 6)))",
        "[a, 5, (b + 6)]",
        """(term (exp (list (term (exp (id a case_insensitive)))
                            (term (exp (lit 5)))
                            (term (exp (+ (term (exp (id b case_insensitive)))
                                          (term (exp (lit 6)))
                            )))
           )))
        """
    )

    @Test
    fun listFunction() = assertExpression(
        "(list (id a case_insensitive) (lit 5))",
        "list(a, 5)",
        "(term (exp (list (term (exp (id a case_insensitive))) (term (exp (lit 5))))))"
    )

    @Test
    fun listFunctionlWithBinary() = assertExpression(
        "(list (id a case_insensitive) (lit 5) (+ (id b case_insensitive) (lit 6)))",
        "LIST(a, 5, (b + 6))",
        """(term (exp (list (term (exp (id a case_insensitive)))
                            (term (exp (lit 5)))
                            (term (exp (+ (term (exp (id b case_insensitive)))
                                          (term (exp (lit 6)))
                            )))
           )))
        """
    )

    @Test
    fun sexpFunction() = assertExpression(
        "(sexp (id a case_insensitive) (lit 5))",
        "sexp(a, 5)",
        "(term (exp (sexp (term (exp (id a case_insensitive))) (term (exp (lit 5))))))"
    )

    @Test
    fun sexpFunctionWithBinary() = assertExpression(
        "(sexp (id a case_insensitive) (lit 5) (+ (id b case_insensitive) (lit 6)))",
        "SEXP(a, 5, (b + 6))",
        """(term (exp (sexp (term (exp (id a case_insensitive)))
                            (term (exp (lit 5)))
                            (term (exp (+ (term (exp (id b case_insensitive)))
                                          (term (exp (lit 6)))
                            )))
           )))
        """
    )

    @Test
    fun bagLiteral() = assertExpression(
        "(bag (id a case_insensitive) (lit 5))",
        "<<a, 5>>",
        "(term (exp (bag (term (exp (id a case_insensitive))) (term (exp (lit 5))))))"
    )

    @Test
    fun bagLiteralWithBinary() = assertExpression(
        "(bag (id a case_insensitive) (lit 5) (+ (id b case_insensitive) (lit 6)))",
        "<<a, 5, (b + 6)>>",
        """(term (exp (bag (term (exp (id a case_insensitive)))
                           (term (exp (lit 5)))
                           (term (exp (+ (term (exp (id b case_insensitive)))
                                         (term (exp (lit 6)))
                           )))
           )))
        """
    )

    @Test
    fun bagFunction() = assertExpression(
        "(bag (id a case_insensitive) (lit 5))",
        "bag(a, 5)",
        "(term (exp (bag (term (exp (id a case_insensitive))) (term (exp (lit 5))))))"
    )

    @Test
    fun bagFunctionWithBinary() = assertExpression(
        "(bag (id a case_insensitive) (lit 5) (+ (id b case_insensitive) (lit 6)))",
        "BAG(a, 5, (b + 6))",
        """(term (exp (bag (term (exp (id a case_insensitive)))
                           (term (exp (lit 5)))
                           (term (exp (+ (term (exp (id b case_insensitive)))
                                         (term (exp (lit 6)))
                           )))
           )))
        """
    )

    @Test
    fun structLiteral() = assertExpression(
        """(struct
                     (lit "x") (id a case_insensitive)
                     (lit "y") (lit 5)
                   )
                """,
        "{'x':a, 'y':5 }",
        """(term (exp (struct (term (exp (lit "x"))) (term (exp (id a case_insensitive)))
                              (term (exp (lit "y"))) (term (exp (lit 5)))
           )))
        """
    )

    @Test
    fun structLiteralWithBinary() = assertExpression(
        """(struct
                     (lit "x") (id a case_insensitive)
                     (lit "y") (lit 5)
                     (lit "z") (+ (id b case_insensitive) (lit 6))
                   )
                """,
        "{'x':a, 'y':5, 'z':(b + 6)}",
        """(term (exp (struct (term (exp (lit "x"))) (term (exp (id a case_insensitive)))
                              (term (exp (lit "y"))) (term (exp (lit 5)))
                              (term (exp (lit "z"))) (term (exp (+ (term (exp (id b case_insensitive)))
                                                                   (term (exp (lit 6)))
                                                     )))
           )))
        """
    )

    @Test
    fun nestedEmptyListLiteral() = assertExpression(
        "(list (list))",
        "[[]]",
        "(term (exp (list (term (exp (list))) )))"
    )

    @Test
    fun nestedEmptyBagLiteral() = assertExpression(
        "(bag (bag))",
        "<<<<>>>>",
        "(term (exp (bag (term (exp (bag))) )))"
    )

    @Test
    fun nestedEmptyStructLiteral() = assertExpression(
        """(struct (lit "a") (struct))""",
        "{'a':{}}",
        """(term (exp (struct (term (exp (lit "a"))) (term (exp (struct))) )))"""
    )

    //****************************************
    // container constructors
    //****************************************
    @Test
    fun rowValueConstructorWithSimpleExpressions() = assertExpression(
        """(list (lit 1) (lit 2) (lit 3) (lit 4))""",
        "(1, 2, 3, 4)",
        """(term (exp (list (term (exp (lit 1)))
                            (term (exp (lit 2)))
                            (term (exp (lit 3)))
                            (term (exp (lit 4)))
           )))
        """
    )

    @Test
    fun rowValueConstructorWithRowValueConstructors() = assertExpression(
        """(list (list (lit 1) (lit 2)) (list (lit 3) (lit 4)))""",
        "((1, 2), (3, 4))",
        """(term (exp (list (term (exp (list (term (exp (lit 1)))
                                             (term (exp (lit 2))) )))
                            (term (exp (list (term (exp (lit 3)))
                                             (term (exp (lit 4))) )))
           )))
        """
    )

    @Test
    fun tableValueConstructorWithRowValueConstructors() = assertExpression(
        """(bag (list (lit 1) (lit 2)) (list (lit 3) (lit 4)))""",
        "VALUES (1, 2), (3, 4)",
        """(term (exp (bag (term (exp (list (term (exp (lit 1)))
                                            (term (exp (lit 2))) )))
                           (term (exp (list (term (exp (lit 3)))
                                            (term (exp (lit 4))) )))
           )))
        """
    )

    @Test
    fun tableValueConstructorWithSingletonRowValueConstructors() = assertExpression(
        """(bag (list (lit 1)) (list (lit 2)) (list (lit 3)))""",
        "VALUES (1), (2), (3)",
        """(term (exp (bag (term (exp (list (term (exp (lit 1))) )))
                           (term (exp (list (term (exp (lit 2))) )))
                           (term (exp (list (term (exp (lit 3))) )))
           )))
        """
    )

    //****************************************
    // identifiers
    //****************************************
    @Test
    fun id_case_insensitive() = assertExpression(
        "(id kumo case_insensitive)",
        "kumo",
        "(term (exp (id kumo case_insensitive)))"
    )

    @Test
    fun id_case_sensitive() = assertExpression(
        "(id kumo case_sensitive)",
        "\"kumo\"",
        "(term (exp (id kumo case_sensitive)))"
    )

    //****************************************
    // call
    //****************************************
    @Test
    fun callEmpty() = assertExpression(
        "(call foobar)",
        "foobar()",
        "(term (exp (call foobar)))"
    )

    @Test
    fun callOneArgument() = assertExpression(
        "(call foobar (lit 1))",
        "foobar(1)",
        "(term (exp (call foobar (term (exp (lit 1))) )))"
    )

    @Test
    fun callTwoArgument() = assertExpression(
        "(call foobar (lit 1) (lit 2))",
        "foobar(1, 2)",
        "(term (exp (call foobar (term (exp (lit 1))) (term (exp (lit 2))) )))"
    )

    @Test
    fun callSubstringSql92Syntax() = assertExpression(
        "(call substring (lit \"test\") (lit 100))",
        "substring('test' from 100)",
        """(term (exp (call substring
                            (term (exp (lit "test")))
                            (term (exp (lit 100)))
           )))
        """
    )

    @Test
    fun callSubstringSql92SyntaxWithLength() = assertExpression(
        "(call substring (lit \"test\") (lit 100) (lit 50))",
        "substring('test' from 100 for 50)",
        """(term (exp (call substring
                            (term (exp (lit "test")))
                            (term (exp (lit 100)))
                            (term (exp (lit 50)))
           )))
        """
    )

    @Test
    fun callSubstringNormalSyntax() = assertExpression(
        "(call substring (lit \"test\") (lit 100))",
        "substring('test', 100)",
        """(term (exp (call substring
                            (term (exp (lit "test")))
                            (term (exp (lit 100)))
           )))
        """
    )

    @Test
    fun callSubstringNormalSyntaxWithLength() = assertExpression(
        "(call substring (lit \"test\") (lit 100) (lit 50))",
        "substring('test', 100, 50)",
        """(term (exp (call substring
                            (term (exp (lit "test")))
                            (term (exp (lit 100)))
                            (term (exp (lit 50)))
           )))
        """
    )

    @Test
    fun callTrimSingleArgument() = assertExpression(
        "(call trim (lit \"test\"))",
        "trim('test')",
        "(term (exp (call trim (term (exp (lit \"test\"))) )))")



    @Test
    fun callTrimTwoArgumentsDefaultSpecification() = assertExpression(
        "(call trim (lit \" \") (lit \"test\"))",
        "trim(' ' from 'test')",
        "(term (exp (call trim (term (exp (lit \" \"))) (term (exp (lit \"test\"))) )))")

    @Test
    fun callTrimTwoArgumentsUsingBoth() = assertExpression(
        "(call trim (lit \"both\") (lit \"test\"))",
        "trim(both from 'test')",
        "(term (exp (call trim (term (exp (lit \"both\"))) (term (exp (lit \"test\"))) )))")

    @Test
    fun callTrimTwoArgumentsUsingLeading() = assertExpression(
        "(call trim (lit \"leading\") (lit \"test\"))",
        "trim(leading from 'test')",
        "(term (exp (call trim (term (exp (lit \"leading\"))) (term (exp (lit \"test\"))) )))")

    @Test
    fun callTrimTwoArgumentsUsingTrailing() = assertExpression(
        "(call trim (lit \"trailing\") (lit \"test\"))",
        "trim(trailing from 'test')",
        "(term (exp (call trim (term (exp (lit \"trailing\"))) (term (exp (lit \"test\"))) )))")

    //****************************************
    // Unary operators
    //****************************************

    @Test
    fun unaryMinusCall() = assertExpression(
        "(- (call baz))",
        "-baz()",
        "(term (exp (- (term (exp (call baz))) )))"
    )

    @Test
    fun unaryPlusMinusIdent() = assertExpression(
        "(+ (- (call baz)))",
        "+(-baz())",
        "(term (exp (+ (term (exp (- (term (exp (call baz))) ))) )))"
    )

    @Test
    fun unaryPlusMinusIdentNoSpaces() = assertExpression(
        "(+ (- (call baz)))",
        "+-baz()",
        "(term (exp (+ (term (exp (- (term (exp (call baz))) ))) )))"
    )

    @Test
    fun unaryIonIntLiteral() = assertExpression(
        "(lit -1)",
        "-1",
        "(term (exp (lit -1)))"
    )

    @Test
    fun unaryIonFloatLiteral() = assertExpression(
        "(lit 5e0)",
        "+-+-+-`-5e0`",
        "(term (exp (lit 5e0)))"
    )

    @Test
    fun unaryIonTimestampLiteral() = assertExpression(
        "(+ (- (lit 2017-01-01T)))",
        "+-`2017-01-01`",
        "(term (exp (+ (term (exp (- (term (exp (lit 2017-01-01))) ))) )))"
    )

    @Test
    fun unaryNotLiteral() = assertExpression(
        "(not (lit 1))",
        "not 1",
        "(term (exp (not (term (exp (lit 1))) )))"
    )

    //****************************************
    // BETWEEN
    //****************************************
    @Test
    fun betweenOperator() = assertExpression(
        "(between (lit 5) (lit 1) (lit 10))",
        "5 BETWEEN 1 AND 10",
        "(term (exp (between (term (exp (lit 5))) (term (exp (lit 1))) (term (exp (lit 10))) )))"
    )

    @Test
    fun notBetweenOperator() = assertExpression(
        "(not_between (lit 5) (lit 1) (lit 10))",
        "5 NOT BETWEEN 1 AND 10",
        "(term (exp (not (term (exp (between (term (exp (lit 5))) (term (exp (lit 1))) (term (exp (lit 10))) ))) )))"
    )
    //****************************************
    // @ operator
    //****************************************

    @Test
    fun atOperatorOnIdentifier() = assertExpression(
        "(@ (id a case_insensitive))",
        "@a",
        "(term (exp (@ (id a case_insensitive))))"
    )

    @Test
    fun atOperatorOnPath() = assertExpression(
        """(path (@ (id a case_insensitive)) (case_insensitive (lit "b")))""",
        "@a.b",
        """
            (term
                (exp
                    (path
                        (term
                            (exp
                                (@
                                    (id a case_insensitive))))
                        (path_element
                            (term
                                (exp
                                    (lit "b")))
                            case_insensitive))))
        """
    )

    //****************************************
    // IS operator
    //****************************************
    @Test
    fun nullIsNull() = assertExpression(
        "(is (lit null) (type 'null'))",
        "null IS NULL",
        """(term (exp (is (term (exp (lit null)))
                          (term (exp (type 'null'))))))"""
    )

    @Test
    fun missingIsMissing() = assertExpression(
        "(is (missing) (type missing))",
        "mIsSiNg IS MISSING",
        "(term (exp (is (term (exp (missing))) (term (exp (type missing))))))"
    )

    @Test
    fun callIsVarchar() = assertExpression(
        "(is (call f) (type character_varying 200))",
        "f() IS VARCHAR(200)",
        "(term (exp (is (term (exp (call f))) (term (exp (type character_varying 200))))))"
    )

    @Test
    fun nullIsNotNull() = assertExpression(
        "(is_not (lit null) (type 'null'))",
        "null IS NOT NULL",
        "(term (exp (not (term (exp (is (term (exp (lit null))) (term (exp (type 'null') ))) )))))"
    )

    @Test
    fun missingIsNotMissing() = assertExpression(
        "(is_not (missing) (type missing))",
        "mIsSiNg IS NOT MISSING",
        "(term (exp (not (term (exp (is (term (exp (missing))) (term (exp (type missing))) ))) )))"
    )

    @Test
    fun callIsNotVarchar() = assertExpression(
        "(is_not (call f) (type character_varying 200))",
        "f() IS NOT VARCHAR(200)",
        """(term (exp (not (term (exp (is (term (exp (call f )))
                                          (term (exp (type character_varying 200)))
                           )))
           )))
        """
    )

    @Test
    fun callWithMultiple() = assertExpression(
        "(call foobar (lit 5) (lit 6) (id a case_insensitive))",
        "foobar(5, 6, a)",
        """(term (exp (call foobar
                            (term (exp (lit 5)))
                            (term (exp (lit 6)))
                            (term (exp (id a case_insensitive)))
           )))
        """
    )

    @Test
    fun aggregateFunctionCall() = assertExpression(
        """(call_agg sum all (id a case_insensitive))""",
        "SUM(a)",
        "(term (exp (call_agg sum all (term (exp (id a case_insensitive))))))")

    @Test
    fun aggregateDistinctFunctionCall() = assertExpression(
        """(call_agg sum distinct (id a case_insensitive))""",
        "SUM(DISTINCT a)",
        "(term (exp (call_agg sum distinct (term (exp (id a case_insensitive))))))")

    @Test
    fun countStarFunctionCall() = assertExpression(
        """(call_agg_wildcard count)""",
        "COUNT(*)",
        "(term (exp (call_agg_wildcard count)))")
    
    @Test
    fun countFunctionCall() = assertExpression(
        """(call_agg count all (id a case_insensitive))""",
        "COUNT(a)",
        "(term (exp (call_agg count all (term (exp (id a case_insensitive))))))")

    @Test
    fun countDistinctFunctionCall() = assertExpression(
        """(call_agg count distinct (id a case_insensitive))""",
        "COUNT(DISTINCT a)",
        "(term (exp (call_agg count distinct (term (exp (id a case_insensitive))))))")

    //****************************************
    // path expression
    //****************************************
    @Test
    fun dot_case_1_insensitive_component() = assertExpression(
        """(path (id a case_insensitive) (case_insensitive (lit "b")))""",
        "a.b",
        """
            (term
                (exp
                    (path
                        (term
                            (exp
                                (id a case_insensitive)))
                        (path_element
                            (term
                                (exp
                                    (lit "b")))
                            case_insensitive))))
        """
    )

    @Test
    fun dot_case_2_insensitive_component() = assertExpression(
        """(path (id a case_insensitive) (case_insensitive (lit "b")) (case_insensitive (lit "c")))""",
        "a.b.c",
        """
            (term
                (exp
                    (path
                        (term
                            (exp
                                (id a case_insensitive)))
                        (path_element
                            (term
                                (exp
                                    (lit "b")))
                            case_insensitive)
                        (path_element
                            (term
                                (exp
                                    (lit "c")))
                            case_insensitive))))
        """
    )
    @Test
    fun dot_case_3_insensitive_components() = assertExpression(
        """(path (id a case_insensitive) (case_insensitive (lit "b")) (case_insensitive (lit "c")) (case_insensitive (lit "d")))""",
        "a.b.c.d",
        """
            (term
                (exp
                    (path
                        (term
                            (exp
                                (id a case_insensitive)))
                        (path_element
                            (term
                                (exp
                                    (lit "b")))
                            case_insensitive)
                        (path_element
                            (term
                                (exp
                                    (lit "c")))
                            case_insensitive)
                        (path_element
                            (term
                                (exp
                                    (lit "d")))
                            case_insensitive))))
        """
    )

    @Test
    fun dot_case_sensitive() = assertExpression(
        """(path (id a case_sensitive) (case_sensitive (lit "b")))""",
        """ "a"."b" """,
        """
            (term
                (exp
                    (path
                        (term
                            (exp
                                (id a case_sensitive)))
                        (path_element
                            (term
                                (exp
                                    (lit "b")))
                            case_sensitive))))
        """
    )

    @Test
    fun dot_case_sensitive_component() = assertExpression(
        """(path (id a case_insensitive) (case_sensitive (lit "b")))""",
        "a.\"b\"",
        """
            (term
                (exp
                    (path
                        (term
                            (exp
                                (id a case_insensitive)))
                        (path_element
                            (term
                                (exp
                                    (lit "b")))
                            case_sensitive))))
        """
    )

    @Test
    fun groupDot() = assertExpression(
        """(path (id a case_insensitive) (case_insensitive (lit "b")))""",
        "(a).b",
        """
            (term
                (exp
                    (path
                        (term
                            (exp
                                (id a case_insensitive)))
                        (path_element
                            (term
                                (exp
                                    (lit "b")))
                            case_insensitive))))
        """
    )

    @Test
    fun pathWith1SquareBracket() = assertExpression(
        """(path (id a case_insensitive) (lit 5))""",
        """a[5]""",
        """
            (term
                (exp
                    (path
                        (term
                            (exp
                                (id a case_insensitive)))
                        (path_element
                            (term
                                (exp
                                    (lit 5)))
                            case_sensitive))))
        """
    )
    @Test
    fun pathWith3SquareBrackets() = assertExpression(
        """(path (id a case_insensitive) (lit 5) (case_sensitive (lit "b")) (+ (id a case_insensitive) (lit 3)))""",
        """a[5]['b'][(a + 3)]""",
        """
            (term
                (exp
                    (path
                        (term
                            (exp
                                (id a case_insensitive)))
                        (path_element
                            (term
                                (exp
                                    (lit 5)))
                            case_sensitive)
                        (path_element
                            (term
                                (exp
                                    (lit "b")))
                            case_sensitive)
                        (path_element
                            (term
                                (exp
                                    (+
                                        (term
                                            (exp
                                                (id a case_insensitive)))
                                        (term
                                            (exp
                                                (lit 3))))))
                            case_sensitive))))
        """
    )

    @Test
    fun dotStar() = assertExpression(
        """(path (id a case_insensitive) (* unpivot))""",
        "a.*",
        """
            (term
                (exp
                    (path
                        (term
                            (exp
                                (id a case_insensitive)))
                        (path_element
                            (term
                                (exp
                                    (star unpivot)))))))
        """
    )

    @Test
    fun dot2Star() = assertExpression(
        """(path (id a case_insensitive) (case_insensitive (lit "b")) (* unpivot))""",
        "a.b.*",
        """
            (term
                (exp
                    (path
                        (term
                            (exp
                                (id a case_insensitive)))
                        (path_element
                            (term
                                (exp
                                    (lit "b")))
                            case_insensitive)
                        (path_element
                            (term
                                (exp
                                    (star unpivot)))))))
        """
    )

    @Test
    fun dotWildcard() = assertExpression(
        """(path (id a case_insensitive) (*))""",
        "a[*]",
        """
            (term
                (exp
                    (path
                        (term
                            (exp
                                (id a case_insensitive)))
                        (path_element
                            (term
                                (exp
                                    (star)))))))
        """
    )

    @Test
    fun dot2Wildcard() = assertExpression(
        """(path (id a case_insensitive) (case_insensitive (lit "b")) (*))""",
        "a.b[*]",
        """
            (term
                (exp
                    (path
                        (term
                            (exp
                                (id a case_insensitive)))
                        (path_element
                            (term
                                (exp
                                    (lit "b")))
                            case_insensitive)
                        (path_element
                            (term
                                (exp
                                    (star)))))))
        """
    )

    @Test
    fun pathWithCallAndDotStar() = assertExpression(
        """(path (call foo (id x case_insensitive) (id y case_insensitive)) (case_insensitive (lit "a")) (* unpivot) (case_insensitive (lit "b")))""",
        "foo(x, y).a.*.b",
        """
            (term
                (exp
                    (path
                        (term
                            (exp
                                (call
                                    foo
                                    (term
                                        (exp
                                            (id x case_insensitive)))
                                    (term
                                        (exp
                                            (id y case_insensitive))))))
                        (path_element
                            (term
                                (exp
                                    (lit "a")))
                            case_insensitive)
                        (path_element
                            (term
                                (exp
                                    (star unpivot))))
                        (path_element
                            (term
                                (exp
                                    (lit "b")))
                            case_insensitive))))
        """
    )

    @Test
    fun dotAndBracketStar() = assertExpression(
        """(path (id x case_insensitive) (case_insensitive (lit "a")) (*) (case_insensitive (lit "b")))""",
        "x.a[*].b",
        """
            (term
                (exp
                    (path
                        (term
                            (exp
                                (id x case_insensitive)))
                        (path_element
                            (term
                                (exp
                                    (lit "a")))
                            case_insensitive)
                        (path_element
                            (term
                                (exp
                                    (star))))
                        (path_element
                            (term
                                (exp
                                    (lit "b")))
                            case_insensitive))))
        """
    )

    //****************************************
    // cast
    //****************************************
    @Test
    fun castNoArgs() = assertExpression(
        """(cast
             (lit 5)
             (type character_varying)
           )
        """,
        "CAST(5 AS VARCHAR)",
        "(term (exp (cast (term (exp (lit 5))) (term (exp (type character_varying))))))"
    )

    @Test
    fun castArg() = assertExpression(
        """(cast
             (+ (lit 5) (id a case_insensitive))
             (type character_varying 1)
           )
        """,
        "CAST(5 + a AS VARCHAR(1))",
        """(term (exp (cast (term (exp (+ (term (exp (lit 5)))
                                          (term (exp (id a case_insensitive)))
                            )))
                            (term (exp (type character_varying 1)))
           )))
        """
    )

    //****************************************
    // searched CASE
    //****************************************
    @Test
    fun searchedCaseSingleNoElse() = assertExpression(
        """(searched_case
             (when
               (= (id name case_insensitive) (lit "zoe"))
               (lit 1)
             )
           )
        """,
        "CASE WHEN name = 'zoe' THEN 1 END",
        """(term
             (exp
               (searched_case
                 (when
                   (term (exp (= (term (exp (id name case_insensitive))) (term (exp (lit "zoe"))) )))
                   (term (exp (lit 1)))
                 )
               )
             )
           )
        """
    )

    @Test
    fun searchedCaseSingleWithElse() = assertExpression(
        """(searched_case
             (when
               (= (id name case_insensitive) (lit "zoe"))
               (lit 1)
             )
             (else (lit 0))
           )
        """,
        "CASE WHEN name = 'zoe' THEN 1 ELSE 0 END",
        """(term
             (exp
               (searched_case
                 (when
                   (term (exp (= (term (exp (id name case_insensitive))) (term (exp (lit "zoe"))) )))
                   (term (exp (lit 1)))
                 )
                 (else (term (exp (lit 0))))
               )
             )
           )
        """
    )

    @Test
    fun searchedCaseMultiWithElse() = assertExpression(
        """(searched_case
             (when
               (= (id name case_insensitive) (lit "zoe"))
               (lit 1)
             )
             (when
               (> (id name case_insensitive) (lit "kumo"))
               (lit 2)
             )
             (else (lit 0))
           )
        """,
        "CASE WHEN name = 'zoe' THEN 1 WHEN name > 'kumo' THEN 2 ELSE 0 END",
        """(term
             (exp
               (searched_case
                   (when
                     (term (exp (= (term (exp (id name case_insensitive))) (term (exp (lit "zoe"))) )))
                     (term (exp (lit 1)))
                   )
                   (when
                     (term (exp (> (term (exp (id name case_insensitive))) (term (exp (lit "kumo"))) )))
                     (term (exp (lit 2)))
                   )
                   (else (term (exp (lit 0))))
                  )
               )
           )
        """
    )

    //****************************************
    // simple CASE
    //****************************************
    @Test
    fun simpleCaseSingleNoElse() = assertExpression(
        """(simple_case
             (id name case_insensitive)
             (when
               (lit "zoe")
               (lit 1)
             )
           )
        """,
        "CASE name WHEN 'zoe' THEN 1 END",
        """(term
             (exp
               (simple_case
                   (term (exp (id name case_insensitive)))
                   (when
                     (term (exp (lit "zoe")))
                     (term (exp (lit 1)))
                   )
                 )
               )
           )
        """
    )

    @Test
    fun simpleCaseSingleWithElse() = assertExpression(
        """(simple_case
             (id name case_insensitive)
             (when
               (lit "zoe")
               (lit 1)
             )
             (else (lit 0))
           )
        """,
        "CASE name WHEN 'zoe' THEN 1 ELSE 0 END",
        """(term
             (exp
                (simple_case
                   (term (exp (id name case_insensitive)))
                   (when
                     (term (exp (lit "zoe")))
                     (term (exp (lit 1)))
                   )
                   (else (term (exp (lit 0))))
                 )
             )
           )
        """
    )

    @Test
    fun simpleCaseMultiWithElse() = assertExpression(
        """(simple_case
            (id name case_insensitive)
            (when
              (lit "zoe")
              (lit 1)
            )
            (when
              (lit "kumo")
              (lit 2)
            )
            (when
              (lit "mary")
              (lit 3)
            )
            (else (lit 0))
           )
        """,
        "CASE name WHEN 'zoe' THEN 1 WHEN 'kumo' THEN 2 WHEN 'mary' THEN 3 ELSE 0 END",
        """(term
             (exp
               (simple_case
                   (term (exp (id name case_insensitive)))
                   (when
                     (term (exp (lit "zoe")))
                     (term (exp (lit 1)))
                   )
                   (when
                     (term (exp (lit "kumo")))
                     (term (exp (lit 2)))
                   )
                   (when
                     (term (exp (lit "mary")))
                     (term (exp (lit 3)))
                   )
                   (else (term (exp (lit 0))))
                 )
               )
           )
        """
    )

    //****************************************
    // IN operator
    //****************************************
    @Test
    fun inOperatorWithImplicitValues() = assertExpression(
        """(in
             (id a case_insensitive)
             (list (lit 1) (lit 2) (lit 3) (lit 4))
           )
        """,
        "a IN (1, 2, 3, 4)",
        """(term
             (exp
               (in
                 (term (exp (id a case_insensitive)))
                 (term
                   (exp
                     (list
                       (term (exp (lit 1)))
                       (term (exp (lit 2)))
                       (term (exp (lit 3)))
                       (term (exp (lit 4)))
                     )
                   )
                 )
               )
             )
           )
        """
    )

    @Test
    fun notInOperatorWithImplicitValues() = assertExpression(
        """(not_in
             (id a case_insensitive)
             (list (lit 1) (lit 2) (lit 3) (lit 4))
           )
        """,
        "a NOT IN (1, 2, 3, 4)",
        """(term (exp (not (term (exp (in (term (exp (id a case_insensitive)))
                                          (term (exp (list (term (exp (lit 1)))
                                                           (term (exp (lit 2)))
                                                           (term (exp (lit 3)))
                                                           (term (exp (lit 4)))
                                          )))
                           )))
           )))
        """
    )

    @Test
    fun inOperatorWithImplicitValuesRowConstructor() = assertExpression(
        """(in
             (list (id a case_insensitive) (id b case_insensitive))
             (list (list (lit 1) (lit 2)) (list (lit 3) (lit 4)))
           )
        """,
        "(a, b) IN ((1, 2), (3, 4))",
        """(term (exp (in (term (exp (list (term (exp (id a case_insensitive)))
                                           (term (exp (id b case_insensitive)))
                          )))
                          (term (exp (list
                                           (term (exp (list (term (exp (lit 1)))
                                                            (term (exp (lit 2)))
                                           )))
                                           (term (exp (list (term (exp (lit 3)))
                                                            (term (exp (lit 4)))
                                           )))
                          )))
           )))
        """
    )



    //****************************************
    // LIKE operator
    //****************************************
    /*
    From SQL92 https://www.contrib.andrew.cmu.edu/~shadow/sql/sql1992.txt
         <like predicate>   ::= <match value> [ NOT ] LIKE <pattern>
                                    [ ESCAPE <escape character> ]

         <match value>      ::= <character value expression>
         <pattern>          ::= <character value expression>
         <escape character> ::= <character value expression>
     */
    @Test
    fun likeColNameLikeString() = assertExpression(
        """
        (like (id a case_insensitive) (lit "_AAA%"))
        """,
        "a LIKE '_AAA%'",
        """(term (exp (like (term (exp (id a case_insensitive))) (term (exp (lit "_AAA%"))) )))"""
    )

    @Test
    fun likeColNameLikeColName() = assertExpression(
        """
        (like (id a case_insensitive) (id b case_insensitive))
        """,
        "a LIKE b",
        """(term (exp (like (term (exp (id a case_insensitive)))
                            (term (exp (id b case_insensitive)))
           )))
        """
    )

    @Test
    fun pathLikePath() = assertExpression(
        """
        (like
            (path (id a case_insensitive) (case_insensitive (lit "name")))
            (path (id b case_insensitive) (case_insensitive (lit "pattern"))))
        """,
        "a.name LIKE b.pattern",
        """
            (term
    (exp
        (like
            (term
                (exp
                    (path
                        (term
                            (exp
                                (id a case_insensitive)))
                        (path_element
                            (term
                                (exp
                                    (lit "name")))
                            case_insensitive))))
            (term
                (exp
                    (path
                        (term
                            (exp
                                (id b case_insensitive)))
                        (path_element
                            (term
                                (exp
                                    (lit "pattern")))
                            case_insensitive)))))))
        """
    )

    @Test
    fun likeColNameLikeColNamePath() = assertExpression(
        """
        (like
            (path (id a case_insensitive) (case_insensitive (lit "name")))
            (path (id b case_insensitive) (case_insensitive (lit "pattern"))))
        """,
        "a.name LIKE b.pattern",
        """
            (term
                (exp
                    (like
                        (term
                            (exp
                                (path
                                    (term
                                        (exp
                                            (id a case_insensitive)))
                                    (path_element
                                        (term
                                            (exp
                                                (lit "name")))
                                        case_insensitive))))
                        (term
                            (exp
                                (path
                                    (term
                                        (exp
                                            (id b case_insensitive)))
                                    (path_element
                                        (term
                                            (exp
                                                (lit "pattern")))
                                        case_insensitive)))))))
        """
    )

    @Test
    fun likeColNameLikeStringEscape() = assertExpression(
        """
        (like
            (id a case_insensitive)
            (lit "_AAA%")
            (lit "["))
        """,
        "a LIKE '_AAA%' ESCAPE '['",
        """(term (exp (like (term (exp (id a case_insensitive)))
                            (term (exp (lit "_AAA%")))
                            (term (exp (lit "[")))
            )))
        """
    )

    @Test
    fun notLikeColNameLikeString() = assertExpression(
        """
        (not_like
            (id a case_insensitive)
            (lit "_AAA%"))
        """,
        "a NOT LIKE '_AAA%'",
        """(term (exp (not (term (exp (like (term (exp (id a case_insensitive)))
                                            (term (exp (lit "_AAA%")))
                           )))
           )))
        """
    )

    @Test
    fun likeColNameLikeColNameEscape() = assertExpression(
        """
        (like
            (id a case_insensitive)
            (id b case_insensitive)
            (lit "\\"))
        """, //  escape \ inside a Kotlin/Java String
        "a LIKE b ESCAPE '\\'", // escape \ inside a Kotlin/Java String
        """(term (exp (like (term (exp (id a case_insensitive)))
                            (term (exp (id b case_insensitive)))
                            (term (exp (lit "\\")))
           )))
        """
    )

    @Test
    fun likeColNameLikeColNameEscapeNonLit() = assertExpression(
        """
            (like (id a case_insensitive) (id b case_insensitive) (id c case_insensitive))
        """,
        "a LIKE b ESCAPE c",
        """(term (exp (like (term (exp (id a case_insensitive)))
                            (term (exp (id b case_insensitive)))
                            (term (exp (id c case_insensitive)))
           )))
        """
    )

    @Test
    fun likeColNameLikeColNameEscapePath() = assertExpression(
        """
        (like (id a case_insensitive) (id b case_insensitive) (path (id x case_insensitive) (case_insensitive (lit "c"))))
        """,
        "a LIKE b ESCAPE x.c",
        """
            (term
                (exp
                    (like
                        (term
                            (exp
                                (id a case_insensitive)))
                        (term
                            (exp
                                (id b case_insensitive)))
                        (term
                            (exp
                                (path
                                    (term
                                        (exp
                                            (id x case_insensitive)))
                                    (path_element
                                        (term
                                            (exp
                                                (lit "c")))
                                        case_insensitive)))))))
        """
    )

    //****************************************
    // date part
    //****************************************
    @Test
    fun datePartYear() = assertExpression(
        "(lit \"year\")",
        "year",
        """(term (exp (lit "year")))""")

    @Test
    fun datePartMonth() = assertExpression(
        "(lit \"month\")",
        "month",
        """(term (exp (lit "month")))""")

    @Test
    fun datePartDay() = assertExpression(
        "(lit \"day\")",
        "day",
        """(term (exp (lit "day")))""")

    @Test
    fun datePartHour() = assertExpression(
        "(lit \"hour\")",
        "hour",
        """(term (exp (lit "hour")))""")

    @Test
    fun datePartMinutes() = assertExpression(
        "(lit \"minute\")",
        "minute",
        """(term (exp (lit "minute")))""")

    @Test
    fun datePartSeconds() = assertExpression(
        "(lit \"second\")",
        "second",
        """(term (exp (lit "second")))""")

    @Test
    fun datePartTimestampHour() = assertExpression(
        "(lit \"timezone_hour\")",
        "timezone_hour",
        """(term (exp (lit "timezone_hour")))""")

    @Test
    fun datePartTimezoneMinute() = assertExpression(
        "(lit \"timezone_minute\")",
        "timezone_minute",
        """(term (exp (lit "timezone_minute")))""")


    //****************************************
    // call date add (special syntax)
    //****************************************
    @Test
    fun callDateAddYear() = assertExpression(
        "(call date_add (lit \"year\") (id a case_insensitive) (id b case_insensitive))",
        "date_add(year, a, b)",
        """(term (exp (call date_add
                            (term (exp (lit "year")))
                            (term (exp (id a case_insensitive)))
                            (term (exp (id b case_insensitive)))
           )))
        """
    )

    @Test
    fun callDateAddMonth() = assertExpression(
        "(call date_add (lit \"month\") (id a case_insensitive) (id b case_insensitive))",
        "date_add(month, a, b)",
        """(term (exp (call date_add
                            (term (exp (lit "month")))
                            (term (exp (id a case_insensitive)))
                            (term (exp (id b case_insensitive)))
           )))
        """
    )

    @Test
    fun callDateAddDay() = assertExpression(
        "(call date_add (lit \"day\") (id a case_insensitive) (id b case_insensitive))",
        "date_add(day, a, b)",
        """(term (exp (call date_add
                            (term (exp (lit "day")))
                            (term (exp (id a case_insensitive)))
                            (term (exp (id b case_insensitive)))
           )))
        """
    )

    @Test
    fun callDateAddHour() = assertExpression(
        "(call date_add (lit \"hour\") (id a case_insensitive) (id b case_insensitive))",
        "date_add(hour, a, b)",
        """(term (exp (call date_add
                            (term (exp (lit "hour")))
                            (term (exp (id a case_insensitive)))
                            (term (exp (id b case_insensitive)))
           )))
        """
    )

    @Test
    fun callDateAddMinute() = assertExpression(
        "(call date_add (lit \"minute\") (id a case_insensitive) (id b case_insensitive))",
        "date_add(minute, a, b)",
        """(term (exp (call date_add
                            (term (exp (lit "minute")))
                            (term (exp (id a case_insensitive)))
                            (term (exp (id b case_insensitive)))
           )))
        """
    )

    @Test
    fun callDateAddSecond() = assertExpression(
        "(call date_add (lit \"second\") (id a case_insensitive) (id b case_insensitive))",
        "date_add(second, a, b)",
        """(term (exp (call date_add
                            (term (exp (lit "second")))
                            (term (exp (id a case_insensitive)))
                            (term (exp (id b case_insensitive)))
           )))
        """
    )

    @Test // invalid evaluation, but valid parsing
    fun callDateAddTwoArguments() = assertExpression(
        "(call date_add (lit \"second\") (id a case_insensitive))",
        "date_add(second, a)",
        """(term (exp (call date_add
                            (term (exp (lit "second")))
                            (term (exp (id a case_insensitive)))
           )))
        """
    )

    @Test // invalid evaluation, but valid parsing
    fun callDateAddTimezoneHour() = assertExpression(
        "(call date_add (lit \"timezone_hour\") (id a case_insensitive) (id b case_insensitive))",
        "date_add(timezone_hour, a, b)",
        """(term (exp (call date_add
                            (term (exp (lit "timezone_hour")))
                            (term (exp (id a case_insensitive)))
                            (term (exp (id b case_insensitive)))
           )))
        """
    )

    @Test // invalid evaluation, but valid parsing
    fun callDateAddTimezoneMinute() = assertExpression(
        "(call date_add (lit \"timezone_minute\") (id a case_insensitive) (id b case_insensitive))",
        "date_add(timezone_minute, a, b)",
        """(term (exp (call date_add
                            (term (exp (lit "timezone_minute")))
                            (term (exp (id a case_insensitive)))
                            (term (exp (id b case_insensitive)))
           )))
        """
    )

    //****************************************
    // call extract (special syntax)
    //****************************************
    @Test
    fun callExtractYear() = assertExpression(
        "(call extract (lit \"year\") (id a case_insensitive))",
        "extract(year from a)",
        """(term (exp (call extract
                            (term (exp (lit "year")))
                            (term (exp (id a case_insensitive)))
           )))
        """
    )

    @Test
    fun callExtractMonth() = assertExpression(
        "(call extract (lit \"month\") (id a case_insensitive))",
        "extract(month from a)",
        """(term (exp (call extract
                            (term (exp (lit "month")))
                            (term (exp (id a case_insensitive)))
           )))
        """
    )

    @Test
    fun callExtractDay() = assertExpression(
        "(call extract (lit \"day\") (id a case_insensitive))",
        "extract(day from a)",
        """(term (exp (call extract
                            (term (exp (lit "day")))
                            (term (exp (id a case_insensitive)))
           )))
        """
    )

    @Test
    fun callExtractHour() = assertExpression(
        "(call extract (lit \"hour\") (id a case_insensitive))",
        "extract(hour from a)",
        """(term (exp (call extract
                            (term (exp (lit "hour")))
                            (term (exp (id a case_insensitive)))
           )))
        """
    )

    @Test
    fun callExtractMinute() = assertExpression(
        "(call extract (lit \"minute\") (id a case_insensitive))",
        "extract(minute from a)",
        """(term (exp (call extract
                            (term (exp (lit "minute")))
                            (term (exp (id a case_insensitive)))
           )))
        """
    )

    @Test
    fun callExtractSecond() = assertExpression(
        "(call extract (lit \"second\") (id a case_insensitive))",
        "extract(second from a)",
        """(term (exp (call extract
                            (term (exp (lit "second")))
                            (term (exp (id a case_insensitive)))
           )))
        """
    )

    @Test
    fun callExtractTimezoneHour() = assertExpression(
        "(call extract (lit \"timezone_hour\") (id a case_insensitive))",
        "extract(timezone_hour from a)",
        """(term (exp (call extract
                            (term (exp (lit "timezone_hour")))
                            (term (exp (id a case_insensitive)))
           )))
        """
    )

    @Test
    fun callExtractTimezoneMinute() = assertExpression(
        "(call extract (lit \"timezone_minute\") (id a case_insensitive))",
        "extract(timezone_minute from a)",
        """(term (exp (call extract
                            (term (exp (lit "timezone_minute")))
                            (term (exp (id a case_insensitive)))
           )))
        """
    )

    @Test
    fun caseInsensitiveFunctionName() = assertExpression(
        "(call my_function (id a case_insensitive))",
        "mY_fUnCtIoN(a)",
        """(term (exp (call my_function
                            (term (exp (id a case_insensitive)))
           )))
        """
    )

    @Test
    fun parameterExpression() = assertExpression(
            "(parameter 1)",
            "?",
            "(term (exp (parameter 1)))")

    //****************************************
    // SELECT
    //****************************************
    @Test
    fun selectWithSingleFrom() = assertExpression(
        "(select (project (list (id a case_insensitive))) (from (id table1 case_insensitive)))",
        "SELECT a FROM table1",
        """
        (term
            (exp
                (select
                    (project
                        (list
                            (term
                                (exp
                                    (id a case_insensitive)))))
                    (from
                        (term
                            (exp
                                (id table1 case_insensitive)))))))
        """
    )

    @Test
    fun selectAllWithSingleFrom() = assertExpression(
        "(select (project (list (id a case_insensitive))) (from (id table1 case_insensitive)))",
        "SELECT ALL a FROM table1",
        """
            (term
                (exp
                    (select
                        (project
                            (list
                                (term
                                    (exp
                                        (id a case_insensitive)))))
                        (from
                            (term
                                (exp
                                    (id table1 case_insensitive)))))))
        """
    )

    @Test
    fun selectDistinctWithSingleFrom() = assertExpression(
        "(select (project_distinct (list (id a case_insensitive))) (from (id table1 case_insensitive)))",
        "SELECT DISTINCT a FROM table1",
        """
        (term
            (exp
                (select
                    (project_distinct
                        (list
                            (term
                                (exp
                                    (id a case_insensitive)))))
                    (from
                        (term
                            (exp
                                (id table1 case_insensitive)))))))
        """
    )

    @Test
    fun selectStar() = assertExpression(
        "(select (project (list (project_all))) (from (id table1 case_insensitive)))",
        "SELECT * FROM table1",
        """
        (term
            (exp
                (select
                    (project
                        (list
                            (term
                                (exp
                                    (star)))))
                    (from
                        (term
                            (exp
                                (id table1 case_insensitive)))))))
        """
    )

    @Test
    fun selectAliasDotStar() = assertExpression(
        "(select (project (list (project_all (id t case_insensitive)))) (from (as t (id table1 case_insensitive))))",
        "SELECT t.* FROM table1 AS t",
        """
        (term
            (exp
                (select
                    (project
                        (list
                            (path_project_all
                                (term
                                    (exp
                                        (id t case_insensitive))))))
                    (from
                        (term
                            (exp
                                (as
                                    t
                                    (term
                                        (exp
                                            (id table1 case_insensitive))))))))))
        """
    )

    @Test
    fun selectPathAliasDotStar() = assertExpression(
        "(select (project (list (project_all (path (id a case_insensitive) (case_insensitive (lit \"b\")))))) (from (as t (id table1 case_insensitive))))",
        "SELECT a.b.* FROM table1 AS t",
        """
        (term
            (exp
                (select
                    (project
                        (list
                            (path_project_all
                                (term
                                    (exp
                                        (path
                                            (term
                                                (exp
                                                    (id a case_insensitive)))
                                            (path_element
                                                (term
                                                    (exp
                                                        (lit "b")))
                                                case_insensitive)))))))
                    (from
                        (term
                            (exp
                                (as
                                    t
                                    (term
                                        (exp
                                            (id table1 case_insensitive))))))))))
        """
    )


    @Test
    fun selectWithFromAt() = assertExpression(
        "(select (project (list (id ord case_insensitive))) (from (at ord (id table1 case_insensitive))))",
        "SELECT ord FROM table1 AT ord",
        """
        (term
            (exp
                (select
                    (project
                        (list
                            (term
                                (exp
                                    (id ord case_insensitive)))))
                    (from
                        (term
                            (exp
                                (at
                                    ord
                                    (term
                                        (exp
                                            (id table1 case_insensitive))))))))))
        """
    )

    @Test
    fun selectWithFromAsAndAt() = assertExpression(
        "(select (project (list (id ord case_insensitive) (id val case_insensitive))) (from (at ord (as val (id table1 case_insensitive)))))",
        "SELECT ord, val FROM table1 AS val AT ord",
        """
        (term
            (exp
                (select
                    (project
                        (list
                            (term
                                (exp
                                    (id ord case_insensitive)))
                            (term
                                (exp
                                    (id val case_insensitive)))))
                    (from
                        (term
                            (exp
                                (at
                                    ord
                                    (term
                                        (exp
                                            (as
                                                val
                                                (term
                                                    (exp
                                                        (id table1 case_insensitive)))))))))))))

        """
    )


    @Test
    fun selectWithFromIdBy() = assertExpression(
        "(select (project (list (project_all))) (from (by uid (id table1 case_insensitive))))",
        "SELECT * FROM table1 BY uid",
        """
         (term
            (exp
                (select
                    (project
                        (list
                            (term
                                (exp
                                    (star)))))
                    (from
                        (term
                            (exp
                                (by
                                uid
                                   (term
                                      (exp
                                          (id table1 case_insensitive))))))))))
        """
    )

    @Test
    fun selectWithFromAtIdBy() = assertExpression(
        "(select (project (list (project_all))) (from (by uid (at ord (id table1 case_insensitive)))))",
        "SELECT * FROM table1 AT ord BY uid",
        """
        (term
            (exp
                (select
                    (project
                        (list
                            (term
                                (exp
                                    (star)))))
                    (from
                        (term
                            (exp
                                (by
                                    uid
                                    (term
                                        (exp
                                            (at
                                                ord
                                                (term
                                                    (exp
                                                        (id table1 case_insensitive)))))))))))))
        """
    )

    @Test
    fun selectWithFromAsIdBy() = assertExpression(
        "(select (project (list (project_all))) (from (by uid (as t (id table1 case_insensitive)))))",
        "SELECT * FROM table1 AS t BY uid",
        """
        (term
            (exp
                (select
                    (project
                        (list
                            (term
                                (exp
                                    (star)))))
                    (from
                        (term
                            (exp
                                (by
                                    uid
                                    (term
                                        (exp
                                            (as
                                                t
                                                (term
                                                    (exp
                                                        (id table1 case_insensitive)))))))))))))
        """
    )

    @Test
    fun selectWithFromAsAndAtIdBy() = assertExpression(
        "(select (project (list (project_all))) (from (by uid (at ord (as val (id table1 case_insensitive))))))",
        "SELECT * FROM table1 AS val AT ord BY uid",
        """
        (term
            (exp
                (select
                    (project
                        (list
                            (term
                                (exp
                                    (star)))))
                    (from
                        (term
                            (exp
                                    (by
                                        uid
                                        (term
                                            (exp
                                            (at
                                                ord
                                                (term
                                                    (exp
                                                        (as
                                                            val
                                                            (term
                                                                (exp
                                                                    (id table1 case_insensitive))))))))))))))))
        """
    )


    @Test
    fun selectWithFromUnpivot() = assertExpression(
        """
        (select
          (project (list (project_all)))
          (from (unpivot (id item case_insensitive)))
        )
        """,
        "SELECT * FROM UNPIVOT item",
        """
        (term
            (exp
                (select
                    (project
                        (list
                            (term
                                (exp
                                    (star)))))
                    (from
                        (term
                            (exp
                                (unpivot
                                    (term
                                        (exp
                                            (id item case_insensitive))))))))))
        """
    )

    @Test
    fun selectWithFromUnpivotWithAt() = assertExpression(
        """
        (select
          (project (list (id ord case_insensitive)))
          (from (at name (unpivot (id item case_insensitive))))
        )
        """,
        "SELECT ord FROM UNPIVOT item AT name",
        """
        (term
            (exp
                (select
                    (project
                        (list
                            (term
                                (exp
                                    (id ord case_insensitive)))))
                    (from
                        (term
                            (exp
                                (at
                                    name
                                    (term
                                        (exp
                                            (unpivot
                                                (term
                                                    (exp
                                                        (id item case_insensitive)))))))))))))
        """
    )

    @Test
    fun selectWithFromUnpivotWithAs() = assertExpression(
        """
        (select
          (project (list (id ord case_insensitive)))
          (from (as val (unpivot (id item case_insensitive))))
        )
        """,
        "SELECT ord FROM UNPIVOT item AS val",
        """
        (term
            (exp
                (select
                    (project
                        (list
                            (term
                                (exp
                                    (id ord case_insensitive)))))
                    (from
                        (term
                            (exp
                                (as
                                    val
                                    (term
                                        (exp
                                            (unpivot
                                                (term
                                                    (exp
                                                        (id item case_insensitive)))))))))))))
        """
    )

    @Test
    fun selectWithFromUnpivotWithAsAndAt() = assertExpression(
        """
        (select
          (project (list (id ord case_insensitive)))
          (from (at name (as val (unpivot (id item case_insensitive)))))
        )
        """,
        "SELECT ord FROM UNPIVOT item AS val AT name",
        """
        (term
            (exp
                (select
                    (project
                        (list
                            (term
                                (exp
                                    (id ord case_insensitive)))))
                    (from
                        (term
                            (exp
                                (at
                                    name
                                    (term
                                        (exp
                                            (as
                                                val
                                                (term
                                                    (exp
                                                        (unpivot
                                                            (term
                                                                (exp
                                                                    (id item case_insensitive))))))))))))))))
        """
    )

    @Test
    fun selectAllStar() = assertExpression(
        "(select (project (list (project_all))) (from (id table1 case_insensitive)))",
        "SELECT ALL * FROM table1",
        """
        (term
            (exp
                (select
                    (project
                        (list
                            (term
                                (exp
                                    (star)))))
                    (from
                        (term
                            (exp
                                (id table1 case_insensitive)))))))
        """
    )

    @Test
    fun selectDistinctStar() = assertExpression(
        "(select (project_distinct (list (project_all))) (from (id table1 case_insensitive)))",
        "SELECT DISTINCT * FROM table1",
        """
        (term
            (exp
                (select
                    (project_distinct
                        (list
                            (term
                                (exp
                                    (star)))))
                    (from
                        (term
                            (exp
                                (id table1 case_insensitive)))))))
        """
    )

    @Test
    fun selectWhereMissing() = assertExpression(
        "(select (project (list (id a case_insensitive))) (from (id stuff case_insensitive)) (where (is (id b case_insensitive) (type missing))))",
        "SELECT a FROM stuff WHERE b IS MISSING",
        """
        (term
            (exp
                (select
                    (project
                        (list
                            (term
                                (exp
                                    (id a case_insensitive)))))
                    (from
                        (term
                            (exp
                                (id stuff case_insensitive))))
                    (where
                        (term
                            (exp
                                (is
                                    (term
                                        (exp
                                            (id b case_insensitive)))
                                    (term
                                        (exp
                                            (type
                                                missing))))))))))
        """
    )

    @Test
    fun selectCommaCrossJoin1() = assertExpression(
        "(select (project (list (id a case_insensitive))) (from (inner_join (id table1 case_insensitive) (id table2 case_insensitive))))",
        "SELECT a FROM table1, table2",
        """
        (term
            (exp
                (select
                    (project
                        (list
                            (term
                                (exp
                                    (id a case_insensitive)))))
                    (from
                        (term
                            (exp
                                (inner_join
                                    (term
                                        (exp
                                            (id table1 case_insensitive)))
                                    (term
                                        (exp
                                            (id table2 case_insensitive))))))))))
        """
    )

    @Test
    fun selectCommaCrossJoin2() = assertExpression(
        "(select (project (list (id a case_insensitive))) (from (inner_join (inner_join (id table1 case_insensitive) (id table2 case_insensitive)) (id table3 case_insensitive))))",
        "SELECT a FROM table1, table2, table3",
        """
        (term
            (exp
                (select
                    (project
                        (list
                            (term
                                (exp
                                    (id a case_insensitive)))))
                    (from
                        (term
                            (exp
                                (inner_join
                                    (term
                                        (exp
                                            (inner_join
                                                (term
                                                    (exp
                                                        (id table1 case_insensitive)))
                                                (term
                                                    (exp
                                                        (id table2 case_insensitive))))))
                                    (term
                                        (exp
                                            (id table3 case_insensitive))))))))))
        """

    )

    @Test
    fun selectMultipleWithMultipleFromSimpleWhere() = assertExpression(
        """(select
             (project (list (id a case_insensitive) (id b case_insensitive)))
             (from (inner_join (as t1 (id table1 case_insensitive)) (id table2 case_insensitive)))
             (where (call f (id t1 case_insensitive)))
           )
        """,
        "SELECT a, b FROM table1 as t1, table2 WHERE f(t1)",
        """
        (term
            (exp
                (select
                    (project
                        (list
                            (term
                                (exp
                                    (id a case_insensitive)))
                            (term
                                (exp
                                    (id b case_insensitive)))))
                    (from
                        (term
                            (exp
                                (inner_join
                                    (term
                                        (exp
                                            (as
                                                t1
                                                (term
                                                    (exp
                                                        (id table1 case_insensitive))))))
                                    (term
                                        (exp
                                            (id table2 case_insensitive)))))))
                    (where
                        (term
                            (exp
                                (call
                                    f
                                    (term
                                        (exp
                                            (id t1 case_insensitive))))))))))
        """
    )

    @Test
    fun selectMultipleWithMultipleFromSimpleWhereNoAsAlias() = assertExpression(
        """(select
             (project (list (as a1 (id a case_insensitive)) (as b1 (id b case_insensitive))))
             (from (inner_join (as t1 (id table1 case_insensitive)) (id table2 case_insensitive)))
             (where (call f (id t1 case_insensitive)))
           )
        """,
        "SELECT a a1, b b1 FROM table1 t1, table2 WHERE f(t1)",
        """
        (term
            (exp
                (select
                    (project
                        (list
                            (term
                                (exp
                                    (as
                                        a1
                                        (term
                                            (exp
                                                (id a case_insensitive))))))
                            (term
                                (exp
                                    (as
                                        b1
                                        (term
                                            (exp
                                                (id b case_insensitive))))))))
                    (from
                        (term
                            (exp
                                (inner_join
                                    (term
                                        (exp
                                            (as
                                                t1
                                                (term
                                                    (exp
                                                        (id table1 case_insensitive))))))
                                    (term
                                        (exp
                                            (id table2 case_insensitive)))))))
                    (where
                        (term
                            (exp
                                (call
                                    f
                                    (term
                                        (exp
                                            (id t1 case_insensitive))))))))))
        """
    )

    @Test
    fun selectCorrelatedJoin() = assertExpression(
        """(select
             (project (list (id a case_insensitive) (id b case_insensitive)))
             (from (inner_join (as s (id stuff case_insensitive)) (@ (id s case_insensitive))))
             (where (call f (id s case_insensitive)))
           )
        """,
        "SELECT a, b FROM stuff s, @s WHERE f(s)",
        """
        (term
            (exp
                (select
                    (project
                        (list
                            (term
                                (exp
                                    (id a case_insensitive)))
                            (term
                                (exp
                                    (id b case_insensitive)))))
                    (from
                        (term
                            (exp
                                (inner_join
                                    (term
                                        (exp
                                            (as
                                                s
                                                (term
                                                    (exp
                                                        (id stuff case_insensitive))))))
                                    (term
                                        (exp
                                            (@
                                                (id s case_insensitive))))))))
                    (where
                        (term
                            (exp
                                (call
                                    f
                                    (term
                                        (exp
                                            (id s case_insensitive))))))))))
        """
    )

    @Test
    fun selectCorrelatedExplicitInnerJoin() = assertExpression(
        """(select
             (project (list (id a case_insensitive) (id b case_insensitive)))
             (from (inner_join (as s (id stuff case_insensitive)) (@ (id s case_insensitive))))
             (where (call f (id s case_insensitive)))
           )
        """,
        "SELECT a, b FROM stuff s INNER CROSS JOIN @s WHERE f(s)",
        """
        (term
            (exp
                (select
                    (project
                        (list
                            (term
                                (exp
                                    (id a case_insensitive)))
                            (term
                                (exp
                                    (id b case_insensitive)))))
                    (from
                        (term
                            (exp
                                (inner_join
                                    (term
                                        (exp
                                            (as
                                                s
                                                (term
                                                    (exp
                                                        (id stuff case_insensitive))))))
                                    (term
                                        (exp
                                            (@
                                                (id s case_insensitive))))))))
                    (where
                        (term
                            (exp
                                (call
                                    f
                                    (term
                                        (exp
                                            (id s case_insensitive))))))))))
        """
    )

    @Test
    fun selectCorrelatedExplicitCrossJoin() = assertExpression(
        """(select
             (project (list (id a case_insensitive) (id b case_insensitive)))
             (from (inner_join (as s (id stuff case_insensitive)) (@ (id s case_insensitive))))
             (where (call f (id s case_insensitive)))
           )
        """,
        "SELECT a, b FROM stuff s CROSS JOIN @s WHERE f(s)",
        """
        (term
            (exp
                (select
                    (project
                        (list
                            (term
                                (exp
                                    (id a case_insensitive)))
                            (term
                                (exp
                                    (id b case_insensitive)))))
                    (from
                        (term
                            (exp
                                (inner_join
                                    (term
                                        (exp
                                            (as
                                                s
                                                (term
                                                    (exp
                                                        (id stuff case_insensitive))))))
                                    (term
                                        (exp
                                            (@
                                                (id s case_insensitive))))))))
                    (where
                        (term
                            (exp
                                (call
                                    f
                                    (term
                                        (exp
                                            (id s case_insensitive))))))))))
        """
    )

    @Test
    fun selectCorrelatedExplicitLeftJoin() = assertExpression(
        """(select
             (project (list (id a case_insensitive) (id b case_insensitive)))
             (from (left_join (as s (id stuff case_insensitive)) (@ (id s case_insensitive))))
             (where (call f (id s case_insensitive)))
           )
        """,
        "SELECT a, b FROM stuff s LEFT CROSS JOIN @s WHERE f(s)",
        """
        (term
            (exp
                (select
                    (project
                        (list
                            (term
                                (exp
                                    (id a case_insensitive)))
                            (term
                                (exp
                                    (id b case_insensitive)))))
                    (from
                        (term
                            (exp
                                (left_join
                                    (term
                                        (exp
                                            (as
                                                s
                                                (term
                                                    (exp
                                                        (id stuff case_insensitive))))))
                                    (term
                                        (exp
                                            (@
                                                (id s case_insensitive))))))))
                    (where
                        (term
                            (exp
                                (call
                                    f
                                    (term
                                        (exp
                                            (id s case_insensitive))))))))))
        """
    )

    @Test
    fun selectCorrelatedLeftOuterJoinOn() = assertExpression(
        """(select
             (project (list (id a case_insensitive) (id b case_insensitive)))
             (from
               (left_join
                 (as s (id stuff case_insensitive))
                 (@ (id s case_insensitive))
                 (call f (id s case_insensitive))
               )
             )
           )
        """,
        "SELECT a, b FROM stuff s LEFT JOIN @s ON f(s)",
        """
        (term
            (exp
                (select
                    (project
                        (list
                            (term
                                (exp
                                    (id a case_insensitive)))
                            (term
                                (exp
                                    (id b case_insensitive)))))
                    (from
                        (term
                            (exp
                                (left_join
                                    (term
                                        (exp
                                            (as
                                                s
                                                (term
                                                    (exp
                                                        (id stuff case_insensitive))))))
                                    (term
                                        (exp
                                            (@
                                                (id s case_insensitive))))
                                    (term
                                        (exp
                                            (call
                                                f
                                                (term
                                                    (exp
                                                        (id s case_insensitive)))))))))))))
        """
    )

    @Test
    fun selectRightJoin() = assertExpression(
        """(select
             (project (list (id a case_insensitive) (id b case_insensitive)))
             (from
               (right_join
                 (as s (id stuff case_insensitive))
                 (as f (id foo case_insensitive))
               )
             )
           )
        """,
        "SELECT a, b FROM stuff s RIGHT CROSS JOIN foo f",
        """
        (term
            (exp
                (select
                    (project
                        (list
                            (term
                                (exp
                                    (id a case_insensitive)))
                            (term
                                (exp
                                    (id b case_insensitive)))))
                    (from
                        (term
                            (exp
                                (right_join
                                    (term
                                        (exp
                                            (as
                                                s
                                                (term
                                                    (exp
                                                        (id stuff case_insensitive))))))
                                    (term
                                        (exp
                                            (as
                                                f
                                                (term
                                                    (exp
                                                        (id foo case_insensitive)))))))))))))
        """
    )

    @Test
    fun selectFullOuterJoinOn() = assertExpression(
        """(select
             (project (list (id a case_insensitive) (id b case_insensitive)))
             (from
               (outer_join
                 (as s (id stuff case_insensitive))
                 (as f (id foo case_insensitive))
                 (= (id s case_insensitive) (id f case_insensitive))
               )
             )
           )
        """,
        "SELECT a, b FROM stuff s FULL OUTER JOIN foo f ON s = f",
        """
        (term
            (exp
                (select
                    (project
                        (list
                            (term
                                (exp
                                    (id a case_insensitive)))
                            (term
                                (exp
                                    (id b case_insensitive)))))
                    (from
                        (term
                            (exp
                                (outer_join
                                    (term
                                        (exp
                                            (as
                                                s
                                                (term
                                                    (exp
                                                        (id stuff case_insensitive))))))
                                    (term
                                        (exp
                                            (as
                                                f
                                                (term
                                                    (exp
                                                        (id foo case_insensitive))))))
                                    (term
                                        (exp
                                            (=
                                                (term
                                                    (exp
                                                        (id s case_insensitive)))
                                                (term
                                                    (exp
                                                        (id f case_insensitive)))))))))))))
        """
    )

    @Test
    fun selectJoins() = assertExpression(
        """(select
             (project (list (id x case_insensitive)))
             (from
               (outer_join
                 (right_join
                   (left_join
                     (inner_join
                       (inner_join
                         (id a case_insensitive)
                         (id b case_insensitive)
                       )
                       (id c case_insensitive)
                     )
                     (id d case_insensitive)
                     (id e case_insensitive)
                   )
                   (id f case_insensitive)
                 )
                 (id g case_insensitive)
                 (id h case_insensitive)
               )
             )
           )
        """,
        "SELECT x FROM a, b CROSS JOIN c LEFT JOIN d ON e RIGHT OUTER CROSS JOIN f OUTER JOIN g ON h",
        """
        (term
            (exp
                (select
                    (project
                        (list
                            (term
                                (exp
                                    (id x case_insensitive)))))
                    (from
                        (term
                            (exp
                                (outer_join
                                    (term
                                        (exp
                                            (right_join
                                                (term
                                                    (exp
                                                        (left_join
                                                            (term
                                                                (exp
                                                                    (inner_join
                                                                        (term
                                                                            (exp
                                                                                (inner_join
                                                                                    (term
                                                                                        (exp
                                                                                            (id a case_insensitive)))
                                                                                    (term
                                                                                        (exp
                                                                                            (id b case_insensitive))))))
                                                                        (term
                                                                            (exp
                                                                                (id c case_insensitive))))))
                                                            (term
                                                                (exp
                                                                    (id d case_insensitive)))
                                                            (term
                                                                (exp
                                                                    (id e case_insensitive))))))
                                                (term
                                                    (exp
                                                        (id f case_insensitive))))))
                                    (term
                                        (exp
                                            (id g case_insensitive)))
                                    (term
                                        (exp
                                            (id h case_insensitive))))))))))
        """
    )


    @Test
    fun selectJoins2() = assertExpression(
        """(select
             (project (list (id x case_insensitive)))
             (from
               (outer_join
                 (right_join
                   (left_join
                     (inner_join
                       (inner_join
                         (id a case_insensitive)
                         (id b case_insensitive)
                       )
                       (id c case_insensitive)
                     )
                     (id d case_insensitive)
                     (id e case_insensitive)
                   )
                   (id f case_insensitive)
                 )
                 (id g case_insensitive)
                 (id h case_insensitive)
               )
             )
           )
        """,
        "SELECT x FROM a INNER CROSS JOIN b CROSS JOIN c LEFT JOIN d ON e RIGHT OUTER CROSS JOIN f OUTER JOIN g ON h",
        """
        (term
            (exp
                (select
                    (project
                        (list
                            (term
                                (exp
                                    (id x case_insensitive)))))
                    (from
                        (term
                            (exp
                                (outer_join
                                    (term
                                        (exp
                                            (right_join
                                                (term
                                                    (exp
                                                        (left_join
                                                            (term
                                                                (exp
                                                                    (inner_join
                                                                        (term
                                                                            (exp
                                                                                (inner_join
                                                                                    (term
                                                                                        (exp
                                                                                            (id a case_insensitive)))
                                                                                    (term
                                                                                        (exp
                                                                                            (id b case_insensitive))))))
                                                                        (term
                                                                            (exp
                                                                                (id c case_insensitive))))))
                                                            (term
                                                                (exp
                                                                    (id d case_insensitive)))
                                                            (term
                                                                (exp
                                                                    (id e case_insensitive))))))
                                                (term
                                                    (exp
                                                        (id f case_insensitive))))))
                                    (term
                                        (exp
                                            (id g case_insensitive)))
                                    (term
                                        (exp
                                            (id h case_insensitive))))))))))
        """

    )

    @Test
    fun selectListWithAggregateWildcardCall() = assertExpression(
        """
        (select
          (project
            (list
              (+ (call_agg sum all (id a case_insensitive)) (call_agg_wildcard count))
              (call_agg avg all (id b case_insensitive))
              (call_agg min all (id c case_insensitive))
              (call_agg max all (+ (id d case_insensitive) (id e case_insensitive)))
            )
          )
          (from (id foo case_insensitive))
        )
        """,
        "SELECT sum(a) + count(*), AVG(b), MIN(c), MAX(d + e) FROM foo",
        """
        (term
            (exp
                (select
                    (project
                        (list
                            (term
                                (exp
                                    (+
                                        (term
                                            (exp
                                                (call_agg
                                                    sum
                                                    all
                                                    (term
                                                        (exp
                                                            (id a case_insensitive))))))
                                        (term
                                            (exp
                                                (call_agg_wildcard
                                                    count))))))
                            (term
                                (exp
                                    (call_agg
                                        avg
                                        all
                                        (term
                                            (exp
                                                (id b case_insensitive))))))
                            (term
                                (exp
                                    (call_agg
                                        min
                                        all
                                        (term
                                            (exp
                                                (id c case_insensitive))))))
                            (term
                                (exp
                                    (call_agg
                                        max
                                        all
                                        (term
                                            (exp
                                                (+
                                                    (term
                                                        (exp
                                                            (id d case_insensitive)))
                                                    (term
                                                        (exp
                                                            (id e case_insensitive)))))))))))
                    (from
                        (term
                            (exp
                                (id foo case_insensitive)))))))
        """
    )

    @Test
    fun pathsAndSelect() = assertExpression(
        """(select
             (project
               (list
                 (as a (path (call process (id t case_insensitive)) (case_insensitive (lit "a")) (lit 0)))
                 (as b (path (id t2 case_insensitive) (case_insensitive (lit "b"))))
               )
             )
             (from
               (inner_join
                 (as t (path (id t1 case_insensitive) (case_insensitive (lit "a"))))
                 (path (id t2 case_insensitive) (case_insensitive (lit "x")) (* unpivot) (case_insensitive (lit "b")))
               )
             )
             (where
               (and
                 (call test (path (id t2 case_insensitive) (case_insensitive (lit "name"))) (path (id t1 case_insensitive) (case_insensitive (lit "name"))))
                 (= (path (id t1 case_insensitive) (case_insensitive (lit "id"))) (path (id t2 case_insensitive) (case_insensitive (lit "id"))))
               )
             )
           )
        """,
        """SELECT process(t).a[0] AS a, t2.b AS b
           FROM t1.a AS t, t2.x.*.b
           WHERE test(t2.name, t1.name) AND t1.id = t2.id
        """,
        """
        (term
            (exp
                (select
                    (project
                        (list
                            (term
                                (exp
                                    (as
                                        a
                                        (term
                                            (exp
                                                (path
                                                    (term
                                                        (exp
                                                            (call
                                                                process
                                                                (term
                                                                    (exp
                                                                        (id t case_insensitive))))))
                                                    (path_element
                                                        (term
                                                            (exp
                                                                (lit "a")))
                                                        case_insensitive)
                                                    (path_element
                                                        (term
                                                            (exp
                                                                (lit 0)))
                                                        case_sensitive)))))))
                            (term
                                (exp
                                    (as
                                        b
                                        (term
                                            (exp
                                                (path
                                                    (term
                                                        (exp
                                                            (id t2 case_insensitive)))
                                                    (path_element
                                                        (term
                                                            (exp
                                                                (lit "b")))
                                                        case_insensitive)))))))))
                    (from
                        (term
                            (exp
                                (inner_join
                                    (term
                                        (exp
                                            (as
                                                t
                                                (term
                                                    (exp
                                                        (path
                                                            (term
                                                                (exp
                                                                    (id t1 case_insensitive)))
                                                            (path_element
                                                                (term
                                                                    (exp
                                                                        (lit "a")))
                                                                case_insensitive)))))))
                                    (term
                                        (exp
                                            (path
                                                (term
                                                    (exp
                                                        (id t2 case_insensitive)))
                                                (path_element
                                                    (term
                                                        (exp
                                                            (lit "x")))
                                                    case_insensitive)
                                                (path_element
                                                    (term
                                                        (exp
                                                            (star unpivot))))
                                                (path_element
                                                    (term
                                                        (exp
                                                            (lit "b")))
                                                    case_insensitive))))))))
                    (where
                        (term
                            (exp
                                (and
                                    (term
                                        (exp
                                            (call
                                                test
                                                (term
                                                    (exp
                                                        (path
                                                            (term
                                                                (exp
                                                                    (id t2 case_insensitive)))
                                                            (path_element
                                                                (term
                                                                    (exp
                                                                        (lit "name")))
                                                                case_insensitive))))
                                                (term
                                                    (exp
                                                        (path
                                                            (term
                                                                (exp
                                                                    (id t1 case_insensitive)))
                                                            (path_element
                                                                (term
                                                                    (exp
                                                                        (lit "name")))
                                                                case_insensitive)))))))
                                    (term
                                        (exp
                                            (=
                                                (term
                                                    (exp
                                                        (path
                                                            (term
                                                                (exp
                                                                    (id t1 case_insensitive)))
                                                            (path_element
                                                                (term
                                                                    (exp
                                                                        (lit "id")))
                                                                case_insensitive))))
                                                (term
                                                    (exp
                                                        (path
                                                            (term
                                                                (exp
                                                                    (id t2 case_insensitive)))
                                                            (path_element
                                                                (term
                                                                    (exp
                                                                        (lit "id")))
                                                                case_insensitive))))))))))))))
        """
    )

    @Test
    fun selectValueWithSingleFrom() = assertExpression(
        "(select (project (value (id a case_insensitive))) (from (id table1 case_insensitive)))",
        "SELECT VALUE a FROM table1",
        """
        (term
            (exp
                (select
                    (project
                         (value
                            (term
                                (exp
                                    (id a case_insensitive)))))
                    (from
                        (term
                            (exp
                                (id table1 case_insensitive)))))))
        """
    )

    @Test
    fun selectValueWithSingleAliasedFrom() = assertExpression(
        "(select (project (value (id v case_insensitive))) (from (as v (id table1 case_insensitive))))",
        "SELECT VALUE v FROM table1 AS v",
        """
        (term
            (exp
                (select
                    (project
                        (value
                            (term
                                (exp
                                    (id v case_insensitive)))))
                    (from
                        (term
                            (exp
                                (as
                                    v
                                    (term
                                        (exp
                                            (id table1 case_insensitive))))))))))
        """
    )

    @Test
    fun selectAllValues() = assertExpression(
        "(select (project (value (id v case_insensitive))) (from (as v (id table1 case_insensitive))))",
        "SELECT ALL VALUE v FROM table1 AS v",
        """
            (term
                (exp
                    (select
                        (project
                            (value
                                (term
                                    (exp
                                        (id v case_insensitive)))))
                        (from
                            (term
                                (exp
                                    (as
                                        v
                                        (term
                                            (exp
                                                (id table1 case_insensitive))))))))))
    """)

    @Test
    fun selectDistinctValues() = assertExpression(
        "(select (project_distinct (value (id v case_insensitive))) (from (as v (id table1 case_insensitive))))",
        "SELECT DISTINCT VALUE v FROM table1 AS v",
        """
        (term
            (exp
                (select
                    (project_distinct
                        (value
                            (term
                                (exp
                                    (id v case_insensitive)))))
                    (from
                        (term
                            (exp
                                (as
                                    v
                                    (term
                                        (exp
                                            (id table1 case_insensitive))))))))))
        """
    )

    @Test
    fun nestedSelectNoWhere() = assertExpression(
        """(select
             (project (list (project_all)))
             (from
               (path
                 (select
                   (project (list (project_all)))
                   (from (id x case_insensitive))
                 )
                 (case_insensitive (lit "a"))
               )
             )
           )
        """,
        "SELECT * FROM (SELECT * FROM x).a",
        """
        (term
            (exp
                (select
                    (project
                        (list
                            (term
                                (exp
                                    (star)))))
                    (from
                        (term
                            (exp
                                (path
                                    (term
                                        (exp
                                            (select
                                                (project
                                                    (list
                                                        (term
                                                            (exp
                                                                (star)))))
                                                (from
                                                    (term
                                                        (exp
                                                            (id x case_insensitive)))))))
                                    (path_element
                                        (term
                                            (exp
                                                (lit "a")))
                                        case_insensitive))))))))
        """
    )

    @Test
    fun nestedSelect() = assertExpression(
        """(select
             (project (list (project_all)))
             (from
               (path
                 (select
                   (project (list (project_all)))
                   (from (id x case_insensitive))
                   (where (id b case_insensitive))
                 )
                 (case_insensitive (lit "a"))
               )
             )
           )
        """,
        "SELECT * FROM (SELECT * FROM x WHERE b).a",
        """
        (term
            (exp
                (select
                    (project
                        (list
                            (term
                                (exp
                                    (star)))))
                    (from
                        (term
                            (exp
                                (path
                                    (term
                                        (exp
                                            (select
                                                (project
                                                    (list
                                                        (term
                                                            (exp
                                                                (star)))))
                                                (from
                                                    (term
                                                        (exp
                                                            (id x case_insensitive))))
                                                (where
                                                    (term
                                                        (exp
                                                            (id b case_insensitive)))))))
                                    (path_element
                                        (term
                                            (exp
                                                (lit "a")))
                                        case_insensitive))))))))
        """
    )

    @Test
    fun selectLimit() = assertExpression(
        """(select
             (project (list (project_all)))
             (from (id a case_insensitive))
             (limit (lit 10))
           )
        """,
        "SELECT * FROM a LIMIT 10",
        """
        (term
            (exp
                (select
                    (project
                        (list
                            (term
                                (exp
                                    (star)))))
                    (from
                        (term
                            (exp
                                (id a case_insensitive))))
                    (limit
                        (term
                            (exp
                                (lit 10)))))))
        """
    )

    @Test
    fun selectWhereLimit() = assertExpression(
        """(select
             (project (list (project_all)))
             (from (id a case_insensitive))
             (where (= (id a case_insensitive) (lit 5)))
             (limit (lit 10))
           )
        """,
        "SELECT * FROM a WHERE a = 5 LIMIT 10",
        """
        (term
            (exp
                (select
                    (project
                        (list
                            (term
                                (exp
                                    (star)))))
                    (from
                        (term
                            (exp
                                (id a case_insensitive))))
                    (where
                        (term
                            (exp
                                (=
                                    (term
                                        (exp
                                            (id a case_insensitive)))
                                    (term
                                        (exp
                                            (lit 5)))))))
                    (limit
                        (term
                            (exp
                                (lit 10)))))))
        """
    )

    @Test
    fun selectWithParametersAndLiterals() = assertExpression(
        """
        (select
            (project
                (list
                    (parameter
                        1)
                    (path
                        (id f case_insensitive)
                        (case_insensitive
                            (lit "a")))))
            (from
                (as
                    f
                    (id foo case_insensitive)))
            (where
                (and
                    (and
                        (=
                            (path
                                (id f case_insensitive)
                                (case_insensitive
                                    (lit "bar")))
                            (parameter
                                2))
                        (=
                            (path
                                (id f case_insensitive)
                                (case_insensitive
                                    (lit "spam")))
                            (lit "eggs")))
                    (=
                        (path
                            (id f case_insensitive)
                            (case_insensitive
                                (lit "baz")))
                        (parameter
                            3)))))
        """,
        "SELECT ?, f.a from foo f where f.bar = ? and f.spam = 'eggs' and f.baz = ?",
        """
        (term
            (exp
                (select
                    (project
                        (list
                            (term
                                (exp
                                    (parameter
                                        1)))
                            (term
                                (exp
                                    (path
                                        (term
                                            (exp
                                                (id f case_insensitive)))
                                        (path_element
                                            (term
                                                (exp
                                                    (lit "a")))
                                            case_insensitive))))))
                    (from
                        (term
                            (exp
                                (as
                                    f
                                    (term
                                        (exp
                                            (id foo case_insensitive)))))))
                    (where
                        (term
                            (exp
                                (and
                                    (term
                                        (exp
                                            (and
                                                (term
                                                    (exp
                                                        (=
                                                            (term
                                                                (exp
                                                                    (path
                                                                        (term
                                                                            (exp
                                                                                (id f case_insensitive)))
                                                                        (path_element
                                                                            (term
                                                                                (exp
                                                                                    (lit "bar")))
                                                                            case_insensitive))))
                                                            (term
                                                                (exp
                                                                    (parameter
                                                                        2))))))
                                                (term
                                                    (exp
                                                        (=
                                                            (term
                                                                (exp
                                                                    (path
                                                                        (term
                                                                            (exp
                                                                                (id f case_insensitive)))
                                                                        (path_element
                                                                            (term
                                                                                (exp
                                                                                    (lit "spam")))
                                                                            case_insensitive))))
                                                            (term
                                                                (exp
                                                                    (lit "eggs")))))))))
                                    (term
                                        (exp
                                            (=
                                                (term
                                                    (exp
                                                        (path
                                                            (term
                                                                (exp
                                                                    (id f case_insensitive)))
                                                            (path_element
                                                                (term
                                                                    (exp
                                                                        (lit "baz")))
                                                                case_insensitive))))
                                                (term
                                                    (exp
                                                        (parameter
                                                            3)))))))))))))
        """
    )

    //****************************************
    // GROUP BY and GROUP PARTIAL BY
    //****************************************
    @Test
    fun groupBySingleId() = assertExpression(
        """(select
             (project (list (id a case_insensitive)))
             (from (id data case_insensitive))
             (group (by (id a case_insensitive)))
           )
        """,
        "SELECT a FROM data GROUP BY a",
        """
        (term
            (exp
                (select
                    (project
                        (list
                            (term
                                (exp
                                    (id a case_insensitive)))))
                    (from
                        (term
                            (exp
                                (id data case_insensitive))))
                    (group
                        (by
                            (term
                                (exp
                                    (id a case_insensitive))))))))
        """
    )

    @Test
    fun groupBySingleExpr() = assertExpression(
        """(select
             (project (list (+ (id a case_insensitive) (id b case_insensitive))))
             (from (id data case_insensitive))
             (group (by (+ (id a case_insensitive) (id b case_insensitive))))
           )
        """,
        "SELECT a + b FROM data GROUP BY a + b",
        """
            (term
                (exp
                    (select
                        (project
                            (list
                                (term
                                    (exp
                                        (+
                                            (term
                                                (exp
                                                    (id a case_insensitive)))
                                            (term
                                                (exp
                                                    (id b case_insensitive))))))))
                        (from
                            (term
                                (exp
                                    (id data case_insensitive))))
                        (group
                            (by
                                (term
                                    (exp
                                        (+
                                            (term
                                                (exp
                                                    (id a case_insensitive)))
                                            (term
                                                (exp
                                                    (id b case_insensitive)))))))))))
        """
    )

    @Test
    fun groupPartialByMultiAliasedAndGroupAliased() = assertExpression(
        """(select
             (project (list (id g case_insensitive)))
             (from (id data case_insensitive))
             (group_partial
               (by
                 (as x (id a case_insensitive))
                 (as y (+ (id b case_insensitive) (id c case_insensitive)))
                 (as z (call foo (id d case_insensitive)))
               )
               (name g)
             )
           )
        """,
        "SELECT g FROM data GROUP PARTIAL BY a AS x, b + c AS y, foo(d) AS z GROUP AS g",
        """
        (term
            (exp
                (select
                    (project
                        (list
                            (term
                                (exp
                                    (id g case_insensitive)))))
                    (from
                        (term
                            (exp
                                (id data case_insensitive))))
                    (group_partial
                        (by
                            (term
                                (exp
                                    (as
                                        x
                                        (term
                                            (exp
                                                (id a case_insensitive))))))
                            (term
                                (exp
                                    (as
                                        y
                                        (term
                                            (exp
                                                (+
                                                    (term
                                                        (exp
                                                            (id b case_insensitive)))
                                                    (term
                                                        (exp
                                                            (id c case_insensitive)))))))))
                            (term
                                (exp
                                    (as
                                        z
                                        (term
                                            (exp
                                                (call
                                                    foo
                                                    (term
                                                        (exp
                                                            (id d case_insensitive))))))))))
                        (term
                            (exp (name
                                    g)))))))

        """
    )

    //****************************************
    // HAVING
    //****************************************
    @Test
    fun havingMinimal() = assertExpression(
        """
          (select
            (project (list (id a case_insensitive)))
            (from (id data case_insensitive))
            (having (= (id a case_insensitive) (id b case_insensitive)))
          )
        """,
        "SELECT a FROM data HAVING a = b",
        """
        (term
            (exp
                (select
                    (project
                        (list
                            (term
                                (exp
                                    (id a case_insensitive)))))
                    (from
                        (term
                            (exp
                                (id data case_insensitive))))
                    (having
                        (term
                            (exp
                                (=
                                    (term
                                        (exp
                                            (id a case_insensitive)))
                                    (term
                                        (exp
                                            (id b case_insensitive))))))))))
        """
    )

    @Test
    fun havingWithWhere() = assertExpression(
        """
          (select
            (project (list (id a case_insensitive)))
            (from (id data case_insensitive))
            (where (= (id a case_insensitive) (id b case_insensitive)))
            (having (= (id c case_insensitive) (id d case_insensitive)))
          )
        """,
        "SELECT a FROM data WHERE a = b HAVING c = d",
        """
        (term
            (exp
                (select
                    (project
                        (list
                            (term
                                (exp
                                    (id a case_insensitive)))))
                    (from
                        (term
                            (exp
                                (id data case_insensitive))))
                    (where
                        (term
                            (exp
                                (=
                                    (term
                                        (exp
                                            (id a case_insensitive)))
                                    (term
                                        (exp
                                            (id b case_insensitive)))))))
                    (having
                        (term
                            (exp
                                (=
                                    (term
                                        (exp
                                            (id c case_insensitive)))
                                    (term
                                        (exp
                                            (id d case_insensitive))))))))))
        """

    )

    @Test
    fun havingWithWhereAndGroupBy() = assertExpression(
        """
          (select
            (project (list (id g case_insensitive)))
            (from (id data case_insensitive))
            (where (= (id a case_insensitive) (id b case_insensitive)))
            (group (by (id c case_insensitive) (id d case_insensitive)) (name g))
            (having (> (id d case_insensitive) (lit 6)))
          )
        """,
        "SELECT g FROM data WHERE a = b GROUP BY c, d GROUP AS g HAVING d > 6",
        """
        (term
            (exp
                (select
                    (project
                        (list
                            (term
                                (exp
                                    (id g case_insensitive)))))
                    (from
                        (term
                            (exp
                                (id data case_insensitive))))
                    (where
                        (term
                            (exp
                                (=
                                    (term
                                        (exp
                                            (id a case_insensitive)))
                                    (term
                                        (exp
                                            (id b case_insensitive)))))))
                    (group
                        (by
                            (term
                                (exp
                                    (id c case_insensitive)))
                            (term
                                (exp
                                    (id d case_insensitive))))
                        (term
                            (exp
                                (name
                                    g))))
                    (having
                        (term
                            (exp
                                (>
                                    (term
                                        (exp
                                            (id d case_insensitive)))
                                    (term
                                        (exp
                                            (lit 6))))))))))
        """
    )

    //****************************************
    // PIVOT
    //****************************************
    @Test
    fun pivotWithOnlyFrom() = assertExpression(
        """
          (pivot
            (member (id n case_insensitive) (id v case_insensitive))
            (from (id data case_insensitive))
          )
        """,
        "PIVOT v AT n FROM data",
        """
        (term
            (exp
                (pivot
                    (member
                        (term
                            (exp
                                (id n case_insensitive)))
                        (term
                            (exp
                                (id v case_insensitive))))
                    (from
                        (term
                            (exp
                                (id data case_insensitive)))))))
        """

    )

    @Test
    fun pivotHavingWithWhereAndGroupBy() = assertExpression(
        """
          (pivot
            (member (|| (lit "prefix:") (id c case_insensitive)) (id g case_insensitive))
            (from (id data case_insensitive))
            (where (= (id a case_insensitive) (id b case_insensitive)))
            (group (by (id c case_insensitive) (id d case_insensitive)) (name g))
            (having (> (id d case_insensitive) (lit 6)))
          )
        """,
        "PIVOT g AT ('prefix:' || c) FROM data WHERE a = b GROUP BY c, d GROUP AS g HAVING d > 6",
        """
        (term
            (exp
                (pivot
                    (member
                        (term
                            (exp
                                (||
                                    (term
                                        (exp
                                            (lit "prefix:")))
                                    (term
                                        (exp
                                            (id c case_insensitive))))))
                        (term
                            (exp
                                (id g case_insensitive))))
                    (from
                        (term
                            (exp
                                (id data case_insensitive))))
                    (where
                        (term
                            (exp
                                (=
                                    (term
                                        (exp
                                            (id a case_insensitive)))
                                    (term
                                        (exp
                                            (id b case_insensitive)))))))
                    (group
                        (by
                            (term
                                (exp
                                    (id c case_insensitive)))
                            (term
                                (exp
                                    (id d case_insensitive))))
                        (term
                            (exp
                                (name
                                    g))))
                    (having
                        (term
                            (exp
                                (>
                                    (term
                                        (exp
                                            (id d case_insensitive)))
                                    (term
                                        (exp
                                            (lit 6))))))))))
        """
    )

    //****************************************
    // DML
    //****************************************

    @Test
    fun fromInsertValuesDml() = assertExpression(
        expectedSexpAstV0String = """
          (dml
            (insert
              (id foo case_insensitive)
              (bag
                (list (lit 1) (lit 2))
                (list (lit 3) (lit 4))
              )
            )
            (from (id x case_insensitive))
          )
        """,
        source = "FROM x INSERT INTO foo VALUES (1, 2), (3, 4)",
        expectedSexpAstV1String = """
          (term (exp (dml
            (insert
              (term (exp (id foo case_insensitive)))
              (term (exp (bag
                (term (exp (list (term (exp (lit 1))) (term (exp (lit 2))))))
                (term (exp (list (term (exp (lit 3))) (term (exp (lit 4))))))
              )))
            )
            (from (term (exp (id x case_insensitive))))
          )))
        """
    )

    @Test
    fun fromInsertValueAtDml() = assertExpression(
        expectedSexpAstV0String = """
          (dml
              (insert_value
                (id foo case_insensitive)
                (lit 1)
                (id bar case_insensitive)
              )
              (from (id x case_insensitive))
          )
        """,
        source = "FROM x INSERT INTO foo VALUE 1 AT bar",
        expectedSexpAstV1String = """
          (term (exp (dml
            (insert_value
              (term (exp (id foo case_insensitive)))
              (term (exp (lit 1)))
              (term (exp (id bar case_insensitive)))
            )
            (from (term (exp (id x case_insensitive))))
          )))
        """)

    @Test
    fun fromInsertValueDml() = assertExpression(
        expectedSexpAstV0String = """
          (dml
              (insert_value
                (id foo case_insensitive)
                (lit 1)
              )
              (from (id x case_insensitive))
          )
        """,
        source = "FROM x INSERT INTO foo VALUE 1",
        expectedSexpAstV1String = """
          (term (exp (dml
            (insert_value
              (term (exp (id foo case_insensitive)))
              (term (exp (lit 1)))
            )
            (from (term (exp (id x case_insensitive))))
          )))
        """)

    @Test
    fun fromInsertQueryDml() = assertExpression(
        expectedSexpAstV0String = """
          (dml
            (insert
              (id foo case_insensitive)
              (select
                (project (list (id y case_insensitive)))
                (from (id bar case_insensitive))
              )
            )
            (from (id x case_insensitive))
          )
        """,
        source = "FROM x INSERT INTO foo SELECT y FROM bar",
        expectedSexpAstV1String = """
          (term (exp (dml
            (insert
              (term (exp (id foo case_insensitive)))
              (term (exp (select
                (project (list (term (exp (id y case_insensitive)))))
                (from (term (exp (id bar case_insensitive))))
              )))
            )
            (from (term (exp (id x case_insensitive))))
          )))
        """
    )

    @Test
    fun insertValueDml() = assertExpression(
        expectedSexpAstV0String = """
          (dml
              (insert_value
                (id foo case_insensitive)
                (lit 1)
              )
          )
        """,
        source = "INSERT INTO foo VALUE 1",
        expectedSexpAstV1String = """
          (term (exp (dml
              (insert_value
                (term (exp (id foo case_insensitive)))
                (term (exp (lit 1)))
              )
          )))
        """
    )

    @Test
    fun insertValuesDml() = assertExpression(
        expectedSexpAstV0String = """
          (dml
              (insert
                (id foo case_insensitive)
                (bag
                  (list (lit 1) (lit 2))
                  (list (lit 3) (lit 4))
                )
              )
          )
        """,
        source = "INSERT INTO foo VALUES (1, 2), (3, 4)",
        expectedSexpAstV1String = """
          (term (exp (dml
              (insert
                (term (exp (id foo case_insensitive)))
                (term (exp (bag
                  (term (exp (list (term (exp (lit 1))) (term (exp (lit 2))))))
                  (term (exp (list (term (exp (lit 3))) (term (exp (lit 4))))))
                )))
              )
          )))
        """
    )

    @Test
    fun insertValueAtDml() = assertExpression(
        expectedSexpAstV0String = """
          (dml
              (insert_value
                (id foo case_insensitive)
                (lit 1)
                (id bar case_insensitive)
              )
          )
        """,
        source = "INSERT INTO foo VALUE 1 AT bar",
        expectedSexpAstV1String = """
          (term (exp (dml
            (insert_value
                (term (exp (id foo case_insensitive)))
                (term (exp (lit 1)))
                (term (exp (id bar case_insensitive)))))))
        """)

    @Test
    fun insertQueryDml() = assertExpression(
        expectedSexpAstV0String = """
          (dml
              (insert
                (id foo case_insensitive)
                (select
                  (project (list (id y case_insensitive)))
                  (from (id bar case_insensitive))
                )
              )
          )
        """,
        source = "INSERT INTO foo SELECT y FROM bar",
        expectedSexpAstV1String = """
          (term (exp (dml
              (insert
                (term (exp (id foo case_insensitive)))
                (term (exp (select
                  (project (list (term (exp (id y case_insensitive)))))
                  (from (term (exp (id bar case_insensitive))))
                )))
              )
          )))
        """
    )

    @Test
    fun fromSetSingleDml() = assertExpression(
        expectedSexpAstV0String = """
          (dml
            (set
              (assignment
                (id k case_insensitive)
                (lit 5)
              )
            )
            (from (id x case_insensitive))
            (where (= (id a case_insensitive) (id b case_insensitive)))
          )
        """,
        source = "FROM x WHERE a = b SET k = 5",
        expectedSexpAstV1String = """
          (term (exp (dml
            (set
              (assignment
                (term (exp (id k case_insensitive)))
                (term (exp (lit 5)))
              )
            )
            (from (term (exp (id x case_insensitive))))
            (where
              (term (exp (=
                (term (exp (id a case_insensitive)))
                (term (exp (id b case_insensitive)))
              )))
            )
          )))
        """
    )

    @Test
    fun fromSetSinglePathFieldDml() = assertExpression(
        expectedSexpAstV0String = """
          (dml
            (set
              (assignment
                (path (id k case_insensitive) (case_insensitive (lit "m")))
                (lit 5)
              )
            )
            (from (id x case_insensitive))
            (where (= (id a case_insensitive) (id b case_insensitive)))
          )
        """,
        source = "FROM x WHERE a = b SET k.m = 5",
        expectedSexpAstV1String = """
          (term (exp (dml
            (set
              (assignment
               (term (exp (path (term (exp (id k case_insensitive))) (path_element (term (exp (lit "m"))) case_insensitive))))
                (term (exp (lit 5)))
              )
            )
            (from (term (exp (id x case_insensitive))))
            (where
              (term (exp (=
                (term (exp (id a case_insensitive)))
                (term (exp (id b case_insensitive)))
              )))
            )
          )))
        """
    )

    @Test
    fun fromSetSinglePathStringIndexDml() = assertExpression(
        expectedSexpAstV0String = """
          (dml
            (set
              (assignment
                (path (id k case_insensitive) (case_sensitive (lit "m")))
                (lit 5)
              )
            )
            (from (id x case_insensitive))
            (where (= (id a case_insensitive) (id b case_insensitive)))
          )
        """,
        source = "FROM x WHERE a = b SET k['m'] = 5",
        expectedSexpAstV1String = """
          (term (exp (dml
            (set
              (assignment
               (term (exp (path (term (exp (id k case_insensitive))) (path_element (term (exp (lit "m"))) case_sensitive))))
                (term (exp (lit 5)))
              )
            )
            (from (term (exp (id x case_insensitive))))
            (where
              (term (exp (=
                (term (exp (id a case_insensitive)))
                (term (exp (id b case_insensitive)))
              )))
            )
          )))
        """
    )



    @Test
    fun fromSetSinglePathOrdinalDml() = assertExpression(
        expectedSexpAstV0String = """
          (dml
            (set
              (assignment
                (path (id k case_insensitive) (lit 3))
                (lit 5)
              )
            )
            (from (id x case_insensitive))
            (where (= (id a case_insensitive) (id b case_insensitive)))
          )
        """,
        source = "FROM x WHERE a = b SET k[3] = 5",
        expectedSexpAstV1String = """
          (term (exp (dml
            (set
              (assignment
               (term (exp (path (term (exp (id k case_insensitive))) (path_element (term (exp (lit 3))) case_sensitive))))
                (term (exp (lit 5)))
              )
            )
            (from (term (exp (id x case_insensitive))))
            (where
              (term (exp (=
                (term (exp (id a case_insensitive)))
                (term (exp (id b case_insensitive)))
              )))
            )
          )))
        """
    )

    @Test
    fun fromSetMultiDml() = assertExpression(
        expectedSexpAstV0String = """
          (dml
            (set
              (assignment
                (id k case_insensitive)
                (lit 5)
              )
              (assignment
                (id m case_insensitive)
                (lit 6)
              )
            )
            (from (id x case_insensitive))
            (where (= (id a case_insensitive) (id b case_insensitive)))
          )
        """,
        source = "FROM x WHERE a = b SET k = 5, m = 6",
        expectedSexpAstV1String = """
          (term (exp (dml
            (set
              (assignment
                (term (exp (id k case_insensitive)))
                (term (exp (lit 5)))
              )
              (assignment
                (term (exp (id m case_insensitive)))
                (term (exp (lit 6)))
              )
            )
            (from (term (exp (id x case_insensitive))))
            (where
              (term (exp (=
                (term (exp (id a case_insensitive)))
                (term (exp (id b case_insensitive)))
              )))
            )
          )))
        """
    )

    @Test
    fun setSingleDml() = assertExpression(
        expectedSexpAstV0String = """
          (dml
              (set
                (assignment
                  (id k case_insensitive)
                  (lit 5)
                )
              )
          )
        """,
        source = "SET k = 5",
        expectedSexpAstV1String = """
          (term (exp (dml
              (set(assignment
                (term (exp (id k case_insensitive)))
                (term (exp (lit 5)))
              ))
          )))
        """
    )

    @Test
    fun setSingleDmlWithQuotedIdentifierAtHead() = assertExpression(
        expectedSexpAstV0String = """
          (dml
              (set
                (assignment
                  (id k case_sensitive)
                  (lit 5)
                )
              )
          )
        """,
        source = "SET \"k\" = 5",
        expectedSexpAstV1String = """
          (term (exp (dml
              (set(assignment
                (term (exp (id k case_sensitive)))
                (term (exp (lit 5)))
              ))
          )))
        """
    )

    @Test
    fun setMultiDml() = assertExpression(
        expectedSexpAstV0String = """
          (dml
              (set
                (assignment
                  (id k case_insensitive)
                  (lit 5)
                )
                (assignment
                  (id m case_insensitive)
                  (lit 6)
                )
              )
          )
        """,
        source = "SET k = 5, m = 6",
        expectedSexpAstV1String = """
          (term (exp (dml
              (set
                (assignment
                  (term (exp (id k case_insensitive)))
                  (term (exp (lit 5)))
                )
                (assignment
                  (term (exp (id m case_insensitive)))
                  (term (exp (lit 6)))
                )
              )
          )))
        """
    )

    @Test
    fun fromRemoveDml() = assertExpression(
        expectedSexpAstV0String = """
          (dml
            (remove
              (id y case_insensitive)
            )
            (from (id x case_insensitive))
            (where (= (id a case_insensitive) (id b case_insensitive)))
          )
        """,
        source = "FROM x WHERE a = b REMOVE y",
        expectedSexpAstV1String = """
          (term (exp (dml
            (remove
              (term (exp (id y case_insensitive)))
            )
            (from (term (exp (id x case_insensitive))))
            (where
              (term (exp (=
                (term (exp (id a case_insensitive)))
                (term (exp (id b case_insensitive)))
              )))
            )
          )))
        """
    )

    @Test
    fun removeDml() = assertExpression(
        expectedSexpAstV0String = """
          (dml
              (remove
                (id y case_insensitive)
              )
          )
        """,
        source = "REMOVE y",
        expectedSexpAstV1String = """
          (term (exp (dml
              (remove
                (term (exp (id y case_insensitive)))
              )
          )))
        """
    )

    @Test
    fun removeDmlPath() = assertExpression(
        expectedSexpAstV0String = """
          (dml
              (remove
                (path
                  (id a case_insensitive)
                  (case_insensitive (lit "b"))
                  (case_sensitive (lit "c"))
                  (lit 2)
                )
              )
          )
        """,
        source = "REMOVE a.b['c'][2]",
        expectedSexpAstV1String = """
          (term (exp (dml
              (remove
                (term (exp (path
                  (term (exp (id a case_insensitive)))
                  (path_element (term (exp (lit "b"))) case_insensitive)
                  (path_element (term (exp (lit "c"))) case_sensitive)
                  (path_element (term (exp (lit 2))) case_sensitive)
                )))
              )
          )))
        """
    )

    @Test
    fun updateDml() = assertExpression(
        expectedSexpAstV0String = """
          (dml
            (set
              (assignment
                (id k case_insensitive)
                (lit 5)
              )
              (assignment
                (id m case_insensitive)
                (lit 6)
              )
            )
            (from (as y (id x case_insensitive)))
            (where (= (id a case_insensitive) (id b case_insensitive)))
          )
        """,
        source = "UPDATE x AS y SET k = 5, m = 6 WHERE a = b",
        expectedSexpAstV1String = """
          (term (exp (dml
            (set
              (assignment
                (term (exp (id k case_insensitive)))
                (term (exp (lit 5)))
              )
              (assignment
                (term (exp (id m case_insensitive)))
                (term (exp (lit 6)))
              )
            )
            (from (term (exp (as y (term (exp (id x case_insensitive)))))))
            (where
              (term (exp (=
                (term (exp (id a case_insensitive)))
                (term (exp (id b case_insensitive)))
              )))
            )
          )))
        """
    )

    @Test
    fun updateWithInsert() = assertExpression(
        expectedSexpAstV0String = """
          (dml
            (insert
              (id k case_insensitive)
              (bag
                (lit 1)))
            (from
              (as
                y
                (id x case_insensitive)))
            (where
              (=
                (id a case_insensitive)
                (id b case_insensitive))))
        """,
        source = "UPDATE x AS y INSERT INTO k << 1 >> WHERE a = b",
        expectedSexpAstV1String = """
          (term (exp (dml
            (insert
              (term (exp (id k case_insensitive)))
              (term (exp (bag (term (exp (lit 1))))))
            )
            (from (term (exp (as y (term (exp (id x case_insensitive)))))))
            (where
              (term (exp (=
                (term (exp (id a case_insensitive)))
                (term (exp (id b case_insensitive)))
              )))
            )
          )))
        """
    )

    @Test
    fun updateWithInsertValueAt() = assertExpression(
        expectedSexpAstV0String = """
          (dml
            (insert_value
              (id k case_insensitive)
              (lit 1)
              (lit "j"))
            (from
              (as
                y
                (id x case_insensitive)))
            (where
              (=
                (id a case_insensitive)
                (id b case_insensitive))))
        """,
        source = "UPDATE x AS y INSERT INTO k VALUE 1 AT 'j' WHERE a = b",
        expectedSexpAstV1String = """
          (term (exp (dml
            (insert_value
              (term (exp (id k case_insensitive)))
              (term (exp (lit 1)))
              (term (exp (lit "j"))))
            (from (term (exp (as y (term (exp (id x case_insensitive)))))))
            (where
              (term (exp (=
                (term (exp (id a case_insensitive)))
                (term (exp (id b case_insensitive)))
              )))
            )
          )))
        """
    )

    @Test
    fun updateWithRemove() = assertExpression(
        expectedSexpAstV0String = """
          (dml
            (remove
              (path
                (id y case_insensitive)
                (case_insensitive (lit "a"))))
            (from (as y (id x case_insensitive)))
            (where (= (id a case_insensitive) (id b case_insensitive)))
          )
        """,
        source = "UPDATE x AS y REMOVE y.a WHERE a = b",
        expectedSexpAstV1String = """
          (term (exp (dml
            (remove
              (term (exp
                  (path (term (exp (id y case_insensitive))) (path_element (term (exp (lit "a"))) case_insensitive)))))
            (from (term (exp (as y (term (exp (id x case_insensitive)))))))
            (where
              (term (exp (=
                (term (exp (id a case_insensitive)))
                (term (exp (id b case_insensitive)))
              )))
            )
          )))
        """
    )

    @Test
    fun updateDmlWithImplicitAs() = assertExpression(
        expectedSexpAstV0String = """
          (dml
            (set
              (assignment
                (path (id z case_insensitive) (case_insensitive (lit "kingdom")))
                (lit "Fungi")))
            (from
              (as z (id zoo case_insensitive))))
        """,
        source = "UPDATE zoo z SET z.kingdom = 'Fungi'",
        expectedSexpAstV1String = """
        (term (exp (dml
          (set
            (assignment
              (term (exp (path
                (term (exp (id z case_insensitive)))
                (path_element (term (exp (lit "kingdom"))) case_insensitive))))
                (term (exp (lit "Fungi")))))
            (from
                (term (exp (as z (term (exp (id zoo case_insensitive))))))))))
        """
    )

    @Test
    fun updateDmlWithAt() = assertExpression(
        expectedSexpAstV0String = """
          (dml
            (set
              (assignment
                (path (id z case_insensitive) (case_insensitive (lit "kingdom")))
                (lit "Fungi")))
            (from
              (at z_ord (id zoo case_insensitive))))
        """,
        source = "UPDATE zoo AT z_ord SET z.kingdom = 'Fungi'",
        expectedSexpAstV1String = """
        (term (exp (dml
          (set
            (assignment
              (term (exp (path
                (term (exp (id z case_insensitive)))
                (path_element (term (exp (lit "kingdom"))) case_insensitive))))
                (term (exp (lit "Fungi")))))
            (from
                (term (exp (at z_ord (term (exp (id zoo case_insensitive))))))))))
        """
    )

    @Test
    fun updateDmlWithBy() = assertExpression(
        expectedSexpAstV0String = """
          (dml
            (set
              (assignment
                (path (id z case_insensitive) (case_insensitive (lit "kingdom")))
                (lit "Fungi")))
            (from
              (by z_id (id zoo case_insensitive))))
        """,
        source = "UPDATE zoo BY z_id SET z.kingdom = 'Fungi'",
        expectedSexpAstV1String = """
        (term (exp (dml
          (set
            (assignment
              (term (exp (path
                (term (exp (id z case_insensitive)))
                (path_element (term (exp (lit "kingdom"))) case_insensitive))))
                (term (exp (lit "Fungi")))))
            (from
                (term (exp (by z_id (term (exp (id zoo case_insensitive))))))))))
        """
    )


    @Test
    fun updateDmlWithAtAndBy() = assertExpression(
        expectedSexpAstV0String = """
          (dml
            (set
              (assignment
                (path (id z case_insensitive) (case_insensitive (lit "kingdom")))
                (lit "Fungi")))
            (from
              (by z_id (at z_ord (id zoo case_insensitive)))))
        """,
        source = "UPDATE zoo AT z_ord BY z_id SET z.kingdom = 'Fungi'",
        expectedSexpAstV1String = """
        (term (exp (dml
          (set
            (assignment
              (term (exp (path
                (term (exp (id z case_insensitive)))
                (path_element (term (exp (lit "kingdom"))) case_insensitive))))
                (term (exp (lit "Fungi")))))
            (from
                (term (exp (by z_id (term (exp (at z_ord (term (exp (id zoo case_insensitive)))))))))))))
        """
    )


    @Test
    fun updateWhereDml() = assertExpression(
        expectedSexpAstV0String = """
          (dml
            (set
              (assignment
                (id k case_insensitive)
                (lit 5)
              )
              (assignment
                (id m case_insensitive)
                (lit 6)
              )
            )
            (from (id x case_insensitive))
            (where (= (id a case_insensitive) (id b case_insensitive)))
          )
        """,
        source = "UPDATE x SET k = 5, m = 6 WHERE a = b",
        expectedSexpAstV1String = """
          (term (exp (dml
            (set
              (assignment
                (term (exp (id k case_insensitive)))
                (term (exp (lit 5)))
              )
              (assignment
                (term (exp (id m case_insensitive)))
                (term (exp (lit 6)))
              )
            )
            (from (term (exp (id x case_insensitive))))
            (where
              (term (exp (=
                (term (exp (id a case_insensitive)))
                (term (exp (id b case_insensitive)))
              )))
            )
          )))
        """
    )

    @Test
    fun deleteDml() = assertExpression(
        expectedSexpAstV0String = """
          (dml
              (delete)
              (from (id y case_insensitive))
          )
        """,
        source = "DELETE FROM y",
        expectedSexpAstV1String = """
          (term (exp (dml
              (delete)
              (from (term (exp (id y case_insensitive))))
          )))
        """
    )

    @Test
    fun deleteDmlAliased() = assertExpression(
        expectedSexpAstV0String = """
          (dml
              (delete)
              (from (as y (id x case_insensitive)))
          )
        """,
        source = "DELETE FROM x AS y",
        expectedSexpAstV1String = """
          (term (exp (dml
              (delete)
              (from (term (exp (as y (term (exp (id x case_insensitive)))))))
          )))
        """
    )

    @Test
    fun canParseADeleteQueryWithAPositionClause() = assertExpression(
            expectedSexpAstV0String = """
                (dml
                    (delete)
                    (from
                        (at
                            y
                            (id x case_insensitive))))""".trimIndent(),
            source = "DELETE FROM x AT y",
            expectedSexpAstV1String = """
                (term
                    (exp
                        (dml
                            (delete)
                            (from
                                (term
                                    (exp
                                        (at
                                            y
                                            (term
                                                (exp
                                                    (id x case_insensitive))))))))))"""
    )

    @Test
    fun canParseADeleteQueryWithAliasAndPositionClause() = assertExpression(
            expectedSexpAstV0String = """
                (dml
                    (delete)
                    (from
                        (at
                            z
                            (as
                                y
                                (id x case_insensitive)))))""",
            source = "DELETE FROM x AS y AT z",
            expectedSexpAstV1String = """
                (term
                    (exp
                        (dml
                            (delete)
                            (from
                                (term
                                    (exp
                                        (at
                                            z
                                            (term
                                                (exp
                                                    (as
                                                        y
                                                        (term
                                                            (exp
                                                                (id x case_insensitive)))))))))))))"""
    )

    @Test
    fun canParseADeleteQueryWithPath() = assertExpression(
            expectedSexpAstV0String = """
                (dml
                    (delete)
                    (from
                        (path
                            (id x case_insensitive)
                            (case_insensitive
                                (lit "n")))))""".trimIndent(),
            source = "DELETE FROM x.n",
            expectedSexpAstV1String = """
                (term
                    (exp
                        (dml
                            (delete)
                            (from
                                (term
                                    (exp
                                        (path
                                            (term
                                                (exp
                                                    (id x case_insensitive)))
                                            (path_element
                                                (term
                                                    (exp
                                                        (lit "n")))
                                                case_insensitive))))))))"""
    )

    @Test
    fun canParseADeleteQueryWithNestedPath() = assertExpression(
            expectedSexpAstV0String = """
                (dml
                    (delete)
                    (from
                        (path
                            (id x case_insensitive)
                            (case_insensitive
                                (lit "n"))
                            (case_insensitive
                                (lit "m")))))""".trimIndent(),
            source = "DELETE FROM x.n.m",
            expectedSexpAstV1String = """
                (term
                    (exp
                        (dml
                            (delete)
                            (from
                                (term
                                    (exp
                                        (path
                                            (term
                                                (exp
                                                    (id x case_insensitive)))
                                            (path_element
                                                (term
                                                    (exp
                                                        (lit "n")))
                                                case_insensitive)
                                            (path_element
                                                (term
                                                    (exp
                                                        (lit "m")))
                                                case_insensitive))))))))"""
    )

    @Test
    fun canParseADeleteQueryWithNestedPathAndAlias() = assertExpression(
            expectedSexpAstV0String = """
                (dml
                    (delete)
                    (from
                        (as
                            y
                            (path
                                (id x case_insensitive)
                                (case_insensitive
                                    (lit "n"))
                                (case_insensitive
                                    (lit "m"))))))""".trimIndent(),
            source = "DELETE FROM x.n.m AS y",
            expectedSexpAstV1String = """
                (term
                    (exp
                        (dml
                            (delete)
                            (from
                                (term
                                    (exp
                                        (as
                                            y
                                            (term
                                                (exp
                                                    (path
                                                        (term
                                                            (exp
                                                                (id x case_insensitive)))
                                                        (path_element
                                                            (term
                                                                (exp
                                                                    (lit "n")))
                                                            case_insensitive)
                                                        (path_element
                                                            (term
                                                                (exp
                                                                    (lit "m")))
                                                            case_insensitive)))))))))))"""
    )

    @Test
    fun canParseADeleteQueryWithNestedPathAndAliasAndPosition() = assertExpression(
            expectedSexpAstV0String = """
                (dml
                    (delete)
                    (from
                        (at
                            z
                            (as
                                y
                                (path
                                    (id x case_insensitive)
                                    (case_insensitive
                                        (lit "n"))
                                    (case_insensitive
                                        (lit "m")))))))""".trimIndent(),
            source = "DELETE FROM x.n.m AS y AT z",
            expectedSexpAstV1String = """
                (term
                    (exp
                        (dml
                            (delete)
                            (from
                                (term
                                    (exp
                                        (at
                                            z
                                            (term
                                                (exp
                                                    (as
                                                        y
                                                        (term
                                                            (exp
                                                                (path
                                                                    (term
                                                                        (exp
                                                                            (id x case_insensitive)))
                                                                    (path_element
                                                                        (term
                                                                            (exp
                                                                                (lit "n")))
                                                                        case_insensitive)
                                                                    (path_element
                                                                        (term
                                                                            (exp
                                                                                (lit "m")))
                                                                        case_insensitive))))))))))))))"""
    )

    //****************************************
    // semicolon at end of sqlUnderTest
    //****************************************
    @Test
    fun semicolonAtEndOfQuery() = assertExpression(
        "(select (project (list (project_all))) (from (bag (lit 1))))",
        "SELECT * FROM <<1>>;",
        """
        (term
            (exp
                (select
                    (project
                        (list
                            (term
                                (exp
                                    (star)))))
                    (from
                        (term
                            (exp
                                (bag
                                    (term
                                        (exp
                                            (lit 1))))))))))
        """)

    @Test
    fun rootSelectNodeHasSourceLocation() {
        val ast = parse("select 1 from dogs")

        assertEquals(SourceLocationMeta(1L, 1L), ast.metas.sourceLocation)
    }

    @Test
    fun semicolonAtEndOfQueryHasNoEffect() {
        val query = "SELECT * FROM <<1>>"
        val withSemicolon = parse("$query;")
        val withoutSemicolon = parse(query)

        assertEquals(withoutSemicolon, withSemicolon)
    }

    @Test
    fun semicolonAtEndOfLiteralHasNoEffect() {
        val withSemicolon = parse("1;")
        val withoutSemicolon = parse("1")

        assertEquals(withoutSemicolon, withSemicolon)
    }

    @Test
    fun semicolonAtEndOfExpressionHasNoEffect() {
        val withSemicolon = parse("(1+1);")
        val withoutSemicolon = parse("(1+1)")

        assertEquals(withoutSemicolon, withSemicolon)
    }
}
