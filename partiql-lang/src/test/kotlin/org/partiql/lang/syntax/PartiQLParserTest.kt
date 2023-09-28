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

import com.amazon.ion.Decimal
import com.amazon.ionelement.api.ionDecimal
import com.amazon.ionelement.api.ionInt
import com.amazon.ionelement.api.ionString
import com.amazon.ionelement.api.loadSingleElement
import org.junit.Ignore
import org.junit.Test
import org.partiql.errors.ErrorCode
import org.partiql.errors.Property
import org.partiql.lang.ION
import org.partiql.lang.ast.SourceLocationMeta
import org.partiql.lang.ast.sourceLocation
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.domains.id
import org.partiql.lang.util.getAntlrDisplayString
import org.partiql.parser.antlr.PartiQLParser
import kotlin.concurrent.thread

/**
 * Test parsing of query to PIG-generated AST
 */
class PartiQLParserTest : PartiQLParserTestBase() {

    override val targets: Array<ParserTarget> = arrayOf(ParserTarget.DEFAULT, ParserTarget.EXPERIMENTAL)

    // ****************************************
    // literals
    // ****************************************
    @Test
    fun litInt() = assertExpression(
        "5",
        "(lit 5)"
    )

    @Test
    fun litNull() = assertExpression(
        "null",
        "(lit null)"
    )

    @Test
    fun litMissing() = assertExpression(
        "missing",
        "(missing)"
    )

    @Test
    fun listLiteral() = assertExpression(
        "[a, 5]",
        "(list (id a (case_insensitive) (unqualified)) (lit 5))"
    )

    @Test
    fun listLiteralWithBinary() = assertExpression(
        "[a, 5, (b + 6)]",
        "(list (id a (case_insensitive) (unqualified)) (lit 5) (plus (id b (case_insensitive) (unqualified)) (lit 6)))"
    )

    @Test
    fun listFunction() = assertExpression(
        "list(a, 5)",
        "(list (id a (case_insensitive) (unqualified)) (lit 5))"
    )

    @Test
    fun listFunctionlWithBinary() = assertExpression(
        "LIST(a, 5, (b + 6))",
        "(list (id a (case_insensitive) (unqualified)) (lit 5) (plus (id b (case_insensitive) (unqualified)) (lit 6)))"
    )

    @Test
    fun sexpFunction() = assertExpression(
        "sexp(a, 5)",
        "(sexp (id a (case_insensitive) (unqualified)) (lit 5))"
    )

    @Test
    fun sexpFunctionWithBinary() = assertExpression(
        "SEXP(a, 5, (b + 6))",
        "(sexp (id a (case_insensitive) (unqualified)) (lit 5) (plus (id b (case_insensitive) (unqualified)) (lit 6)))"
    )

    @Test
    fun structLiteral() = assertExpression(
        "{'x':a, 'y':5 }",
        """(struct
             (expr_pair (lit "x") (id a (case_insensitive) (unqualified)))
             (expr_pair (lit "y") (lit 5))
           )
        """
    )

    @Test
    fun structLiteralWithBinary() = assertExpression(
        "{'x':a, 'y':5, 'z':(b + 6)}",
        """(struct
             (expr_pair (lit "x") (id a (case_insensitive) (unqualified)))
             (expr_pair (lit "y") (lit 5))
             (expr_pair (lit "z") (plus (id b (case_insensitive) (unqualified)) (lit 6)))
           )
        """
    )

    @Test
    fun nestedEmptyListLiteral() = assertExpression(
        "[[]]",
        "(list (list))"
    )

    @Test
    fun nestedEmptyBagLiteral() = assertExpression(
        "<<<<>>>>",
        "(bag (bag))"
    )

    @Test
    fun nestedEmptyStructLiteral() = assertExpression(
        "{'a':{}}",
        """(struct (expr_pair (lit "a") (struct)))"""
    )

    // ****************************************
    // container constructors
    // ****************************************
    @Test
    fun rowValueConstructorWithSimpleExpressions() = assertExpression(
        "(1, 2, 3, 4)",
        """(list (lit 1) (lit 2) (lit 3) (lit 4))"""
    )

    @Test
    fun rowValueConstructorWithRowValueConstructors() = assertExpression(
        "((1, 2), (3, 4))",
        """(list (list (lit 1) (lit 2)) (list (lit 3) (lit 4)))"""
    )

    @Test
    fun tableValueConstructorWithRowValueConstructors() = assertExpression(
        "VALUES (1, 2), (3, 4)",
        """(bag (list (lit 1) (lit 2)) (list (lit 3) (lit 4)))"""
    )

    @Test
    fun tableValueConstructorWithSingletonRowValueConstructors() = assertExpression(
        "VALUES (1), (2), (3)",
        """(bag (list (lit 1)) (list (lit 2)) (list (lit 3)))"""
    )

    // ****************************************
    // identifiers
    // ****************************************
    @Test
    fun id_case_insensitive() = assertExpression(
        "kumo",
        "(id kumo (case_insensitive) (unqualified))"
    )

    @Test
    fun id_case_sensitive() = assertExpression(
        "\"kumo\"",
        "(id kumo (case_sensitive) (unqualified))"
    )

    @Test
    fun nonReservedKeyword() = assertExpression(
        "excluded",
        "(id excluded (case_insensitive) (unqualified))"
    )

    @Test
    fun nonReservedKeywordQualified() = assertExpression(
        "@excluded",
        "(id excluded (case_insensitive) (locals_first))"
    )

    // ****************************************
    // call
    // ****************************************
    @Test
    fun callEmpty() = assertExpression(
        "foobar()",
        "(call foobar)"
    )

    @Test
    fun callOneArgument() = assertExpression(
        "foobar(1)",
        "(call foobar (lit 1))"
    )

    @Test
    fun callTwoArgument() = assertExpression(
        "foobar(1, 2)",
        "(call foobar (lit 1) (lit 2))"
    )

    @Test
    fun callSubstringSql92Syntax() = assertExpression(
        "substring('test' from 100)",
        "(call substring (lit \"test\") (lit 100))"
    )

    @Test
    fun callSubstringSql92SyntaxWithLength() = assertExpression(
        "substring('test' from 100 for 50)",
        "(call substring (lit \"test\") (lit 100) (lit 50))"
    )

    @Test
    fun callSubstringNormalSyntax() = assertExpression(
        "substring('test', 100)",
        "(call substring (lit \"test\") (lit 100))"
    )

    @Test
    fun callSubstringNormalSyntaxWithLength() = assertExpression(
        "substring('test', 100, 50)",
        "(call substring (lit \"test\") (lit 100) (lit 50))"
    )

    @Test
    fun callTrimSingleArgument() = assertExpression(
        "trim('test')",
        "(call trim (lit \"test\"))"
    )

    @Test
    fun callTrimTwoArgumentsDefaultSpecification() = assertExpression(
        "trim(' ' from 'test')",
        "(call trim (lit \" \") (lit \"test\"))"
    )

    @Test
    fun callTrimTwoArgumentsUsingBoth() = assertExpression(
        "trim(both from 'test')",
        "(call trim (lit both) (lit \"test\"))"
    )

    @Test
    fun callTrimTwoArgumentsUsingLeading() = assertExpression(
        "trim(leading from 'test')",
        "(call trim (lit leading) (lit \"test\"))"
    )

    @Test
    fun callTrimTwoArgumentsUsingTrailing() = assertExpression(
        "trim(trailing from 'test')",
        "(call trim (lit trailing) (lit \"test\"))"
    )

    // ****************************************
    // Unary operators
    // ****************************************

    @Test
    fun negCall() = assertExpression(
        "-baz()",
        "(neg (call baz))"
    )

    @Test
    fun posNegIdent() = assertExpression(
        "+(-baz())",
        "(pos (neg (call baz)))"
    )

    @Test
    fun posNegIdentNoSpaces() = assertExpression(
        "+-baz()",
        "(pos (neg (call baz)))"
    )

    @Test
    fun unaryIonIntLiteral() {
        assertExpression(
            "-1",
            "(lit -1)"
        )
    }

    @Test
    fun unaryIonFloatLiteral() {
        assertExpression(
            "+-+-+-`-5e0`",
            "(lit 5e0)",
        )
    }

    @Test
    fun unaryIonTimestampLiteral() = assertExpression(
        "+-`2017-01-01`",
        "(pos (neg (lit 2017-01-01T)))"
    )

    @Test
    fun unaryNotLiteral() = assertExpression(
        "not 1",
        "(not (lit 1))"
    )

    // ****************************************
    // BETWEEN
    // ****************************************
    @Test
    fun betweenOperator() = assertExpression(
        "5 BETWEEN 1 AND 10",
        "(between (lit 5) (lit 1) (lit 10))"
    )

    @Test
    fun notBetweenOperator() = assertExpression(
        "5 NOT BETWEEN 1 AND 10",
        "(not (between (lit 5) (lit 1) (lit 10)))"
    )
    // ****************************************
    // @ operator
    // ****************************************

    @Test
    fun atOperatorOnIdentifier() = assertExpression(
        "@a",
        "(id a (case_insensitive) (locals_first))"
    )

    @Test
    fun atOperatorOnPath() = assertExpression(
        "@a.b",
        """(path (id a (case_insensitive) (locals_first)) (path_expr (lit "b") (case_insensitive)))"""
    )

    // ****************************************
    // IS operator
    // ****************************************
    @Test
    fun nullIsNull() = assertExpression(
        "null IS NULL",
        "(is_type (lit null) (null_type))"
    )

    @Test
    fun missingIsMissing() = assertExpression(
        "mIsSiNg IS MISSING",
        "(is_type (missing) (missing_type))"
    )

    @Test
    fun callIsVarchar() = assertExpression(
        "f() IS VARCHAR(200)",
        "(is_type (call f) (character_varying_type 200))"
    )

    @Test
    fun nullIsNotNull() = assertExpression(
        "null IS NOT NULL",
        "(not (is_type (lit null) (null_type)))"
    )

    @Test
    fun missingIsNotMissing() = assertExpression(
        "mIsSiNg IS NOT MISSING",
        "(not (is_type (missing) (missing_type)))"
    )

    @Test
    fun callIsNotVarchar() = assertExpression(
        "f() IS NOT VARCHAR(200)",
        "(not (is_type (call f) (character_varying_type 200)))"
    )

    @Test
    fun callWithMultiple() = assertExpression(
        "foobar(5, 6, a)",
        "(call foobar (lit 5) (lit 6) (id a (case_insensitive) (unqualified)))"
    )

    @Test
    fun aggregateFunctionCall() = assertExpression(
        "COUNT(a)",
        """(call_agg (all) count (id a (case_insensitive) (unqualified)))"""
    )

    @Test
    fun aggregateDistinctFunctionCall() = assertExpression(
        "SUM(DISTINCT a)",
        "(call_agg (distinct) sum (id a (case_insensitive) (unqualified)))"
    )

    @Test
    fun countStarFunctionCall() = assertExpression(
        "COUNT(*)",
        "(call_agg (all) count (lit 1))"
    )

    @Test
    fun countFunctionCall() = assertExpression(
        "COUNT(a)",
        "(call_agg (all) count (id a (case_insensitive) (unqualified)))"
    )

    @Test
    fun countDistinctFunctionCall() = assertExpression(
        "COUNT(DISTINCT a)",
        "(call_agg (distinct) count (id a (case_insensitive) (unqualified)))"
    )

    // ****************************************
    // path expression
    // ****************************************
    @Test
    fun dot_case_1_insensitive_component() = assertExpression(
        "a.b",
        """(path (id a (case_insensitive) (unqualified)) (path_expr (lit "b") (case_insensitive)))"""
    )

    @Test
    fun dot_case_2_insensitive_component() = assertExpression(
        "a.b.c",
        """(path (id a (case_insensitive) (unqualified))
           (path_expr (lit "b") (case_insensitive))
           (path_expr (lit "c") (case_insensitive)))""".trimMargin()
    )

    @Test
    fun dot_case_3_insensitive_components() = assertExpression(
        "a.b.c.d",
        """(path (id a (case_insensitive) (unqualified))
           (path_expr (lit "b") (case_insensitive))
           (path_expr (lit "c") (case_insensitive))
           (path_expr (lit "d") (case_insensitive)))""".trimMargin()
    )

    @Test
    fun dot_case_sensitive() = assertExpression(
        """ "a"."b" """,
        """(path (id a (case_sensitive) (unqualified))
           (path_expr (lit "b") (case_sensitive)))""".trimMargin()
    )

    @Test
    fun dot_case_sensitive_component() = assertExpression(
        "a.\"b\"",
        """(path (id a (case_insensitive) (unqualified))
           (path_expr (lit "b") (case_sensitive)))""".trimMargin()
    )

    @Test
    fun groupDot() = assertExpression(
        "(a).b",
        """(path (id a (case_insensitive) (unqualified))
           (path_expr (lit "b") (case_insensitive)))""".trimMargin()
    )

    @Test
    fun pathWith1SquareBracket() = assertExpression(
        """a[5]""",
        """(path (id a (case_insensitive) (unqualified))
           (path_expr (lit 5) (case_sensitive)))""".trimMargin()
    )

    @Test
    fun pathWith3SquareBrackets() = assertExpression(
        """a[5]['b'][(a + 3)]""",
        """(path (id a (case_insensitive) (unqualified))
           (path_expr (lit 5) (case_sensitive))
           (path_expr (lit "b") (case_sensitive))
           (path_expr (plus (id a (case_insensitive) (unqualified)) (lit 3)) (case_sensitive)))"""
    )

    @Test
    fun dotStar() = assertExpression(
        "a.*",
        """(path (id a (case_insensitive) (unqualified)) (path_unpivot))""".trimMargin()
    )

    @Test
    fun dot2Star() = assertExpression(
        "a.b.*",
        """(path (id a (case_insensitive) (unqualified))
           (path_expr (lit "b") (case_insensitive))
           (path_unpivot))""".trimMargin()
    )

    @Test
    fun dotWildcard() = assertExpression(
        "a[*]",
        """(path (id a (case_insensitive) (unqualified)) (path_wildcard))"""
    )

    @Test
    fun dot2Wildcard() = assertExpression(
        "a.b[*]",
        """(path (id a (case_insensitive) (unqualified))
           (path_expr (lit "b") (case_insensitive))
           (path_wildcard))""".trimMargin()
    )

    @Test
    fun pathWithCallAndDotStar() = assertExpression(
        "foo(x, y).a.*.b",
        """(path (call foo (id x (case_insensitive) (unqualified)) (id y (case_insensitive) (unqualified)))
           (path_expr (lit "a") (case_insensitive))
           (path_unpivot)
           (path_expr (lit "b") (case_insensitive)))""".trimMargin()
    )

    @Test
    fun dotAndBracketStar() = assertExpression(
        "x.a[*].b",
        """(path (id x (case_insensitive) (unqualified))
           (path_expr (lit "a") (case_insensitive))
           (path_wildcard)
           (path_expr (lit "b") (case_insensitive)))""".trimMargin()
    )

    // ****************************************
    // cast
    // ****************************************
    @Test
    fun castNoArgs() = assertExpression(
        "CAST(5 AS VARCHAR)",
        """(cast
             (lit 5)
             (character_varying_type null)
           )
        """
    )

    @Test
    fun castASVarCharWithLength() = assertExpression(
        "CAST(5 AS VARCHAR(5))",
        "(cast (lit 5) (character_varying_type 5))"
    )

    @Test
    fun castAsDecimal() = assertExpression(
        "CAST(a AS DECIMAL)",
        "(cast (id a (case_insensitive) (unqualified)) (decimal_type null null))"
    )

    @Test
    fun castAsDecimalScaleOnly() = assertExpression(
        "CAST(a AS DECIMAL(1))",
        "(cast (id a (case_insensitive) (unqualified)) (decimal_type 1 null))"
    )

    @Test
    fun castAsDecimalScaleAndPrecision() = assertExpression(
        "CAST(a AS DECIMAL(1, 2))",
        "(cast (id a (case_insensitive) (unqualified)) (decimal_type 1 2))"
    )

    @Test
    fun castAsNumeric() = assertExpression(
        "CAST(a AS NUMERIC)",
        """(cast (id a (case_insensitive) (unqualified)) (numeric_type null null))"""
    )

    @Test
    fun castAsNumericScaleOnly() = assertExpression(
        "CAST(a AS NUMERIC(1))",
        "(cast (id a (case_insensitive) (unqualified)) (numeric_type 1 null))"
    )

    @Test
    fun castAsNumericScaleAndPrecision() = assertExpression(
        "CAST(a AS NUMERIC(1, 2))",
        "(cast (id a (case_insensitive) (unqualified)) (numeric_type 1 2))"
    )

