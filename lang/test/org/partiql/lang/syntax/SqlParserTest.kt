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

import org.junit.Test
import org.partiql.lang.ast.ExprNode
import org.partiql.lang.ast.SourceLocationMeta
import org.partiql.lang.ast.sourceLocation

/**
 * Originally just meant to test the parser, this class now tests several different things because
 * the same test cases can be used for all three:
 *
 * - Parsing of query to [ExprNode]s
 * - Conversion of [ExprNode]s to legacy and new s-exp ASTs.
 * - Conversion of both AST forms to [ExprNode]s.
 */
class SqlParserTest : SqlParserTestBase() {


    //****************************************
    // literals
    //****************************************
    @Test
    fun litInt() = assertExpression(
        "5",
        "(lit 5)",
        skipPig = false
    )

    @Test
    fun litNull() = assertExpression(
        "null",
        "(lit null)",
        skipPig = false
    )

    @Test
    fun litMissing() = assertExpression(
        "missing",
        "(missing)",
        skipPig = false
    )

    @Test
    fun listLiteral() = assertExpression(
        "[a, 5]",
        "(list (id a case_insensitive) (lit 5))",
        "(list (id a (case_insensitive) (unqualified)) (lit 5))",
        skipPig = false
    )

    @Test
    fun listLiteralWithBinary() = assertExpression(
        "[a, 5, (b + 6)]",
        "(list (id a case_insensitive) (lit 5) (+ (id b case_insensitive) (lit 6)))",
        "(list (id a (case_insensitive) (unqualified)) (lit 5) (plus (id b (case_insensitive) (unqualified)) (lit 6)))",
        skipPig = false
    )

    @Test
    fun listFunction() = assertExpression(
        "list(a, 5)",
        "(list (id a case_insensitive) (lit 5))",
        "(list (id a (case_insensitive) (unqualified)) (lit 5))",
        skipPig = false
    )

    @Test
    fun listFunctionlWithBinary() = assertExpression(
        "LIST(a, 5, (b + 6))",
        "(list (id a case_insensitive) (lit 5) (+ (id b case_insensitive) (lit 6)))",
        "(list (id a (case_insensitive) (unqualified)) (lit 5) (plus (id b (case_insensitive) (unqualified)) (lit 6)))",
        skipPig = false
    )

    @Test
    fun sexpFunction() = assertExpression(
        "sexp(a, 5)",
        "(sexp (id a case_insensitive) (lit 5))",
        "(sexp (id a (case_insensitive) (unqualified)) (lit 5))",
        skipPig = false
    )

    @Test
    fun sexpFunctionWithBinary() = assertExpression(
        "SEXP(a, 5, (b + 6))",
        "(sexp (id a case_insensitive) (lit 5) (+ (id b case_insensitive) (lit 6)))",
        "(sexp (id a (case_insensitive) (unqualified)) (lit 5) (plus (id b (case_insensitive) (unqualified)) (lit 6)))",
        skipPig = false
    )

    @Test
    fun structLiteral() = assertExpression(
        "{'x':a, 'y':5 }",
        """(struct
             (lit "x") (id a case_insensitive)
             (lit "y") (lit 5)
           )
        """,
        """(struct
             (expr_pair (lit "x") (id a (case_insensitive) (unqualified)))
             (expr_pair (lit "y") (lit 5))
           )
        """,
        skipPig = false
    )

    @Test
    fun structLiteralWithBinary() = assertExpression(
        "{'x':a, 'y':5, 'z':(b + 6)}",
        """(struct
             (lit "x") (id a case_insensitive)
             (lit "y") (lit 5)
             (lit "z") (+ (id b case_insensitive) (lit 6))
           )
        """,
        """(struct
             (expr_pair (lit "x") (id a (case_insensitive) (unqualified)))
             (expr_pair (lit "y") (lit 5))
             (expr_pair (lit "z") (plus (id b (case_insensitive) (unqualified)) (lit 6)))
           )
        """,
        skipPig = false
    )

    @Test
    fun nestedEmptyListLiteral() = assertExpression(
        "[[]]",
        "(list (list))",
        skipPig = false
    )

    @Test
    fun nestedEmptyBagLiteral() = assertExpression(
        "<<<<>>>>",
        "(bag (bag))",
        skipPig = false
    )

    @Test
    fun nestedEmptyStructLiteral() = assertExpression(
        "{'a':{}}",
        """(struct (lit "a") (struct))""",
        """(struct (expr_pair (lit "a") (struct)))""",
        skipPig = false
    )

    //****************************************
    // container constructors
    //****************************************
    @Test
    fun rowValueConstructorWithSimpleExpressions() = assertExpression(
        "(1, 2, 3, 4)",
        """(list (lit 1) (lit 2) (lit 3) (lit 4))""",
        skipPig = false
    )

    @Test
    fun rowValueConstructorWithRowValueConstructors() = assertExpression(
        "((1, 2), (3, 4))",
        """(list (list (lit 1) (lit 2)) (list (lit 3) (lit 4)))""",
        skipPig = false
    )

    @Test
    fun tableValueConstructorWithRowValueConstructors() = assertExpression(
        "VALUES (1, 2), (3, 4)",
        """(bag (list (lit 1) (lit 2)) (list (lit 3) (lit 4)))""",
        skipPig = false
    )

    @Test
    fun tableValueConstructorWithSingletonRowValueConstructors() = assertExpression(
        "VALUES (1), (2), (3)",
        """(bag (list (lit 1)) (list (lit 2)) (list (lit 3)))""",
        skipPig = false
    )

    //****************************************
    // identifiers
    //****************************************
    @Test
    fun id_case_insensitive() = assertExpression(
        "kumo",
        "(id kumo case_insensitive)",
        "(id kumo (case_insensitive) (unqualified))",
        skipPig = false
    )

    @Test
    fun id_case_sensitive() = assertExpression(
        "\"kumo\"",
        "(id kumo case_sensitive)",
        "(id kumo (case_sensitive) (unqualified))",
        skipPig = false
    )

    //****************************************
    // call
    //****************************************
    @Test
    fun callEmpty() = assertExpression(
        "foobar()",
        "(call foobar)",
        skipPig = false
    )

    @Test
    fun callOneArgument() = assertExpression(
        "foobar(1)",
        "(call foobar (lit 1))",
        skipPig = false
    )

    @Test
    fun callTwoArgument() = assertExpression(
        "foobar(1, 2)",
        "(call foobar (lit 1) (lit 2))",
        skipPig = false
    )

    @Test
    fun callSubstringSql92Syntax() = assertExpression(
        "substring('test' from 100)",
        "(call substring (lit \"test\") (lit 100))",
        skipPig = false
    )

    @Test
    fun callSubstringSql92SyntaxWithLength() = assertExpression(
        "substring('test' from 100 for 50)",
        "(call substring (lit \"test\") (lit 100) (lit 50))",
        skipPig = false
    )

    @Test
    fun callSubstringNormalSyntax() = assertExpression(
        "substring('test', 100)",
        "(call substring (lit \"test\") (lit 100))",
        skipPig = false
    )

    @Test
    fun callSubstringNormalSyntaxWithLength() = assertExpression(
        "substring('test', 100, 50)",
        "(call substring (lit \"test\") (lit 100) (lit 50))",
        skipPig = false
    )

    @Test
    fun callTrimSingleArgument() = assertExpression(
        "trim('test')",
        "(call trim (lit \"test\"))",
        skipPig = false)



    @Test
    fun callTrimTwoArgumentsDefaultSpecification() = assertExpression(
        "trim(' ' from 'test')",
        "(call trim (lit \" \") (lit \"test\"))",
        skipPig = false)

    @Test
    fun callTrimTwoArgumentsUsingBoth() = assertExpression(
        "trim(both from 'test')",
        "(call trim (lit \"both\") (lit \"test\"))",
        skipPig = false)

    @Test
    fun callTrimTwoArgumentsUsingLeading() = assertExpression(
        "trim(leading from 'test')",
        "(call trim (lit \"leading\") (lit \"test\"))",
        skipPig = false)

    @Test
    fun callTrimTwoArgumentsUsingTrailing() = assertExpression(
        "trim(trailing from 'test')",
        "(call trim (lit \"trailing\") (lit \"test\"))",
        skipPig = false)

    //****************************************
    // Unary operators
    //****************************************

    @Test
    fun unaryMinusCall() = assertExpression(
        "-baz()",
        "(- (call baz))",
        "(minus (call baz))",
        skipPig = false
    )

    @Test
    fun unaryPlusMinusIdent() = assertExpression(
        "+(-baz())",
        "(+ (- (call baz)))",
        "(plus (minus (call baz)))",
        skipPig = false
    )

    @Test
    fun unaryPlusMinusIdentNoSpaces() = assertExpression(
        "+-baz()",
        "(+ (- (call baz)))",
        "(plus (minus (call baz)))",
        skipPig = false
    )

    @Test
    fun unaryIonIntLiteral() = assertExpression(
        "-1",
        "(lit -1)",
        skipPig = false
    )

    @Test
    fun unaryIonFloatLiteral() = assertExpression(
        "+-+-+-`-5e0`",
        "(lit 5e0)",
        skipPig = false
    )

    @Test
    fun unaryIonTimestampLiteral() = assertExpression(
        "+-`2017-01-01`",
        "(+ (- (lit 2017-01-01T)))",
        "(plus (minus (lit 2017-01-01T)))",
        skipPig = false
    )

    @Test
    fun unaryNotLiteral() = assertExpression(
        "not 1",
        "(not (lit 1))",
        skipPig = false
    )

    //****************************************
    // BETWEEN
    //****************************************
    @Test
    fun betweenOperator() = assertExpression(
        "5 BETWEEN 1 AND 10",
        "(between (lit 5) (lit 1) (lit 10))",
        skipPig = false
    )

    @Test
    fun notBetweenOperator() = assertExpression(
        "5 NOT BETWEEN 1 AND 10",
        "(not_between (lit 5) (lit 1) (lit 10))",
        "(not (between (lit 5) (lit 1) (lit 10)))",
        skipPig = false
    )
    //****************************************
    // @ operator
    //****************************************

    @Test
    fun atOperatorOnIdentifier() = assertExpression(
        "@a",
        "(@ (id a case_insensitive))",
        "(id a (case_insensitive) (locals_first))",
        skipPig = false
    )