    // ****************************************
    // custom type cast
    // ****************************************
    @Test
    fun castAsEsBoolean() = assertExpression(
        "CAST(TRUE AS ES_BOOLEAN)",
        "(cast (lit true) (custom_type es_boolean))"
    )

    @Test
    fun castAsRsInteger() = assertExpression(
        "CAST(1.123 AS RS_INTEGER)",
        "(cast (lit 1.123) (custom_type rs_integer))"
    )

    // ****************************************
    // searched CASE
    // ****************************************
    @Test
    fun searchedCaseSingleNoElse() = assertExpression(
        "CASE WHEN name = 'zoe' THEN 1 END",
        """(searched_case
          (expr_pair_list
            (expr_pair (eq (id name (case_insensitive) (unqualified)) (lit "zoe")) (lit 1)))
          null
        )
        """
    )

    @Test
    fun searchedCaseSingleWithElse() = assertExpression(
        "CASE WHEN name = 'zoe' THEN 1 ELSE 0 END",
        """(searched_case
          (expr_pair_list
            (expr_pair (eq (id name (case_insensitive) (unqualified)) (lit "zoe")) (lit 1)))
          (lit 0)
        )
        """
    )

    @Test
    fun searchedCaseMultiWithElse() = assertExpression(
        "CASE WHEN name = 'zoe' THEN 1 WHEN name > 'kumo' THEN 2 ELSE 0 END",
        """(searched_case
          (expr_pair_list
            (expr_pair (eq (id name (case_insensitive) (unqualified)) (lit "zoe")) (lit 1))
            (expr_pair (gt (id name (case_insensitive) (unqualified)) (lit "kumo")) (lit 2)))
          (lit 0)
        )
        """
    )

    // ****************************************
    // simple CASE
    // ****************************************
    @Test
    fun simpleCaseSingleNoElse() = assertExpression(
        "CASE name WHEN 'zoe' THEN 1 END",
        """(simple_case
          (id name (case_insensitive) (unqualified))
          (expr_pair_list
            (expr_pair (lit "zoe") (lit 1)))
          null
        )
        """
    )

    @Test
    fun simpleCaseSingleWithElse() = assertExpression(
        "CASE name WHEN 'zoe' THEN 1 ELSE 0 END",
        """(simple_case
             (id name (case_insensitive) (unqualified))
             (expr_pair_list
                (expr_pair (lit "zoe") (lit 1)))
             (lit 0)
           )
        """

    )

    @Test
    fun simpleCaseMultiWithElse() = assertExpression(
        "CASE name WHEN 'zoe' THEN 1 WHEN 'kumo' THEN 2 WHEN 'mary' THEN 3 ELSE 0 END",
        """(simple_case
          (id name (case_insensitive) (unqualified))
          (expr_pair_list
            (expr_pair (lit "zoe") (lit 1))
            (expr_pair (lit "kumo") (lit 2))
            (expr_pair (lit "mary") (lit 3)))
          (lit 0)
        )
        """
    )

    // ****************************************
    // IN operator
    // ****************************************
    @Test
    fun inOperatorWithImplicitValues() = assertExpression(
        "a IN (1, 2, 3, 4)",
        """(in_collection
             (id a (case_insensitive) (unqualified))
             (list (lit 1) (lit 2) (lit 3) (lit 4))
           )
        """
    )

    @Test
    fun notInOperatorWithImplicitValues() = assertExpression(
        "a NOT IN (1, 2, 3, 4)",
        """(not
          (in_collection
             (id a (case_insensitive) (unqualified))
             (list (lit 1) (lit 2) (lit 3) (lit 4))))
        """
    )

    @Test
    fun inOperatorWithImplicitValuesRowConstructor() = assertExpression(
        "(a, b) IN ((1, 2), (3, 4))",
        """(in_collection
             (list (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)))
             (list (list (lit 1) (lit 2)) (list (lit 3) (lit 4)))
           )
        """
    )