    @Test
    fun atOperatorOnPath() = assertExpression(
        "@a.b",
        """(path (@ (id a case_insensitive)) (case_insensitive (lit "b")))""",
        """(path (id a (case_insensitive) (locals_first)) (path_expr (lit "b") (case_insensitive)))""",
        skipPig = false
    )

    //****************************************
    // IS operator
    //****************************************
    @Test
    fun nullIsNull() = assertExpression(
        "null IS NULL",
        "(is (lit null) (type 'null'))",
        "(is_type (lit null) (null_type))",
        skipPig = false
    )

    @Test
    fun missingIsMissing() = assertExpression(
        "mIsSiNg IS MISSING",
        "(is (missing) (type missing))",
        "(is_type (missing) (missing_type))",
        skipPig = false
    )

    @Test
    fun callIsVarchar() = assertExpression(
        "f() IS VARCHAR(200)",
        "(is (call f) (type character_varying 200))",
        "(is_type (call f) (character_varying_type 200))",
        skipPig = false
    )

    @Test
    fun nullIsNotNull() = assertExpression(
        "null IS NOT NULL",
        "(is_not (lit null) (type 'null'))",
        "(not (is_type (lit null) (null_type)))",
        skipPig = false
    )

    @Test
    fun missingIsNotMissing() = assertExpression(
        "mIsSiNg IS NOT MISSING",
        "(is_not (missing) (type missing))",
        "(not (is_type (missing) (missing_type)))",
        skipPig = false
    )

    @Test
    fun callIsNotVarchar() = assertExpression(
        "f() IS NOT VARCHAR(200)",
        "(is_not (call f) (type character_varying 200))",
        "(not (is_type (call f) (character_varying_type 200)))",
        skipPig = false
    )

    @Test
    fun callWithMultiple() = assertExpression(
        "foobar(5, 6, a)",
        "(call foobar (lit 5) (lit 6) (id a case_insensitive))",
        "(call foobar (lit 5) (lit 6) (id a (case_insensitive) (unqualified)))",
        skipPig = false
    )

    @Test
    fun aggregateFunctionCall() = assertExpression(
        "COUNT(a)",
        """(call_agg count all (id a case_insensitive))""",
        """(call_agg (all) count (id a (case_insensitive) (unqualified)))""",
        skipPig = false
    )

    @Test
    fun aggregateDistinctFunctionCall() = assertExpression(
        "SUM(DISTINCT a)",
        "(call_agg sum distinct (id a case_insensitive))",
        "(call_agg (distinct) sum (id a (case_insensitive) (unqualified)))",
        skipPig = false
    )

    @Test
    fun countStarFunctionCall() = assertExpression(
        "COUNT(*)",
        "(call_agg_wildcard count)"
    )

    @Test
    fun countFunctionCall() = assertExpression(
        "COUNT(a)",
        "(call_agg count all (id a case_insensitive))",
        "(call_agg (all) count (id a (case_insensitive) (unqualified)))",
        skipPig = false
    )

    @Test
    fun countDistinctFunctionCall() = assertExpression(
        "COUNT(DISTINCT a)",
        "(call_agg count distinct (id a case_insensitive))",
        "(call_agg (distinct) count (id a (case_insensitive) (unqualified)))",
        skipPig = false
    )

    //****************************************
    // path expression
    //****************************************
    @Test
    fun dot_case_1_insensitive_component() = assertExpression(
        "a.b",
        """(path (id a case_insensitive) (case_insensitive (lit "b")))""",
        """(path (id a (case_insensitive) (unqualified)) (path_expr (lit "b") (case_insensitive)))""",
        skipPig = false
    )

    @Test
    fun dot_case_2_insensitive_component() = assertExpression(
        "a.b.c",
        """(path (id a case_insensitive) (case_insensitive (lit "b")) (case_insensitive (lit "c")))""",
        """(path (id a (case_insensitive) (unqualified))
           (path_expr (lit "b") (case_insensitive))
           (path_expr (lit "c") (case_insensitive)))""".trimMargin(),
        skipPig = false
    )
    @Test
    fun dot_case_3_insensitive_components() = assertExpression(
        "a.b.c.d",
        """(path (id a case_insensitive) (case_insensitive (lit "b")) (case_insensitive (lit "c")) (case_insensitive (lit "d")))""",
        """(path (id a (case_insensitive) (unqualified))
           (path_expr (lit "b") (case_insensitive))
           (path_expr (lit "c") (case_insensitive))
           (path_expr (lit "d") (case_insensitive)))""".trimMargin(),
        skipPig = false
    )

    @Test
    fun dot_case_sensitive() = assertExpression(
        """ "a"."b" """,
        """(path (id a case_sensitive) (case_sensitive (lit "b")))""",
        """(path (id a (case_sensitive) (unqualified))
           (path_expr (lit "b") (case_sensitive)))""".trimMargin(),
        skipPig = false
    )

    @Test
    fun dot_case_sensitive_component() = assertExpression(
        "a.\"b\"",
        """(path (id a case_insensitive) (case_sensitive (lit "b")))""",
        """(path (id a (case_insensitive) (unqualified))
           (path_expr (lit "b") (case_sensitive)))""".trimMargin(),
        skipPig = false
    )

    @Test
    fun groupDot() = assertExpression(
        "(a).b",
        """(path (id a case_insensitive) (case_insensitive (lit "b")))""",
        """(path (id a (case_insensitive) (unqualified))
           (path_expr (lit "b") (case_insensitive)))""".trimMargin(),
        skipPig = false
    )

    @Test
    fun pathWith1SquareBracket() = assertExpression(
        """a[5]""",
        """(path (id a case_insensitive) (lit 5))""",
        """(path (id a (case_insensitive) (unqualified))
           (path_expr (lit 5) (case_sensitive)))""".trimMargin(),
        skipPig = false
    )
    @Test
    fun pathWith3SquareBrackets() = assertExpression(
        """a[5]['b'][(a + 3)]""",
        """(path (id a case_insensitive) (lit 5) (case_sensitive (lit "b")) (+ (id a case_insensitive) (lit 3)))""",
        """(path (id a (case_insensitive) (unqualified))
           (path_expr (lit 5) (case_sensitive))
           (path_expr (lit "b") (case_sensitive))
           (path_expr (plus (id a (case_insensitive) (unqualified)) (lit 3)) (case_sensitive)))""",
        skipPig = false
    )

    @Test
    fun dotStar() = assertExpression(
        "a.*",
        """(path (id a case_insensitive) (* unpivot))""",
        """(path (id a (case_insensitive) (unqualified)) (path_unpivot))""".trimMargin(),
        skipPig = false
    )

    @Test
    fun dot2Star() = assertExpression(
        "a.b.*",
        """(path (id a case_insensitive) (case_insensitive (lit "b")) (* unpivot))""",
        """(path (id a (case_insensitive) (unqualified))
           (path_expr (lit "b") (case_insensitive))
           (path_unpivot))""".trimMargin(),
        skipPig = false
    )

    @Test
    fun dotWildcard() = assertExpression(
        "a[*]",
        """(path (id a case_insensitive) (*))""",
        """(path (id a (case_insensitive) (unqualified)) (path_wildcard))""",
        skipPig = false
    )

    @Test
    fun dot2Wildcard() = assertExpression(
        "a.b[*]",
        """(path (id a case_insensitive) (case_insensitive (lit "b")) (*))""",
        """(path (id a (case_insensitive) (unqualified))
           (path_expr (lit "b") (case_insensitive))
           (path_wildcard))""".trimMargin(),
        skipPig = false
    )

    @Test
    fun pathWithCallAndDotStar() = assertExpression(
        "foo(x, y).a.*.b",
        """(path (call foo (id x case_insensitive) (id y case_insensitive)) (case_insensitive (lit "a")) (* unpivot) (case_insensitive (lit "b")))""",
        """(path (call foo (id x (case_insensitive) (unqualified)) (id y (case_insensitive) (unqualified)))
           (path_expr (lit "a") (case_insensitive))
           (path_unpivot)
           (path_expr (lit "b") (case_insensitive)))""".trimMargin(),
        skipPig = false
    )

    @Test
    fun dotAndBracketStar() = assertExpression(
        "x.a[*].b",
        """(path (id x case_insensitive) (case_insensitive (lit "a")) (*) (case_insensitive (lit "b")))""",
        """(path (id x (case_insensitive) (unqualified))
           (path_expr (lit "a") (case_insensitive))
           (path_wildcard)
           (path_expr (lit "b") (case_insensitive)))""".trimMargin(),
        skipPig = false
    )

    //****************************************
    // cast
    //****************************************
    @Test
    fun castNoArgs() = assertExpression(
        "CAST(5 AS VARCHAR)",
        """(cast
             (lit 5)
             (type character_varying)
           )
        """,
        """(cast
             (lit 5)
             (character_varying_type null)
           )
        """,
        skipPig = false
    )

    @Test
    fun castASVarCharWithLength() = assertExpression(
        "CAST(5 AS VARCHAR(5))",
        "(cast (lit 5) (type character_varying 5))",
        "(cast (lit 5) (character_varying_type 5))",
        skipPig = false
    )

    @Test
    fun castAsDecimal() = assertExpression(
        "CAST(a AS DECIMAL)",
        "(cast (id a case_insensitive) (type decimal) )",
        "(cast (id a (case_insensitive) (unqualified)) (decimal_type null null))",
        skipPig = false
    )

    @Test
    fun castAsDecimalScaleOnly() = assertExpression(
        "CAST(a AS DECIMAL(1))",
        "(cast (id a case_insensitive) (type decimal 1))",
        "(cast (id a (case_insensitive) (unqualified)) (decimal_type 1 null))",
        skipPig = false
    )

    @Test
    fun castAsDecimalScaleAndPrecision() = assertExpression(
        "CAST(a AS DECIMAL(1, 2))",
        "(cast (id a case_insensitive) (type decimal 1 2))",
        "(cast (id a (case_insensitive) (unqualified)) (decimal_type 1 2))",
        skipPig = false
    )

    @Test
    fun castAsNumeric() = assertExpression(
        "CAST(a AS NUMERIC)",
        "(cast (id a case_insensitive) (type numeric))",
        """(cast (id a (case_insensitive) (unqualified)) (numeric_type null null))""",
        skipPig = false
    )

    @Test
    fun castAsNumericScaleOnly() = assertExpression(
        "CAST(a AS NUMERIC(1))",
        "(cast (id a case_insensitive) (type numeric 1))",
        "(cast (id a (case_insensitive) (unqualified)) (numeric_type 1 null))",
        skipPig = false
    )

    @Test
    fun castAsNumericScaleAndPrecision() = assertExpression(
        "CAST(a AS NUMERIC(1, 2))",
        "(cast (id a case_insensitive) (type numeric 1 2))",
        "(cast (id a (case_insensitive) (unqualified)) (numeric_type 1 2))",
        skipPig = false
    )

    //****************************************
    // searched CASE
    //****************************************
    @Test
    fun searchedCaseSingleNoElse() = assertExpression(
        "CASE WHEN name = 'zoe' THEN 1 END",
        """(searched_case
             (when
               (= (id name case_insensitive) (lit "zoe"))
               (lit 1)
             )
           )
        """,
        """(searched_case
          (expr_pair_list
            (expr_pair (eq (id name (case_insensitive) (unqualified)) (lit "zoe")) (lit 1)))
          null
        )
        """,
        skipPig = false
    )

    @Test
    fun searchedCaseSingleWithElse() = assertExpression(
        "CASE WHEN name = 'zoe' THEN 1 ELSE 0 END",
        """(searched_case
             (when
               (= (id name case_insensitive) (lit "zoe"))
               (lit 1)
             )
             (else (lit 0))
           )
        """,
        """(searched_case
          (expr_pair_list
            (expr_pair (eq (id name (case_insensitive) (unqualified)) (lit "zoe")) (lit 1)))
          (lit 0)
        )
        """,
        skipPig = false
    )

    @Test
    fun searchedCaseMultiWithElse() = assertExpression(
        "CASE WHEN name = 'zoe' THEN 1 WHEN name > 'kumo' THEN 2 ELSE 0 END",
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
        """(searched_case
          (expr_pair_list
            (expr_pair (eq (id name (case_insensitive) (unqualified)) (lit "zoe")) (lit 1))
            (expr_pair (gt (id name (case_insensitive) (unqualified)) (lit "kumo")) (lit 2)))
          (lit 0)
        )
        """,
        skipPig = false
    )

    //****************************************
    // simple CASE
    //****************************************
    @Test
    fun simpleCaseSingleNoElse() = assertExpression(
        "CASE name WHEN 'zoe' THEN 1 END",
        """(simple_case
             (id name case_insensitive)
             (when
               (lit "zoe")
               (lit 1)
             )
           )
        """,
        """(simple_case
          (id name (case_insensitive) (unqualified))
          (expr_pair_list
            (expr_pair (lit "zoe") (lit 1)))
          null
        )
        """,
        skipPig = false
    )

    @Test
    fun simpleCaseSingleWithElse() = assertExpression(
        "CASE name WHEN 'zoe' THEN 1 ELSE 0 END",
        """(simple_case
             (id name case_insensitive)
             (when
               (lit "zoe")
               (lit 1)
             )
             (else (lit 0))
           )
        """,
        """(simple_case
             (id name (case_insensitive) (unqualified))
             (expr_pair_list
                (expr_pair (lit "zoe") (lit 1)))
             (lit 0)
           )
        """,
        skipPig = false

    )

    @Test
    fun simpleCaseMultiWithElse() = assertExpression(
        "CASE name WHEN 'zoe' THEN 1 WHEN 'kumo' THEN 2 WHEN 'mary' THEN 3 ELSE 0 END",
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
        """(simple_case
          (id name (case_insensitive) (unqualified))
          (expr_pair_list
            (expr_pair (lit "zoe") (lit 1))
            (expr_pair (lit "kumo") (lit 2))
            (expr_pair (lit "mary") (lit 3)))
          (lit 0)
        )
        """,
        skipPig = false
    )

    //****************************************
    // IN operator
    //****************************************
    @Test
    fun inOperatorWithImplicitValues() = assertExpression(
        "a IN (1, 2, 3, 4)",
        """(in
             (id a case_insensitive)
             (list (lit 1) (lit 2) (lit 3) (lit 4))
           )
        """,
        """(in_collection
             (id a (case_insensitive) (unqualified))
             (list (lit 1) (lit 2) (lit 3) (lit 4))
           )
        """,
        skipPig = false
    )

    @Test
    fun notInOperatorWithImplicitValues() = assertExpression(
        "a NOT IN (1, 2, 3, 4)",
        """(not_in
             (id a case_insensitive)
             (list (lit 1) (lit 2) (lit 3) (lit 4))
           )
        """,
        """(not
          (in_collection
             (id a (case_insensitive) (unqualified))
             (list (lit 1) (lit 2) (lit 3) (lit 4))))
        """,
        skipPig = false
    )

    @Test
    fun inOperatorWithImplicitValuesRowConstructor() = assertExpression(
        "(a, b) IN ((1, 2), (3, 4))",
        """(in
             (list (id a case_insensitive) (id b case_insensitive))
             (list (list (lit 1) (lit 2)) (list (lit 3) (lit 4)))
           )
        """,
        """(in_collection
             (list (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)))
             (list (list (lit 1) (lit 2)) (list (lit 3) (lit 4)))
           )
        """,
        skipPig = false
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
        "a LIKE '_AAA%'",
        """(like (id a case_insensitive) (lit "_AAA%"))""",
        """(like (id a (case_insensitive) (unqualified)) (lit "_AAA%") null)""",
        skipPig = false
    )

    @Test
    fun likeColNameLikeColName() = assertExpression(
        "a LIKE b",
        "(like (id a case_insensitive) (id b case_insensitive))",
        "(like (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)) null)",
        skipPig = false
    )

    @Test
    fun pathLikePath() = assertExpression(
        "a.name LIKE b.pattern",
        """
        (like
            (path (id a case_insensitive) (case_insensitive (lit "name")))
            (path (id b case_insensitive) (case_insensitive (lit "pattern"))))
        """,
        """
        (like
            (path (id a (case_insensitive) (unqualified)) (path_expr (lit "name") (case_insensitive)))
            (path (id b (case_insensitive) (unqualified)) (path_expr (lit "pattern") (case_insensitive)))
            null)
        """,
        skipPig = false
    )

    @Test
    fun likeColNameLikeColNamePath() = assertExpression(
        "a.name LIKE b.pattern",
        """
        (like
            (path (id a case_insensitive) (case_insensitive (lit "name")))
            (path (id b case_insensitive) (case_insensitive (lit "pattern"))))
        """,
        """
        (like
            (path (id a (case_insensitive) (unqualified)) (path_expr (lit "name") (case_insensitive)))
            (path (id b (case_insensitive) (unqualified)) (path_expr (lit "pattern") (case_insensitive)))
            null)
        """,
        skipPig = false
    )

    @Test
    fun likeColNameLikeStringEscape() = assertExpression(
        "a LIKE '_AAA%' ESCAPE '['",
        """
        (like
            (id a case_insensitive)
            (lit "_AAA%")
            (lit "["))
        """,
        """
        (like
            (id a (case_insensitive) (unqualified))
            (lit "_AAA%")
            (lit "["))
        """,
        skipPig = false
    )

    @Test
    fun notLikeColNameLikeString() = assertExpression(
        "a NOT LIKE '_AAA%'",
        """
        (not_like
            (id a case_insensitive)
            (lit "_AAA%"))
        """,
        """
        (not
          (like
            (id a (case_insensitive) (unqualified))
            (lit "_AAA%")
            null))
        """,
        skipPig = false
    )

    @Test
    fun likeColNameLikeColNameEscape() = assertExpression(
        "a LIKE b ESCAPE '\\'", //  escape \ inside a Kotlin/Java String
        """
        (like
            (id a case_insensitive)
            (id b case_insensitive)
            (lit "\\"))
        """, // escape \ inside a Kotlin/Java String
        """
        (like
            (id a (case_insensitive) (unqualified))
            (id b (case_insensitive) (unqualified))
            (lit "\\"))
        """,
        skipPig = false
    )

    @Test
    fun likeColNameLikeColNameEscapeNonLit() = assertExpression(
        "a LIKE b ESCAPE c",
        "(like (id a case_insensitive) (id b case_insensitive) (id c case_insensitive))",
        "(like (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified)))",
        skipPig = false
    )

    @Test
    fun likeColNameLikeColNameEscapePath() = assertExpression(
        "a LIKE b ESCAPE x.c",
        """(like (id a case_insensitive) (id b case_insensitive) (path (id x case_insensitive) (case_insensitive (lit "c"))))""",
        """(like (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)) (path (id x (case_insensitive) (unqualified)) (path_expr (lit "c") (case_insensitive))))""",
        skipPig = false
    )

    //****************************************
    // date part
    //****************************************
    @Test
    fun datePartYear() = assertExpression(
        "year",
        "(lit \"year\")",
        skipPig = false)

    @Test
    fun datePartMonth() = assertExpression(
        "month",
        "(lit \"month\")",
        skipPig = false)

    @Test
    fun datePartDay() = assertExpression(
        "day",
        "(lit \"day\")",
        skipPig = false)

    @Test
    fun datePartHour() = assertExpression(
        "hour",
        "(lit \"hour\")",
        skipPig = false)

    @Test
    fun datePartMinutes() = assertExpression(
        "minute",
        "(lit \"minute\")",
        skipPig = false)

    @Test
    fun datePartSeconds() = assertExpression(
        "second",
        "(lit \"second\")",
        skipPig = false)

    @Test
    fun datePartTimestampHour() = assertExpression(
        "timezone_hour",
        "(lit \"timezone_hour\")",
        skipPig = false)

    @Test
    fun datePartTimezoneMinute() = assertExpression(
        "timezone_minute",
        "(lit \"timezone_minute\")",
        skipPig = false)


    //****************************************
    // call date add (special syntax)
    //****************************************
    @Test
    fun callDateAddYear() = assertExpression(
        "date_add(year, a, b)",
        "(call date_add (lit \"year\") (id a case_insensitive) (id b case_insensitive))",
        "(call date_add (lit \"year\") (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)))",
        skipPig = false
    )