    // ****************************************
    // LIKE operator
    // ****************************************
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
        """(like (id a (case_insensitive) (unqualified)) (lit "_AAA%") null)"""
    )

    @Test
    fun likeColNameLikeColName() = assertExpression(
        "a LIKE b",
        "(like (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)) null)"
    )

    @Test
    fun pathLikePath() = assertExpression(
        "a.name LIKE b.pattern",
        """
        (like
            (path (id a (case_insensitive) (unqualified)) (path_expr (lit "name") (case_insensitive)))
            (path (id b (case_insensitive) (unqualified)) (path_expr (lit "pattern") (case_insensitive)))
            null)
        """
    )

    @Test
    fun likeColNameLikeColNamePath() = assertExpression(
        "a.name LIKE b.pattern",
        """
        (like
            (path (id a (case_insensitive) (unqualified)) (path_expr (lit "name") (case_insensitive)))
            (path (id b (case_insensitive) (unqualified)) (path_expr (lit "pattern") (case_insensitive)))
            null)
        """
    )

    @Test
    fun likeColNameLikeStringEscape() = assertExpression(
        "a LIKE '_AAA%' ESCAPE '['",
        """
        (like
            (id a (case_insensitive) (unqualified))
            (lit "_AAA%")
            (lit "["))
        """
    )

    @Test
    fun notLikeColNameLikeString() = assertExpression(
        "a NOT LIKE '_AAA%'",
        """
        (not
          (like
            (id a (case_insensitive) (unqualified))
            (lit "_AAA%")
            null))
        """
    )

    @Test
    fun likeColNameLikeColNameEscape() = assertExpression(
        "a LIKE b ESCAPE '\\'", //  escape \ inside a Kotlin/Java String
        // escape \ inside a Kotlin/Java String
        """
        (like
            (id a (case_insensitive) (unqualified))
            (id b (case_insensitive) (unqualified))
            (lit "\\"))
        """
    )

    @Test
    fun likeColNameLikeColNameEscapeNonLit() = assertExpression(
        "a LIKE b ESCAPE c",
        "(like (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)) (id c (case_insensitive) (unqualified)))"
    )

    @Test
    fun likeColNameLikeColNameEscapePath() = assertExpression(
        "a LIKE b ESCAPE x.c",
        """(like (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)) (path (id x (case_insensitive) (unqualified)) (path_expr (lit "c") (case_insensitive))))"""
    )

    // ****************************************
    // call date_add and date_diff (special syntax)
    // ****************************************

    private fun assertDateArithmetic(
        templateSql: String,
        templateExpectedPartiqlAst: String
    ) {
        applyAndAssertDateArithmeticFunctions("add", templateSql, templateExpectedPartiqlAst)
    }

    private fun applyAndAssertDateArithmeticFunctions(
        operation: String,
        templateSql: String,
        templateExpectedPartiqlAst: String
    ) {
        assertExpression(
            templateSql.replace("<op>", operation),
            templateExpectedPartiqlAst.replace("<op>", operation)
        )
    }

    @Test
    fun callDateArithYear() = assertDateArithmetic(
        "date_<op>(year, a, b)",
        "(call date_<op> (lit year) (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)))"
    )

    @Test
    fun callDateArithMonth() = assertDateArithmetic(
        "date_<op>(month, a, b)",
        "(call date_<op> (lit month) (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)))"
    )

    @Test
    fun callDateArithDay() = assertDateArithmetic(
        "date_<op>(day, a, b)",
        "(call date_<op> (lit day) (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)))"
    )

    @Test
    fun callDateArithHour() = assertDateArithmetic(
        "date_<op>(hour, a, b)",
        "(call date_<op> (lit hour) (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)))"
    )

    @Test
    fun callDateArithMinute() = assertDateArithmetic(
        "date_<op>(minute, a, b)",
        "(call date_<op> (lit minute) (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)))"
    )

    @Test
    fun callDateArithSecond() = assertDateArithmetic(
        "date_<op>(second, a, b)",
        "(call date_<op> (lit second) (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)))"
    )

    @Test // invalid evaluation, but valid parsing
    fun callDateArithTimezoneHour() = assertDateArithmetic(
        "date_<op>(timezone_hour, a, b)",
        "(call date_<op> (lit timezone_hour) (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)))"
    )

    @Test // invalid evaluation, but valid parsing
    fun callDateArithTimezoneMinute() = assertDateArithmetic(
        "date_<op>(timezone_minute, a, b)",
        "(call date_<op> (lit timezone_minute) (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)))"
    )

    // ****************************************
    // call extract (special syntax)
    // ****************************************
    @Test
    fun callExtractYear() = assertExpression(
        "extract(year from a)",
        "(call extract (lit year) (id a (case_insensitive) (unqualified)))"
    )

    @Test
    fun callExtractMonth() = assertExpression(
        "extract(month from a)",
        "(call extract (lit month) (id a (case_insensitive) (unqualified)))"
    )

    @Test
    fun callExtractDay() = assertExpression(
        "extract(day from a)",
        "(call extract (lit day) (id a (case_insensitive) (unqualified)))"
    )

    @Test
    fun callExtractHour() = assertExpression(
        "extract(hour from a)",
        "(call extract (lit hour) (id a (case_insensitive) (unqualified)))"
    )

    @Test
    fun callExtractMinute() = assertExpression(
        "extract(minute from a)",
        "(call extract (lit minute) (id a (case_insensitive) (unqualified)))"
    )

    @Test
    fun callExtractSecond() = assertExpression(
        "extract(second from a)",
        "(call extract (lit second) (id a (case_insensitive) (unqualified)))"
    )

    @Test
    fun callExtractTimezoneHour() = assertExpression(
        "extract(timezone_hour from a)",
        "(call extract (lit timezone_hour) (id a (case_insensitive) (unqualified)))"
    )

    @Test
    fun callExtractTimezoneMinute() = assertExpression(
        "extract(timezone_minute from a)",
        "(call extract (lit timezone_minute) (id a (case_insensitive) (unqualified)))"
    )

    @Test
    fun caseInsensitiveFunctionName() = assertExpression(
        "mY_fUnCtIoN(a)",
        "(call my_function (id a (case_insensitive) (unqualified)))"
    )

    @Test
    fun parameterExpression() = assertExpression(
        "?",
        "(parameter 1)"
    )

    @Test
    fun currentUserUpperCase() = assertExpression(
        "CURRENT_USER",
        "(session_attribute current_user)"
    )

    @Test
    fun currentUserMixedCase() = assertExpression(
        "CURRENT_user",
        "(session_attribute current_user)"
    )

    @Test
    fun currentUserLowerCase() = assertExpression(
        "current_user",
        "(session_attribute current_user)"
    )

    @Test
    fun currentUserEquals() = assertExpression(
        "1 = current_user",
        "(eq (lit 1) (session_attribute current_user))"
    )

    @Test
    fun currentUserEqualsConcat() = assertExpression(
        "'username' || current_user",
        "(concat (lit \"username\") (session_attribute current_user))"
    )

    // ****************************************
    // SELECT
    // ****************************************
    @Test
    fun selectWithSingleFrom() = assertExpression(
        "SELECT a FROM table1",
        "(select (project (project_list (project_expr (id a (case_insensitive) (unqualified)) null))) (from (scan (id table1 (case_insensitive) (unqualified)) null null null)))"
    )

    @Test
    fun selectAllWithSingleFrom() = assertExpression(
        "SELECT ALL a FROM table1",
        "(select (project (project_list (project_expr (id a (case_insensitive) (unqualified)) null))) (from (scan (id table1 (case_insensitive) (unqualified)) null null null)))"
    )

    @Test
    fun selectDistinctWithSingleFrom() = assertExpression(
        "SELECT DISTINCT a FROM table1",
        "(select (setq (distinct)) (project (project_list (project_expr (id a (case_insensitive) (unqualified)) null))) (from (scan (id table1 (case_insensitive) (unqualified)) null null null)))"
    )

    @Test
    fun selectStar() = assertExpression(
        "SELECT * FROM table1",
        "(select (project (project_star)) (from (scan (id table1 (case_insensitive) (unqualified)) null null null)))"
    )

    @Test
    fun selectAliasDotStar() = assertExpression(
        "SELECT t.* FROM table1 AS t",
        "(select (project (project_list (project_all (id t (case_insensitive) (unqualified))))) (from (scan (id table1 (case_insensitive) (unqualified)) t null null)))"
    )

    @Test
    fun selectPathAliasDotStar() = assertExpression(
        "SELECT a.b.* FROM table1 AS t",
        "(select (project (project_list (project_all (path (id a (case_insensitive) (unqualified)) (path_expr (lit \"b\") (case_insensitive)))))) (from (scan (id table1 (case_insensitive) (unqualified)) t null null)))"
    )

    @Test
    fun selectWithFromAt() = assertExpression(
        "SELECT ord FROM table1 AT ord",
        "(select (project (project_list (project_expr (id ord (case_insensitive) (unqualified)) null))) (from (scan (id table1 (case_insensitive) (unqualified)) null ord null)))"
    )

    @Test
    fun selectWithFromAsAndAt() = assertExpression(
        "SELECT ord, val FROM table1 AS val AT ord",
        "(select (project (project_list (project_expr (id ord (case_insensitive) (unqualified)) null) (project_expr (id val (case_insensitive) (unqualified)) null))) (from (scan (id table1 (case_insensitive) (unqualified)) val ord null)))"
    )

    @Test
    fun selectWithFromIdBy() = assertExpression(
        "SELECT * FROM table1 BY uid",
        "(select (project (project_star)) (from (scan (id table1 (case_insensitive) (unqualified)) null null uid)))"
    )

    @Test
    fun selectWithFromAtIdBy() = assertExpression(
        "SELECT * FROM table1 AT ord BY uid",
        "(select (project (project_star)) (from (scan (id table1 (case_insensitive) (unqualified)) null ord uid)))"
    )

    @Test
    fun selectWithFromAsIdBy() = assertExpression(
        "SELECT * FROM table1 AS t BY uid",
        "(select (project (project_star)) (from (scan (id table1 (case_insensitive) (unqualified)) t null uid)))"
    )

    @Test
    fun selectWithFromAsAndAtIdBy() = assertExpression(
        "SELECT * FROM table1 AS val AT ord BY uid",
        "(select (project (project_star)) (from (scan (id table1 (case_insensitive) (unqualified)) val ord uid)))"
    )

    @Test
    fun selectWithFromUnpivot() = assertExpression(
        "SELECT * FROM UNPIVOT item",
        """
        (select
          (project (project_star))
          (from (unpivot (id item (case_insensitive) (unqualified)) null null null))
        )
        """
    )

    @Test
    fun selectWithFromUnpivotWithAt() = assertExpression(
        "SELECT ord FROM UNPIVOT item AT name",
        """
        (select
          (project (project_list (project_expr (id ord (case_insensitive) (unqualified)) null)))
          (from (unpivot (id item (case_insensitive) (unqualified)) null name null))
        )
        """
    )

    @Test
    fun selectWithFromUnpivotWithAs() = assertExpression(
        "SELECT ord FROM UNPIVOT item AS val",
        """
        (select
          (project (project_list (project_expr (id ord (case_insensitive) (unqualified)) null)))
          (from (unpivot (id item (case_insensitive) (unqualified)) val null null))
        )
        """
    )

    @Test
    fun selectWithFromUnpivotWithAsAndAt() = assertExpression(
        "SELECT ord FROM UNPIVOT item AS val AT name",
        """
        (select
          (project (project_list (project_expr (id ord (case_insensitive) (unqualified)) null)))
          (from (unpivot (id item (case_insensitive) (unqualified)) val name null))
        )
        """
    )

    @Test
    fun selectAllStar() = assertExpression(
        "SELECT ALL * FROM table1",
        """
            (select 
                (project (project_star)) 
                (from (scan (id table1 (case_insensitive) (unqualified)) null null null)))
        """
    )

    @Test
    fun selectDistinctStar() = assertExpression(
        "SELECT DISTINCT * FROM table1",
        """
            (select 
                (setq (distinct)) 
                (project (project_star)) 
                (from (scan (id table1 (case_insensitive) (unqualified)) null null null)))
        """
    )

    @Test
    fun selectWhereMissing() = assertExpression(
        "SELECT a FROM stuff WHERE b IS MISSING",
        """
            (select 
                (project (project_list (project_expr (id a (case_insensitive) (unqualified)) null))) 
                (from (scan (id stuff (case_insensitive) (unqualified)) null null null)) 
                (where (is_type (id b (case_insensitive) (unqualified)) (missing_type))))
        """
    )

    @Test
    fun selectCommaCrossJoin1() = assertExpression(
        "SELECT a FROM table1, table2",
        """
            (select 
                (project (project_list (project_expr (id a (case_insensitive) (unqualified)) null))) 
                (from 
                    (join 
                        (inner) 
                        (scan (id table1 (case_insensitive) (unqualified)) null null null)
                        (scan (id table2 (case_insensitive) (unqualified)) null null null)
                        null)))
        """
    )

    @Test
    fun selectCommaCrossJoin2() = assertExpression(
        "SELECT a FROM table1, table2, table3",
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
        """
    )

    @Test
    fun selectMultipleWithMultipleFromSimpleWhere() = assertExpression(
        "SELECT a, b FROM table1 as t1, table2 WHERE f(t1)",
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
        """
    )

    @Test
    fun selectMultipleWithMultipleFromSimpleWhereNoAsAlias() = assertExpression(
        "SELECT a a1, b b1 FROM table1 t1, table2 WHERE f(t1)",
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
        """
    )

    @Test
    fun selectListWithAggregateWildcardCall() = assertExpression(
        "SELECT sum(a) + count(*), AVG(b), MIN(c), MAX(d + e) FROM foo",
        """
        (select
          (project
            (project_list
              (project_expr (plus (call_agg (all) sum (id a (case_insensitive) (unqualified))) (call_agg (all) count (lit 1))) null)
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
        """
    )

    @Test
    fun selectValueWithSingleFrom() = assertExpression(
        "SELECT VALUE a FROM table1",
        "(select (project (project_value (id a (case_insensitive) (unqualified)))) (from (scan (id table1 (case_insensitive) (unqualified)) null null null)))"
    )

    @Test
    fun selectValueWithSingleAliasedFrom() = assertExpression(
        "SELECT VALUE v FROM table1 AS v",
        "(select (project (project_value (id v (case_insensitive) (unqualified)))) (from (scan (id table1 (case_insensitive) (unqualified)) v null null)))"
    )

    @Test
    fun selectAllValues() = assertExpression(
        "SELECT ALL VALUE v FROM table1 AS v",
        "(select (project (project_value (id v (case_insensitive) (unqualified)))) (from (scan (id table1 (case_insensitive) (unqualified)) v null null)))"
    )

    @Test
    fun selectDistinctValues() = assertExpression(
        "SELECT DISTINCT VALUE v FROM table1 AS v",
        "(select (setq (distinct)) (project (project_value (id v (case_insensitive) (unqualified)))) (from (scan (id table1 (case_insensitive) (unqualified)) v null null)))"
    )

    @Test
    fun nestedSelectNoWhere() = assertExpression(
        "SELECT * FROM (SELECT * FROM x).a",
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
        """
    )

    @Test
    fun nestedSelect() = assertExpression(
        "SELECT * FROM (SELECT * FROM x WHERE b).a",
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
        """
    )

    @Test
    fun selectLimit() = assertExpression(
        "SELECT * FROM a LIMIT 10",
        """(select
             (project (project_star))
             (from (scan (id a (case_insensitive) (unqualified)) null null null))
             (limit (lit 10))
           )
        """
    )

    @Test
    fun selectWhereLimit() = assertExpression(
        "SELECT * FROM a WHERE a = 5 LIMIT 10",
        """(select
             (project (project_star))
             (from (scan (id a (case_insensitive) (unqualified)) null null null))
             (where (eq (id a (case_insensitive) (unqualified)) (lit 5)))
             (limit (lit 10))
           )
        """
    )

    @Test
    fun selectWithParametersAndLiterals() = assertExpression(
        "SELECT ?, f.a from foo f where f.bar = ? and f.spam = 'eggs' and f.baz = ?",
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
        """
    )

    // ****************************************
    // ORDER BY
    // ****************************************
    @Test
    fun orderBySingleId() = assertExpression(
        "SELECT a FROM tb WHERE hk = 1 ORDER BY rk1",
        """(select 
            (project 
                (project_list 
                    (project_expr 
                        (id a (case_insensitive) (unqualified)) 
                        null)))
            (from 
                (scan 
                    (id tb (case_insensitive) (unqualified)) 
                    null null null)) 
            (where 
                (eq 
                    (id hk (case_insensitive) (unqualified)) 
                    (lit 1))) 
            (order 
                (order_by 
                    (sort_spec 
                        (id rk1 (case_insensitive) (unqualified)) 
                        null
                        null))))
        """
    )

    @Test
    fun orderByMultipleIds() = assertExpression(
        "SELECT a FROM tb WHERE hk = 1 ORDER BY rk1, rk2, rk3",
        """(select 
            (project 
                (project_list 
                    (project_expr 
                        (id a (case_insensitive) (unqualified)) 
                        null)))
            (from 
                (scan 
                    (id tb (case_insensitive) (unqualified)) 
                    null null null)) 
            (where 
                (eq 
                    (id hk (case_insensitive) (unqualified)) 
                    (lit 1))) 
            (order 
                (order_by
                    (sort_spec 
                        (id rk1 (case_insensitive) (unqualified)) 
                        null
                        null) 
                    (sort_spec 
                        (id rk2 (case_insensitive) (unqualified)) 
                        null
                        null) 
                    (sort_spec 
                        (id rk3 (case_insensitive) (unqualified)) 
                        null
                        null))))
        """
    )

    @Test
    fun orderBySingleIdDESC() = assertExpression(
        "SELECT a FROM tb WHERE hk = 1 ORDER BY rk1 DESC",
        """(select 
            (project 
                (project_list 
                    (project_expr 
                        (id a (case_insensitive) (unqualified)) 
                        null)))
            (from 
                (scan 
                    (id tb (case_insensitive) (unqualified)) 
                    null null null)) 
            (where 
                (eq 
                    (id hk (case_insensitive) (unqualified)) 
                    (lit 1))) 
            (order 
                (order_by 
                    (sort_spec 
                        (id rk1 (case_insensitive) (unqualified)) 
                        (desc)
                        null))))
        """
    )

    @Test
    fun orderByMultipleIdsWithOrderingSpec() = assertExpression(
        "SELECT a FROM tb WHERE hk = 1 ORDER BY rk1 ASC, rk2 DESC",
        """(select 
            (project 
                (project_list 
                    (project_expr 
                        (id a (case_insensitive) (unqualified)) 
                        null)))
            (from 
                (scan 
                    (id tb (case_insensitive) (unqualified)) 
                    null null null)) 
            (where 
                (eq 
                    (id hk (case_insensitive) (unqualified)) 
                    (lit 1))) 
            (order 
                (order_by 
                    (sort_spec 
                        (id rk1 (case_insensitive) (unqualified)) 
                        (asc)
                        null)
                    (sort_spec 
                        (id rk2 (case_insensitive) (unqualified)) 
                        (desc)
                        null))))
        """
    )

    @Test
    fun orderBySingleIdWithoutOrderingAndNullsSpec() = assertExpression("SELECT x FROM tb ORDER BY rk1") {
        select(
            project = projectX,
            from = scan(id("tb")),
            order = orderBy(
                listOf(
                    sortSpec(id("rk1"))
                )
            )
        )
    }

    @Test
    fun orderByMultipleIdWithoutOrderingAndNullsSpec() =
        assertExpression("SELECT x FROM tb ORDER BY rk1, rk2, rk3, rk4") {
            select(
                project = projectX,
                from = scan(id("tb")),
                order = orderBy(
                    listOf(
                        sortSpec(id("rk1")),
                        sortSpec(id("rk2")),
                        sortSpec(id("rk3")),
                        sortSpec(id("rk4"))
                    )
                )
            )
        }

    @Test
    fun orderByWithAsc() = assertExpression("SELECT x FROM tb ORDER BY rk1 asc") {
        select(
            project = projectX,
            from = scan(id("tb")),
            order = orderBy(
                listOf(
                    sortSpec(id("rk1"), asc())
                )
            )
        )
    }

    @Test
    fun orderByWithDesc() = assertExpression("SELECT x FROM tb ORDER BY rk1 desc") {
        select(
            project = projectX,
            from = scan(id("tb")),
            order = orderBy(
                listOf(
                    sortSpec(id("rk1"), desc())
                )
            )
        )
    }

    @Test
    fun orderByWithAscAndDesc() = assertExpression("SELECT x FROM tb ORDER BY rk1 desc, rk2 asc, rk3 asc, rk4 desc") {
        select(
            project = projectX,
            from = scan(id("tb")),
            order = orderBy(
                listOf(
                    sortSpec(id("rk1"), desc()),
                    sortSpec(id("rk2"), asc()),
                    sortSpec(id("rk3"), asc()),
                    sortSpec(id("rk4"), desc())
                )
            )
        )
    }

    @Test
    fun orderByNoAscOrDescWithNullsFirst() = assertExpression("SELECT x FROM tb ORDER BY rk1 NULLS FIRST") {
        select(
            project = projectX,
            from = scan(id("tb")),
            order = orderBy(
                listOf(
                    sortSpec(id("rk1"), null, nullsFirst())
                )
            )
        )
    }

    @Test
    fun orderByNoAscOrDescWithNullsLast() = assertExpression("SELECT x FROM tb ORDER BY rk1 NULLS LAST") {
        select(
            project = projectX,
            from = scan(id("tb")),
            order = orderBy(
                listOf(
                    sortSpec(id("rk1"), null, nullsLast())
                )
            )
        )
    }

    @Test
    fun orderByAscWithNullsSpec() =
        assertExpression("SELECT x FROM tb ORDER BY rk1 asc NULLS FIRST, rk2 asc NULLS LAST") {
            select(
                project = projectX,
                from = scan(id("tb")),
                order = orderBy(
                    listOf(
                        sortSpec(id("rk1"), asc(), nullsFirst()),
                        sortSpec(id("rk2"), asc(), nullsLast())
                    )
                )
            )
        }

    @Test
    fun orderByDescWithNullsSpec() =
        assertExpression("SELECT x FROM tb ORDER BY rk1 desc NULLS FIRST, rk2 desc NULLS LAST") {
            select(
                project = projectX,
                from = scan(id("tb")),
                order = orderBy(
                    listOf(
                        sortSpec(id("rk1"), desc(), nullsFirst()),
                        sortSpec(id("rk2"), desc(), nullsLast())
                    )
                )
            )
        }

    @Test
    fun orderByWithOrderingAndNullsSpec() =
        assertExpression("SELECT x FROM tb ORDER BY rk1 desc NULLS FIRST, rk2 asc NULLS LAST, rk3 desc NULLS LAST, rk4 asc NULLS FIRST") {
            select(
                project = projectX,
                from = scan(id("tb")),
                order = orderBy(
                    listOf(
                        sortSpec(id("rk1"), desc(), nullsFirst()),
                        sortSpec(id("rk2"), asc(), nullsLast()),
                        sortSpec(id("rk3"), desc(), nullsLast()),
                        sortSpec(id("rk4"), asc(), nullsFirst())
                    )
                )
            )
        }

    // ****************************************
    // GROUP BY and GROUP PARTIAL BY
    // ****************************************
    @Test
    fun groupBySingleId() = assertExpression(
        "SELECT a FROM data GROUP BY a",
        """(select
             (project (project_list (project_expr (id a (case_insensitive) (unqualified)) null)))
             (from (scan (id data (case_insensitive) (unqualified)) null null null))
             (group (group_by (group_full) (group_key_list (group_key (id a (case_insensitive) (unqualified)) null)) null))
           )
        """
    )

    @Test
    fun groupBySingleExpr() = assertExpression(
        "SELECT a + b FROM data GROUP BY a + b",
        """(select
             (project (project_list (project_expr (plus (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) null)))
             (from (scan (id data (case_insensitive) (unqualified)) null null null))
             (group (group_by (group_full) (group_key_list (group_key (plus (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))) null)) null))
           )
        """
    )

    @Test
    fun groupPartialByMultiAliasedAndGroupAliased() = assertExpression(
        "SELECT g FROM data GROUP PARTIAL BY a AS x, b + c AS y, foo(d) AS z GROUP AS g",
        """(select
             (project (project_list (project_expr (id g (case_insensitive) (unqualified)) null)))
             (from (scan (id data (case_insensitive) (unqualified)) null null null))
             (group
                (group_by
                    (group_partial)
                    (group_key_list
                        (group_key
                            (id a (case_insensitive) (unqualified))
                            x)
                        (group_key
                            (plus
                                (id b (case_insensitive) (unqualified))
                                (id c (case_insensitive) (unqualified)))
                            y)
                        (group_key
                            (call
                                foo
                                (id d (case_insensitive) (unqualified)))
                            z)
                        )
                    g
                )
             )
           )
        """
    )

    // ****************************************
    // HAVING
    // ****************************************
    @Test
    fun havingMinimal() = assertExpression(
        "SELECT a FROM data HAVING a = b",
        """
          (select
            (project (project_list (project_expr (id a (case_insensitive) (unqualified)) null)))
            (from (scan (id data (case_insensitive) (unqualified)) null null null))
            (having (eq (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))))
          )
        """
    )

    @Test
    fun havingWithWhere() = assertExpression(
        "SELECT a FROM data WHERE a = b HAVING c = d",
        """
          (select
            (project (project_list (project_expr (id a (case_insensitive) (unqualified)) null)))
            (from (scan (id data (case_insensitive) (unqualified)) null null null))
            (where (eq (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))))
            (having (eq (id c (case_insensitive) (unqualified)) (id d (case_insensitive) (unqualified))))
          )
        """
    )

    @Test
    fun havingWithWhereAndGroupBy() = assertExpression(
        "SELECT g FROM data WHERE a = b GROUP BY c, d GROUP AS g HAVING d > 6",
        """
          (select
            (project (project_list (project_expr (id g (case_insensitive) (unqualified)) null)))
            (from (scan (id data (case_insensitive) (unqualified)) null null null))
            (where (eq (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))))
            (group (group_by (group_full) (group_key_list (group_key (id c (case_insensitive) (unqualified)) null) (group_key (id d (case_insensitive) (unqualified)) null)) g))
            (having (gt (id d (case_insensitive) (unqualified)) (lit 6)))
          )
        """
    )

    // ****************************************
    // PIVOT
    // ****************************************
    @Test
    fun pivotWithOnlyFrom() = assertExpression(
        "PIVOT v AT n FROM data",
        """
          (select
            (project
                (project_pivot 
                    (id n (case_insensitive) (unqualified)) 
                    (id v (case_insensitive) (unqualified))))
            (from (scan (id data (case_insensitive) (unqualified)) null null null))
          )
        """
    )

    @Test
    fun pivotHavingWithWhereAndGroupBy() = assertExpression(
        "PIVOT g AT ('prefix:' || c) FROM data WHERE a = b GROUP BY c, d GROUP AS g HAVING d > 6",
        """
          (select
            (project 
              (project_pivot
                (concat (lit "prefix:") (id c (case_insensitive) (unqualified))) 
                (id g (case_insensitive) (unqualified))))
            (from (scan (id data (case_insensitive) (unqualified)) null null null))
            (where (eq (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))))
            (group (group_by (group_full) (group_key_list (group_key (id c (case_insensitive) (unqualified)) null) (group_key (id d (case_insensitive) (unqualified)) null)) g))
            (having (gt (id d (case_insensitive) (unqualified)) (lit 6)))
          )
        """
    )

    // ****************************************
    // DML
    // ****************************************

    @Test
    fun fromInsertValuesDml() = assertExpression(
        "FROM x INSERT INTO foo VALUES (1, 2), (3, 4)",
        """
        (dml
            (operations
                (dml_op_list
                    (insert
                        (id foo (case_insensitive) (unqualified))
                        null
                        (bag
                            (list
                                (lit 1)
                                (lit 2))
                            (list
                                (lit 3)
                                (lit 4)))
                        null)))
            (from
                (scan
                    (id x (case_insensitive) (unqualified))
                    null
                    null
                    null)))
        """
    )

    @Test
    fun fromInsertValueAtDml() = assertExpression(
        "FROM x INSERT INTO foo VALUE 1 AT bar",
        """
          (dml
            (operations
              (dml_op_list
                (insert_value
                  (id foo (case_insensitive) (unqualified))
                  (lit 1)
                  (id bar (case_insensitive) (unqualified))
                  null
                )
              )
            )
            (from (scan (id x (case_insensitive) (unqualified)) null null null))
          )
        """
    )

    @Test
    @Ignore
    fun fromInsertValueAtReturningDml() = assertExpression(
        "FROM x INSERT INTO foo VALUE 1 AT bar RETURNING ALL OLD foo",
        """
          (dml
            (operations
              (dml_op_list
                (insert_value
                  (id foo (case_insensitive) (unqualified))
                  (lit 1)
                  (id bar (case_insensitive) (unqualified))
                )
              )
            )
            (from (scan (id x (case_insensitive) (unqualified)) null null null))
          )
        """
    )

    @Test
    fun fromInsertValueDml() = assertExpression(
        "FROM x INSERT INTO foo VALUE 1",
        """
          (dml
            (operations
              (dml_op_list
                (insert_value
                  (id foo (case_insensitive) (unqualified))
                  (lit 1)
                  null null)))
            (from (scan (id x (case_insensitive) (unqualified)) null null null))
          )
        """
    )

    @Test
    @Ignore
    fun fromInsertValueReturningDml() = assertExpression(
        "FROM x INSERT INTO foo VALUE 1 RETURNING ALL OLD foo",
        """
          (dml
            (dml_op_list
              (insert_value
                (id foo case_insensitive)
                (lit 1))
            )
            (from (id x case_insensitive))
          )
          """
    )

    @Test
    fun fromInsertQueryDml() = assertExpression(
        "FROM x INSERT INTO foo SELECT y FROM bar",
        """
        (dml
            (operations
                (dml_op_list
                    (insert
                        (id foo (case_insensitive) (unqualified))
                        null
                        (select
                            (project
                                (project_list
                                    (project_expr
                                        (id y (case_insensitive) (unqualified))
                                        null)))
                            (from
                                (scan
                                    (id bar (case_insensitive) (unqualified))
                                    null
                                    null
                                    null)))
                        null)))
            (from
                (scan
                    (id x (case_insensitive) (unqualified))
                    null
                    null
                    null)))
        """
    )

    @Test
    fun insertValueDml() = assertExpression(
        "INSERT INTO foo VALUE 1",
        """
          (dml
            (operations
              (dml_op_list
                (insert_value
                  (id foo (case_insensitive) (unqualified))
                  (lit 1)
                  null null
                )
              )
            )
          )
        """
    )

    @Test
    fun insertValueReturningDml() = assertExpression(
        "INSERT INTO foo VALUE 1 RETURNING MODIFIED OLD foo",
        """
        (dml 
            (operations 
                (dml_op_list (insert_value (id foo (case_insensitive) (unqualified)) (lit 1) null null)))
                (returning 
                    (returning_expr 
                        (returning_elem 
                            (modified_old) 
                            (returning_column (id foo (case_insensitive) (unqualified)))))))
        """
    )

    @Test
    fun insertValueReturningStarDml() = assertExpression(
        "INSERT INTO foo VALUE 1 RETURNING ALL OLD *",
        """
        (dml 
            (operations 
                (dml_op_list (insert_value (id foo (case_insensitive) (unqualified)) (lit 1) null null)))
                (returning 
                    (returning_expr 
                        (returning_elem 
                            (all_old) 
                            (returning_wildcard)))))
        """
    )

    @Test
    fun insertValuesDml() = assertExpression(
        "INSERT INTO foo VALUES (1, 2), (3, 4)",
        """
        (dml
            (operations
                (dml_op_list
                    (insert
                        (id foo (case_insensitive) (unqualified))
                        null
                        (bag
                            (list
                                (lit 1)
                                (lit 2))
                            (list
                                (lit 3)
                                (lit 4)))
                        null))))
        """
    )

    @Test
    fun insertValueAtDml() = assertExpression(
        "INSERT INTO foo VALUE 1 AT bar",
        """
          (dml
            (operations
              (dml_op_list
                (insert_value
                  (id foo (case_insensitive) (unqualified))
                  (lit 1)
                  (id bar (case_insensitive) (unqualified))
                  null
                )
              )
            )
          )
        """
    )

    @Test
    fun insertValueAtReturningDml() = assertExpression(
        "INSERT INTO foo VALUE 1 AT bar RETURNING ALL OLD foo",
        """
        (dml 
            (operations 
                (dml_op_list (insert_value (id foo (case_insensitive) (unqualified)) 
                (lit 1) (id bar (case_insensitive) (unqualified)) null)))
                (returning 
                    (returning_expr 
                        (returning_elem 
                            (all_old) 
                            (returning_column (id foo (case_insensitive) (unqualified)))))))
        """
    )

    @Test
    fun insertValueAtMultiReturningTwoColsDml() = assertExpression(
        "INSERT INTO foo VALUE 1 AT bar RETURNING ALL OLD a",
        """
        (dml 
            (operations 
                (dml_op_list (insert_value (id foo (case_insensitive) (unqualified)) 
                (lit 1) (id bar (case_insensitive) (unqualified)) null)))
                (returning 
                    (returning_expr 
                        (returning_elem 
                            (all_old) 
                            (returning_column (id a (case_insensitive) (unqualified)))))))
        """
    )

    @Test
    fun insertValueAtMultiReturningThreeColsDml() = assertExpression(
        "INSERT INTO foo VALUE 1 AT bar RETURNING MODIFIED OLD bar, MODIFIED NEW bar, ALL NEW *",
        """
            (dml 
                (operations 
                    (dml_op_list (insert_value (id foo (case_insensitive) (unqualified)) 
                    (lit 1) (id bar (case_insensitive) (unqualified)) null)))
                (returning 
                    (returning_expr 
                        (returning_elem 
                            (modified_old) 
                            (returning_column (id bar (case_insensitive) (unqualified)))) 
                        (returning_elem 
                            (modified_new) 
                            (returning_column (id bar (case_insensitive) (unqualified)))) 
                        (returning_elem 
                            (all_new) 
                            (returning_wildcard)))))
        """
    )

    @Test
    fun insertValueAtOnConflictDml() = assertExpression(
        "INSERT INTO foo VALUE 1 AT bar ON CONFLICT WHERE a DO NOTHING",
        """
          (dml
            (operations (dml_op_list
              (insert_value
                (id foo (case_insensitive) (unqualified))
                (lit 1)
                (id bar (case_insensitive) (unqualified))
                (on_conflict
                    (id a (case_insensitive) (unqualified))
                    (do_nothing)
                )
              )
            ))
          )
        """
    )

    @Test
    fun insertValueAtOnConflictReturningDml() = assertExpression(
        "INSERT INTO foo VALUE 1 AT bar ON CONFLICT WHERE a DO NOTHING RETURNING ALL OLD foo",
        """
      (dml
        (operations (dml_op_list
          (insert_value
            (id foo (case_insensitive) (unqualified))
            (lit 1)
            (id bar (case_insensitive) (unqualified))
            (on_conflict
                (id a (case_insensitive) (unqualified))
                (do_nothing)))))
        (returning 
            (returning_expr 
                (returning_elem 
                    (all_old) 
                    (returning_column (id foo (case_insensitive) (unqualified)))))))
        """
    )

    @Test
    fun insertValueOnConflictDml() = assertExpression(
        "INSERT INTO foo VALUE 1 ON CONFLICT WHERE bar DO NOTHING",
        """
          (dml
            (operations (dml_op_list
              (insert_value
                (id foo (case_insensitive) (unqualified))
                (lit 1)
                null
                (on_conflict
                    (id bar (case_insensitive) (unqualified))
                    (do_nothing)
                )
              )
            ))
          )
        """
    )

    @Test
    fun insertValueOnConflictExpr1Dml() = assertExpression(
        "INSERT INTO foo VALUE 1 ON CONFLICT WHERE hk=1 DO NOTHING",
        """
          (dml
            (operations (dml_op_list
              (insert_value
                (id foo (case_insensitive) (unqualified))
                (lit 1)
                null
                (on_conflict
                    (eq (id hk (case_insensitive) (unqualified)) (lit 1))
                    (do_nothing)
                )
              )
            ))
          )
        """
    )

    @Test
    fun insertValueOnConflictExpr2Dml() = assertExpression(
        "INSERT INTO foo VALUE 1 ON CONFLICT WHERE hk=1 and rk=1 DO NOTHING",
        """
          (dml
            (operations (dml_op_list
              (insert_value
                (id foo (case_insensitive) (unqualified))
                (lit 1)
                null
                (on_conflict
                    (and (eq (id hk (case_insensitive) (unqualified)) (lit 1)) (eq (id rk (case_insensitive) (unqualified)) (lit 1)))
                    (do_nothing)
                )
              )
            ))
          )
        """
    )

    @Test
    fun insertValueOnConflictExpr3Dml() = assertExpression(
        "INSERT INTO foo VALUE 1 ON CONFLICT WHERE hk BETWEEN 'a' and 'b' or rk = 'c' DO NOTHING",
        """
          (dml
            (operations (dml_op_list
              (insert_value
                (id foo (case_insensitive) (unqualified))
                (lit 1)
                null
                (on_conflict
                    (or (between (id hk (case_insensitive) (unqualified)) (lit "a") (lit "b")) (eq (id rk (case_insensitive) (unqualified)) (lit "c")))
                    (do_nothing)
                )
              )
            ))
          )
        """
    )

    @Test
    fun insertValueOnConflictExpr4Dml() = assertExpression(
        "INSERT INTO foo VALUE 1 ON CONFLICT WHERE not hk = 'a' DO NOTHING",
        """
          (dml
            (operations (dml_op_list
              (insert_value
                (id foo (case_insensitive) (unqualified))
                (lit 1)
                null
                (on_conflict
                    (not (eq (id hk (case_insensitive) (unqualified)) (lit "a")))
                    (do_nothing)
                )
              )
            ))
          )
        """
    )

    @Test
    fun insertValueOnConflictExpr5Dml() = assertExpression(
        "INSERT INTO foo VALUE 1 ON CONFLICT WHERE attribute_exists(hk) DO NOTHING",
        """
          (dml
            (operations (dml_op_list 
              (insert_value
                (id foo (case_insensitive) (unqualified))
                (lit 1)
                null
                (on_conflict
                    (call attribute_exists (id hk (case_insensitive) (unqualified)))
                    (do_nothing)
                )
              )
            ))
          )
        """
    )

    @Test
    fun insertValueOnConflictExpr6Dml() = assertExpression(
        "INSERT INTO foo VALUE 1 ON CONFLICT WHERE not attribute_exists(hk) DO NOTHING",
        """
          (dml
            (operations (dml_op_list
              (insert_value
                (id foo (case_insensitive) (unqualified))
                (lit 1)
                null
                (on_conflict
                    (not (call attribute_exists (id hk (case_insensitive) (unqualified))))
                    (do_nothing)
                )
              )
            ))
          )
        """
    )

    @Test
    fun insertQueryDml() = assertExpression(
        "INSERT INTO foo SELECT y FROM bar",
        """
        (dml
            (operations
                (dml_op_list
                    (insert
                        (id foo (case_insensitive) (unqualified))
                        null
                        (select
                            (project
                                (project_list
                                    (project_expr
                                        (id y (case_insensitive) (unqualified))
                                        null)))
                            (from
                                (scan
                                    (id bar (case_insensitive) (unqualified))
                                    null
                                    null
                                    null)))
                        null))))
        """
    )

    @Test
    fun insertWithOnConflictReplaceExcludedWithLiteralValue() = assertExpression(
        source = "INSERT into foo VALUES (1, 2), (3, 4) ON CONFLICT DO REPLACE EXCLUDED",
        expectedPigAst = """
        (dml
            (operations
                (dml_op_list
                    (insert
                        (id foo (case_insensitive) (unqualified))
                        null
                        (bag
                            (list
                                (lit 1)
                                (lit 2))
                            (list
                                (lit 3)
                                (lit 4)))
                        (do_replace
                            (excluded)
                            null
                        )))))
        """
    )

    @Test
    fun insertWithOnConflictReplaceExcludedWithLiteralValueAndCondition() = assertExpression(
        source = "INSERT into foo VALUES (1, 2), (3, 4) ON CONFLICT DO REPLACE EXCLUDED WHERE foo.id > 2",
        expectedPigAst = """
        (dml
            (operations
                (dml_op_list
                    (insert
                        (id foo (case_insensitive) (unqualified))
                        null
                        (bag
                            (list
                                (lit 1)
                                (lit 2))
                            (list
                                (lit 3)
                                (lit 4)))
                        (do_replace
                            (excluded)
                            (gt
                                (path
                                    (id foo (case_insensitive) (unqualified))
                                    (path_expr
                                        (lit "id")
                                        (case_insensitive)))
                                (lit 2)
                            )
                        )))))
        """
    )

    @Test
    fun insertWithOnConflictReplaceExcludedWithExcludedInCondition() = assertExpression(
        source = "INSERT into foo VALUES (1, 2), (3, 4) ON CONFLICT DO REPLACE EXCLUDED WHERE excluded.id > 2",
        expectedPigAst = """
        (dml
            (operations
                (dml_op_list
                    (insert
                        (id foo (case_insensitive) (unqualified))
                        null
                        (bag
                            (list
                                (lit 1)
                                (lit 2))
                            (list
                                (lit 3)
                                (lit 4)))
                        (do_replace
                            (excluded)
                            (gt
                                (path
                                    (id excluded (case_insensitive) (unqualified))
                                    (path_expr
                                        (lit "id")
                                        (case_insensitive)))
                                (lit 2)
                            )
                        )))))
        """
    )

    @Test
    fun insertWithOnConflictReplaceExcludedWithLiteralValueWithAlias() = assertExpression(
        source = "INSERT into foo AS f <<{'id': 1, 'name':'bob'}>> ON CONFLICT DO REPLACE EXCLUDED",
        expectedPigAst = """
            (dml
                (operations
                    (dml_op_list
                        (insert
                            (id foo (case_insensitive) (unqualified))
                            f
                            (bag
                                (struct
                                    (expr_pair
                                        (lit "id")
                                        (lit 1))
                                    (expr_pair
                                        (lit "name")
                                        (lit "bob"))))
                            (do_replace
                                (excluded)
                                null
                            )))))
        """
    )

    @Test
    fun insertWithOnConflictReplaceExcludedWithLiteralValueWithAliasAndCondition() = assertExpression(
        source = "INSERT into foo AS f <<{'id': 1, 'name':'bob'}>> ON CONFLICT DO REPLACE EXCLUDED WHERE f.id > 2",
        expectedPigAst = """
            (dml
                (operations
                    (dml_op_list
                        (insert
                            (id foo (case_insensitive) (unqualified))
                            f
                            (bag
                                (struct
                                    (expr_pair
                                        (lit "id")
                                        (lit 1))
                                    (expr_pair
                                        (lit "name")
                                        (lit "bob"))))
                            (do_replace
                                (excluded)
                            (gt
                                (path
                                    (id f (case_insensitive) (unqualified))
                                    (path_expr
                                        (lit "id")
                                        (case_insensitive)))
                                (lit 2)
                            )
                            )))))
        """
    )

    @Test
    fun insertWithOnConflictReplaceExcludedWithAliasAndExcludedInCondition() = assertExpression(
        source = "INSERT into foo AS f <<{'id': 1, 'name':'bob'}>> ON CONFLICT DO REPLACE EXCLUDED WHERE excluded.id > 2",
        expectedPigAst = """
            (dml
                (operations
                    (dml_op_list
                        (insert
                            (id foo (case_insensitive) (unqualified))
                            f
                            (bag
                                (struct
                                    (expr_pair
                                        (lit "id")
                                        (lit 1))
                                    (expr_pair
                                        (lit "name")
                                        (lit "bob"))))
                            (do_replace
                                (excluded)
                            (gt
                                (path
                                    (id excluded (case_insensitive) (unqualified))
                                    (path_expr
                                        (lit "id")
                                        (case_insensitive)))
                                (lit 2)
                            )
                            )))))
        """
    )

    @Test
    fun insertWithOnConflictReplaceExcludedWithSelect() = assertExpression(
        source = "INSERT into foo SELECT bar.id, bar.name FROM bar ON CONFLICT DO REPLACE EXCLUDED",
        expectedPigAst = """
            (dml
                (operations
                    (dml_op_list
                        (insert
                            (id foo (case_insensitive) (unqualified))
                            null
                            (select
                                (project
                                    (project_list
                                        (project_expr
                                            (path
                                                (id bar (case_insensitive) (unqualified))
                                                (path_expr
                                                    (lit "id")
                                                    (case_insensitive)))
                                            null)
                                        (project_expr
                                            (path
                                                (id bar (case_insensitive) (unqualified))
                                                (path_expr
                                                    (lit "name")
                                                    (case_insensitive)))
                                            null)))
                                (from
                                    (scan
                                        (id bar (case_insensitive) (unqualified))
                                        null
                                        null
                                        null)))
                            (do_replace
                                (excluded)
                                null
                            )))))
        """
    )

    @Test
    fun insertWithOnConflictUpdateExcludedWithLiteralValue() = assertExpression(
        source = "INSERT into foo VALUES (1, 2), (3, 4) ON CONFLICT DO UPDATE EXCLUDED",
        expectedPigAst = """
        (dml
            (operations
                (dml_op_list
                    (insert
                        (id foo (case_insensitive) (unqualified))
                        null
                        (bag
                            (list
                                (lit 1)
                                (lit 2))
                            (list
                                (lit 3)
                                (lit 4)))
                        (do_update
                            (excluded)
                            null
                        )))))
        """
    )

    @Test
    fun insertWithOnConflictUpdateExcludedWithLiteralValueAndCondition() = assertExpression(
        source = "INSERT into foo VALUES (1, 2), (3, 4) ON CONFLICT DO UPDATE EXCLUDED WHERE foo.id > 2",
        expectedPigAst = """
        (dml
            (operations
                (dml_op_list
                    (insert
                        (id foo (case_insensitive) (unqualified))
                        null
                        (bag
                            (list
                                (lit 1)
                                (lit 2))
                            (list
                                (lit 3)
                                (lit 4)))
                        (do_update
                            (excluded)
                            (gt
                                (path
                                    (id foo (case_insensitive) (unqualified))
                                    (path_expr
                                        (lit "id")
                                        (case_insensitive)))
                                (lit 2)
                            )
                        )))))
        """
    )

    @Test
    fun insertWithOnConflictUpdateExcludedWithExcludedCondition() = assertExpression(
        source = "INSERT into foo VALUES (1, 2), (3, 4) ON CONFLICT DO UPDATE EXCLUDED WHERE excluded.id > 2",
        expectedPigAst = """
        (dml
            (operations
                (dml_op_list
                    (insert
                        (id foo (case_insensitive) (unqualified))
                        null
                        (bag
                            (list
                                (lit 1)
                                (lit 2))
                            (list
                                (lit 3)
                                (lit 4)))
                        (do_update
                            (excluded)
                            (gt
                                (path
                                    (id excluded (case_insensitive) (unqualified))
                                    (path_expr
                                        (lit "id")
                                        (case_insensitive)))
                                (lit 2)
                            )
                        )))))
        """
    )

    @Test
    fun insertWithOnConflictUpdateExcludedWithLiteralValueWithAlias() = assertExpression(
        source = "INSERT into foo AS f <<{'id': 1, 'name':'bob'}>> ON CONFLICT DO UPDATE EXCLUDED",
        expectedPigAst = """
            (dml
                (operations
                    (dml_op_list
                        (insert
                            (id foo (case_insensitive) (unqualified))
                            f
                            (bag
                                (struct
                                    (expr_pair
                                        (lit "id")
                                        (lit 1))
                                    (expr_pair
                                        (lit "name")
                                        (lit "bob"))))
                            (do_update
                                (excluded)
                                null
                            )))))
        """
    )

    @Test
    fun insertWithOnConflictUpdateExcludedWithLiteralValueWithAliasAndCondition() = assertExpression(
        source = "INSERT into foo AS f <<{'id': 1, 'name':'bob'}>> ON CONFLICT DO UPDATE EXCLUDED WHERE f.id > 2",
        expectedPigAst = """
            (dml
                (operations
                    (dml_op_list
                        (insert
                            (id foo (case_insensitive) (unqualified))
                            f
                            (bag
                                (struct
                                    (expr_pair
                                        (lit "id")
                                        (lit 1))
                                    (expr_pair
                                        (lit "name")
                                        (lit "bob"))))
                            (do_update
                                (excluded)
                            (gt
                                (path
                                    (id f (case_insensitive) (unqualified))
                                    (path_expr
                                        (lit "id")
                                        (case_insensitive)))
                                (lit 2)
                            )
                            )))))
        """
    )

    @Test
    fun insertWithOnConflictUpdateExcludedWithAliasAndExcludedCondition() = assertExpression(
        source = "INSERT into foo AS f <<{'id': 1, 'name':'bob'}>> ON CONFLICT DO UPDATE EXCLUDED WHERE excluded.id > 2",
        expectedPigAst = """
            (dml
                (operations
                    (dml_op_list
                        (insert
                            (id foo (case_insensitive) (unqualified))
                            f
                            (bag
                                (struct
                                    (expr_pair
                                        (lit "id")
                                        (lit 1))
                                    (expr_pair
                                        (lit "name")
                                        (lit "bob"))))
                            (do_update
                                (excluded)
                            (gt
                                (path
                                    (id excluded (case_insensitive) (unqualified))
                                    (path_expr
                                        (lit "id")
                                        (case_insensitive)))
                                (lit 2)
                            )
                            )))))
        """
    )

    @Test
    fun insertWithOnConflictUpdateExcludedWithSelect() = assertExpression(
        source = "INSERT into foo SELECT bar.id, bar.name FROM bar ON CONFLICT DO UPDATE EXCLUDED",
        expectedPigAst = """
            (dml
                (operations
                    (dml_op_list
                        (insert
                            (id foo (case_insensitive) (unqualified))
                            null
                            (select
                                (project
                                    (project_list
                                        (project_expr
                                            (path
                                                (id bar (case_insensitive) (unqualified))
                                                (path_expr
                                                    (lit "id")
                                                    (case_insensitive)))
                                            null)
                                        (project_expr
                                            (path
                                                (id bar (case_insensitive) (unqualified))
                                                (path_expr
                                                    (lit "name")
                                                    (case_insensitive)))
                                            null)))
                                (from
                                    (scan
                                        (id bar (case_insensitive) (unqualified))
                                        null
                                        null
                                        null)))
                            (do_update
                                (excluded)
                                null
                            )))))
        """
    )

    @Test
    fun insertWithOnConflictDoNothing() = assertExpression(
        source = "INSERT into foo <<{'id': 1, 'name':'bob'}>> ON CONFLICT DO NOTHING",
        expectedPigAst = """
            (dml
                (operations
                    (dml_op_list
                        (insert
                            (id foo (case_insensitive) (unqualified))
                            null
                            (bag
                                (struct
                                    (expr_pair
                                        (lit "id")
                                        (lit 1))
                                    (expr_pair
                                        (lit "name")
                                        (lit "bob"))))
                            (do_nothing)))))
        """
    )

    @Test
    fun attemptConditionWithInsertDoNothing() = checkInputThrowingParserException(
        "INSERT into foo <<{'id': 1, 'name':'bob'}>> ON CONFLICT DO NOTHING WHERE foo.id > 2",
        ErrorCode.PARSE_UNEXPECTED_TOKEN,
        expectErrorContextValues = mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 68L,
            Property.TOKEN_DESCRIPTION to PartiQLParser.WHERE.getAntlrDisplayString(),
            Property.TOKEN_VALUE to ION.newSymbol("WHERE")
        )
    )

    @Test
    fun mixAndMatchInsertWithLegacy() = checkInputThrowingParserException(
        "INSERT INTO foo <<{'id': 1, 'name':'bob'}>> ON CONFLICT WHERE TRUE DO NOTHING",
        ErrorCode.PARSE_UNEXPECTED_TOKEN,
        expectErrorContextValues = mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 57L,
            Property.TOKEN_DESCRIPTION to PartiQLParser.WHERE.getAntlrDisplayString(),
            Property.TOKEN_VALUE to ION.newSymbol("WHERE")
        )
    )

    @Test
    fun mixAndMatchInsertLegacyWithCurrent() = checkInputThrowingParserException(
        "INSERT INTO foo VALUE {'id': 1, 'name':'bob'} ON CONFLICT DO UPDATE EXCLUDED",
        ErrorCode.PARSE_UNEXPECTED_TOKEN,
        expectErrorContextValues = mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 59L,
            Property.TOKEN_DESCRIPTION to PartiQLParser.DO.getAntlrDisplayString(),
            Property.TOKEN_VALUE to ION.newSymbol("DO")
        )
    )

    @Test
    fun insertWithOnConflictDoNothingWithLiteralValueWithAlias() = assertExpression(
        source = "INSERT into foo AS f <<{'id': 1, 'name':'bob'}>> ON CONFLICT DO NOTHING",
        expectedPigAst = """
            (dml
                (operations
                    (dml_op_list
                        (insert
                            (id foo (case_insensitive) (unqualified))
                            f
                            (bag
                                (struct
                                    (expr_pair
                                        (lit "id")
                                        (lit 1))
                                    (expr_pair
                                        (lit "name")
                                        (lit "bob"))))
                            (do_nothing)))))
        """
    )

    @Test
    fun insertWithOnConflictDoNothingWithSelect() = assertExpression(
        source = "INSERT into foo SELECT bar.id, bar.name FROM bar ON CONFLICT DO NOTHING",
        expectedPigAst = """
            (dml
                (operations
                    (dml_op_list
                        (insert
                            (id foo (case_insensitive) (unqualified))
                            null
                            (select
                                (project
                                    (project_list
                                        (project_expr
                                            (path
                                                (id bar (case_insensitive) (unqualified))
                                                (path_expr
                                                    (lit "id")
                                                    (case_insensitive)))
                                            null)
                                        (project_expr
                                            (path
                                                (id bar (case_insensitive) (unqualified))
                                                (path_expr
                                                    (lit "name")
                                                    (case_insensitive)))
                                            null)))
                                (from
                                    (scan
                                        (id bar (case_insensitive) (unqualified))
                                        null
                                        null
                                        null)))
                            (do_nothing)))))
        """
    )

    @Test
    fun replaceCommand() = assertExpression(
        source = "REPLACE INTO foo <<{'id': 1, 'name':'bob'}>>",
        expectedPigAst = """
            (dml
                (operations
                    (dml_op_list
                        (insert
                            (id foo (case_insensitive) (unqualified))
                            null
                            (bag
                                (struct
                                    (expr_pair
                                        (lit "id")
                                        (lit 1))
                                    (expr_pair
                                        (lit "name")
                                        (lit "bob"))))
                            (do_replace
                                (excluded)
                                null
                            )))))
        """
    )

    @Test
    fun replaceCommandWithAsAlias() = assertExpression(
        source = "REPLACE INTO foo As f <<{'id': 1, 'name':'bob'}>>",
        expectedPigAst = """
            (dml
                (operations
                    (dml_op_list
                        (insert
                            (id foo (case_insensitive) (unqualified))
                            f
                            (bag
                                (struct
                                    (expr_pair
                                        (lit "id")
                                        (lit 1))
                                    (expr_pair
                                        (lit "name")
                                        (lit "bob"))))
                            (do_replace
                                (excluded)
                                null
                            )))))
        """
    )

    @Test
    fun upsertCommand() = assertExpression(
        source = "UPSERT INTO foo <<{'id': 1, 'name':'bob'}>>",
        expectedPigAst = """
            (dml
                (operations
                    (dml_op_list
                        (insert
                            (id foo (case_insensitive) (unqualified))
                            null
                            (bag
                                (struct
                                    (expr_pair
                                        (lit "id")
                                        (lit 1))
                                    (expr_pair
                                        (lit "name")
                                        (lit "bob"))))
                            (do_update
                                (excluded)
                                null
                            )))))
        """
    )

    @Test
    fun upsertCommandWithAsAlias() = assertExpression(
        source = "UPSERT INTO foo As f <<{'id': 1, 'name':'bob'}>>",
        expectedPigAst = """
            (dml
                (operations
                    (dml_op_list
                        (insert
                            (id foo (case_insensitive) (unqualified))
                            f
                            (bag
                                (struct
                                    (expr_pair
                                        (lit "id")
                                        (lit 1))
                                    (expr_pair
                                        (lit "name")
                                        (lit "bob"))))
                            (do_update
                                (excluded)
                            null
                            )))))
        """
    )

    @Test
    fun replaceCommandWithSelect() = assertExpression(
        source = "REPLACE INTO foo SELECT bar.id, bar.name FROM bar",
        expectedPigAst = """
            (dml
                (operations
                    (dml_op_list
                        (insert
                            (id foo (case_insensitive) (unqualified))
                            null
                            (select
                                (project
                                    (project_list
                                        (project_expr
                                            (path
                                                (id bar (case_insensitive) (unqualified))
                                                (path_expr
                                                    (lit "id")
                                                    (case_insensitive)))
                                            null)
                                        (project_expr
                                            (path
                                                (id bar (case_insensitive) (unqualified))
                                                (path_expr
                                                    (lit "name")
                                                    (case_insensitive)))
                                            null)))
                                (from
                                    (scan
                                        (id bar (case_insensitive) (unqualified))
                                        null
                                        null
                                        null)))
                            (do_replace
                                (excluded)
                                null
                            )))))
        """
    )

    @Test
    fun upsertCommandWithSelect() = assertExpression(
        source = "UPSERT INTO foo SELECT bar.id, bar.name FROM bar",
        expectedPigAst = """
        (dml
            (operations
                (dml_op_list
                    (insert
                        (id foo (case_insensitive) (unqualified))
                        null
                        (select
                            (project
                                (project_list
                                    (project_expr
                                        (path
                                            (id bar (case_insensitive) (unqualified))
                                            (path_expr
                                                (lit "id")
                                                (case_insensitive)))
                                        null)
                                    (project_expr
                                        (path
                                            (id bar (case_insensitive) (unqualified))
                                            (path_expr
                                                (lit "name")
                                                (case_insensitive)))
                                        null)))
                            (from
                                (scan
                                    (id bar (case_insensitive) (unqualified))
                                    null
                                    null
                                    null)))
                        (do_update
                            (excluded)
                            null
                        )
                        ))))
        """
    )

    @Test
    @Ignore
    fun insertQueryReturningDml() = assertExpression(
        "INSERT INTO foo SELECT y FROM bar RETURNING ALL NEW foo",
        """
          (dml
            (operations
              (dml_op_list
                (insert
                  (id foo (case_insensitive) (unqualified))
                  (select
                    (project (project_list (project_expr (id y (case_insensitive) (unqualified)) null)))
                    (from (scan (id bar (case_insensitive) (unqualified)) null null null))
                  )
                )
              )
            )
          )
        """
    )

    @Test
    fun fromSetSingleDml() = assertExpression(
        "FROM x WHERE a = b SET k = 5",
        """
          (dml
            (operations
              (dml_op_list
                (set
                  (assignment
                    (id k (case_insensitive) (unqualified))
                    (lit 5)
                  )
                )
              )
            )
            (from (scan (id x (case_insensitive) (unqualified)) null null null))
            (where (eq (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))))
          )
        """
    )

    @Test
    fun fromSetSingleReturningDml() = assertExpression(
        "FROM x WHERE a = b SET k = 5 RETURNING ALL OLD x",
        """
        (dml (operations (dml_op_list (set (assignment (id k (case_insensitive) (unqualified)) (lit 5)))))
        (from (scan (id x (case_insensitive) (unqualified)) null null null))
        (where (eq (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))))
        (returning 
            (returning_expr 
                (returning_elem 
                    (all_old) 
                    (returning_column (id x (case_insensitive) (unqualified)))))))
        """
    )

    @Test
    fun fromSetSinglePathFieldDml() = assertExpression(
        "FROM x WHERE a = b SET k.m = 5",
        """
          (dml
            (operations
              (dml_op_list
                (set
                  (assignment
                    (path (id k (case_insensitive) (unqualified)) (path_expr (lit "m") (case_insensitive)))
                    (lit 5)
                  )
                )
              )
            )
            (from (scan (id x (case_insensitive) (unqualified)) null null null))
            (where (eq (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))))
          )
        """
    )

    @Test
    fun fromSetSinglePathStringIndexDml() = assertExpression(
        "FROM x WHERE a = b SET k['m'] = 5",
        """
          (dml
            (operations
              (dml_op_list
                (set
                  (assignment
                    (path (id k (case_insensitive) (unqualified)) (path_expr (lit "m") (case_sensitive)))
                    (lit 5)
                  )
                )
              )
            )
            (from (scan (id x (case_insensitive) (unqualified)) null null null))
            (where (eq (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))))
          )
        """
    )

    @Test
    fun fromSetSinglePathOrdinalDml() = assertExpression(
        "FROM x WHERE a = b SET k[3] = 5",
        """
          (dml
            (operations
              (dml_op_list
                (set
                  (assignment
                    (path (id k (case_insensitive) (unqualified)) (path_expr (lit 3) (case_sensitive)))
                    (lit 5)
                  )
                )
              )
            )
            (from (scan (id x (case_insensitive) (unqualified)) null null null))
            (where (eq (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))))
          )
        """
    )

    @Test
    fun fromSetMultiDml() = assertExpression(
        "FROM x WHERE a = b SET k = 5, m = 6",
        """
          (dml
            (operations
              (dml_op_list
                (set
                  (assignment
                    (id k (case_insensitive) (unqualified))
                    (lit 5)
                  )
                )
                (set
                  (assignment
                    (id m (case_insensitive) (unqualified))
                    (lit 6)
                  )
                )
              )
            )
            (from (scan (id x (case_insensitive) (unqualified)) null null null))
            (where (eq (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))))
          )
        """
    )

    @Test
    fun fromSetMultiReturningDml() = assertExpression(
        "FROM x WHERE a = b SET k = 5, m = 6 RETURNING ALL OLD x",
        """
        (dml (operations 
            (dml_op_list 
                (set (assignment (id k (case_insensitive) (unqualified)) (lit 5))) 
                (set (assignment (id m (case_insensitive) (unqualified)) (lit 6)))))
        (from (scan (id x (case_insensitive) (unqualified)) null null null))
        (where (eq (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))))
        (returning 
            (returning_expr 
                (returning_elem 
                    (all_old) 
                    (returning_column (id x (case_insensitive) (unqualified)))))))
        """
    )

    @Test
    fun fromComplexDml() = assertExpression(
        "FROM x WHERE a = b SET k = 5, m = 6 INSERT INTO c VALUE << 1 >> REMOVE a SET l = 3 REMOVE b",
        // Note that this query cannot be represented with the V0 AST.
        """ 
        (dml
            (operations
                (dml_op_list
                    (set
                        (assignment
                            (id k (case_insensitive) (unqualified))
                            (lit 5)))
                    (set
                        (assignment
                            (id m (case_insensitive) (unqualified))
                            (lit 6)))
                    (insert_value
                        (id c (case_insensitive) (unqualified))
                        (bag
                            (lit 1))
                        null null)
                    (remove
                        (id a (case_insensitive) (unqualified)))
                    (set
                        (assignment
                            (id l (case_insensitive) (unqualified))
                            (lit 3)))
                    (remove
                        (id b (case_insensitive) (unqualified)))))
            (from
                (scan
                    (id x (case_insensitive) (unqualified))
                    null
                    null
                    null))
            (where
                (eq
                    (id a (case_insensitive) (unqualified))
                    (id b (case_insensitive) (unqualified)))))
    """
    )

    @Test
    fun legacyUpdateComplexDml() = assertExpression(
        "UPDATE x SET k = 5, m = 6 INSERT INTO c VALUE << 1 >> REMOVE a SET l = 3 REMOVE b WHERE a = b",
        // Note that this query cannot be represented with the V0 AST.
        """
            (dml
                (operations
                    (dml_op_list
                        (set
                            (assignment
                                (id k (case_insensitive) (unqualified))
                                (lit 5)))
                        (set
                            (assignment
                                (id m (case_insensitive) (unqualified))
                                (lit 6)))
                        (insert_value
                            (id c (case_insensitive) (unqualified))
                            (bag
                                (lit 1))
                            null null)
                        (remove
                            (id a (case_insensitive) (unqualified)))
                        (set
                            (assignment
                                (id l (case_insensitive) (unqualified))
                                (lit 3)))
                        (remove
                            (id b (case_insensitive) (unqualified)))))
                (from
                    (scan
                        (id x (case_insensitive) (unqualified))
                        null
                        null
                        null))
                (where
                    (eq
                        (id a (case_insensitive) (unqualified))
                        (id b (case_insensitive) (unqualified)))))
        """
    )

    @Test
    fun legacyUpdateReturningComplexDml() = assertExpression(
        "UPDATE x SET k = 5, m = 6 INSERT INTO c VALUE << 1 >> REMOVE a SET l = 3 REMOVE b WHERE a = b RETURNING MODIFIED OLD a",
        """
        (dml
            (operations
                (dml_op_list
                    (set
                        (assignment
                            (id k (case_insensitive) (unqualified))
                            (lit 5)))
                    (set
                        (assignment
                            (id m (case_insensitive) (unqualified))
                            (lit 6)))
                    (insert_value
                        (id c (case_insensitive) (unqualified))
                        (bag
                            (lit 1))
                        null null)
                    (remove
                        (id a (case_insensitive) (unqualified)))
                    (set
                        (assignment
                            (id l (case_insensitive) (unqualified))
                            (lit 3)))
                    (remove
                        (id b (case_insensitive) (unqualified)))))
            (from
                (scan
                    (id x (case_insensitive) (unqualified))
                    null
                    null
                    null))
            (where
                (eq
                    (id a (case_insensitive) (unqualified))
                    (id b (case_insensitive) (unqualified))))
            (returning 
                (returning_expr 
                    (returning_elem 
                        (modified_old) 
                        (returning_column (id a (case_insensitive) (unqualified)))))))        
        """
    )

    @Test
    fun setSingleDml() = assertExpression(
        "SET k = 5",
        """
          (dml
            (operations
              (dml_op_list
                (set
                  (assignment
                    (id k (case_insensitive) (unqualified))
                    (lit 5)
                  )
                )
              )
            )
          )
        """
    )

    @Test
    fun setSingleDmlWithQuotedIdentifierAtHead() = assertExpression(
        "SET \"k\" = 5",
        """
          (dml
            (operations
              (dml_op_list
                (set
                  (assignment
                    (id k (case_sensitive) (unqualified))
                    (lit 5)
                  )
                )
              )
            )
          )
        """
    )

    @Test
    fun setMultiDml() = assertExpression(
        "SET k = 5, m = 6",
        """
          (dml
            (operations
              (dml_op_list 
                (set
                  (assignment
                    (id k (case_insensitive) (unqualified))
                    (lit 5)
                  )
                )
                (set
                  (assignment
                    (id m (case_insensitive) (unqualified))
                    (lit 6)
                  )
                )
              )
            )
          )
        """
    )

    @Test
    fun fromRemoveDml() = assertExpression(
        "FROM x WHERE a = b REMOVE y",
        """
          (dml
            (operations
              (dml_op_list
                (remove
                  (id y (case_insensitive) (unqualified))
                )
              )
            )
            (from (scan (id x (case_insensitive) (unqualified)) null null null))
            (where (eq (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))))
          )
        """
    )

    @Test
    fun fromRemoveReturningDml() = assertExpression(
        "FROM x WHERE a = b REMOVE y RETURNING MODIFIED NEW a",
        """
        (dml (operations 
            (dml_op_list (remove (id y (case_insensitive) (unqualified))))) 
            (from (scan (id x (case_insensitive) (unqualified)) null null null)) 
            (where (eq (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)))) 
            (returning 
                (returning_expr 
                    (returning_elem 
                        (modified_new) 
                        (returning_column (id a (case_insensitive) (unqualified)))))))
        """
    )

    @Test
    fun fromMultipleRemoveDml() = assertExpression(
        "FROM x WHERE a = b REMOVE y REMOVE z",
        """
          (dml
            (operations
              (dml_op_list
                (remove
                  (id y (case_insensitive) (unqualified))
                )
                (remove
                  (id z (case_insensitive) (unqualified))
                )
              )
            )
            (from (scan (id x (case_insensitive) (unqualified)) null null null))
            (where (eq (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))))
          )
        """
    )

    @Test
    fun fromMultipleRemoveReturningDml() = assertExpression(
        "FROM x WHERE a = b REMOVE y REMOVE z RETURNING MODIFIED OLD a",
        """
        (dml
        (operations
          (dml_op_list
            (remove
              (id y (case_insensitive) (unqualified))
            )
            (remove
              (id z (case_insensitive) (unqualified))
            )
          )
        )
        (from (scan (id x (case_insensitive) (unqualified)) null null null))
        (where (eq (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))))
        (returning 
            (returning_expr 
                (returning_elem 
                    (modified_old) 
                    (returning_column (id a (case_insensitive) (unqualified)))))))
        """
    )

    @Test
    fun removeDml() = assertExpression(
        "REMOVE y",
        """
          (dml
            (operations
              (dml_op_list
                (remove
                  (id y (case_insensitive) (unqualified))
                )
              )
            )
          )
        """
    )

    @Test
    fun removeDmlPath() = assertExpression(
        "REMOVE a.b['c'][2]",
        """
          (dml
            (operations
              (dml_op_list
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
          )
        """
    )

    @Test
    fun updateDml() = assertExpression(
        "UPDATE x AS y SET k = 5, m = 6 WHERE a = b",
        """
          (dml
            (operations
              (dml_op_list
                (set
                  (assignment
                    (id k (case_insensitive) (unqualified))
                    (lit 5)
                  )
                )
                (set
                  (assignment
                    (id m (case_insensitive) (unqualified))
                    (lit 6)
                  )
                )
              )
            )
            (from (scan (id x (case_insensitive) (unqualified)) y null null))
            (where (eq (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))))
          )
        """
    )

    @Test
    fun updateReturningDml() = assertExpression(
        "UPDATE x AS y SET k = 5, m = 6 WHERE a = b RETURNING MODIFIED OLD a",
        """
      (dml
        (operations
          (dml_op_list
            (set
              (assignment
                (id k (case_insensitive) (unqualified))
                (lit 5)
              )
            )
            (set
              (assignment
                (id m (case_insensitive) (unqualified))
                (lit 6)
              )
            )
          )
        )
        (from (scan (id x (case_insensitive) (unqualified)) y null null))
        (where (eq (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))))
        (returning 
            (returning_expr 
                (returning_elem 
                    (modified_old) 
                    (returning_column (id a (case_insensitive) (unqualified)))))))
        """
    )

    @Test
    fun updateWithInsert() = assertExpression(
        "UPDATE x AS y INSERT INTO k << 1 >> WHERE a = b",
        """
        (dml
            (operations
                (dml_op_list
                    (insert
                        (id k (case_insensitive) (unqualified))
                        null
                        (bag
                            (lit 1))
                        null)))
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
        """
    )

    @Test
    fun updateWithInsertReturningDml() = assertExpression(
        "UPDATE x AS y INSERT INTO k << 1 >> WHERE a = b RETURNING MODIFIED OLD a",
        """
        (dml
            (operations
                (dml_op_list
                    (insert
                        (id k (case_insensitive) (unqualified))
                        null
                        (bag
                            (lit 1))
                        null)))
            (from
                (scan
                    (id x (case_insensitive) (unqualified))
                    y
                    null
                    null))
            (where
                (eq
                    (id a (case_insensitive) (unqualified))
                    (id b (case_insensitive) (unqualified))))
            (returning
                (returning_expr
                    (returning_elem
                        (modified_old)
                        (returning_column
                            (id a (case_insensitive) (unqualified)))))))
        """
    )

    @Test
    fun updateWithInsertValueAt() = assertExpression(
        "UPDATE x AS y INSERT INTO k VALUE 1 AT 'j' WHERE a = b",
        """
          (dml
            (operations
              (dml_op_list
                (insert_value
                  (id k (case_insensitive) (unqualified))
                  (lit 1)
                  (lit "j")
                  null
                )
              )
            )    
            (from
              (scan (id x (case_insensitive) (unqualified)) y null null))
            (where
              (eq
                (id a (case_insensitive) (unqualified))
                (id b (case_insensitive) (unqualified)))))
        """
    )

    @Test
    fun updateWithRemove() = assertExpression(
        "UPDATE x AS y REMOVE y.a WHERE a = b",
        """
          (dml  
            (operations
              (dml_op_list
                (remove
                  (path
                    (id y (case_insensitive) (unqualified))
                    (path_expr (lit "a") (case_insensitive))))))
            (from (scan (id x (case_insensitive) (unqualified)) y null null))
            (where (eq (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))))
          )
        """
    )

    @Test
    fun updateDmlWithImplicitAs() = assertExpression(
        "UPDATE zoo z SET z.kingdom = 'Fungi'",
        """
          (dml
            (operations
              (dml_op_list
                (set
                  (assignment
                    (path (id z (case_insensitive) (unqualified)) (path_expr (lit "kingdom") (case_insensitive)))
                    (lit "Fungi")))))
            (from
              (scan (id zoo (case_insensitive) (unqualified)) z null null)))
        """
    )

    @Test
    fun updateDmlWithAt() = assertExpression(
        "UPDATE zoo AT z_ord SET z.kingdom = 'Fungi'",
        """
          (dml
            (operations
              (dml_op_list
                (set
                  (assignment
                    (path (id z (case_insensitive) (unqualified)) (path_expr (lit "kingdom") (case_insensitive)))
                    (lit "Fungi")))))
            (from
              (scan (id zoo (case_insensitive) (unqualified)) null z_ord null)))
        """
    )

    @Test
    fun updateDmlWithBy() = assertExpression(
        "UPDATE zoo BY z_id SET z.kingdom = 'Fungi'",
        """
          (dml
            (operations
              (dml_op_list
                (set
                  (assignment
                    (path (id z (case_insensitive) (unqualified)) (path_expr (lit "kingdom") (case_insensitive)))
                    (lit "Fungi")))))
            (from
              (scan (id zoo (case_insensitive) (unqualified)) null null z_id)))
        """
    )

    @Test
    fun updateDmlWithAtAndBy() = assertExpression(
        "UPDATE zoo AT z_ord BY z_id SET z.kingdom = 'Fungi'",
        """
          (dml
            (operations
              (dml_op_list
                (set
                  (assignment
                    (path (id z (case_insensitive) (unqualified)) (path_expr (lit "kingdom") (case_insensitive)))
                    (lit "Fungi")))))
            (from
              (scan (id zoo (case_insensitive) (unqualified)) null z_ord z_id)))
        """
    )

    @Test
    fun updateWhereDml() = assertExpression(
        "UPDATE x SET k = 5, m = 6 WHERE a = b",
        """
          (dml
            (operations
              (dml_op_list
                (set
                  (assignment
                    (id k (case_insensitive) (unqualified))
                    (lit 5)
                  )
                )
                (set
                  (assignment
                    (id m (case_insensitive) (unqualified))
                    (lit 6)
                  )
                )
              )
            )
            (from (scan (id x (case_insensitive) (unqualified)) null null null))
            (where (eq (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))))
          )
        """
    )

    @Test
    fun updateWhereReturningDml() = assertExpression(
        "UPDATE x SET k = 5, m = 6 WHERE a = b RETURNING MODIFIED OLD a, MODIFIED OLD b",
        """(dml 
        (operations 
            (dml_op_list 
                (set (assignment (id k (case_insensitive) (unqualified)) (lit 5))) 
                (set (assignment (id m (case_insensitive) (unqualified)) (lit 6))))) 
        (from (scan (id x (case_insensitive) (unqualified)) null null null)) 
        (where (eq (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))))
        (returning 
            (returning_expr 
                (returning_elem 
                    (modified_old) 
                    (returning_column (id a (case_insensitive) (unqualified)))) 
                (returning_elem 
                    (modified_old) 
                    (returning_column (id b (case_insensitive) (unqualified)))))))
        """
    )

    @Test
    fun updateWhereReturningPathDml() = assertExpression(
        "UPDATE x SET k = 5, m = 6 WHERE a = b RETURNING MODIFIED OLD a.b",
        """(dml 
            (operations 
                (dml_op_list 
                    (set (assignment (id k (case_insensitive) (unqualified)) (lit 5))) 
                    (set (assignment (id m (case_insensitive) (unqualified)) (lit 6))))) 
            (from (scan (id x (case_insensitive) (unqualified)) null null null)) 
            (where (eq (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))))
            (returning 
                (returning_expr 
                    (returning_elem 
                        (modified_old) 
                        (returning_column 
                            (path (id a (case_insensitive) (unqualified)) 
                            (path_expr (lit "b") (case_insensitive))))))))
        """
    )

    @Test
    fun updateWhereReturningPathAsteriskDml() = assertExpression(
        "UPDATE x SET k = 5, m = 6 WHERE a = b RETURNING MODIFIED OLD '1234'.*",
        """(dml 
        (operations 
            (dml_op_list 
                (set (assignment (id k (case_insensitive) (unqualified)) (lit 5))) 
                (set (assignment (id m (case_insensitive) (unqualified)) (lit 6))))) 
        (from (scan (id x (case_insensitive) (unqualified)) null null null)) 
        (where (eq (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))))
        (returning 
            (returning_expr 
                (returning_elem 
                    (modified_old) 
                    (returning_column (path (lit "1234") (path_unpivot)))))))
        """
    )

    @Test
    fun updateMultipleSetsWhereDml() = assertExpression(
        "UPDATE x SET k = 5 SET m = 6 WHERE a = b",
        """
          (dml
            (operations
              (dml_op_list
                  (set
                    (assignment
                      (id k (case_insensitive) (unqualified))
                      (lit 5)
                    )
                  )
                  (set
                    (assignment
                      (id m (case_insensitive) (unqualified))
                      (lit 6)
                    )
                  )
               )
            )
            (from (scan (id x (case_insensitive) (unqualified)) null null null))
            (where (eq (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))))
          )
        """
    )

    @Test
    fun updateMultipleSetsWhereReturningDml() = assertExpression(
        "UPDATE x SET k = 5 SET m = 6 WHERE a = b RETURNING ALL OLD x.*",
        """
        (dml (operations (dml_op_list 
            (set (assignment (id k (case_insensitive) (unqualified)) (lit 5))) 
            (set (assignment (id m (case_insensitive) (unqualified)) (lit 6)))))
        (from (scan (id x (case_insensitive) (unqualified)) null null null))
        (where (eq (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))))
        (returning 
            (returning_expr 
                (returning_elem 
                    (all_old) 
                    (returning_column 
                        (path (id x (case_insensitive) (unqualified)) 
                        (path_unpivot)))))))
        """
    )

    @Test
    fun deleteDml() = assertExpression(
        "DELETE FROM y",
        """
          (dml
            (operations (dml_op_list (delete)))
            (from (scan (id y (case_insensitive) (unqualified)) null null null))
          )
        """
    )

    @Test
    fun deleteReturningDml() = assertExpression(
        "DELETE FROM y RETURNING MODIFIED NEW a",
        """
      (dml
        (operations (dml_op_list (delete)))
        (from (scan (id y (case_insensitive) (unqualified)) null null null))
        (returning 
            (returning_expr 
                (returning_elem 
                    (modified_new) 
                    (returning_column (id a (case_insensitive) (unqualified)))))))
        """
    )

    @Test
    fun deleteDmlAliased() = assertExpression(
        "DELETE FROM x AS y",
        """
          (dml
            (operations (dml_op_list (delete)))
            (from (scan (id x (case_insensitive) (unqualified)) y null null))
          )
        """
    )

    @Test
    fun canParseADeleteQueryWithAPositionClause() = assertExpression(
        "DELETE FROM x AT y",
        """
            (dml
              (operations ( dml_op_list (delete)))
              (from (scan (id x (case_insensitive) (unqualified)) null y null)))
        """
    )

    @Test
    fun canParseADeleteQueryWithAliasAndPositionClause() = assertExpression(
        "DELETE FROM x AS y AT z",
        """
            (dml
               (operations (dml_op_list (delete)))
               (from (scan (id x (case_insensitive) (unqualified)) y z null)))
        """
    )

    @Test
    fun canParseADeleteQueryWithPath() = assertExpression(
        "DELETE FROM x.n",
        """
            (dml
                (operations (dml_op_list (delete)))
                (from
                    (scan
                        (path (id x (case_insensitive) (unqualified)) (path_expr (lit "n") (case_insensitive)))
                        null 
                        null
                        null)))
        """
    )

    @Test
    fun canParseADeleteQueryWithNestedPath() = assertExpression(
        "DELETE FROM x.n.m",
        """
            (dml
                (operations (dml_op_list(delete)))
                (from
                    (scan 
                        (path
                            (id x (case_insensitive) (unqualified))
                            (path_expr (lit "n") (case_insensitive))
                            (path_expr (lit "m") (case_insensitive)))
                        null
                        null
                        null)))
        """
    )

    @Test
    fun canParseADeleteQueryWithNestedPathAndAlias() = assertExpression(
        "DELETE FROM x.n.m AS y",
        """
            (dml
                (operations (dml_op_list (delete)))
                (from
                    (scan
                        (path
                            (id x (case_insensitive) (unqualified))
                            (path_expr (lit "n") (case_insensitive))
                            (path_expr (lit "m") (case_insensitive)))
                        y
                        null    
                        null)))
        """
    )

    @Test
    fun canParseADeleteQueryWithNestedPathAndAliasAndPosition() = assertExpression(
        "DELETE FROM x.n.m AS y AT z",
        """
            (dml
                (operations (dml_op_list (delete)))
                (from
                    (scan 
                        (path
                            (id x (case_insensitive) (unqualified))
                            (path_expr (lit "n") (case_insensitive))
                            (path_expr (lit "m") (case_insensitive)))
                        y
                        z
                        null)))
        """
    )

    // DDL
    // ****************************************
    @Test
    fun createTable() = assertExpression(
        "CREATE TABLE foo",
        "(ddl (create_table foo null))"
    )

    @Test
    fun createTableWithColumn() = assertExpression(
        "CREATE TABLE foo (boo string)",
        """
            (ddl (create_table foo  (table_def
                (column_declaration boo (string_type)))))
        """.trimIndent()
    )

    @Test
    fun createTableWithQuotedIdentifier() = assertExpression(
        "CREATE TABLE \"user\" (\"lastname\" string)",
        """
            (ddl (create_table user (table_def
                (column_declaration lastname (string_type)))))
        """.trimIndent()
    )

    @Test
    fun createTableWithConstraints() = assertExpression(
        """
            CREATE TABLE Customer (
               name string CONSTRAINT name_is_present NOT NULL, 
               age int,
               city string NULL,
               state string NULL
            )
        """.trimIndent(),
        """
            (ddl
                (create_table
                    Customer (table_def
                        (column_declaration name (string_type)
                            (column_constraint name_is_present (column_notnull)))
                        (column_declaration age (integer_type))
                        (column_declaration city (string_type)
                            (column_constraint null (column_null)))
                        (column_declaration state (string_type)
                            (column_constraint null (column_null))))))
        """.trimIndent()
    )

    @Test
    fun dropTable() = assertExpression(
        "DROP TABLE foo",
        "(ddl (drop_table (identifier foo (case_insensitive))))"
    )

    @Test
    fun dropTableWithQuotedIdentifier() = assertExpression(
        "DROP TABLE \"user\"",
        "(ddl (drop_table (identifier user (case_sensitive))))"
    )

    @Test
    fun createIndex() = assertExpression(
        "CREATE INDEX ON foo (x, y.z)",
        """
        (ddl
          (create_index
            (identifier foo (case_insensitive))
            (id x (case_insensitive) (unqualified))
            (path (id y (case_insensitive) (unqualified)) (path_expr (lit "z") (case_insensitive)))))
        """
    )

    @Test
    fun createIndexWithQuotedIdentifiers() = assertExpression(
        "CREATE INDEX ON \"user\" (\"group\")",
        """
        (ddl
          (create_index
            (identifier user (case_sensitive))
            (id group (case_sensitive) (unqualified))))
        """
    )

    @Test
    fun dropIndex() = assertExpression(
        "DROP INDEX bar ON foo",
        "(ddl (drop_index (table (identifier foo (case_insensitive))) (keys (identifier bar (case_insensitive)))))"
    )

    @Test
    fun dropIndexWithQuotedIdentifiers() = assertExpression(
        "DROP INDEX \"bar\" ON \"foo\"",
        "(ddl (drop_index (table (identifier foo (case_sensitive))) (keys (identifier bar (case_sensitive)))))"
    )

    @Test
    fun unionSelectPrecedence() = assertExpression(
        "SELECT * FROM foo UNION SELECT * FROM bar",
        """
        (bag_op
            (union)
            (distinct)
            (select
                (project
                    (project_star))
                (from
                    (scan
                        (id foo (case_insensitive) (unqualified))
                        null
                        null
                        null)))
            (select
                (project
                    (project_star))
                (from
                    (scan
                        (id bar (case_insensitive) (unqualified))
                        null
                        null
                        null))))
        """
    )

    @Test
    fun union() = assertExpression(
        "a UNION b"
    ) {
        bagOp(
            op = PartiqlAst.BagOpType.Union(),
            quantifier = PartiqlAst.SetQuantifier.Distinct(),
            operands = listOf(
                id("a"),
                id("b")
            )
        )
    }

    @Test
    fun unionDistinct() = assertExpression(
        "a UNION DISTINCT b"
    ) {
        bagOp(
            op = PartiqlAst.BagOpType.Union(),
            quantifier = PartiqlAst.SetQuantifier.Distinct(),
            operands = listOf(
                id("a"),
                id("b")
            )
        )
    }

    @Test
    fun unionAll() = assertExpression(
        "a UNION ALL b"
    ) {
        bagOp(
            op = PartiqlAst.BagOpType.Union(),
            quantifier = PartiqlAst.SetQuantifier.All(),
            operands = listOf(
                id("a"),
                id("b")
            )
        )
    }

    @Test
    fun intersect() = assertExpression(
        "a INTERSECT b"
    ) {
        bagOp(
            op = PartiqlAst.BagOpType.Intersect(),
            quantifier = PartiqlAst.SetQuantifier.Distinct(),
            operands = listOf(
                id("a"),
                id("b")
            )
        )
    }

    @Test
    fun intersectDistinct() = assertExpression(
        "a INTERSECT DISTINCT b"
    ) {
        bagOp(
            op = PartiqlAst.BagOpType.Intersect(),
            quantifier = PartiqlAst.SetQuantifier.Distinct(),
            operands = listOf(
                id("a"),
                id("b")
            )
        )
    }

    @Test
    fun intersectAll() = assertExpression(
        "a INTERSECT ALL b"
    ) {
        bagOp(
            op = PartiqlAst.BagOpType.Intersect(),
            quantifier = PartiqlAst.SetQuantifier.All(),
            operands = listOf(
                id("a"),
                id("b")
            )
        )
    }

    @Test
    fun except() = assertExpression(
        "a EXCEPT b"
    ) {
        bagOp(
            op = PartiqlAst.BagOpType.Except(),
            quantifier = PartiqlAst.SetQuantifier.Distinct(),
            operands = listOf(
                id("a"),
                id("b")
            )
        )
    }

    @Test
    fun exceptDistinct() = assertExpression(
        "a EXCEPT DISTINCT b"
    ) {
        bagOp(
            op = PartiqlAst.BagOpType.Except(),
            quantifier = PartiqlAst.SetQuantifier.Distinct(),
            operands = listOf(
                id("a"),
                id("b")
            )
        )
    }

    @Test
    fun exceptAll() = assertExpression(
        "a EXCEPT ALL b"
    ) {
        bagOp(
            op = PartiqlAst.BagOpType.Except(),
            quantifier = PartiqlAst.SetQuantifier.All(),
            operands = listOf(
                id("a"),
                id("b")
            )
        )
    }

    @Test
    fun outerUnion() = assertExpression(
        "a OUTER UNION b"
    ) {
        bagOp(
            op = PartiqlAst.BagOpType.OuterUnion(),
            quantifier = PartiqlAst.SetQuantifier.Distinct(),
            operands = listOf(
                this.id("a"),
                this.id("b")
            )
        )
    }

    @Test
    fun outerUnionDistinct() = assertExpression(
        "a OUTER UNION DISTINCT b"
    ) {
        bagOp(
            op = PartiqlAst.BagOpType.OuterUnion(),
            quantifier = PartiqlAst.SetQuantifier.Distinct(),
            operands = listOf(
                this.id("a"),
                this.id("b")
            )
        )
    }

    @Test
    fun outerUnionAll() = assertExpression(
        "a OUTER UNION ALL b"
    ) {
        bagOp(
            op = PartiqlAst.BagOpType.OuterUnion(),
            quantifier = PartiqlAst.SetQuantifier.All(),
            operands = listOf(
                this.id("a"),
                this.id("b")
            )
        )
    }

    @Test
    fun outerIntersect() = assertExpression(
        "a OUTER INTERSECT b"
    ) {
        bagOp(
            op = PartiqlAst.BagOpType.OuterIntersect(),
            quantifier = PartiqlAst.SetQuantifier.Distinct(),
            operands = listOf(
                this.id("a"),
                this.id("b")
            )
        )
    }

    @Test
    fun outerIntersectDistinct() = assertExpression(
        "a OUTER INTERSECT DISTINCT b"
    ) {
        bagOp(
            op = PartiqlAst.BagOpType.OuterIntersect(),
            quantifier = PartiqlAst.SetQuantifier.Distinct(),
            operands = listOf(
                this.id("a"),
                this.id("b")
            )
        )
    }

    @Test
    fun outerIntersectAll() = assertExpression(
        "a OUTER INTERSECT ALL b"
    ) {
        bagOp(
            op = PartiqlAst.BagOpType.OuterIntersect(),
            quantifier = PartiqlAst.SetQuantifier.All(),
            operands = listOf(
                this.id("a"),
                this.id("b")
            )
        )
    }

    @Test
    fun outerExcept() = assertExpression(
        "a OUTER EXCEPT b"
    ) {
        bagOp(
            op = PartiqlAst.BagOpType.OuterExcept(),
            quantifier = PartiqlAst.SetQuantifier.Distinct(),
            operands = listOf(
                this.id("a"),
                this.id("b")
            )
        )
    }

    @Test
    fun outerExceptDistinct() = assertExpression(
        "a OUTER EXCEPT DISTINCT b"
    ) {
        bagOp(
            op = PartiqlAst.BagOpType.OuterExcept(),
            quantifier = PartiqlAst.SetQuantifier.Distinct(),
            operands = listOf(
                this.id("a"),
                this.id("b")
            )
        )
    }

    @Test
    fun outerExceptAll() = assertExpression(
        "a OUTER EXCEPT ALL b"
    ) {
        bagOp(
            op = PartiqlAst.BagOpType.OuterExcept(),
            quantifier = PartiqlAst.SetQuantifier.All(),
            operands = listOf(
                this.id("a"),
                this.id("b")
            )
        )
    }

    // ****************************************
    // semicolon at end of sqlUnderTest
    // ****************************************
    @Test
    fun semicolonAtEndOfQuery() = assertExpression(
        "SELECT * FROM <<1>>;",
        "(select (project (project_star)) (from (scan (bag (lit 1)) null null null)))"
    )

    @Test
    fun rootSelectNodeHasSourceLocation() {
        targets.forEach { target ->
            val ast = target.parser.parseAstStatement("select 1 from dogs")
            assertEquals(SourceLocationMeta(1L, 1L, 6L), ast.metas.sourceLocation)
        }
    }

    @Test
    fun semicolonAtEndOfQueryHasNoEffect() {
        targets.forEach { target ->
            val query = "SELECT * FROM <<1>>"
            val withSemicolon = target.parser.parseAstStatement("$query;")
            val withoutSemicolon = target.parser.parseAstStatement(query)
            assertEquals(withoutSemicolon, withSemicolon)
        }
    }

    @Test
    fun semicolonAtEndOfLiteralHasNoEffect() {
        targets.forEach { target ->
            val withSemicolon = target.parser.parseAstStatement("1;")
            val withoutSemicolon = target.parser.parseAstStatement("1")
            assertEquals(withoutSemicolon, withSemicolon)
        }
    }

    @Test
    fun semicolonAtEndOfExpressionHasNoEffect() {
        targets.forEach { target ->
            val withSemicolon = target.parser.parseAstStatement("(1+1);")
            val withoutSemicolon = target.parser.parseAstStatement("(1+1)")
            assertEquals(withoutSemicolon, withSemicolon)
        }
    }

    // ****************************************
    // LET clause parsing
    // ****************************************

    private val projectX = PartiqlAst.build { projectList(projectExpr(id("x"))) }

    @Test
    fun selectFromLetTest() = assertExpression("SELECT x FROM table1 LET 1 AS A") {
        select(
            project = projectX,
            from = scan(id("table1")),
            fromLet = let(letBinding(lit(ionInt(1)), "A"))
        )
    }

    @Test
    fun selectFromLetTwoBindingsTest() = assertExpression("SELECT x FROM table1 LET 1 AS A, 2 AS B") {
        select(
            project = projectX,
            from = scan(id("table1")),
            fromLet = let(letBinding(lit(ionInt(1)), "A"), letBinding(lit(ionInt(2)), "B"))
        )
    }

    @Test
    fun selectFromLetTableBindingTest() = assertExpression("SELECT x FROM table1 LET table1 AS A") {
        select(
            project = projectX,
            from = scan(id("table1")),
            fromLet = let(letBinding(id("table1"), "A"))
        )
    }

    @Test
    fun selectFromLetFunctionBindingTest() = assertExpression("SELECT x FROM table1 LET foo() AS A") {
        select(
            project = projectX,
            from = scan(id("table1")),
            fromLet = let(letBinding(call("foo", emptyList()), "A"))
        )
    }

    @Test
    fun selectFromLetFunctionWithLiteralsTest() = assertExpression(
        "SELECT x FROM table1 LET foo(42, 'bar') AS A"
    ) {
        select(
            project = projectX,
            from = scan(id("table1")),
            fromLet = let(letBinding(call("foo", listOf(lit(ionInt(42)), lit(ionString("bar")))), "A"))
        )
    }

    @Test
    fun selectFromLetFunctionWithVariablesTest() = assertExpression(
        "SELECT x FROM table1 LET foo(table1) AS A"
    ) {
        select(
            project = projectX,
            from = scan(id("table1")),
            fromLet = let(letBinding(call("foo", listOf(id("table1"))), "A"))
        )
    }

    // ****************************************
    // OFFSET clause parsing
    // ****************************************

    private fun buildProject(project: String) = PartiqlAst.build { projectList(projectExpr(id(project))) }

    private fun buildLit(lit: String) = PartiqlAst.Expr.Lit(loadSingleElement(lit))

    @Test
    fun selectOffsetTest() = assertExpression("SELECT x FROM a OFFSET 5") {
        select(
            project = buildProject("x"),
            from = scan(id("a")),
            offset = buildLit("5")
        )
    }

    @Test
    fun selectLimitOffsetTest() = assertExpression("SELECT x FROM a LIMIT 7 OFFSET 5") {
        select(
            project = buildProject("x"),
            from = scan(id("a")),
            limit = buildLit("7"),
            offset = buildLit("5")
        )
    }

    @Test
    fun selectWhereLimitOffsetTest() = assertExpression("SELECT x FROM a WHERE y = 10 LIMIT 7 OFFSET 5") {
        select(
            project = buildProject("x"),
            from = scan(id("a")),
            where = PartiqlAst.Expr.Eq(listOf(id("y"), buildLit("10"))),
            limit = buildLit("7"),
            offset = buildLit("5")
        )
    }

    @Test
    fun selectOrderbyLimitOffsetTest() = assertExpression("SELECT x FROM a ORDER BY y DESC LIMIT 10 OFFSET 5") {
        select(
            project = buildProject("x"),
            from = scan(id("a")),
            order = PartiqlAst.OrderBy(listOf(PartiqlAst.SortSpec(id("y"), PartiqlAst.OrderingSpec.Desc(), null))),
            limit = buildLit("10"),
            offset = buildLit("5")
        )
    }

    // ****************************************
    // EXEC clause parsing
    // ****************************************
    @Test
    fun execNoArgs() = assertExpression(
        "EXEC foo"
    ) {
        exec("foo", emptyList())
    }

    @Test
    fun execOneStringArg() = assertExpression(
        "EXEC foo 'bar'"
    ) {
        exec("foo", listOf(lit(ionString("bar"))))
    }

    @Test
    fun execOneIntArg() = assertExpression(
        "EXEC foo 1"
    ) {
        exec("foo", listOf(lit(ionInt(1))))
    }

    @Test
    fun execMultipleArg() = assertExpression(
        "EXEC foo 'bar0', `1d0`, 2, [3]"
    ) {
        exec(
            "foo",
            listOf(lit(ionString("bar0")), lit(ionDecimal(Decimal.valueOf(1))), lit(ionInt(2)), list(lit(ionInt(3))))
        )
    }

    @Test
    fun execWithMissing() = assertExpression(
        "EXEC foo MISSING"
    ) {
        exec("foo", listOf(missing()))
    }

    @Test
    fun execWithBag() = assertExpression(
        "EXEC foo <<1>>"
    ) {
        exec("foo", listOf(bag(lit(ionInt(1)))))
    }

    @Test
    fun execWithSelectQuery() = assertExpression(
        "EXEC foo SELECT baz FROM bar"
    ) {
        exec(
            "foo",
            listOf(
                select(
                    project = projectList(projectExpr(id("baz"))),
                    from = scan(id("bar"))
                )
            )
        )
    }

    // EXCLUDE tests
    @Test
    fun selectStarExcludeAttrs() = assertExpression(
        "SELECT * EXCLUDE t.a, t.b, t.c FROM t"
    ) {
        select(
            project = projectStar(),
            excludeClause = excludeOp(
                excludeExpr(
                    root = identifier("t", caseInsensitive()),
                    steps = listOf(
                        excludeTupleAttr(identifier("a", caseInsensitive()))
                    ),
                ),
                excludeExpr(
                    root = identifier("t", caseInsensitive()),
                    steps = listOf(
                        excludeTupleAttr(identifier("b", caseInsensitive()))
                    ),
                ),
                excludeExpr(
                    root = identifier("t", caseInsensitive()),
                    steps = listOf(
                        excludeTupleAttr(identifier("c", caseInsensitive()))
                    ),
                )
            ),
            from = scan(id("t"))
        )
    }

    @Test
    fun selectListExcludeAttrs() = assertExpression(
        "SELECT x, y, z EXCLUDE t.a, t.b, t.c FROM t"
    ) {
        select(
            project = projectList(
                projectItems = listOf(
                    projectExpr(
                        id("x"),
                    ),
                    projectExpr(
                        id("y"),
                    ),
                    projectExpr(
                        id("z"),
                    ),
                ),
            ),
            excludeClause = excludeOp(
                excludeExpr(
                    root = identifier("t", caseInsensitive()),
                    steps = listOf(
                        excludeTupleAttr(identifier("a", caseInsensitive()))
                    ),
                ),
                excludeExpr(
                    root = identifier("t", caseInsensitive()),
                    steps = listOf(
                        excludeTupleAttr(identifier("b", caseInsensitive()))
                    ),
                ),
                excludeExpr(
                    root = identifier("t", caseInsensitive()),
                    steps = listOf(
                        excludeTupleAttr(identifier("c", caseInsensitive()))
                    ),
                )
            ),
            from = scan(id("t"))
        )
    }

    @Test
    fun selectValueExcludeAttrs() = assertExpression(
        "SELECT VALUE { 'x': 1, 'y': 2, 'z': 3 } EXCLUDE t.a, t.b, t.c FROM t"
    ) {
        select(
            project = projectValue(
                value = struct(
                    exprPair(lit(ionString("x")), lit(ionInt(1))),
                    exprPair(lit(ionString("y")), lit(ionInt(2))),
                    exprPair(lit(ionString("z")), lit(ionInt(3)))
                )
            ),
            excludeClause = excludeOp(
                excludeExpr(
                    root = identifier("t", caseInsensitive()),
                    steps = listOf(
                        excludeTupleAttr(identifier("a", caseInsensitive()))
                    ),
                ),
                excludeExpr(
                    root = identifier("t", caseInsensitive()),
                    steps = listOf(
                        excludeTupleAttr(identifier("b", caseInsensitive()))
                    ),
                ),
                excludeExpr(
                    root = identifier("t", caseInsensitive()),
                    steps = listOf(
                        excludeTupleAttr(identifier("c", caseInsensitive()))
                    ),
                )
            ),
            from = scan(id("t"))
        )
    }

    @Test
    fun selectStarExcludeNestedAttrs() = assertExpression(
        "SELECT * EXCLUDE t.a.foo.bar, t.b[0].*[*].baz, t.c.d.*.e[*].f.* FROM t"
    ) {
        select(
            project = projectStar(),
            excludeClause = excludeOp(
                excludeExpr(
                    root = identifier("t", caseInsensitive()),
                    steps = listOf(
                        excludeTupleAttr(identifier("a", caseInsensitive())),
                        excludeTupleAttr(identifier("foo", caseInsensitive())),
                        excludeTupleAttr(identifier("bar", caseInsensitive()))
                    ),
                ),
                excludeExpr(
                    root = identifier("t", caseInsensitive()),
                    steps = listOf(
                        excludeTupleAttr(identifier("b", caseInsensitive())),
                        excludeCollectionIndex(0),
                        excludeTupleWildcard(),
                        excludeCollectionWildcard(),
                        excludeTupleAttr(identifier("baz", caseInsensitive()))
                    ),
                ),
                excludeExpr(
                    root = identifier("t", caseInsensitive()),
                    steps = listOf(
                        excludeTupleAttr(identifier("c", caseInsensitive())),
                        excludeTupleAttr(identifier("d", caseInsensitive())),
                        excludeTupleWildcard(),
                        excludeTupleAttr(identifier("e", caseInsensitive())),
                        excludeCollectionWildcard(),
                        excludeTupleAttr(identifier("f", caseInsensitive())),
                        excludeTupleWildcard()
                    ),
                )
            ),
            from = scan(id("t"))
        )
    }

    @Test
    fun selectListExcludeNestedAttrs() = assertExpression(
        "SELECT x, y, z EXCLUDE t.a.foo.bar, t.b[0].*[*].baz, t.c.d.*.e[*].f.* FROM t"
    ) {
        select(
            project = projectList(
                projectItems = listOf(
                    projectExpr(
                        id("x"),
                    ),
                    projectExpr(
                        id("y"),
                    ),
                    projectExpr(
                        id("z"),
                    ),
                ),
            ),
            excludeClause = excludeOp(
                excludeExpr(
                    root = identifier("t", caseInsensitive()),
                    steps = listOf(
                        excludeTupleAttr(identifier("a", caseInsensitive())),
                        excludeTupleAttr(identifier("foo", caseInsensitive())),
                        excludeTupleAttr(identifier("bar", caseInsensitive()))
                    ),
                ),
                excludeExpr(
                    root = identifier("t", caseInsensitive()),
                    steps = listOf(
                        excludeTupleAttr(identifier("b", caseInsensitive())),
                        excludeCollectionIndex(0),
                        excludeTupleWildcard(),
                        excludeCollectionWildcard(),
                        excludeTupleAttr(identifier("baz", caseInsensitive()))
                    ),
                ),
                excludeExpr(
                    root = identifier("t", caseInsensitive()),
                    steps = listOf(
                        excludeTupleAttr(identifier("c", caseInsensitive())),
                        excludeTupleAttr(identifier("d", caseInsensitive())),
                        excludeTupleWildcard(),
                        excludeTupleAttr(identifier("e", caseInsensitive())),
                        excludeCollectionWildcard(),
                        excludeTupleAttr(identifier("f", caseInsensitive())),
                        excludeTupleWildcard()
                    ),
                )
            ),
            from = scan(id("t"))
        )
    }

    @Test
    fun selectValueExcludeNestedAttrs() = assertExpression(
        "SELECT VALUE { 'x': 1, 'y': 2, 'z': 3 } EXCLUDE t.a.foo.bar, t.b[0].*[*].baz, t.c.d.*.e[*].f.* FROM t"
    ) {
        select(
            project = projectValue(
                value = struct(
                    exprPair(lit(ionString("x")), lit(ionInt(1))),
                    exprPair(lit(ionString("y")), lit(ionInt(2))),
                    exprPair(lit(ionString("z")), lit(ionInt(3)))
                )
            ),
            excludeClause = excludeOp(
                excludeExpr(
                    root = identifier("t", caseInsensitive()),
                    steps = listOf(
                        excludeTupleAttr(identifier("a", caseInsensitive())),
                        excludeTupleAttr(identifier("foo", caseInsensitive())),
                        excludeTupleAttr(identifier("bar", caseInsensitive()))
                    ),
                ),
                excludeExpr(
                    root = identifier("t", caseInsensitive()),
                    steps = listOf(
                        excludeTupleAttr(identifier("b", caseInsensitive())),
                        excludeCollectionIndex(0),
                        excludeTupleWildcard(),
                        excludeCollectionWildcard(),
                        excludeTupleAttr(identifier("baz", caseInsensitive()))
                    ),
                ),
                excludeExpr(
                    root = identifier("t", caseInsensitive()),
                    steps = listOf(
                        excludeTupleAttr(identifier("c", caseInsensitive())),
                        excludeTupleAttr(identifier("d", caseInsensitive())),
                        excludeTupleWildcard(),
                        excludeTupleAttr(identifier("e", caseInsensitive())),
                        excludeCollectionWildcard(),
                        excludeTupleAttr(identifier("f", caseInsensitive())),
                        excludeTupleWildcard()
                    ),
                )
            ),
            from = scan(id("t"))
        )
    }

    @Test
    fun selectStarExcludeCaseSensitiveAndInsensitiveAttrs() = assertExpression(
        """SELECT * EXCLUDE t.a."b".C['d']."E" FROM t"""
    ) {
        select(
            project = projectStar(),
            excludeClause = excludeOp(
                excludeExpr(
                    root = identifier("t", caseInsensitive()),
                    steps = listOf(
                        excludeTupleAttr(identifier("a", caseInsensitive())),
                        excludeTupleAttr(identifier("b", caseSensitive())),
                        excludeTupleAttr(identifier("C", caseInsensitive())),
                        excludeTupleAttr(identifier("d", caseSensitive())),
                        excludeTupleAttr(identifier("E", caseSensitive())),
                    ),
                ),
            ),
            from = scan(id("t"))
        )
    }

    @Test
    fun pivotExclude() = assertExpression(
        """PIVOT v AT attr EXCLUDE t.a[*].b.c.*.d, t.foo.bar[*] FROM t"""
    ) {
        select(
            project = projectPivot(
                key = id("v"),
                value = id("attr"),
            ),
            excludeClause = excludeOp(
                excludeExpr(
                    root = identifier("t", caseInsensitive()),
                    steps = listOf(
                        excludeTupleAttr(identifier("a", caseInsensitive())),
                        excludeCollectionWildcard(),
                        excludeTupleAttr(identifier("b", caseInsensitive())),
                        excludeTupleAttr(identifier("c", caseInsensitive())),
                        excludeTupleWildcard(),
                        excludeTupleAttr(identifier("d", caseInsensitive())),
                    ),
                ),
                excludeExpr(
                    root = identifier("t", caseInsensitive()),
                    steps = listOf(
                        excludeTupleAttr(identifier("foo", caseInsensitive())),
                        excludeTupleAttr(identifier("bar", caseInsensitive())),
                        excludeCollectionWildcard(),
                    ),
                ),
            ),
            from = scan(id("t"))
        )
    }

    @Test
    fun selectStarExcludeErrorBinding() = checkInputThrowingParserException(
        "SELECT * EXCLUDE t FROM t",
        ErrorCode.PARSE_UNEXPECTED_TOKEN,
        expectErrorContextValues = mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 20L,
            Property.TOKEN_DESCRIPTION to PartiQLParser.FROM.getAntlrDisplayString(),
            Property.TOKEN_VALUE to ION.newSymbol("FROM")
        )
    )

    @Test
    fun selectStarExcludeErrorBindingWithJoin() = checkInputThrowingParserException(
        "SELECT * EXCLUDE t FROM s, t",
        ErrorCode.PARSE_UNEXPECTED_TOKEN,
        expectErrorContextValues = mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 20L,
            Property.TOKEN_DESCRIPTION to PartiQLParser.FROM.getAntlrDisplayString(),
            Property.TOKEN_VALUE to ION.newSymbol("FROM")
        )
    )

    @Test
    fun selectStarExcludeErrorTrailingComma() = checkInputThrowingParserException(
        "SELECT * EXCLUDE t.a.b.c, t.d.e, FROM t",
        ErrorCode.PARSE_UNEXPECTED_TOKEN,
        expectErrorContextValues = mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 34L,
            Property.TOKEN_DESCRIPTION to PartiQLParser.FROM.getAntlrDisplayString(),
            Property.TOKEN_VALUE to ION.newSymbol("FROM")
        )
    )

    @Test
    fun selectStarExcludeErrorStar() = checkInputThrowingParserException(
        "SELECT * EXCLUDE * FROM",
        ErrorCode.PARSE_UNEXPECTED_TOKEN,
        expectErrorContextValues = mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 18L,
            Property.TOKEN_DESCRIPTION to PartiQLParser.ASTERISK.getAntlrDisplayString(),
            Property.TOKEN_VALUE to ION.newSymbol("*")
        )
    )

    @Test
    fun selectStarExcludeErrorNonLiteralExpr() = checkInputThrowingParserException(
        "SELECT * EXCLUDE t.a[x + y] FROM t",
        ErrorCode.PARSE_UNEXPECTED_TOKEN,
        expectErrorContextValues = mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 22L,
            Property.TOKEN_DESCRIPTION to PartiQLParser.IDENTIFIER.getAntlrDisplayString(),
            Property.TOKEN_VALUE to ION.newSymbol("x")
        )
    )

    @Test
    fun manyNestedNotPerformanceRegressionTest(): Unit = forEachTarget {
        val startTime = System.currentTimeMillis()
        val t = thread {
            parser.parseAstStatement(
                """
                not not not not not not not not not not not not not not not not not not not not not not not not 
                not not not not not not not not not not not not not not not not not not not not not not not not 
                not not not not not not not not not not not not not not not not not not not not not not not not 
                not not not not not not not not not not not not not not not not not not not not not not not not 
                not not not not not not not not not not not not not not not not not not not not not not not not 
                not not not not not not not not not not not not not not not not not not not not not not not not 
                not not not not not not not not not not not not not not not not not not not not not not not not 
                not not not not not not not not not not not not not not not not not not not not not not not not 
                not not not not not not not not not not not not not not not not not not not not not not not not 
                not not not not not not not not not not not not not not not not not not not not not not not not 
                not not not not not not not not not not not not not not not not not not not not not not not not 
                not not not not not not not not not not not not not not not not not not not not not not not not 
                not not not not not not not not not not not not not not not not not not not not not not not not 
                not not not not not not not not not not not not not not not not not not not not not not not not false
                """
            )
        }
        val maxParseTime: Long = 5000
        t.join(maxParseTime)
        t.interrupt()

        assertTrue(
            "parsing many nested unary nots should take less than $maxParseTime",
            System.currentTimeMillis() - startTime < maxParseTime
        )
    }

    @Test
    fun testOrderByMetas(): Unit = forEachTarget {
        // Arrange
        val query = "SELECT * FROM << { 'x': 2 } >> ORDER BY x"
        val expected = SourceLocationMeta(1, 32, 5)

        // Act
        val stmt = parser.parseAstStatement(query)

        // Gather Metas and Assert
        val expr = when (stmt) {
            is PartiqlAst.Statement.Query -> stmt.expr
            else -> throw AssertionError("Expected a PartiqlAst.Statement.Query")
        }
        val orderExpr = when (expr) {
            is PartiqlAst.Expr.Select -> expr.order
            else -> throw AssertionError("Expected query to be a SELECT expression")
        }
        val metas = orderExpr?.metas ?: throw AssertionError("Expected ORDER BY clause to have metas")
        assertEquals(expected, metas.sourceLocation)
    }
}