    @Test
    fun callDateAddMonth() = assertExpression(
        "date_add(month, a, b)",
        "(call date_add (lit \"month\") (id a case_insensitive) (id b case_insensitive))",
        "(call date_add (lit \"month\") (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)))",
        skipPig = false
    )

    @Test
    fun callDateAddDay() = assertExpression(
        "date_add(day, a, b)",
        "(call date_add (lit \"day\") (id a case_insensitive) (id b case_insensitive))",
        "(call date_add (lit \"day\") (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)))",
        skipPig = false
    )

    @Test
    fun callDateAddHour() = assertExpression(
        "date_add(hour, a, b)",
        "(call date_add (lit \"hour\") (id a case_insensitive) (id b case_insensitive))",
        "(call date_add (lit \"hour\") (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)))",
        skipPig = false
    )

    @Test
    fun callDateAddMinute() = assertExpression(
        "date_add(minute, a, b)",
        "(call date_add (lit \"minute\") (id a case_insensitive) (id b case_insensitive))",
        "(call date_add (lit \"minute\") (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)))",
        skipPig = false
    )

    @Test
    fun callDateAddSecond() = assertExpression(
        "date_add(second, a, b)",
        "(call date_add (lit \"second\") (id a case_insensitive) (id b case_insensitive))",
        "(call date_add (lit \"second\") (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)))",
        skipPig = false
    )

    @Test // invalid evaluation, but valid parsing
    fun callDateAddTwoArguments() = assertExpression(
        "date_add(second, a)",
        "(call date_add (lit \"second\") (id a case_insensitive))",
        "(call date_add (lit \"second\") (id a (case_insensitive) (unqualified)))",
        skipPig = false
    )

    @Test // invalid evaluation, but valid parsing
    fun callDateAddTimezoneHour() = assertExpression(
        "date_add(timezone_hour, a, b)",
        "(call date_add (lit \"timezone_hour\") (id a case_insensitive) (id b case_insensitive))",
        "(call date_add (lit \"timezone_hour\") (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)))",
        skipPig = false
    )

    @Test // invalid evaluation, but valid parsing
    fun callDateAddTimezoneMinute() = assertExpression(
        "date_add(timezone_minute, a, b)",
        "(call date_add (lit \"timezone_minute\") (id a case_insensitive) (id b case_insensitive))",
        "(call date_add (lit \"timezone_minute\") (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)))",
        skipPig = false
    )

    //****************************************
    // call extract (special syntax)
    //****************************************
    @Test
    fun callExtractYear() = assertExpression(
        "extract(year from a)",
        "(call extract (lit \"year\") (id a case_insensitive))",
        "(call extract (lit \"year\") (id a (case_insensitive) (unqualified)))",
        skipPig = false
    )

    @Test
    fun callExtractMonth() = assertExpression(
        "extract(month from a)",
        "(call extract (lit \"month\") (id a case_insensitive))",
        "(call extract (lit \"month\") (id a (case_insensitive) (unqualified)))",
        skipPig = false
    )

    @Test
    fun callExtractDay() = assertExpression(
        "extract(day from a)",
        "(call extract (lit \"day\") (id a case_insensitive))",
        "(call extract (lit \"day\") (id a (case_insensitive) (unqualified)))",
        skipPig = false
    )

    @Test
    fun callExtractHour() = assertExpression(
        "extract(hour from a)",
        "(call extract (lit \"hour\") (id a case_insensitive))",
        "(call extract (lit \"hour\") (id a (case_insensitive) (unqualified)))",
        skipPig = false
    )

    @Test
    fun callExtractMinute() = assertExpression(
        "extract(minute from a)",
        "(call extract (lit \"minute\") (id a case_insensitive))",
        "(call extract (lit \"minute\") (id a (case_insensitive) (unqualified)))",
        skipPig = false
    )

    @Test
    fun callExtractSecond() = assertExpression(
        "extract(second from a)",
        "(call extract (lit \"second\") (id a case_insensitive))",
        "(call extract (lit \"second\") (id a (case_insensitive) (unqualified)))",
        skipPig = false
    )

    @Test
    fun callExtractTimezoneHour() = assertExpression(
        "extract(timezone_hour from a)",
        "(call extract (lit \"timezone_hour\") (id a case_insensitive))",
        "(call extract (lit \"timezone_hour\") (id a (case_insensitive) (unqualified)))",
        skipPig = false
    )

    @Test
    fun callExtractTimezoneMinute() = assertExpression(
        "extract(timezone_minute from a)",
        "(call extract (lit \"timezone_minute\") (id a case_insensitive))",
        "(call extract (lit \"timezone_minute\") (id a (case_insensitive) (unqualified)))",
        skipPig = false
    )

    @Test
    fun caseInsensitiveFunctionName() = assertExpression(
        "mY_fUnCtIoN(a)",
        "(call my_function (id a case_insensitive))",
        "(call my_function (id a (case_insensitive) (unqualified)))",
        skipPig = false
    )

    @Test
    fun parameterExpression() = assertExpression(
        "?",
        "(parameter 1)",
        skipPig = false)

    //****************************************
    // SELECT
    //****************************************
    @Test
    fun selectWithSingleFrom() = assertExpression(
        "SELECT a FROM table1",
        "(select (project (list (id a case_insensitive))) (from (id table1 case_insensitive)))",
        "(select (project (project_list (project_expr (id a (case_insensitive) (unqualified)) null))) (from (scan (id table1 (case_insensitive) (unqualified)) null null null)))",
        skipPig = false
    )

    @Test
    fun selectAllWithSingleFrom() = assertExpression(
        "SELECT ALL a FROM table1",
        "(select (project (list (id a case_insensitive))) (from (id table1 case_insensitive)))",
        "(select (project (project_list (project_expr (id a (case_insensitive) (unqualified)) null))) (from (scan (id table1 (case_insensitive) (unqualified)) null null null)))",
        skipPig = false
    )

    @Test
    fun selectDistinctWithSingleFrom() = assertExpression(
        "SELECT DISTINCT a FROM table1",
        "(select (project_distinct (list (id a case_insensitive))) (from (id table1 case_insensitive)))",
        "(select (setq (distinct)) (project (project_list (project_expr (id a (case_insensitive) (unqualified)) null))) (from (scan (id table1 (case_insensitive) (unqualified)) null null null)))",
        skipPig = false
    )

    @Test
    fun selectStar() = assertExpression(
        "SELECT * FROM table1",
        "(select (project (list (project_all))) (from (id table1 case_insensitive)))",
        "(select (project (project_star)) (from (scan (id table1 (case_insensitive) (unqualified)) null null null)))",
        skipPig = false
    )

    @Test
    fun selectAliasDotStar() = assertExpression(
        "SELECT t.* FROM table1 AS t",
        "(select (project (list (project_all (id t case_insensitive)))) (from (as t (id table1 case_insensitive))))",
        "(select (project (project_list (project_all (id t (case_insensitive) (unqualified))))) (from (scan (id table1 (case_insensitive) (unqualified)) t null null)))",
        skipPig = false
    )

    @Test
    fun selectPathAliasDotStar() = assertExpression(
        "SELECT a.b.* FROM table1 AS t",
        "(select (project (list (project_all (path (id a case_insensitive) (case_insensitive (lit \"b\")))))) (from (as t (id table1 case_insensitive))))",
        "(select (project (project_list (project_all (path (id a (case_insensitive) (unqualified)) (path_expr (lit \"b\") (case_insensitive)))))) (from (scan (id table1 (case_insensitive) (unqualified)) t null null)))",
        skipPig = false
    )


    @Test
    fun selectWithFromAt() = assertExpression(
        "SELECT ord FROM table1 AT ord",
        "(select (project (list (id ord case_insensitive))) (from (at ord (id table1 case_insensitive))))",
        "(select (project (project_list (project_expr (id ord (case_insensitive) (unqualified)) null))) (from (scan (id table1 (case_insensitive) (unqualified)) null ord null)))",
        skipPig = false
    )

    @Test
    fun selectWithFromAsAndAt() = assertExpression(
        "SELECT ord, val FROM table1 AS val AT ord",
        "(select (project (list (id ord case_insensitive) (id val case_insensitive))) (from (at ord (as val (id table1 case_insensitive)))))",
        "(select (project (project_list (project_expr (id ord (case_insensitive) (unqualified)) null) (project_expr (id val (case_insensitive) (unqualified)) null))) (from (scan (id table1 (case_insensitive) (unqualified)) val ord null)))",
        skipPig = false
    )


    @Test
    fun selectWithFromIdBy() = assertExpression(
        "SELECT * FROM table1 BY uid",
        "(select (project (list (project_all))) (from (by uid (id table1 case_insensitive))))",
        "(select (project (project_star)) (from (scan (id table1 (case_insensitive) (unqualified)) null null uid)))",
        skipPig = false
    )

    @Test
    fun selectWithFromAtIdBy() = assertExpression(
        "SELECT * FROM table1 AT ord BY uid",
        "(select (project (list (project_all))) (from (by uid (at ord (id table1 case_insensitive)))))",
        "(select (project (project_star)) (from (scan (id table1 (case_insensitive) (unqualified)) null ord uid)))",
        skipPig = false
    )

    @Test
    fun selectWithFromAsIdBy() = assertExpression(
        "SELECT * FROM table1 AS t BY uid",
        "(select (project (list (project_all))) (from (by uid (as t (id table1 case_insensitive)))))",
        "(select (project (project_star)) (from (scan (id table1 (case_insensitive) (unqualified)) t null uid)))",
        skipPig = false
    )

    @Test
    fun selectWithFromAsAndAtIdBy() = assertExpression(
        "SELECT * FROM table1 AS val AT ord BY uid",
        "(select (project (list (project_all))) (from (by uid (at ord (as val (id table1 case_insensitive))))))",
        "(select (project (project_star)) (from (scan (id table1 (case_insensitive) (unqualified)) val ord uid)))",
        skipPig = false
    )


    @Test
    fun selectWithFromUnpivot() = assertExpression(
        "SELECT * FROM UNPIVOT item",
        """
        (select
          (project (list (project_all)))
          (from (unpivot (id item case_insensitive)))
        )
        """,
        """
        (select
          (project (project_star))
          (from (unpivot (id item (case_insensitive) (unqualified)) null null null))
        )
        """,
        skipPig = false
    )

    @Test
    fun selectWithFromUnpivotWithAt() = assertExpression(
        "SELECT ord FROM UNPIVOT item AT name",
        """
        (select
          (project (list (id ord case_insensitive)))
          (from (at name (unpivot (id item case_insensitive))))
        )
        """,
        """
        (select
          (project (project_list (project_expr (id ord (case_insensitive) (unqualified)) null)))
          (from (unpivot (id item (case_insensitive) (unqualified)) null name null))
        )
        """,
        skipPig = false
    )

    @Test
    fun selectWithFromUnpivotWithAs() = assertExpression(
        "SELECT ord FROM UNPIVOT item AS val",
        """
        (select
          (project (list (id ord case_insensitive)))
          (from (as val (unpivot (id item case_insensitive))))
        )
        """,
        """
        (select
          (project (project_list (project_expr (id ord (case_insensitive) (unqualified)) null)))
          (from (unpivot (id item (case_insensitive) (unqualified)) val null null))
        )
        """,
        skipPig = false
    )

    @Test
    fun selectWithFromUnpivotWithAsAndAt() = assertExpression(
        "SELECT ord FROM UNPIVOT item AS val AT name",
        """
        (select
          (project (list (id ord case_insensitive)))
          (from (at name (as val (unpivot (id item case_insensitive)))))
        )
        """,
        """
        (select
          (project (project_list (project_expr (id ord (case_insensitive) (unqualified)) null)))
          (from (unpivot (id item (case_insensitive) (unqualified)) val name null))
        )
        """,
        skipPig = false
    )

    @Test
    fun selectAllStar() = assertExpression(
        "SELECT ALL * FROM table1",
        "(select (project (list (project_all))) (from (id table1 case_insensitive)))",
        """
            (select 
                (project (project_star)) 
                (from (scan (id table1 (case_insensitive) (unqualified)) null null null)))
        """,
        skipPig = false
    )

    @Test
    fun selectDistinctStar() = assertExpression(
        "SELECT DISTINCT * FROM table1",
        "(select (project_distinct (list (project_all))) (from (id table1 case_insensitive)))",
        """
            (select 
                (setq (distinct)) 
                (project (project_star)) 
                (from (scan (id table1 (case_insensitive) (unqualified)) null null null)))
        """,
        skipPig = false
    )

    @Test
    fun selectWhereMissing() = assertExpression(
        "SELECT a FROM stuff WHERE b IS MISSING",
        "(select (project (list (id a case_insensitive))) (from (id stuff case_insensitive)) (where (is (id b case_insensitive) (type missing))))",
        """
            (select 
                (project (project_list (project_expr (id a (case_insensitive) (unqualified)) null))) 
                (from (scan (id stuff (case_insensitive) (unqualified)) null null null)) 
                (where (is_type (id b (case_insensitive) (unqualified)) (missing_type))))
        """,
        skipPig = false
    )

    @Test
    fun selectCommaCrossJoin1() = assertExpression(
        "SELECT a FROM table1, table2",
        "(select (project (list (id a case_insensitive))) (from (inner_join (id table1 case_insensitive) (id table2 case_insensitive))))",
        """
            (select 
                (project (project_list (project_expr (id a (case_insensitive) (unqualified)) null))) 
                (from 
                    (join 
                        (inner) 
                        (scan (id table1 (case_insensitive) (unqualified)) null null null)
                        (scan (id table2 (case_insensitive) (unqualified)) null null null)
                        null)))
        """,
        skipPig = false
    )

    @Test
    fun selectCommaCrossJoin2() = assertExpression(
        "SELECT a FROM table1, table2, table3",
        "(select (project (list (id a case_insensitive))) (from (inner_join (inner_join (id table1 case_insensitive) (id table2 case_insensitive)) (id table3 case_insensitive))))",
        """
            (select 
                (project (project_list (project_expr (id a (case_insensitive) (unqualified)) null))) 
                (from 
                    (join
                        (inner)
                        (join
                            (inner)
                            (scan (id table1 (case_insensitive) (unqualified)) null null null) 
                            (scan (id table2 (case_insensitive) (unqualified)) null null null)
                            null) 
                        (scan (id table3 (case_insensitive) (unqualified)) null null null)
                        null)))
        """,
        skipPig = false
    )

    @Test
    fun selectMultipleWithMultipleFromSimpleWhere() = assertExpression(
        "SELECT a, b FROM table1 as t1, table2 WHERE f(t1)",
        """(select
             (project (list (id a case_insensitive) (id b case_insensitive)))
             (from (inner_join (as t1 (id table1 case_insensitive)) (id table2 case_insensitive)))
             (where (call f (id t1 case_insensitive)))
           )
        """,
        """(select
             (project (project_list (project_expr (id a (case_insensitive) (unqualified)) null) (project_expr (id b (case_insensitive) (unqualified)) null)))
             (from 
                (join
                    (inner)
                    (scan (id table1 (case_insensitive) (unqualified)) t1 null null) 
                    (scan (id table2 (case_insensitive) (unqualified)) null null null)
                    null))
             (where (call f (id t1 (case_insensitive) (unqualified))))
           )
        """,
        skipPig = false
    )

    @Test
    fun selectMultipleWithMultipleFromSimpleWhereNoAsAlias() = assertExpression(
        "SELECT a a1, b b1 FROM table1 t1, table2 WHERE f(t1)",
        """(select
             (project (list (as a1 (id a case_insensitive)) (as b1 (id b case_insensitive))))
             (from (inner_join (as t1 (id table1 case_insensitive)) (id table2 case_insensitive)))
             (where (call f (id t1 case_insensitive)))
           )
        """,
        """
        (select
            (project (project_list (project_expr (id a (case_insensitive) (unqualified)) a1) (project_expr (id b (case_insensitive) (unqualified)) b1)))
            (from 
                (join 
                    (inner) 
                    (scan (id table1 (case_insensitive) (unqualified)) t1 null null) 
                    (scan (id table2 (case_insensitive) (unqualified)) null null null) 
                    null))
            (where (call f (id t1 (case_insensitive) (unqualified))))
        )
        """,
        skipPig = false
    )


    @Test
    fun selectListWithAggregateWildcardCall() = assertExpression(
        "SELECT sum(a) + count(*), AVG(b), MIN(c), MAX(d + e) FROM foo",
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
        """
        (select
          (project
            (project_list
              (project_expr (plus (call_agg (all) sum (id a (case_insensitive) (unqualified))) (call_agg_wildcard count)) null)
              (project_expr (call_agg (all) avg (id b (case_insensitive) (unqualified))) null)
              (project_expr (call_agg (all) min (id c (case_insensitive) (unqualified))) null)
              (project_expr (call_agg (all) max (plus (id d (case_insensitive) (unqualified)) (id e (case_insensitive) (unqualified)))) null)
            )
          )
          (from (scan (id foo (case_insensitive) (unqualified)) null null null))
        )
        """
    )

    @Test
    fun pathsAndSelect() = assertExpression(
        """SELECT process(t).a[0] AS a, t2.b AS b
                   FROM t1.a AS t, t2.x.*.b
                   WHERE test(t2.name, t1.name) AND t1.id = t2.id
                """,
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
        """(select
             (project
               (project_list
                 (project_expr (path (call process (id t (case_insensitive) (unqualified))) (path_expr (lit "a") (case_insensitive)) (path_expr (lit 0) (case_sensitive))) a)
                 (project_expr (path (id t2 (case_insensitive) (unqualified)) (path_expr (lit "b") (case_insensitive))) b)
               )
             )
             (from
               (join 
                 (inner) 
                 (scan (path (id t1 (case_insensitive) (unqualified)) (path_expr (lit "a") (case_insensitive))) t null null) 
                 (scan (path (id t2 (case_insensitive) (unqualified)) (path_expr (lit "x") (case_insensitive)) (path_unpivot) (path_expr (lit "b") (case_insensitive))) null null null)
                 null
               )
             )
             (where
               (and
                 (call test (path (id t2 (case_insensitive) (unqualified)) (path_expr (lit "name") (case_insensitive))) (path (id t1 (case_insensitive) (unqualified)) (path_expr (lit "name") (case_insensitive))))
                 (eq (path (id t1 (case_insensitive) (unqualified)) (path_expr (lit "id") (case_insensitive))) (path (id t2 (case_insensitive) (unqualified)) (path_expr (lit "id") (case_insensitive))))
               )
             )
           )
        """,
        skipPig = false
    )

    @Test
    fun selectValueWithSingleFrom() = assertExpression(
        "SELECT VALUE a FROM table1",
        "(select (project (value (id a case_insensitive))) (from (id table1 case_insensitive)))",
        "(select (project (project_value (id a (case_insensitive) (unqualified)))) (from (scan (id table1 (case_insensitive) (unqualified)) null null null)))",
        skipPig = false
    )

    @Test
    fun selectValueWithSingleAliasedFrom() = assertExpression(
        "SELECT VALUE v FROM table1 AS v",
        "(select (project (value (id v case_insensitive))) (from (as v (id table1 case_insensitive))))",
        "(select (project (project_value (id v (case_insensitive) (unqualified)))) (from (scan (id table1 (case_insensitive) (unqualified)) v null null)))",
        skipPig = false
    )

    @Test
    fun selectAllValues() = assertExpression(
        "SELECT ALL VALUE v FROM table1 AS v",
        "(select (project (value (id v case_insensitive))) (from (as v (id table1 case_insensitive))))",
        "(select (project (project_value (id v (case_insensitive) (unqualified)))) (from (scan (id table1 (case_insensitive) (unqualified)) v null null)))",
        skipPig = false
    )

    @Test
    fun selectDistinctValues() = assertExpression(
        "SELECT DISTINCT VALUE v FROM table1 AS v",
        "(select (project_distinct (value (id v case_insensitive))) (from (as v (id table1 case_insensitive))))",
        "(select (setq (distinct)) (project (project_value (id v (case_insensitive) (unqualified)))) (from (scan (id table1 (case_insensitive) (unqualified)) v null null)))",
        skipPig = false
    )

    @Test
    fun nestedSelectNoWhere() = assertExpression(
        "SELECT * FROM (SELECT * FROM x).a",
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
        """
            (select
                (project (project_star))
                    (from
                        (scan
                           (path
                                (select
                                    (project (project_star))
                                    (from (scan (id x (case_insensitive) (unqualified)) null null null)))
                                (path_expr (lit "a") (case_insensitive)))
                            null 
                            null
                            null)))
        """,
        skipPig = false
    )

    @Test
    fun nestedSelect() = assertExpression(
        "SELECT * FROM (SELECT * FROM x WHERE b).a",
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
        """(select
             (project (project_star))
             (from
               (scan 
                 (path
                   (select
                     (project (project_star))
                     (from (scan (id x (case_insensitive) (unqualified)) null null null))
                     (where (id b (case_insensitive) (unqualified)))
                   )
                   (path_expr (lit "a") (case_insensitive))
                 )
                 null
                 null
                 null
               )
             )
           )
        """,
        skipPig = false
    )

    @Test
    fun selectLimit() = assertExpression(
        "SELECT * FROM a LIMIT 10",
        """(select
             (project (list (project_all)))
             (from (id a case_insensitive))
             (limit (lit 10))
           )
        """,
        """(select
             (project (project_star))
             (from (scan (id a (case_insensitive) (unqualified)) null null null))
             (limit (lit 10))
           )
        """,
        skipPig = false
    )

    @Test
    fun selectWhereLimit() = assertExpression(
        "SELECT * FROM a WHERE a = 5 LIMIT 10",
        """(select
             (project (list (project_all)))             
             (from (id a case_insensitive))
             (where (= (id a case_insensitive) (lit 5)))
             (limit (lit 10))
           )
        """,
        """(select
             (project (project_star))
             (from (scan (id a (case_insensitive) (unqualified)) null null null))
             (where (eq (id a (case_insensitive) (unqualified)) (lit 5)))
             (limit (lit 10))
           )
        """,
        skipPig = false
    )

    @Test
    fun selectWithParametersAndLiterals() = assertExpression(
        "SELECT ?, f.a from foo f where f.bar = ? and f.spam = 'eggs' and f.baz = ?",
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
        """
        (select
            (project
                (project_list
                    (project_expr (parameter 1) null)
                    (project_expr (path (id f (case_insensitive) (unqualified)) (path_expr (lit "a") (case_insensitive))) null)))
            (from
                (scan 
                    (id foo (case_insensitive) (unqualified))
                    f
                    null
                    null))
            (where
                (and
                    (and
                        (eq
                            (path
                                (id f (case_insensitive) (unqualified))
                                (path_expr (lit "bar") (case_insensitive)))
                            (parameter
                                2))
                        (eq
                            (path
                                (id f (case_insensitive) (unqualified))
                                (path_expr (lit "spam") (case_insensitive)))
                            (lit "eggs")))
                    (eq
                        (path
                            (id f (case_insensitive) (unqualified))
                            (path_expr (lit "baz") (case_insensitive)))
                        (parameter
                            3)))))
        """,
        skipPig = false
    )

    //****************************************
    // GROUP BY and GROUP PARTIAL BY
    //****************************************
    @Test
    fun groupBySingleId() = assertExpression(
        "SELECT a FROM data GROUP BY a",
        """(select
             (project (list (id a case_insensitive)))
             (from (id data case_insensitive))
             (group (by (id a case_insensitive)))
           )
        """,
        """(select
             (project (project_list (project_expr (id a (case_insensitive) (unqualified)) null)))
             (from (scan (id data (case_insensitive) (unqualified)) null null null))
             (group (by (id a (case_insensitive) (unqualified))))
           )
        """
    )

    @Test
    fun groupBySingleExpr() = assertExpression(
        "SELECT a + b FROM data GROUP BY a + b",
        """(select
             (project (list (+ (id a case_insensitive) (id b case_insensitive))))
             (from (id data case_insensitive))
             (group (by (+ (id a case_insensitive) (id b case_insensitive))))
           )
        """,
        """(select
             (project (project_list (project_expr (plus (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) null)))
             (from (scan (id data (case_insensitive) (unqualified)) null null null))
             (group (by (plus (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)))))
           )
        """
    )

    @Test
    fun groupPartialByMultiAliasedAndGroupAliased() = assertExpression(
        "SELECT g FROM data GROUP PARTIAL BY a AS x, b + c AS y, foo(d) AS z GROUP AS g",
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
        """(select
             (project (project_list (project_expr (id g (case_insensitive) (unqualified)) null)))
             (from (scan (id data (case_insensitive) (unqualified)) null null null))
             (group_partial
               (by
                 (as x (id a (case_insensitive) (unqualified)))
                 (as y (plus (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified))))
                 (as z (call foo (id d (case_insensitive) (unqualified))))
               )
               (name g)
             )
           )
        """
    )

    //****************************************
    // HAVING
    //****************************************
    @Test
    fun havingMinimal() = assertExpression(
        "SELECT a FROM data HAVING a = b",
        """
          (select
            (project (list (id a case_insensitive)))
            (from (id data case_insensitive))
            (having (= (id a case_insensitive) (id b case_insensitive)))
          )
        """,
        """
          (select
            (project (project_list (project_expr (id a (case_insensitive) (unqualified)) null)))
            (from (scan (id data (case_insensitive) (unqualified)) null null null))
            (having (eq (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))))
          )
        """,
        skipPig = false
    )

    @Test
    fun havingWithWhere() = assertExpression(
        "SELECT a FROM data WHERE a = b HAVING c = d",
        """
          (select
            (project (list (id a case_insensitive)))
            (from (id data case_insensitive))
            (where (= (id a case_insensitive) (id b case_insensitive)))
            (having (= (id c case_insensitive) (id d case_insensitive)))
          )
        """,
        """
          (select
            (project (project_list (project_expr (id a (case_insensitive) (unqualified)) null)))
            (from (scan (id data (case_insensitive) (unqualified)) null null null))
            (where (eq (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))))
            (having (eq (id c (case_insensitive) (unqualified)) (id d (case_insensitive) (unqualified))))
          )
        """,
        skipPig = false
    )

    @Test
    fun havingWithWhereAndGroupBy() = assertExpression(
        "SELECT g FROM data WHERE a = b GROUP BY c, d GROUP AS g HAVING d > 6",
        """
          (select
            (project (list (id g case_insensitive)))
            (from (id data case_insensitive))
            (where (= (id a case_insensitive) (id b case_insensitive)))
            (group (by (id c case_insensitive) (id d case_insensitive)) (name g))
            (having (> (id d case_insensitive) (lit 6)))
          )
        """,
        """
          (select
            (project (project_list (project_expr (id g (case_insensitive) (unqualified)) null)))
            (from (scan (id data (case_insensitive) (unqualified)) null null null))
            (where (eq (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))))
            (group (by (id c (case_insensitive) (unqualified)) (id d (case_insensitive) (unqualified))) (name g))
            (having (gt (id d (case_insensitive) (unqualified)) (lit 6)))
          )
        """
    )

    //****************************************
    // PIVOT
    //****************************************
    @Test
    fun pivotWithOnlyFrom() = assertExpression(
        "PIVOT v AT n FROM data",
        """
          (pivot
            (member (id n case_insensitive) (id v case_insensitive))
            (from (id data case_insensitive))
          )
        """,
        """
          (select
            (project
                (project_pivot 
                    (id n (case_insensitive) (unqualified)) 
                    (id v (case_insensitive) (unqualified))))
            (from (scan (id data (case_insensitive) (unqualified)) null null null))
          )
        """,
        skipPig = false
    )

    @Test
    fun pivotHavingWithWhereAndGroupBy() = assertExpression(
        "PIVOT g AT ('prefix:' || c) FROM data WHERE a = b GROUP BY c, d GROUP AS g HAVING d > 6",
        """
          (pivot
            (member (|| (lit "prefix:") (id c case_insensitive)) (id g case_insensitive))
            (from (id data case_insensitive))
            (where (= (id a case_insensitive) (id b case_insensitive)))
            (group (by (id c case_insensitive) (id d case_insensitive)) (name g))
            (having (> (id d case_insensitive) (lit 6)))
          )
        """,
        """
          (select
            (project 
              (project_pivot
                (concat (lit "prefix:") (id c (case_insensitive) (unqualified))) 
                (id g (case_insensitive) (unqualified))))
            (from (scan (id data (case_insensitive) (unqualified)) null null null))
            (where (eq (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))))
            (group (by (id c (case_insensitive) (unqualified)) (id d (case_insensitive) (unqualified))) (name g))
            (having (gt (id d (case_insensitive) (unqualified)) (lit 6)))
          )
        """
    )

    //****************************************
    // DML
    //****************************************

    @Test
    fun fromInsertValuesDml() = assertExpression(
        "FROM x INSERT INTO foo VALUES (1, 2), (3, 4)",
        """
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
        """
          (dml
            (operation
              (insert
                (id foo (case_insensitive) (unqualified))
                (bag
                  (list (lit 1) (lit 2))
                  (list (lit 3) (lit 4))
                )
              )
            )
            (from (scan (id x (case_insensitive) (unqualified)) null null null))
          )
        """,
        skipPig = false
    )

    @Test
    fun fromInsertValueAtDml() = assertExpression(
        "FROM x INSERT INTO foo VALUE 1 AT bar",
        """
          (dml
              (insert_value
                (id foo case_insensitive)
                (lit 1)
                (id bar case_insensitive)
              )
              (from (id x case_insensitive))
          )
        """,
        """
          (dml
            (operation
              (insert_value
                (id foo (case_insensitive) (unqualified))
                (lit 1)
                (id bar (case_insensitive) (unqualified))))
              (from (scan (id x (case_insensitive) (unqualified)) null null null))
          )
        """,
        skipPig = false
    )

    @Test
    fun fromInsertValueDml() = assertExpression(
        "FROM x INSERT INTO foo VALUE 1",
        """
          (dml
              (insert_value
                (id foo case_insensitive)
                (lit 1))
              (from (id x case_insensitive))
          )
        """,
        """
          (dml
            (operation
              (insert_value
                (id foo (case_insensitive) (unqualified))
                (lit 1)
                null))
              (from (scan (id x (case_insensitive) (unqualified)) null null null))
          )
        """,
        skipPig = false
    )

    @Test
    fun fromInsertQueryDml() = assertExpression(
        "FROM x INSERT INTO foo SELECT y FROM bar",
        """
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
        """
          (dml
            (operation
              (insert
                (id foo (case_insensitive) (unqualified))
                (select
                  (project (project_list (project_expr (id y (case_insensitive) (unqualified)) null)))
                  (from (scan (id bar (case_insensitive) (unqualified)) null null null))
                )
              )  
            )
            (from (scan (id x (case_insensitive) (unqualified)) null null null))
          )
        """,
        skipPig = false
    )

    @Test
    fun insertValueDml() = assertExpression(
        "INSERT INTO foo VALUE 1",
        """
          (dml
              (insert_value
                (id foo case_insensitive)
                (lit 1)
              )
          )
        """,
        """
          (dml
            (operation
              (insert_value
                (id foo (case_insensitive) (unqualified))
                (lit 1)
                null)))
        """,
        skipPig = false
    )

    @Test
    fun insertValuesDml() = assertExpression(
        "INSERT INTO foo VALUES (1, 2), (3, 4)",
        """
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
        """
          (dml
            (operation
              (insert
                (id foo (case_insensitive) (unqualified))
                (bag
                  (list (lit 1) (lit 2))
                  (list (lit 3) (lit 4))
                )
              )
            )
          )
        """,
        skipPig = false
    )

    @Test
    fun insertValueAtDml() = assertExpression(
        "INSERT INTO foo VALUE 1 AT bar",
        """
          (dml
              (insert_value
                (id foo case_insensitive)
                (lit 1)
                (id bar case_insensitive)
              )
          )
        """,
        """
          (dml
            (operation
              (insert_value
                (id foo (case_insensitive) (unqualified))
                (lit 1)
                (id bar (case_insensitive) (unqualified))
              )
            )
          )
        """,
        skipPig = false)

    @Test
    fun insertQueryDml() = assertExpression(
        "INSERT INTO foo SELECT y FROM bar",
        """
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
        """
          (dml
            (operation
              (insert
                (id foo (case_insensitive) (unqualified))
                (select
                  (project (project_list (project_expr (id y (case_insensitive) (unqualified)) null)))
                  (from (scan (id bar (case_insensitive) (unqualified)) null null null))
                )
              )
            )
          )
        """,
        skipPig = false
    )

    @Test
    fun fromSetSingleDml() = assertExpression(
        "FROM x WHERE a = b SET k = 5",
        """
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
        """
          (dml
            (operation
              (set
                (assignment
                  (id k (case_insensitive) (unqualified))
                  (lit 5)
                )
              )
            )
            (from (scan (id x (case_insensitive) (unqualified)) null null null))
            (where (eq (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))))
          )
        """,
        skipPig = false
    )

    @Test
    fun fromSetSinglePathFieldDml() = assertExpression(
        "FROM x WHERE a = b SET k.m = 5",
        """
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
        """
          (dml
            (operation
              (set
                (assignment
                  (path (id k (case_insensitive) (unqualified)) (path_expr (lit "m") (case_insensitive)))
                  (lit 5)
                )
              )
            )
            (from (scan (id x (case_insensitive) (unqualified)) null null null))
            (where (eq (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))))
          )
        """,
        skipPig = false
    )

    @Test
    fun fromSetSinglePathStringIndexDml() = assertExpression(
        "FROM x WHERE a = b SET k['m'] = 5",
        """
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
        """
          (dml
            (operation
              (set
                (assignment
                  (path (id k (case_insensitive) (unqualified)) (path_expr (lit "m") (case_sensitive)))
                  (lit 5)
                )
              )
            )
            (from (scan (id x (case_insensitive) (unqualified)) null null null))
            (where (eq (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))))
          )
        """,
        skipPig = false
    )



    @Test
    fun fromSetSinglePathOrdinalDml() = assertExpression(
        "FROM x WHERE a = b SET k[3] = 5",
        """
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
        """
          (dml
            (operation
              (set
                (assignment
                  (path (id k (case_insensitive) (unqualified)) (path_expr (lit 3) (case_sensitive)))
                  (lit 5)
                )
              )
            )
            (from (scan (id x (case_insensitive) (unqualified)) null null null))
            (where (eq (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))))
          )
        """,
        skipPig = false
    )

    @Test
    fun fromSetMultiDml() = assertExpression(
        "FROM x WHERE a = b SET k = 5, m = 6",
        """
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
        """
          (dml
            (operation
              (set
                (assignment
                  (id k (case_insensitive) (unqualified))
                  (lit 5)
                )
                (assignment
                  (id m (case_insensitive) (unqualified))
                  (lit 6)
                )
              )
            )
            (from (scan (id x (case_insensitive) (unqualified)) null null null))
            (where (eq (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))))
          )
        """,
        skipPig = false
    )

    @Test
    fun setSingleDml() = assertExpression(
        "SET k = 5",
        """
          (dml
              (set
                (assignment
                  (id k case_insensitive)
                  (lit 5)
                )
              )
          )
        """,
        """
          (dml
            (operation
              (set
                (assignment
                  (id k (case_insensitive) (unqualified))
                  (lit 5)
                )
              )
            )
          )
        """,
        skipPig = false
    )

    @Test
    fun setSingleDmlWithQuotedIdentifierAtHead() = assertExpression(
        "SET \"k\" = 5",
        """
          (dml
              (set
                (assignment
                  (id k case_sensitive)
                  (lit 5)
                )
              )
          )
        """,
        """
          (dml
            (operation
              (set
                (assignment
                  (id k (case_sensitive) (unqualified))
                  (lit 5)
                )
              )
            )
          )
        """,
        skipPig = false
    )

    @Test
    fun setMultiDml() = assertExpression(
        "SET k = 5, m = 6",
        """
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
        """
          (dml
            (operation
              (set
                (assignment
                  (id k (case_insensitive) (unqualified))
                  (lit 5)
                )
                (assignment
                  (id m (case_insensitive) (unqualified))
                  (lit 6)
                )
              )
            )
          )
        """,
        skipPig = false
    )

    @Test
    fun fromRemoveDml() = assertExpression(
        "FROM x WHERE a = b REMOVE y",
        """
          (dml
            (remove
              (id y case_insensitive)
            )
            (from (id x case_insensitive))
            (where (= (id a case_insensitive) (id b case_insensitive)))
          )
        """,
        """
          (dml
            (operation
              (remove
                (id y (case_insensitive) (unqualified))
              )
            )
            (from (scan (id x (case_insensitive) (unqualified)) null null null))
            (where (eq (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))))
          )
        """,
        skipPig = false
    )

    @Test
    fun removeDml() = assertExpression(
        "REMOVE y",
        """
          (dml
              (remove
                (id y case_insensitive)
              )
          )
        """,
        """
          (dml
            (operation
              (remove
                (id y (case_insensitive) (unqualified))
              )
            )
          )
        """,
        skipPig = false
    )

    @Test
    fun removeDmlPath() = assertExpression(
        "REMOVE a.b['c'][2]",
        """
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
        """
          (dml
            (operation
              (remove
                (path
                  (id a (case_insensitive) (unqualified))
                  (path_expr (lit "b") (case_insensitive))
                  (path_expr (lit "c") (case_sensitive))
                  (path_expr (lit 2) (case_sensitive))
                )
              )
            )
          )
        """,
        skipPig = false
    )

    @Test
    fun updateDml() = assertExpression(
        "UPDATE x AS y SET k = 5, m = 6 WHERE a = b",
        """
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
        """
          (dml
            (operation
              (set
                (assignment
                  (id k (case_insensitive) (unqualified))
                  (lit 5)
                )
                (assignment
                  (id m (case_insensitive) (unqualified))
                  (lit 6)
                )
              )
            )
            (from (scan (id x (case_insensitive) (unqualified)) y null null))
            (where (eq (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))))
          )
        """,
        skipPig = false
    )

    @Test
    fun updateWithInsert() = assertExpression(
        "UPDATE x AS y INSERT INTO k << 1 >> WHERE a = b",
        """
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
        """
          (dml
            (operation
              (insert
                (id k (case_insensitive) (unqualified))
                (bag
                  (lit 1))))
            (from
              (scan
                (id x (case_insensitive) (unqualified))
                y 
                null
                null))
            (where
              (eq
                (id a (case_insensitive) (unqualified))
                (id b (case_insensitive) (unqualified)))))
        """,
        skipPig = false
    )

    @Test
    fun updateWithInsertValueAt() = assertExpression(
        "UPDATE x AS y INSERT INTO k VALUE 1 AT 'j' WHERE a = b",
        """
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
        """
          (dml
            (operation
              (insert_value
                (id k (case_insensitive) (unqualified))
                (lit 1)
                (lit "j")))
            (from
              (scan (id x (case_insensitive) (unqualified)) y null null))
            (where
              (eq
                (id a (case_insensitive) (unqualified))
                (id b (case_insensitive) (unqualified)))))
        """,
        skipPig = false
    )

    @Test
    fun updateWithRemove() = assertExpression(
        "UPDATE x AS y REMOVE y.a WHERE a = b",
        """
          (dml
            (remove
              (path
                (id y case_insensitive)
                (case_insensitive (lit "a"))))
            (from (as y (id x case_insensitive)))
            (where (= (id a case_insensitive) (id b case_insensitive)))
          )
        """,
        """
          (dml  
            (operation
              (remove
                (path
                  (id y (case_insensitive) (unqualified))
                  (path_expr (lit "a") (case_insensitive)))))
            (from (scan (id x (case_insensitive) (unqualified)) y null null))
            (where (eq (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))))
          )
        """,
        skipPig = false
    )

    @Test
    fun updateDmlWithImplicitAs() = assertExpression(
        "UPDATE zoo z SET z.kingdom = 'Fungi'",
        """
          (dml
            (set
              (assignment
                (path (id z case_insensitive) (case_insensitive (lit "kingdom")))
                (lit "Fungi")))
            (from
              (as z (id zoo case_insensitive))))
        """,
        """
          (dml
            (operation
              (set
                (assignment
                  (path (id z (case_insensitive) (unqualified)) (path_expr (lit "kingdom") (case_insensitive)))
                  (lit "Fungi"))))
            (from
              (scan (id zoo (case_insensitive) (unqualified)) z null null)))
        """,
        skipPig = false
    )

    @Test
    fun updateDmlWithAt() = assertExpression(
        "UPDATE zoo AT z_ord SET z.kingdom = 'Fungi'",
        """
          (dml
            (set
              (assignment
                (path (id z case_insensitive) (case_insensitive (lit "kingdom")))
                (lit "Fungi")))
            (from
              (at z_ord (id zoo case_insensitive))))
        """,
        """
          (dml
            (operation
              (set
                (assignment
                  (path (id z (case_insensitive) (unqualified)) (path_expr (lit "kingdom") (case_insensitive)))
                  (lit "Fungi"))))
            (from
              (scan (id zoo (case_insensitive) (unqualified)) null z_ord null)))
        """,
        skipPig = false
    )

    @Test
    fun updateDmlWithBy() = assertExpression(
        "UPDATE zoo BY z_id SET z.kingdom = 'Fungi'",
        """
          (dml
            (set
              (assignment
                (path (id z case_insensitive) (case_insensitive (lit "kingdom")))
                (lit "Fungi")))
            (from
              (by z_id (id zoo case_insensitive))))
        """,
        """
          (dml
            (operation
              (set
                (assignment
                  (path (id z (case_insensitive) (unqualified)) (path_expr (lit "kingdom") (case_insensitive)))
                  (lit "Fungi"))))
            (from
              (scan (id zoo (case_insensitive) (unqualified)) null null z_id)))
        """,
        skipPig = false
    )


    @Test
    fun updateDmlWithAtAndBy() = assertExpression(
        "UPDATE zoo AT z_ord BY z_id SET z.kingdom = 'Fungi'",
        """
          (dml
            (set
              (assignment
                (path (id z case_insensitive) (case_insensitive (lit "kingdom")))
                (lit "Fungi")))
            (from
              (by z_id (at z_ord (id zoo case_insensitive)))))
        """,
        """
          (dml
            (operation
              (set
                (assignment
                  (path (id z (case_insensitive) (unqualified)) (path_expr (lit "kingdom") (case_insensitive)))
                  (lit "Fungi"))))
            (from
              (scan (id zoo (case_insensitive) (unqualified)) null z_ord z_id)))
        """,
        skipPig = false
    )


    @Test
    fun updateWhereDml() = assertExpression(
        "UPDATE x SET k = 5, m = 6 WHERE a = b",
        """
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
        """
          (dml
            (operation
              (set
                (assignment
                  (id k (case_insensitive) (unqualified))
                  (lit 5)
                )
                (assignment
                  (id m (case_insensitive) (unqualified))
                  (lit 6)
                )
              )
            )
            (from (scan (id x (case_insensitive) (unqualified)) null null null))
            (where (eq (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))))
          )
        """,
        skipPig = false
    )

    @Test
    fun deleteDml() = assertExpression(
        "DELETE FROM y",
        """
          (dml
              (delete)
              (from (id y case_insensitive))
          )
        """,
        """
          (dml
            (operation (delete))
            (from (scan (id y (case_insensitive) (unqualified)) null null null))
          )
        """,
        skipPig = false
    )

    @Test
    fun deleteDmlAliased() = assertExpression(
        "DELETE FROM x AS y",
        """
          (dml
              (delete)
              (from (as y (id x case_insensitive)))
          )
        """,
        """
          (dml
            (operation (delete))
            (from (scan (id x (case_insensitive) (unqualified)) y null null))
          )
        """,
        skipPig = false
    )

    @Test
    fun canParseADeleteQueryWithAPositionClause() = assertExpression(
        "DELETE FROM x AT y",
        """
            (dml
                (delete)
                (from
                    (at
                        y
                        (id x case_insensitive))))
        """,
        """
            (dml
              (operation (delete))
              (from (scan (id x (case_insensitive) (unqualified)) null y null)))
        """,
        skipPig = false
    )

    @Test
    fun canParseADeleteQueryWithAliasAndPositionClause() = assertExpression(
        "DELETE FROM x AS y AT z",
        """
            (dml
                (delete)
                (from
                    (at
                        z
                        (as
                            y
                            (id x case_insensitive)))))
        """,
        """
            (dml
                (operation (delete))
               (from (scan (id x (case_insensitive) (unqualified)) y z null)))
        """,
        skipPig = false
    )

    @Test
    fun canParseADeleteQueryWithPath() = assertExpression(
        "DELETE FROM x.n",
        """
            (dml
                (delete)
                (from
                    (path
                        (id x case_insensitive)
                        (case_insensitive (lit "n")))))
        """,
        """
            (dml
                (operation (delete))
                (from
                    (scan
                        (path (id x (case_insensitive) (unqualified)) (path_expr (lit "n") (case_insensitive)))
                        null 
                        null
                        null)))
        """,
        skipPig = false
    )

    @Test
    fun canParseADeleteQueryWithNestedPath() = assertExpression(
        "DELETE FROM x.n.m",
        """
            (dml
                (delete)
                (from
                    (path
                        (id x case_insensitive)
                        (case_insensitive (lit "n"))
                        (case_insensitive (lit "m")))))
        """,
        """
            (dml
                (operation (delete))
                (from
                    (scan 
                        (path
                            (id x (case_insensitive) (unqualified))
                            (path_expr (lit "n") (case_insensitive))
                            (path_expr (lit "m") (case_insensitive)))
                        null
                        null
                        null)))
        """,
        skipPig = false
    )

    @Test
    fun canParseADeleteQueryWithNestedPathAndAlias() = assertExpression(
        "DELETE FROM x.n.m AS y",
        """
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
                                (lit "m"))))))
        """,
        """
            (dml
                (operation (delete))
                (from
                    (scan
                        (path
                            (id x (case_insensitive) (unqualified))
                            (path_expr (lit "n") (case_insensitive))
                            (path_expr (lit "m") (case_insensitive)))
                        y
                        null    
                        null)))
        """,
        skipPig = false
    )

    @Test
    fun canParseADeleteQueryWithNestedPathAndAliasAndPosition() = assertExpression(
        "DELETE FROM x.n.m AS y AT z",
        """
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
                                    (lit "m")))))))
        """,
        """
            (dml
                (operation (delete))
                (from
                    (scan 
                        (path
                            (id x (case_insensitive) (unqualified))
                            (path_expr (lit "n") (case_insensitive))
                            (path_expr (lit "m") (case_insensitive)))
                        y
                        z
                        null)))
        """,
        skipPig = false
    )

    // DDL
    //****************************************
    @Test
    fun createTable() = assertExpression(
        "CREATE TABLE foo",
        "(create foo (table))",
        "(ddl (create_table foo))",
        skipPig = false
    )

    @Test
    fun createTableWithQuotedIdentifier() = assertExpression(
        "CREATE TABLE \"user\"",
        "(create user (table))",
        "(ddl (create_table user))",
        skipPig = false
    )

    @Test
    fun dropTable() = assertExpression(
        "DROP TABLE foo",
        "(drop_table foo)",
        "(ddl (drop_table (identifier foo (case_sensitive))))",
        skipPig = false
    )

    @Test
    fun dropTableWithQuotedIdentifier() = assertExpression(
        "DROP TABLE \"user\"",
        "(drop_table user)",
        "(ddl (drop_table (identifier user (case_sensitive))))",
        skipPig = false
    )

    @Test
    fun createIndex() = assertExpression(
        "CREATE INDEX ON foo (x, y.z)",
        """
        (create
          null.symbol
          (index
            foo
            (keys
              (id x case_insensitive)
              (path (id y case_insensitive) (case_insensitive (lit "z"))))))
        """,
        """
        (ddl
          (create_index
            (identifier foo (case_sensitive))
            (id x (case_insensitive) (unqualified))
            (path (id y (case_insensitive) (unqualified)) (path_expr (lit "z") (case_insensitive)))))
        """,
        skipPig = false
    )

    @Test
    fun createIndexWithQuotedIdentifiers() = assertExpression(
        "CREATE INDEX ON \"user\" (\"group\")",
        """
        (create
          null.symbol
          (index
            user
            (keys
              (id group case_sensitive))))
        """,
        """
        (ddl
          (create_index
            (identifier user (case_sensitive))
            (id group (case_sensitive) (unqualified))))
        """,
        skipPig = false
    )

    @Test
    fun dropIndex() = assertExpression(
        "DROP INDEX bar ON foo",
        "(drop_index foo (id bar case_insensitive))",
        "(ddl (drop_index (table (identifier foo (case_sensitive))) (keys (identifier bar (case_insensitive)))))",
        skipPig = false
    )

    @Test
    fun dropIndexWithQuotedIdentifiers() = assertExpression(
        "DROP INDEX \"bar\" ON \"foo\"",
        "(drop_index foo (id bar case_sensitive))",
        "(ddl (drop_index (table (identifier foo (case_sensitive))) (keys (identifier bar (case_sensitive)))))",
        skipPig = false
    )

    @Test
    fun union() = assertExpression(
        "a UNION b",
        "(union (id a case_insensitive) (id b case_insensitive))",
        "(union (distinct) (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)))",
        skipPig = false
    )

    @Test
    fun unionAll() = assertExpression(
        "a UNION ALL b",
        "(union_all (id a case_insensitive) (id b case_insensitive))",
        "(union (all) (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)))",
        skipPig = false
    )

    @Test
    fun except() = assertExpression(
        "a EXCEPT b",
        "(except (id a case_insensitive) (id b case_insensitive))",
        "(except (distinct) (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)))",
        skipPig = false
    )

    @Test
    fun exceptAll() = assertExpression(
        "a EXCEPT ALL b",
        "(except_all (id a case_insensitive) (id b case_insensitive))",
        "(except (all) (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)))",
        skipPig = false
    )

    @Test
    fun intersect() = assertExpression(
        "a INTERSECT b",
        "(intersect (id a case_insensitive) (id b case_insensitive))",
        "(intersect (distinct) (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)))",
        skipPig = false
    )

    @Test
    fun intersectAll() = assertExpression(
        "a INTERSECT ALL b",
        "(intersect_all (id a case_insensitive) (id b case_insensitive))",
        "(intersect (all) (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)))",
        skipPig = false
    )

    //****************************************
    // semicolon at end of sqlUnderTest
    //****************************************
    @Test
    fun semicolonAtEndOfQuery() = assertExpression(
        "SELECT * FROM <<1>>;",
        "(select (project (list (project_all))) (from (bag (lit 1))))",
        "(select (project (project_star)) (from (scan (bag (lit 1)) null null null)))",
        skipPig = false
    )

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
