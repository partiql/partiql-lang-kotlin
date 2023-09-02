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
import org.partiql.lang.domains.vr
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
        "(list (vr (id a (regular)) (unqualified)) (lit 5))"
    )

    @Test
    fun listLiteralWithBinary() = assertExpression(
        "[a, 5, (b + 6)]",
        "(list (vr (id a (regular)) (unqualified)) (lit 5) (plus (vr (id b (regular)) (unqualified)) (lit 6)))"
    )

    @Test
    fun listFunction() = assertExpression(
        "list(a, 5)",
        "(list (vr (id a (regular)) (unqualified)) (lit 5))"
    )

    @Test
    fun listFunctionlWithBinary() = assertExpression(
        "LIST(a, 5, (b + 6))",
        "(list (vr (id a (regular)) (unqualified)) (lit 5) (plus (vr (id b (regular)) (unqualified)) (lit 6)))"
    )

    @Test
    fun sexpFunction() = assertExpression(
        "sexp(a, 5)",
        "(sexp (vr (id a (regular)) (unqualified)) (lit 5))"
    )

    @Test
    fun sexpFunctionWithBinary() = assertExpression(
        "SEXP(a, 5, (b + 6))",
        "(sexp (vr (id a (regular)) (unqualified)) (lit 5) (plus (vr (id b (regular)) (unqualified)) (lit 6)))"
    )

    @Test
    fun structLiteral() = assertExpression(
        "{'x':a, 'y':5 }",
        """(struct
             (expr_pair (lit "x") (vr (id a (regular)) (unqualified)))
             (expr_pair (lit "y") (lit 5))
           )
        """
    )

    @Test
    fun structLiteralWithBinary() = assertExpression(
        "{'x':a, 'y':5, 'z':(b + 6)}",
        """(struct
             (expr_pair (lit "x") (vr (id a (regular)) (unqualified)))
             (expr_pair (lit "y") (lit 5))
             (expr_pair (lit "z") (plus (vr (id b (regular)) (unqualified)) (lit 6)))
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
        "(vr (id kumo (regular)) (unqualified))"
    )

    @Test
    fun id_case_sensitive() = assertExpression(
        "\"kumo\"",
        "(vr (id kumo (delimited)) (unqualified))"
    )

    @Test
    fun nonReservedKeyword() = assertExpression(
        "excluded",
        "(vr (id excluded (regular)) (unqualified))"
    )

    @Test
    fun nonReservedKeywordQualified() = assertExpression(
        "@excluded",
        "(vr (id excluded (regular)) (locals_first))"
    )

    // ****************************************
    // call
    // ****************************************
    @Test
    fun callEmpty() = assertExpression(
        "foobar()",
        "(call (defnid foobar))"
    )

    @Test
    fun callOneArgument() = assertExpression(
        "foobar(1)",
        "(call (defnid foobar) (lit 1))"
    )

    @Test
    fun callTwoArgument() = assertExpression(
        "foobar(1, 2)",
        "(call (defnid foobar) (lit 1) (lit 2))"
    )

    @Test
    fun callSubstringSql92Syntax() = assertExpression(
        "substring('test' from 100)",
        "(call (defnid substring) (lit \"test\") (lit 100))"
    )

    @Test
    fun callSubstringSql92SyntaxWithLength() = assertExpression(
        "substring('test' from 100 for 50)",
        "(call (defnid substring) (lit \"test\") (lit 100) (lit 50))"
    )

    @Test
    fun callSubstringNormalSyntax() = assertExpression(
        "substring('test', 100)",
        "(call (defnid substring) (lit \"test\") (lit 100))"
    )

    @Test
    fun callSubstringNormalSyntaxWithLength() = assertExpression(
        "substring('test', 100, 50)",
        "(call (defnid substring) (lit \"test\") (lit 100) (lit 50))"
    )

    @Test
    fun callTrimSingleArgument() = assertExpression(
        "trim('test')",
        "(call (defnid trim) (lit \"test\"))"
    )

    @Test
    fun callTrimTwoArgumentsDefaultSpecification() = assertExpression(
        "trim(' ' from 'test')",
        "(call (defnid trim) (lit \" \") (lit \"test\"))"
    )

    @Test
    fun callTrimTwoArgumentsUsingBoth() = assertExpression(
        "trim(both from 'test')",
        "(call (defnid trim) (lit both) (lit \"test\"))"
    )

    @Test
    fun callTrimTwoArgumentsUsingLeading() = assertExpression(
        "trim(leading from 'test')",
        "(call (defnid trim) (lit leading) (lit \"test\"))"
    )

    @Test
    fun callTrimTwoArgumentsUsingTrailing() = assertExpression(
        "trim(trailing from 'test')",
        """(call (defnid trim) (lit trailing) (lit "test"))"""
    )

    // ****************************************
    // Unary operators
    // ****************************************

    @Test
    fun negCall() = assertExpression(
        "-baz()",
        "(neg (call (defnid baz)))"
    )

    @Test
    fun posNegIdent() = assertExpression(
        "+(-baz())",
        "(pos (neg (call (defnid baz))))"
    )

    @Test
    fun posNegIdentNoSpaces() = assertExpression(
        "+-baz()",
        "(pos (neg (call (defnid baz))))"
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
        "(vr (id a (regular)) (locals_first))"
    )

    @Test
    fun atOperatorOnPath() = assertExpression(
        "@a.b",
        """(path (vr (id a (regular)) (locals_first)) (path_expr (lit "b") (regular)))"""
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
        "(is_type (call (defnid f)) (character_varying_type 200))"
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
        "(not (is_type (call (defnid f)) (character_varying_type 200)))"
    )

    @Test
    fun callWithMultiple() = assertExpression(
        "foobar(5, 6, a)",
        "(call (defnid foobar) (lit 5) (lit 6) (vr (id a (regular)) (unqualified)))"
    )

    @Test
    fun aggregateFunctionCall() = assertExpression(
        "COUNT(a)",
        """(call_agg (all) (defnid count) (vr (id a (regular)) (unqualified)))"""
    )

    @Test
    fun aggregateDistinctFunctionCall() = assertExpression(
        "SUM(DISTINCT a)",
        "(call_agg (distinct) (defnid sum) (vr (id a (regular)) (unqualified)))"
    )

    @Test
    fun countStarFunctionCall() = assertExpression(
        "COUNT(*)",
        "(call_agg (all) (defnid count) (lit 1))"
    )

    @Test
    fun countFunctionCall() = assertExpression(
        "COUNT(a)",
        "(call_agg (all) (defnid count) (vr (id a (regular)) (unqualified)))"
    )

    @Test
    fun countDistinctFunctionCall() = assertExpression(
        "COUNT(DISTINCT a)",
        "(call_agg (distinct) (defnid count) (vr (id a (regular)) (unqualified)))"
    )

    // ****************************************
    // path expression
    // ****************************************
    @Test
    fun dot_case_1_insensitive_component() = assertExpression(
        "a.b",
        """(path (vr (id a (regular)) (unqualified)) (path_expr (lit "b") (regular)))"""
    )

    @Test
    fun dot_case_2_insensitive_component() = assertExpression(
        "a.b.c",
        """(path (vr (id a (regular)) (unqualified))
           (path_expr (lit "b") (regular))
           (path_expr (lit "c") (regular)))""".trimMargin()
    )

    @Test
    fun dot_case_3_insensitive_components() = assertExpression(
        "a.b.c.d",
        """(path (vr (id a (regular)) (unqualified))
           (path_expr (lit "b") (regular))
           (path_expr (lit "c") (regular))
           (path_expr (lit "d") (regular)))""".trimMargin()
    )

    @Test
    fun dot_case_sensitive() = assertExpression(
        """ "a"."b" """,
        """(path (vr (id a (delimited)) (unqualified))
           (path_expr (lit "b") (delimited)))""".trimMargin()
    )

    @Test
    fun dot_case_sensitive_component() = assertExpression(
        "a.\"b\"",
        """(path (vr (id a (regular)) (unqualified))
           (path_expr (lit "b") (delimited)))""".trimMargin()
    )

    @Test
    fun groupDot() = assertExpression(
        "(a).b",
        """(path (vr (id a (regular)) (unqualified))
           (path_expr (lit "b") (regular)))""".trimMargin()
    )

    @Test
    fun pathWith1SquareBracket() = assertExpression(
        """a[5]""",
        """(path (vr (id a (regular)) (unqualified))
           (path_expr (lit 5) (delimited)))""".trimMargin()
    )

    @Test
    fun pathWith3SquareBrackets() = assertExpression(
        """a[5]['b'][(a + 3)]""",
        """(path (vr (id a (regular)) (unqualified))
           (path_expr (lit 5) (delimited))
           (path_expr (lit "b") (delimited))
           (path_expr (plus (vr (id a (regular)) (unqualified)) (lit 3)) (delimited)))"""
    )

    @Test
    fun dotStar() = assertExpression(
        "a.*",
        """(path (vr (id a (regular)) (unqualified)) (path_unpivot))""".trimMargin()
    )

    @Test
    fun dot2Star() = assertExpression(
        "a.b.*",
        """(path (vr (id a (regular)) (unqualified))
           (path_expr (lit "b") (regular))
           (path_unpivot))""".trimMargin()
    )

    @Test
    fun dotWildcard() = assertExpression(
        "a[*]",
        """(path (vr (id a (regular)) (unqualified)) (path_wildcard))"""
    )

    @Test
    fun dot2Wildcard() = assertExpression(
        "a.b[*]",
        """(path (vr (id a (regular)) (unqualified))
           (path_expr (lit "b") (regular))
           (path_wildcard))""".trimMargin()
    )

    @Test
    fun pathWithCallAndDotStar() = assertExpression(
        "foo(x, y).a.*.b",
        """(path (call (defnid foo) (vr (id x (regular)) (unqualified)) (vr (id y (regular)) (unqualified)))
           (path_expr (lit "a") (regular))
           (path_unpivot)
           (path_expr (lit "b") (regular)))""".trimMargin()
    )

    @Test
    fun dotAndBracketStar() = assertExpression(
        "x.a[*].b",
        """(path (vr (id x (regular)) (unqualified))
           (path_expr (lit "a") (regular))
           (path_wildcard)
           (path_expr (lit "b") (regular)))""".trimMargin()
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
        "(cast (vr (id a (regular)) (unqualified)) (decimal_type null null))"
    )

    @Test
    fun castAsDecimalScaleOnly() = assertExpression(
        "CAST(a AS DECIMAL(1))",
        "(cast (vr (id a (regular)) (unqualified)) (decimal_type 1 null))"
    )

    @Test
    fun castAsDecimalScaleAndPrecision() = assertExpression(
        "CAST(a AS DECIMAL(1, 2))",
        "(cast (vr (id a (regular)) (unqualified)) (decimal_type 1 2))"
    )

    @Test
    fun castAsNumeric() = assertExpression(
        "CAST(a AS NUMERIC)",
        """(cast (vr (id a (regular)) (unqualified)) (numeric_type null null))"""
    )

    @Test
    fun castAsNumericScaleOnly() = assertExpression(
        "CAST(a AS NUMERIC(1))",
        "(cast (vr (id a (regular)) (unqualified)) (numeric_type 1 null))"
    )

    @Test
    fun castAsNumericScaleAndPrecision() = assertExpression(
        "CAST(a AS NUMERIC(1, 2))",
        "(cast (vr (id a (regular)) (unqualified)) (numeric_type 1 2))"
    )

    // ****************************************
    // custom type cast
    // ****************************************
    @Test
    fun castAsEsBoolean() = assertExpression(
        "CAST(TRUE AS ES_BOOLEAN)",
        "(cast (lit true) (custom_type (defnid es_boolean)))"
    )

    @Test
    fun castAsRsInteger() = assertExpression(
        "CAST(1.123 AS RS_INTEGER)",
        "(cast (lit 1.123) (custom_type (defnid rs_integer)))"
    )

    // ****************************************
    // searched CASE
    // ****************************************
    @Test
    fun searchedCaseSingleNoElse() = assertExpression(
        "CASE WHEN name = 'zoe' THEN 1 END",
        """(searched_case
          (expr_pair_list
            (expr_pair (eq (vr (id name (regular)) (unqualified)) (lit "zoe")) (lit 1)))
          null
        )
        """
    )

    @Test
    fun searchedCaseSingleWithElse() = assertExpression(
        "CASE WHEN name = 'zoe' THEN 1 ELSE 0 END",
        """(searched_case
          (expr_pair_list
            (expr_pair (eq (vr (id name (regular)) (unqualified)) (lit "zoe")) (lit 1)))
          (lit 0)
        )
        """
    )

    @Test
    fun searchedCaseMultiWithElse() = assertExpression(
        "CASE WHEN name = 'zoe' THEN 1 WHEN name > 'kumo' THEN 2 ELSE 0 END",
        """(searched_case
          (expr_pair_list
            (expr_pair (eq (vr (id name (regular)) (unqualified)) (lit "zoe")) (lit 1))
            (expr_pair (gt (vr (id name (regular)) (unqualified)) (lit "kumo")) (lit 2)))
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
          (vr (id name (regular)) (unqualified))
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
             (vr (id name (regular)) (unqualified))
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
          (vr (id name (regular)) (unqualified))
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
             (vr (id a (regular)) (unqualified))
             (list (lit 1) (lit 2) (lit 3) (lit 4))
           )
        """
    )

    @Test
    fun notInOperatorWithImplicitValues() = assertExpression(
        "a NOT IN (1, 2, 3, 4)",
        """(not
          (in_collection
             (vr (id a (regular)) (unqualified))
             (list (lit 1) (lit 2) (lit 3) (lit 4))))
        """
    )

    @Test
    fun inOperatorWithImplicitValuesRowConstructor() = assertExpression(
        "(a, b) IN ((1, 2), (3, 4))",
        """(in_collection
             (list (vr (id a (regular)) (unqualified)) (vr (id b (regular)) (unqualified)))
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
        """(like (vr (id a (regular)) (unqualified)) (lit "_AAA%") null)"""
    )

    @Test
    fun likeColNameLikeColName() = assertExpression(
        "a LIKE b",
        "(like (vr (id a (regular)) (unqualified)) (vr (id b (regular)) (unqualified)) null)"
    )

    @Test
    fun pathLikePath() = assertExpression(
        "a.name LIKE b.pattern",
        """
        (like
            (path (vr (id a (regular)) (unqualified)) (path_expr (lit "name") (regular)))
            (path (vr (id b (regular)) (unqualified)) (path_expr (lit "pattern") (regular)))
            null)
        """
    )

    @Test
    fun likeColNameLikeColNamePath() = assertExpression(
        "a.name LIKE b.pattern",
        """
        (like
            (path (vr (id a (regular)) (unqualified)) (path_expr (lit "name") (regular)))
            (path (vr (id b (regular)) (unqualified)) (path_expr (lit "pattern") (regular)))
            null)
        """
    )

    @Test
    fun likeColNameLikeStringEscape() = assertExpression(
        "a LIKE '_AAA%' ESCAPE '['",
        """
        (like
            (vr (id a (regular)) (unqualified))
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
            (vr (id a (regular)) (unqualified))
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
            (vr (id a (regular)) (unqualified))
            (vr (id b (regular)) (unqualified))
            (lit "\\"))
        """
    )

    @Test
    fun likeColNameLikeColNameEscapeNonLit() = assertExpression(
        "a LIKE b ESCAPE c",
        "(like (vr (id a (regular)) (unqualified)) (vr (id b (regular)) (unqualified)) (vr (id c (regular)) (unqualified)))"
    )

    @Test
    fun likeColNameLikeColNameEscapePath() = assertExpression(
        "a LIKE b ESCAPE x.c",
        """(like (vr (id a (regular)) (unqualified)) (vr (id b (regular)) (unqualified)) (path (vr (id x (regular)) (unqualified)) (path_expr (lit "c") (regular))))"""
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
        "(call (defnid date_<op>) (lit YEAR) (vr (id a (regular)) (unqualified)) (vr (id b (regular)) (unqualified)))"
    )

    @Test
    fun callDateArithMonth() = assertDateArithmetic(
        "date_<op>(month, a, b)",
        "(call (defnid date_<op>) (lit MONTH) (vr (id a (regular)) (unqualified)) (vr (id b (regular)) (unqualified)))"
    )

    @Test
    fun callDateArithDay() = assertDateArithmetic(
        "date_<op>(day, a, b)",
        "(call (defnid date_<op>) (lit DAY) (vr (id a (regular)) (unqualified)) (vr (id b (regular)) (unqualified)))"
    )

    @Test
    fun callDateArithHour() = assertDateArithmetic(
        "date_<op>(hour, a, b)",
        "(call (defnid date_<op>) (lit HOUR) (vr (id a (regular)) (unqualified)) (vr (id b (regular)) (unqualified)))"
    )

    @Test
    fun callDateArithMinute() = assertDateArithmetic(
        "date_<op>(minute, a, b)",
        "(call (defnid date_<op>) (lit MINUTE) (vr (id a (regular)) (unqualified)) (vr (id b (regular)) (unqualified)))"
    )

    @Test
    fun callDateArithSecond() = assertDateArithmetic(
        "date_<op>(second, a, b)",
        "(call (defnid date_<op>) (lit SECOND) (vr (id a (regular)) (unqualified)) (vr (id b (regular)) (unqualified)))"
    )

    @Test // invalid evaluation, but valid parsing
    fun callDateArithTimezoneHour() = assertDateArithmetic(
        "date_<op>(timezone_hour, a, b)",
        "(call (defnid date_<op>) (lit TIMEZONE_HOUR) (vr (id a (regular)) (unqualified)) (vr (id b (regular)) (unqualified)))"
    )

    @Test // invalid evaluation, but valid parsing
    fun callDateArithTimezoneMinute() = assertDateArithmetic(
        "date_<op>(timezone_minute, a, b)",
        "(call (defnid date_<op>) (lit TIMEZONE_MINUTE) (vr (id a (regular)) (unqualified)) (vr (id b (regular)) (unqualified)))"
    )

    // ****************************************
    // call extract (special syntax)
    // ****************************************
    @Test
    fun callExtractYear() = assertExpression(
        "extract(year from a)",
        "(call (defnid extract) (lit YEAR) (vr (id a (regular)) (unqualified)))"
    )

    @Test
    fun callExtractMonth() = assertExpression(
        "extract(month from a)",
        "(call (defnid extract) (lit MONTH) (vr (id a (regular)) (unqualified)))"
    )

    @Test
    fun callExtractDay() = assertExpression(
        "extract(day from a)",
        "(call (defnid extract) (lit DAY) (vr (id a (regular)) (unqualified)))"
    )

    @Test
    fun callExtractHour() = assertExpression(
        "extract(hour from a)",
        "(call (defnid extract) (lit HOUR) (vr (id a (regular)) (unqualified)))"
    )

    @Test
    fun callExtractMinute() = assertExpression(
        "extract(minute from a)",
        "(call (defnid extract) (lit MINUTE) (vr (id a (regular)) (unqualified)))"
    )

    @Test
    fun callExtractSecond() = assertExpression(
        "extract(second from a)",
        "(call (defnid extract) (lit SECOND) (vr (id a (regular)) (unqualified)))"
    )

    @Test
    fun callExtractTimezoneHour() = assertExpression(
        "extract(timezone_hour from a)",
        "(call (defnid extract) (lit TIMEZONE_HOUR) (vr (id a (regular)) (unqualified)))"
    )

    @Test
    fun callExtractTimezoneMinute() = assertExpression(
        "extract(timezone_minute from a)",
        "(call (defnid extract) (lit TIMEZONE_MINUTE) (vr (id a (regular)) (unqualified)))"
    )

    @Test
    fun caseInsensitiveFunctionName() = assertExpression(
        "mY_fUnCtIoN(a)",
        "(call (defnid my_function) (vr (id a (regular)) (unqualified)))"
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
        "(select (project (project_list (project_expr (vr (id a (regular)) (unqualified)) null))) (from (scan (vr (id table1 (regular)) (unqualified)) null null null)))"
    )

    @Test
    fun selectAllWithSingleFrom() = assertExpression(
        "SELECT ALL a FROM table1",
        "(select (project (project_list (project_expr (vr (id a (regular)) (unqualified)) null))) (from (scan (vr (id table1 (regular)) (unqualified)) null null null)))"
    )

    @Test
    fun selectDistinctWithSingleFrom() = assertExpression(
        "SELECT DISTINCT a FROM table1",
        "(select (setq (distinct)) (project (project_list (project_expr (vr (id a (regular)) (unqualified)) null))) (from (scan (vr (id table1 (regular)) (unqualified)) null null null)))"
    )

    @Test
    fun selectStar() = assertExpression(
        "SELECT * FROM table1",
        "(select (project (project_star)) (from (scan (vr (id table1 (regular)) (unqualified)) null null null)))"
    )

    @Test
    fun selectAliasDotStar() = assertExpression(
        "SELECT t.* FROM table1 AS t",
        "(select (project (project_list (project_all (vr (id t (regular)) (unqualified))))) (from (scan (vr (id table1 (regular)) (unqualified)) (defnid t) null null)))"
    )

    @Test
    fun selectPathAliasDotStar() = assertExpression(
        "SELECT a.b.* FROM table1 AS t",
        """
            (select 
               (project (project_list (project_all (path (vr (id a (regular)) (unqualified)) (path_expr (lit "b") (regular)))))) 
               (from (scan (vr (id table1 (regular)) (unqualified)) (defnid t) null null)))
                      """
    )

    @Test
    fun selectWithFromAt() = assertExpression(
        "SELECT ord FROM table1 AT ord",
        "(select (project (project_list (project_expr (vr (id ord (regular)) (unqualified)) null))) (from (scan (vr (id table1 (regular)) (unqualified)) null (defnid ord) null)))"
    )

    @Test
    fun selectWithFromAsAndAt() = assertExpression(
        "SELECT ord, val FROM table1 AS val AT ord",
        "(select (project (project_list (project_expr (vr (id ord (regular)) (unqualified)) null) (project_expr (vr (id val (regular)) (unqualified)) null))) (from (scan (vr (id table1 (regular)) (unqualified)) (defnid val) (defnid ord) null)))"
    )

    @Test
    fun selectWithFromIdBy() = assertExpression(
        "SELECT * FROM table1 BY uid",
        "(select (project (project_star)) (from (scan (vr (id table1 (regular)) (unqualified)) null null (defnid uid))))"
    )

    @Test
    fun selectWithFromAtIdBy() = assertExpression(
        "SELECT * FROM table1 AT ord BY uid",
        "(select (project (project_star)) (from (scan (vr (id table1 (regular)) (unqualified)) null (defnid ord) (defnid uid))))"
    )

    @Test
    fun selectWithFromAsIdBy() = assertExpression(
        "SELECT * FROM table1 AS t BY uid",
        "(select (project (project_star)) (from (scan (vr (id table1 (regular)) (unqualified)) (defnid t) null (defnid uid))))"
    )

    @Test
    fun selectWithFromAsAndAtIdBy() = assertExpression(
        "SELECT * FROM table1 AS val AT ord BY uid",
        "(select (project (project_star)) (from (scan (vr (id table1 (regular)) (unqualified)) (defnid val) (defnid ord) (defnid uid))))"
    )

    @Test
    fun selectWithFromUnpivot() = assertExpression(
        "SELECT * FROM UNPIVOT item",
        """
        (select
          (project (project_star))
          (from (unpivot (vr (id item (regular)) (unqualified)) null null null))
        )
        """
    )

    @Test
    fun selectWithFromUnpivotWithAt() = assertExpression(
        "SELECT ord FROM UNPIVOT item AT name",
        """
        (select
          (project (project_list (project_expr (vr (id ord (regular)) (unqualified)) null)))
          (from (unpivot (vr (id item (regular)) (unqualified)) null (defnid name) null))
        )
        """
    )

    @Test
    fun selectWithFromUnpivotWithAs() = assertExpression(
        "SELECT ord FROM UNPIVOT item AS val",
        """
        (select
          (project (project_list (project_expr (vr (id ord (regular)) (unqualified)) null)))
          (from (unpivot (vr (id item (regular)) (unqualified)) (defnid val) null null))
        )
        """
    )

    @Test
    fun selectWithFromUnpivotWithAsAndAt() = assertExpression(
        "SELECT ord FROM UNPIVOT item AS val AT name",
        """
        (select
          (project (project_list (project_expr (vr (id ord (regular)) (unqualified)) null)))
          (from (unpivot (vr (id item (regular)) (unqualified)) (defnid val) (defnid name) null))
        )
        """
    )

    @Test
    fun selectAllStar() = assertExpression(
        "SELECT ALL * FROM table1",
        """
            (select 
                (project (project_star)) 
                (from (scan (vr (id table1 (regular)) (unqualified)) null null null)))
        """
    )

    @Test
    fun selectDistinctStar() = assertExpression(
        "SELECT DISTINCT * FROM table1",
        """
            (select 
                (setq (distinct)) 
                (project (project_star)) 
                (from (scan (vr (id table1 (regular)) (unqualified)) null null null)))
        """
    )

    @Test
    fun selectWhereMissing() = assertExpression(
        "SELECT a FROM stuff WHERE b IS MISSING",
        """
            (select 
                (project (project_list (project_expr (vr (id a (regular)) (unqualified)) null))) 
                (from (scan (vr (id stuff (regular)) (unqualified)) null null null)) 
                (where (is_type (vr (id b (regular)) (unqualified)) (missing_type))))
        """
    )

    @Test
    fun selectCommaCrossJoin1() = assertExpression(
        "SELECT a FROM table1, table2",
        """
            (select 
                (project (project_list (project_expr (vr (id a (regular)) (unqualified)) null))) 
                (from 
                    (join 
                        (inner) 
                        (scan (vr (id table1 (regular)) (unqualified)) null null null)
                        (scan (vr (id table2 (regular)) (unqualified)) null null null)
                        null)))
        """
    )

    @Test
    fun selectCommaCrossJoin2() = assertExpression(
        "SELECT a FROM table1, table2, table3",
        """
            (select 
                (project (project_list (project_expr (vr (id a (regular)) (unqualified)) null))) 
                (from 
                    (join
                        (inner)
                        (join
                            (inner)
                            (scan (vr (id table1 (regular)) (unqualified)) null null null) 
                            (scan (vr (id table2 (regular)) (unqualified)) null null null)
                            null) 
                        (scan (vr (id table3 (regular)) (unqualified)) null null null)
                        null)))
        """
    )

    @Test
    fun selectMultipleWithMultipleFromSimpleWhere() = assertExpression(
        "SELECT a, b FROM table1 as t1, table2 WHERE f(t1)",
        """(select
             (project (project_list (project_expr (vr (id a (regular)) (unqualified)) null) (project_expr (vr (id b (regular)) (unqualified)) null)))
             (from 
                (join
                    (inner)
                    (scan (vr (id table1 (regular)) (unqualified)) (defnid t1) null null) 
                    (scan (vr (id table2 (regular)) (unqualified)) null null null)
                    null))
             (where (call (defnid f) (vr (id t1 (regular)) (unqualified))))
           )
        """
    )

    @Test
    fun selectMultipleWithMultipleFromSimpleWhereNoAsAlias() = assertExpression(
        "SELECT a a1, b b1 FROM table1 t1, table2 WHERE f(t1)",
        """
        (select
            (project (project_list (project_expr (vr (id a (regular)) (unqualified)) (defnid a1)) 
                                   (project_expr (vr (id b (regular)) (unqualified)) (defnid b1))))
            (from 
                (join 
                    (inner) 
                    (scan (vr (id table1 (regular)) (unqualified)) (defnid t1) null null) 
                    (scan (vr (id table2 (regular)) (unqualified)) null null null) 
                    null))
            (where (call (defnid f) (vr (id t1 (regular)) (unqualified))))
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
              (project_expr (plus (call_agg (all) (defnid sum) (vr (id a (regular)) (unqualified))) (call_agg (all) (defnid count) (lit 1))) null)
              (project_expr (call_agg (all) (defnid avg) (vr (id b (regular)) (unqualified))) null)
              (project_expr (call_agg (all) (defnid min) (vr (id c (regular)) (unqualified))) null)
              (project_expr (call_agg (all) (defnid max) (plus (vr (id d (regular)) (unqualified)) (vr (id e (regular)) (unqualified)))) null)
            )
          )
          (from (scan (vr (id foo (regular)) (unqualified)) null null null))
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
                 (project_expr (path (call (defnid process) (vr (id t (regular)) (unqualified))) (path_expr (lit "a") (regular)) (path_expr (lit 0) (delimited))) 
                               (defnid a))
                 (project_expr (path (vr (id t2 (regular)) (unqualified)) (path_expr (lit "b") (regular))) 
                               (defnid b))
               )
             )
             (from
               (join 
                 (inner) 
                 (scan (path (vr (id t1 (regular)) (unqualified)) (path_expr (lit "a") (regular))) (defnid t) null null) 
                 (scan (path (vr (id t2 (regular)) (unqualified)) (path_expr (lit "x") (regular)) (path_unpivot) (path_expr (lit "b") (regular))) null null null)
                 null
               )
             )
             (where
               (and
                 (call (defnid test) (path (vr (id t2 (regular)) (unqualified)) (path_expr (lit "name") (regular))) (path (vr (id t1 (regular)) (unqualified)) (path_expr (lit "name") (regular))))
                 (eq (path (vr (id t1 (regular)) (unqualified)) (path_expr (lit "id") (regular))) (path (vr (id t2 (regular)) (unqualified)) (path_expr (lit "id") (regular))))
               )
             )
           )
        """
    )

    @Test
    fun selectValueWithSingleFrom() = assertExpression(
        "SELECT VALUE a FROM table1",
        "(select (project (project_value (vr (id a (regular)) (unqualified)))) (from (scan (vr (id table1 (regular)) (unqualified)) null null null)))"
    )

    @Test
    fun selectValueWithSingleAliasedFrom() = assertExpression(
        "SELECT VALUE v FROM table1 AS v",
        "(select (project (project_value (vr (id v (regular)) (unqualified)))) (from (scan (vr (id table1 (regular)) (unqualified)) (defnid v) null null)))"
    )

    @Test
    fun selectAllValues() = assertExpression(
        "SELECT ALL VALUE v FROM table1 AS v",
        "(select (project (project_value (vr (id v (regular)) (unqualified)))) (from (scan (vr (id table1 (regular)) (unqualified)) (defnid v) null null)))"
    )

    @Test
    fun selectDistinctValues() = assertExpression(
        "SELECT DISTINCT VALUE v FROM table1 AS v",
        """
            (select (setq (distinct)) 
                    (project (project_value (vr (id v (regular)) (unqualified)))) 
                    (from (scan (vr (id table1 (regular)) (unqualified)) (defnid v) null null)))"""
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
                                    (from (scan (vr (id x (regular)) (unqualified)) null null null)))
                                (path_expr (lit "a") (regular)))
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
                     (from (scan (vr (id x (regular)) (unqualified)) null null null))
                     (where (vr (id b (regular)) (unqualified)))
                   )
                   (path_expr (lit "a") (regular))
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
             (from (scan (vr (id a (regular)) (unqualified)) null null null))
             (limit (lit 10))
           )
        """
    )

    @Test
    fun selectWhereLimit() = assertExpression(
        "SELECT * FROM a WHERE a = 5 LIMIT 10",
        """(select
             (project (project_star))
             (from (scan (vr (id a (regular)) (unqualified)) null null null))
             (where (eq (vr (id a (regular)) (unqualified)) (lit 5)))
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
                    (project_expr (path (vr (id f (regular)) (unqualified)) (path_expr (lit "a") (regular))) null)))
            (from
                (scan 
                    (vr (id foo (regular)) (unqualified))
                    (defnid f)
                    null
                    null))
            (where
                (and
                    (and
                        (eq
                            (path
                                (vr (id f (regular)) (unqualified))
                                (path_expr (lit "bar") (regular)))
                            (parameter
                                2))
                        (eq
                            (path
                                (vr (id f (regular)) (unqualified))
                                (path_expr (lit "spam") (regular)))
                            (lit "eggs")))
                    (eq
                        (path
                            (vr (id f (regular)) (unqualified))
                            (path_expr (lit "baz") (regular)))
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
                        (vr (id a (regular)) (unqualified)) 
                        null)))
            (from 
                (scan 
                    (vr (id tb (regular)) (unqualified)) 
                    null null null)) 
            (where 
                (eq 
                    (vr (id hk (regular)) (unqualified)) 
                    (lit 1))) 
            (order 
                (order_by 
                    (sort_spec 
                        (vr (id rk1 (regular)) (unqualified)) 
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
                        (vr (id a (regular)) (unqualified)) 
                        null)))
            (from 
                (scan 
                    (vr (id tb (regular)) (unqualified)) 
                    null null null)) 
            (where 
                (eq 
                    (vr (id hk (regular)) (unqualified)) 
                    (lit 1))) 
            (order 
                (order_by
                    (sort_spec 
                        (vr (id rk1 (regular)) (unqualified)) 
                        null
                        null) 
                    (sort_spec 
                        (vr (id rk2 (regular)) (unqualified)) 
                        null
                        null) 
                    (sort_spec 
                        (vr (id rk3 (regular)) (unqualified)) 
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
                        (vr (id a (regular)) (unqualified)) 
                        null)))
            (from 
                (scan 
                    (vr (id tb (regular)) (unqualified)) 
                    null null null)) 
            (where 
                (eq 
                    (vr (id hk (regular)) (unqualified)) 
                    (lit 1))) 
            (order 
                (order_by 
                    (sort_spec 
                        (vr (id rk1 (regular)) (unqualified)) 
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
                        (vr (id a (regular)) (unqualified)) 
                        null)))
            (from 
                (scan 
                    (vr (id tb (regular)) (unqualified)) 
                    null null null)) 
            (where 
                (eq 
                    (vr (id hk (regular)) (unqualified)) 
                    (lit 1))) 
            (order 
                (order_by 
                    (sort_spec 
                        (vr (id rk1 (regular)) (unqualified)) 
                        (asc)
                        null)
                    (sort_spec 
                        (vr (id rk2 (regular)) (unqualified)) 
                        (desc)
                        null))))
        """
    )

    @Test
    fun orderBySingleIdWithoutOrderingAndNullsSpec() = assertExpression("SELECT x FROM tb ORDER BY rk1") {
        select(
            project = projectX,
            from = scan(vr("tb")),
            order = orderBy(
                listOf(
                    sortSpec(vr("rk1"))
                )
            )
        )
    }

    @Test
    fun orderByMultipleIdWithoutOrderingAndNullsSpec() =
        assertExpression("SELECT x FROM tb ORDER BY rk1, rk2, rk3, rk4") {
            select(
                project = projectX,
                from = scan(vr("tb")),
                order = orderBy(
                    listOf(
                        sortSpec(vr("rk1")),
                        sortSpec(vr("rk2")),
                        sortSpec(vr("rk3")),
                        sortSpec(vr("rk4"))
                    )
                )
            )
        }

    @Test
    fun orderByWithAsc() = assertExpression("SELECT x FROM tb ORDER BY rk1 asc") {
        select(
            project = projectX,
            from = scan(vr("tb")),
            order = orderBy(
                listOf(
                    sortSpec(vr("rk1"), asc())
                )
            )
        )
    }

    @Test
    fun orderByWithDesc() = assertExpression("SELECT x FROM tb ORDER BY rk1 desc") {
        select(
            project = projectX,
            from = scan(vr("tb")),
            order = orderBy(
                listOf(
                    sortSpec(vr("rk1"), desc())
                )
            )
        )
    }

    @Test
    fun orderByWithAscAndDesc() = assertExpression("SELECT x FROM tb ORDER BY rk1 desc, rk2 asc, rk3 asc, rk4 desc") {
        select(
            project = projectX,
            from = scan(vr("tb")),
            order = orderBy(
                listOf(
                    sortSpec(vr("rk1"), desc()),
                    sortSpec(vr("rk2"), asc()),
                    sortSpec(vr("rk3"), asc()),
                    sortSpec(vr("rk4"), desc())
                )
            )
        )
    }

    @Test
    fun orderByNoAscOrDescWithNullsFirst() = assertExpression("SELECT x FROM tb ORDER BY rk1 NULLS FIRST") {
        select(
            project = projectX,
            from = scan(vr("tb")),
            order = orderBy(
                listOf(
                    sortSpec(vr("rk1"), null, nullsFirst())
                )
            )
        )
    }

    @Test
    fun orderByNoAscOrDescWithNullsLast() = assertExpression("SELECT x FROM tb ORDER BY rk1 NULLS LAST") {
        select(
            project = projectX,
            from = scan(vr("tb")),
            order = orderBy(
                listOf(
                    sortSpec(vr("rk1"), null, nullsLast())
                )
            )
        )
    }

    @Test
    fun orderByAscWithNullsSpec() =
        assertExpression("SELECT x FROM tb ORDER BY rk1 asc NULLS FIRST, rk2 asc NULLS LAST") {
            select(
                project = projectX,
                from = scan(vr("tb")),
                order = orderBy(
                    listOf(
                        sortSpec(vr("rk1"), asc(), nullsFirst()),
                        sortSpec(vr("rk2"), asc(), nullsLast())
                    )
                )
            )
        }

    @Test
    fun orderByDescWithNullsSpec() =
        assertExpression("SELECT x FROM tb ORDER BY rk1 desc NULLS FIRST, rk2 desc NULLS LAST") {
            select(
                project = projectX,
                from = scan(vr("tb")),
                order = orderBy(
                    listOf(
                        sortSpec(vr("rk1"), desc(), nullsFirst()),
                        sortSpec(vr("rk2"), desc(), nullsLast())
                    )
                )
            )
        }

    @Test
    fun orderByWithOrderingAndNullsSpec() =
        assertExpression("SELECT x FROM tb ORDER BY rk1 desc NULLS FIRST, rk2 asc NULLS LAST, rk3 desc NULLS LAST, rk4 asc NULLS FIRST") {
            select(
                project = projectX,
                from = scan(vr("tb")),
                order = orderBy(
                    listOf(
                        sortSpec(vr("rk1"), desc(), nullsFirst()),
                        sortSpec(vr("rk2"), asc(), nullsLast()),
                        sortSpec(vr("rk3"), desc(), nullsLast()),
                        sortSpec(vr("rk4"), asc(), nullsFirst())
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
             (project (project_list (project_expr (vr (id a (regular)) (unqualified)) null)))
             (from (scan (vr (id data (regular)) (unqualified)) null null null))
             (group (group_by (group_full) (group_key_list (group_key (vr (id a (regular)) (unqualified)) null)) null))
           )
        """
    )

    @Test
    fun groupBySingleExpr() = assertExpression(
        "SELECT a + b FROM data GROUP BY a + b",
        """(select
             (project (project_list (project_expr (plus (vr (id a (regular)) (unqualified)) (vr (id b (regular)) (unqualified))) null)))
             (from (scan (vr (id data (regular)) (unqualified)) null null null))
             (group (group_by (group_full) (group_key_list (group_key (plus (vr (id a (regular)) (unqualified)) (vr (id b (regular)) (unqualified))) null)) null))
           )
        """
    )

    @Test
    fun groupPartialByMultiAliasedAndGroupAliased() = assertExpression(
        "SELECT g FROM data GROUP PARTIAL BY a AS x, b + c AS y, foo(d) AS z GROUP AS g",
        """(select
             (project (project_list (project_expr (vr (id g (regular)) (unqualified)) null)))
             (from (scan (vr (id data (regular)) (unqualified)) null null null))
             (group
                (group_by
                    (group_partial)
                    (group_key_list
                        (group_key
                            (vr (id a (regular)) (unqualified))
                            (defnid x))
                        (group_key
                            (plus
                                (vr (id b (regular)) (unqualified))
                                (vr (id c (regular)) (unqualified)))
                            (defnid y))
                        (group_key
                            (call
                                (defnid foo)
                                (vr (id d (regular)) (unqualified)))
                            (defnid z))
                        )
                    (defnid g)
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
            (project (project_list (project_expr (vr (id a (regular)) (unqualified)) null)))
            (from (scan (vr (id data (regular)) (unqualified)) null null null))
            (having (eq (vr (id a (regular)) (unqualified)) (vr (id b (regular)) (unqualified))))
          )
        """
    )

    @Test
    fun havingWithWhere() = assertExpression(
        "SELECT a FROM data WHERE a = b HAVING c = d",
        """
          (select
            (project (project_list (project_expr (vr (id a (regular)) (unqualified)) null)))
            (from (scan (vr (id data (regular)) (unqualified)) null null null))
            (where (eq (vr (id a (regular)) (unqualified)) (vr (id b (regular)) (unqualified))))
            (having (eq (vr (id c (regular)) (unqualified)) (vr (id d (regular)) (unqualified))))
          )
        """
    )

    @Test
    fun havingWithWhereAndGroupBy() = assertExpression(
        "SELECT g FROM data WHERE a = b GROUP BY c, d GROUP AS g HAVING d > 6",
        """
          (select
            (project (project_list (project_expr (vr (id g (regular)) (unqualified)) null)))
            (from (scan (vr (id data (regular)) (unqualified)) null null null))
            (where (eq (vr (id a (regular)) (unqualified)) (vr (id b (regular)) (unqualified))))
            (group (group_by (group_full) 
                             (group_key_list (group_key (vr (id c (regular)) (unqualified)) null) 
                                             (group_key (vr (id d (regular)) (unqualified)) null)) 
                             (defnid g)))
            (having (gt (vr (id d (regular)) (unqualified)) (lit 6)))
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
                    (vr (id n (regular)) (unqualified)) 
                    (vr (id v (regular)) (unqualified))))
            (from (scan (vr (id data (regular)) (unqualified)) null null null))
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
                (concat (lit "prefix:") (vr (id c (regular)) (unqualified))) 
                (vr (id g (regular)) (unqualified))))
            (from (scan (vr (id data (regular)) (unqualified)) null null null))
            (where (eq (vr (id a (regular)) (unqualified)) (vr (id b (regular)) (unqualified))))
            (group (group_by (group_full) (group_key_list (group_key (vr (id c (regular)) (unqualified)) null) (group_key (vr (id d (regular)) (unqualified)) null)) (defnid g)))
            (having (gt (vr (id d (regular)) (unqualified)) (lit 6)))
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
                        (vr (id foo (regular)) (unqualified))
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
                    (vr (id x (regular)) (unqualified))
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
                  (vr (id foo (regular)) (unqualified))
                  (lit 1)
                  (vr (id bar (regular)) (unqualified))
                  null
                )
              )
            )
            (from (scan (vr (id x (regular)) (unqualified)) null null null))
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
                  (vr (id foo (regular)) (unqualified))
                  (lit 1)
                  (vr (id bar (regular)) (unqualified))
                )
              )
            )
            (from (scan (vr (id x (regular)) (unqualified)) null null null))
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
                  (vr (id foo (regular)) (unqualified))
                  (lit 1)
                  null null)))
            (from (scan (vr (id x (regular)) (unqualified)) null null null))
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
                (vr (id foo case_insensitive)
                (lit 1))
            )
            (from (vr (id x case_insensitive))
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
                        (vr (id foo (regular)) (unqualified))
                        null
                        (select
                            (project
                                (project_list
                                    (project_expr
                                        (vr (id y (regular)) (unqualified))
                                        null)))
                            (from
                                (scan
                                    (vr (id bar (regular)) (unqualified))
                                    null
                                    null
                                    null)))
                        null)))
            (from
                (scan
                    (vr (id x (regular)) (unqualified))
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
                  (vr (id foo (regular)) (unqualified))
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
                (dml_op_list (insert_value (vr (id foo (regular)) (unqualified)) (lit 1) null null)))
                (returning 
                    (returning_expr 
                        (returning_elem 
                            (modified_old) 
                            (returning_column (vr (id foo (regular)) (unqualified)))))))
        """
    )

    @Test
    fun insertValueReturningStarDml() = assertExpression(
        "INSERT INTO foo VALUE 1 RETURNING ALL OLD *",
        """
        (dml 
            (operations 
                (dml_op_list (insert_value (vr (id foo (regular)) (unqualified)) (lit 1) null null)))
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
                        (vr (id foo (regular)) (unqualified))
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
                  (vr (id foo (regular)) (unqualified))
                  (lit 1)
                  (vr (id bar (regular)) (unqualified))
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
                (dml_op_list (insert_value (vr (id foo (regular)) (unqualified)) 
                (lit 1) (vr (id bar (regular)) (unqualified)) null)))
                (returning 
                    (returning_expr 
                        (returning_elem 
                            (all_old) 
                            (returning_column (vr (id foo (regular)) (unqualified)))))))
        """
    )

    @Test
    fun insertValueAtMultiReturningTwoColsDml() = assertExpression(
        "INSERT INTO foo VALUE 1 AT bar RETURNING ALL OLD a",
        """
        (dml 
            (operations 
                (dml_op_list (insert_value (vr (id foo (regular)) (unqualified)) 
                (lit 1) (vr (id bar (regular)) (unqualified)) null)))
                (returning 
                    (returning_expr 
                        (returning_elem 
                            (all_old) 
                            (returning_column (vr (id a (regular)) (unqualified)))))))
        """
    )

    @Test
    fun insertValueAtMultiReturningThreeColsDml() = assertExpression(
        "INSERT INTO foo VALUE 1 AT bar RETURNING MODIFIED OLD bar, MODIFIED NEW bar, ALL NEW *",
        """
            (dml 
                (operations 
                    (dml_op_list (insert_value (vr (id foo (regular)) (unqualified)) 
                    (lit 1) (vr (id bar (regular)) (unqualified)) null)))
                (returning 
                    (returning_expr 
                        (returning_elem 
                            (modified_old) 
                            (returning_column (vr (id bar (regular)) (unqualified)))) 
                        (returning_elem 
                            (modified_new) 
                            (returning_column (vr (id bar (regular)) (unqualified)))) 
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
                (vr (id foo (regular)) (unqualified))
                (lit 1)
                (vr (id bar (regular)) (unqualified))
                (on_conflict
                    (vr (id a (regular)) (unqualified))
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
            (vr (id foo (regular)) (unqualified))
            (lit 1)
            (vr (id bar (regular)) (unqualified))
            (on_conflict
                (vr (id a (regular)) (unqualified))
                (do_nothing)))))
        (returning 
            (returning_expr 
                (returning_elem 
                    (all_old) 
                    (returning_column (vr (id foo (regular)) (unqualified)))))))
        """
    )

    @Test
    fun insertValueOnConflictDml() = assertExpression(
        "INSERT INTO foo VALUE 1 ON CONFLICT WHERE bar DO NOTHING",
        """
          (dml
            (operations (dml_op_list
              (insert_value
                (vr (id foo (regular)) (unqualified))
                (lit 1)
                null
                (on_conflict
                    (vr (id bar (regular)) (unqualified))
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
                (vr (id foo (regular)) (unqualified))
                (lit 1)
                null
                (on_conflict
                    (eq (vr (id hk (regular)) (unqualified)) (lit 1))
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
                (vr (id foo (regular)) (unqualified))
                (lit 1)
                null
                (on_conflict
                    (and (eq (vr (id hk (regular)) (unqualified)) (lit 1)) (eq (vr (id rk (regular)) (unqualified)) (lit 1)))
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
                (vr (id foo (regular)) (unqualified))
                (lit 1)
                null
                (on_conflict
                    (or (between (vr (id hk (regular)) (unqualified)) (lit "a") (lit "b")) (eq (vr (id rk (regular)) (unqualified)) (lit "c")))
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
                (vr (id foo (regular)) (unqualified))
                (lit 1)
                null
                (on_conflict
                    (not (eq (vr (id hk (regular)) (unqualified)) (lit "a")))
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
                (vr (id foo (regular)) (unqualified))
                (lit 1)
                null
                (on_conflict
                    (call (defnid attribute_exists) (vr (id hk (regular)) (unqualified)))
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
                (vr (id foo (regular)) (unqualified))
                (lit 1)
                null
                (on_conflict
                    (not (call (defnid attribute_exists) (vr (id hk (regular)) (unqualified))))
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
                        (vr (id foo (regular)) (unqualified))
                        null
                        (select
                            (project
                                (project_list
                                    (project_expr
                                        (vr (id y (regular)) (unqualified))
                                        null)))
                            (from
                                (scan
                                    (vr (id bar (regular)) (unqualified))
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
                        (vr (id foo (regular)) (unqualified))
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
                        (vr (id foo (regular)) (unqualified))
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
                                    (vr (id foo (regular)) (unqualified))
                                    (path_expr
                                        (lit "id")
                                        (regular)))
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
                        (vr (id foo (regular)) (unqualified))
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
                                    (vr (id excluded (regular)) (unqualified))
                                    (path_expr
                                        (lit "id")
                                        (regular)))
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
                            (vr (id foo (regular)) (unqualified))
                            (defnid f)
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
                            (vr (id foo (regular)) (unqualified))
                            (defnid f)
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
                                    (vr (id f (regular)) (unqualified))
                                    (path_expr
                                        (lit "id")
                                        (regular)))
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
                            (vr (id foo (regular)) (unqualified))
                            (defnid f)
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
                                    (vr (id excluded (regular)) (unqualified))
                                    (path_expr
                                        (lit "id")
                                        (regular)))
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
                            (vr (id foo (regular)) (unqualified))
                            null
                            (select
                                (project
                                    (project_list
                                        (project_expr
                                            (path
                                                (vr (id bar (regular)) (unqualified))
                                                (path_expr
                                                    (lit "id")
                                                    (regular)))
                                            null)
                                        (project_expr
                                            (path
                                                (vr (id bar (regular)) (unqualified))
                                                (path_expr
                                                    (lit "name")
                                                    (regular)))
                                            null)))
                                (from
                                    (scan
                                        (vr (id bar (regular)) (unqualified))
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
                        (vr (id foo (regular)) (unqualified))
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
                        (vr (id foo (regular)) (unqualified))
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
                                    (vr (id foo (regular)) (unqualified))
                                    (path_expr
                                        (lit "id")
                                        (regular)))
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
                        (vr (id foo (regular)) (unqualified))
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
                                    (vr (id excluded (regular)) (unqualified))
                                    (path_expr
                                        (lit "id")
                                        (regular)))
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
                            (vr (id foo (regular)) (unqualified))
                            (defnid f)
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
                            (vr (id foo (regular)) (unqualified))
                            (defnid f)
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
                                    (vr (id f (regular)) (unqualified))
                                    (path_expr
                                        (lit "id")
                                        (regular)))
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
                            (vr (id foo (regular)) (unqualified))
                            (defnid f)
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
                                    (vr (id excluded (regular)) (unqualified))
                                    (path_expr
                                        (lit "id")
                                        (regular)))
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
                            (vr (id foo (regular)) (unqualified))
                            null
                            (select
                                (project
                                    (project_list
                                        (project_expr
                                            (path
                                                (vr (id bar (regular)) (unqualified))
                                                (path_expr
                                                    (lit "id")
                                                    (regular)))
                                            null)
                                        (project_expr
                                            (path
                                                (vr (id bar (regular)) (unqualified))
                                                (path_expr
                                                    (lit "name")
                                                    (regular)))
                                            null)))
                                (from
                                    (scan
                                        (vr (id bar (regular)) (unqualified))
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
                            (vr (id foo (regular)) (unqualified))
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
                            (vr (id foo (regular)) (unqualified))
                            (defnid f)
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
                            (vr (id foo (regular)) (unqualified))
                            null
                            (select
                                (project
                                    (project_list
                                        (project_expr
                                            (path
                                                (vr (id bar (regular)) (unqualified))
                                                (path_expr
                                                    (lit "id")
                                                    (regular)))
                                            null)
                                        (project_expr
                                            (path
                                                (vr (id bar (regular)) (unqualified))
                                                (path_expr
                                                    (lit "name")
                                                    (regular)))
                                            null)))
                                (from
                                    (scan
                                        (vr (id bar (regular)) (unqualified))
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
                            (vr (id foo (regular)) (unqualified))
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
                            (vr (id foo (regular)) (unqualified))
                            (defnid f)
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
                            (vr (id foo (regular)) (unqualified))
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
                            (vr (id foo (regular)) (unqualified))
                            (defnid f)
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
                            (vr (id foo (regular)) (unqualified))
                            null
                            (select
                                (project
                                    (project_list
                                        (project_expr
                                            (path
                                                (vr (id bar (regular)) (unqualified))
                                                (path_expr
                                                    (lit "id")
                                                    (regular)))
                                            null)
                                        (project_expr
                                            (path
                                                (vr (id bar (regular)) (unqualified))
                                                (path_expr
                                                    (lit "name")
                                                    (regular)))
                                            null)))
                                (from
                                    (scan
                                        (vr (id bar (regular)) (unqualified))
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
                        (vr (id foo (regular)) (unqualified))
                        null
                        (select
                            (project
                                (project_list
                                    (project_expr
                                        (path
                                            (vr (id bar (regular)) (unqualified))
                                            (path_expr
                                                (lit "id")
                                                (regular)))
                                        null)
                                    (project_expr
                                        (path
                                            (vr (id bar (regular)) (unqualified))
                                            (path_expr
                                                (lit "name")
                                                (regular)))
                                        null)))
                            (from
                                (scan
                                    (vr (id bar (regular)) (unqualified))
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
                  (vr (id foo (regular)) (unqualified))
                  (select
                    (project (project_list (project_expr (vr (id y (regular)) (unqualified)) null)))
                    (from (scan (vr (id bar (regular)) (unqualified)) null null null))
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
                    (vr (id k (regular)) (unqualified))
                    (lit 5)
                  )
                )
              )
            )
            (from (scan (vr (id x (regular)) (unqualified)) null null null))
            (where (eq (vr (id a (regular)) (unqualified)) (vr (id b (regular)) (unqualified))))
          )
        """
    )

    @Test
    fun fromSetSingleReturningDml() = assertExpression(
        "FROM x WHERE a = b SET k = 5 RETURNING ALL OLD x",
        """
        (dml (operations (dml_op_list (set (assignment (vr (id k (regular)) (unqualified)) (lit 5)))))
        (from (scan (vr (id x (regular)) (unqualified)) null null null))
        (where (eq (vr (id a (regular)) (unqualified)) (vr (id b (regular)) (unqualified))))
        (returning 
            (returning_expr 
                (returning_elem 
                    (all_old) 
                    (returning_column (vr (id x (regular)) (unqualified)))))))
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
                    (path (vr (id k (regular)) (unqualified)) (path_expr (lit "m") (regular)))
                    (lit 5)
                  )
                )
              )
            )
            (from (scan (vr (id x (regular)) (unqualified)) null null null))
            (where (eq (vr (id a (regular)) (unqualified)) (vr (id b (regular)) (unqualified))))
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
                    (path (vr (id k (regular)) (unqualified)) (path_expr (lit "m") (delimited)))
                    (lit 5)
                  )
                )
              )
            )
            (from (scan (vr (id x (regular)) (unqualified)) null null null))
            (where (eq (vr (id a (regular)) (unqualified)) (vr (id b (regular)) (unqualified))))
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
                    (path (vr (id k (regular)) (unqualified)) (path_expr (lit 3) (delimited)))
                    (lit 5)
                  )
                )
              )
            )
            (from (scan (vr (id x (regular)) (unqualified)) null null null))
            (where (eq (vr (id a (regular)) (unqualified)) (vr (id b (regular)) (unqualified))))
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
                    (vr (id k (regular)) (unqualified))
                    (lit 5)
                  )
                )
                (set
                  (assignment
                    (vr (id m (regular)) (unqualified))
                    (lit 6)
                  )
                )
              )
            )
            (from (scan (vr (id x (regular)) (unqualified)) null null null))
            (where (eq (vr (id a (regular)) (unqualified)) (vr (id b (regular)) (unqualified))))
          )
        """
    )

    @Test
    fun fromSetMultiReturningDml() = assertExpression(
        "FROM x WHERE a = b SET k = 5, m = 6 RETURNING ALL OLD x",
        """
        (dml (operations 
            (dml_op_list 
                (set (assignment (vr (id k (regular)) (unqualified)) (lit 5))) 
                (set (assignment (vr (id m (regular)) (unqualified)) (lit 6)))))
        (from (scan (vr (id x (regular)) (unqualified)) null null null))
        (where (eq (vr (id a (regular)) (unqualified)) (vr (id b (regular)) (unqualified))))
        (returning 
            (returning_expr 
                (returning_elem 
                    (all_old) 
                    (returning_column (vr (id x (regular)) (unqualified)))))))
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
                            (vr (id k (regular)) (unqualified))
                            (lit 5)))
                    (set
                        (assignment
                            (vr (id m (regular)) (unqualified))
                            (lit 6)))
                    (insert_value
                        (vr (id c (regular)) (unqualified))
                        (bag
                            (lit 1))
                        null null)
                    (remove
                        (vr (id a (regular)) (unqualified)))
                    (set
                        (assignment
                            (vr (id l (regular)) (unqualified))
                            (lit 3)))
                    (remove
                        (vr (id b (regular)) (unqualified)))))
            (from
                (scan
                    (vr (id x (regular)) (unqualified))
                    null
                    null
                    null))
            (where
                (eq
                    (vr (id a (regular)) (unqualified))
                    (vr (id b (regular)) (unqualified)))))
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
                                (vr (id k (regular)) (unqualified))
                                (lit 5)))
                        (set
                            (assignment
                                (vr (id m (regular)) (unqualified))
                                (lit 6)))
                        (insert_value
                            (vr (id c (regular)) (unqualified))
                            (bag
                                (lit 1))
                            null null)
                        (remove
                            (vr (id a (regular)) (unqualified)))
                        (set
                            (assignment
                                (vr (id l (regular)) (unqualified))
                                (lit 3)))
                        (remove
                            (vr (id b (regular)) (unqualified)))))
                (from
                    (scan
                        (vr (id x (regular)) (unqualified))
                        null
                        null
                        null))
                (where
                    (eq
                        (vr (id a (regular)) (unqualified))
                        (vr (id b (regular)) (unqualified)))))
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
                            (vr (id k (regular)) (unqualified))
                            (lit 5)))
                    (set
                        (assignment
                            (vr (id m (regular)) (unqualified))
                            (lit 6)))
                    (insert_value
                        (vr (id c (regular)) (unqualified))
                        (bag
                            (lit 1))
                        null null)
                    (remove
                        (vr (id a (regular)) (unqualified)))
                    (set
                        (assignment
                            (vr (id l (regular)) (unqualified))
                            (lit 3)))
                    (remove
                        (vr (id b (regular)) (unqualified)))))
            (from
                (scan
                    (vr (id x (regular)) (unqualified))
                    null
                    null
                    null))
            (where
                (eq
                    (vr (id a (regular)) (unqualified))
                    (vr (id b (regular)) (unqualified))))
            (returning 
                (returning_expr 
                    (returning_elem 
                        (modified_old) 
                        (returning_column (vr (id a (regular)) (unqualified)))))))        
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
                    (vr (id k (regular)) (unqualified))
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
                    (vr (id k (delimited)) (unqualified))
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
                    (vr (id k (regular)) (unqualified))
                    (lit 5)
                  )
                )
                (set
                  (assignment
                    (vr (id m (regular)) (unqualified))
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
                  (vr (id y (regular)) (unqualified))
                )
              )
            )
            (from (scan (vr (id x (regular)) (unqualified)) null null null))
            (where (eq (vr (id a (regular)) (unqualified)) (vr (id b (regular)) (unqualified))))
          )
        """
    )

    @Test
    fun fromRemoveReturningDml() = assertExpression(
        "FROM x WHERE a = b REMOVE y RETURNING MODIFIED NEW a",
        """
        (dml (operations 
            (dml_op_list (remove (vr (id y (regular)) (unqualified))))) 
            (from (scan (vr (id x (regular)) (unqualified)) null null null)) 
            (where (eq (vr (id a (regular)) (unqualified)) (vr (id b (regular)) (unqualified)))) 
            (returning 
                (returning_expr 
                    (returning_elem 
                        (modified_new) 
                        (returning_column (vr (id a (regular)) (unqualified)))))))
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
                  (vr (id y (regular)) (unqualified))
                )
                (remove
                  (vr (id z (regular)) (unqualified))
                )
              )
            )
            (from (scan (vr (id x (regular)) (unqualified)) null null null))
            (where (eq (vr (id a (regular)) (unqualified)) (vr (id b (regular)) (unqualified))))
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
              (vr (id y (regular)) (unqualified))
            )
            (remove
              (vr (id z (regular)) (unqualified))
            )
          )
        )
        (from (scan (vr (id x (regular)) (unqualified)) null null null))
        (where (eq (vr (id a (regular)) (unqualified)) (vr (id b (regular)) (unqualified))))
        (returning 
            (returning_expr 
                (returning_elem 
                    (modified_old) 
                    (returning_column (vr (id a (regular)) (unqualified)))))))
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
                  (vr (id y (regular)) (unqualified))
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
                    (vr (id a (regular)) (unqualified))
                    (path_expr (lit "b") (regular))
                    (path_expr (lit "c") (delimited))
                    (path_expr (lit 2) (delimited))
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
                    (vr (id k (regular)) (unqualified))
                    (lit 5)
                  )
                )
                (set
                  (assignment
                    (vr (id m (regular)) (unqualified))
                    (lit 6)
                  )
                )
              )
            )
            (from (scan (vr (id x (regular)) (unqualified)) (defnid y) null null))
            (where (eq (vr (id a (regular)) (unqualified)) (vr (id b (regular)) (unqualified))))
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
                (vr (id k (regular)) (unqualified))
                (lit 5)
              )
            )
            (set
              (assignment
                (vr (id m (regular)) (unqualified))
                (lit 6)
              )
            )
          )
        )
        (from (scan (vr (id x (regular)) (unqualified)) (defnid y) null null))
        (where (eq (vr (id a (regular)) (unqualified)) (vr (id b (regular)) (unqualified))))
        (returning 
            (returning_expr 
                (returning_elem 
                    (modified_old) 
                    (returning_column (vr (id a (regular)) (unqualified)))))))
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
                        (vr (id k (regular)) (unqualified))
                        null
                        (bag
                            (lit 1))
                        null)))
            (from
                (scan
                    (vr (id x (regular)) (unqualified))
                    (defnid y)
                    null
                    null))
            (where
                (eq
                    (vr (id a (regular)) (unqualified))
                    (vr (id b (regular)) (unqualified)))))
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
                        (vr (id k (regular)) (unqualified))
                        null
                        (bag
                            (lit 1))
                        null)))
            (from
                (scan
                    (vr (id x (regular)) (unqualified))
                    (defnid y)
                    null
                    null))
            (where
                (eq
                    (vr (id a (regular)) (unqualified))
                    (vr (id b (regular)) (unqualified))))
            (returning
                (returning_expr
                    (returning_elem
                        (modified_old)
                        (returning_column
                            (vr (id a (regular)) (unqualified)))))))
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
                  (vr (id k (regular)) (unqualified))
                  (lit 1)
                  (lit "j")
                  null
                )
              )
            )    
            (from
              (scan (vr (id x (regular)) (unqualified)) (defnid y) null null))
            (where
              (eq
                (vr (id a (regular)) (unqualified))
                (vr (id b (regular)) (unqualified)))))
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
                    (vr (id y (regular)) (unqualified))
                    (path_expr (lit "a") (regular))))))
            (from (scan (vr (id x (regular)) (unqualified)) (defnid y) null null))
            (where (eq (vr (id a (regular)) (unqualified)) (vr (id b (regular)) (unqualified))))
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
                    (path (vr (id z (regular)) (unqualified)) (path_expr (lit "kingdom") (regular)))
                    (lit "Fungi")))))
            (from
              (scan (vr (id zoo (regular)) (unqualified)) (defnid z) null null)))
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
                    (path (vr (id z (regular)) (unqualified)) (path_expr (lit "kingdom") (regular)))
                    (lit "Fungi")))))
            (from
              (scan (vr (id zoo (regular)) (unqualified)) null (defnid z_ord) null)))
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
                    (path (vr (id z (regular)) (unqualified)) (path_expr (lit "kingdom") (regular)))
                    (lit "Fungi")))))
            (from
              (scan (vr (id zoo (regular)) (unqualified)) null null (defnid z_id))))
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
                    (path (vr (id z (regular)) (unqualified)) (path_expr (lit "kingdom") (regular)))
                    (lit "Fungi")))))
            (from
              (scan (vr (id zoo (regular)) (unqualified)) null (defnid z_ord) (defnid z_id))))
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
                    (vr (id k (regular)) (unqualified))
                    (lit 5)
                  )
                )
                (set
                  (assignment
                    (vr (id m (regular)) (unqualified))
                    (lit 6)
                  )
                )
              )
            )
            (from (scan (vr (id x (regular)) (unqualified)) null null null))
            (where (eq (vr (id a (regular)) (unqualified)) (vr (id b (regular)) (unqualified))))
          )
        """
    )

    @Test
    fun updateWhereReturningDml() = assertExpression(
        "UPDATE x SET k = 5, m = 6 WHERE a = b RETURNING MODIFIED OLD a, MODIFIED OLD b",
        """(dml 
        (operations 
            (dml_op_list 
                (set (assignment (vr (id k (regular)) (unqualified)) (lit 5))) 
                (set (assignment (vr (id m (regular)) (unqualified)) (lit 6))))) 
        (from (scan (vr (id x (regular)) (unqualified)) null null null)) 
        (where (eq (vr (id a (regular)) (unqualified)) (vr (id b (regular)) (unqualified))))
        (returning 
            (returning_expr 
                (returning_elem 
                    (modified_old) 
                    (returning_column (vr (id a (regular)) (unqualified)))) 
                (returning_elem 
                    (modified_old) 
                    (returning_column (vr (id b (regular)) (unqualified)))))))
        """
    )

    @Test
    fun updateWhereReturningPathDml() = assertExpression(
        "UPDATE x SET k = 5, m = 6 WHERE a = b RETURNING MODIFIED OLD a.b",
        """(dml 
            (operations 
                (dml_op_list 
                    (set (assignment (vr (id k (regular)) (unqualified)) (lit 5))) 
                    (set (assignment (vr (id m (regular)) (unqualified)) (lit 6))))) 
            (from (scan (vr (id x (regular)) (unqualified)) null null null)) 
            (where (eq (vr (id a (regular)) (unqualified)) (vr (id b (regular)) (unqualified))))
            (returning 
                (returning_expr 
                    (returning_elem 
                        (modified_old) 
                        (returning_column 
                            (path (vr (id a (regular)) (unqualified)) 
                            (path_expr (lit "b") (regular))))))))
        """
    )

    @Test
    fun updateWhereReturningPathAsteriskDml() = assertExpression(
        "UPDATE x SET k = 5, m = 6 WHERE a = b RETURNING MODIFIED OLD '1234'.*",
        """(dml 
        (operations 
            (dml_op_list 
                (set (assignment (vr (id k (regular)) (unqualified)) (lit 5))) 
                (set (assignment (vr (id m (regular)) (unqualified)) (lit 6))))) 
        (from (scan (vr (id x (regular)) (unqualified)) null null null)) 
        (where (eq (vr (id a (regular)) (unqualified)) (vr (id b (regular)) (unqualified))))
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
                      (vr (id k (regular)) (unqualified))
                      (lit 5)
                    )
                  )
                  (set
                    (assignment
                      (vr (id m (regular)) (unqualified))
                      (lit 6)
                    )
                  )
               )
            )
            (from (scan (vr (id x (regular)) (unqualified)) null null null))
            (where (eq (vr (id a (regular)) (unqualified)) (vr (id b (regular)) (unqualified))))
          )
        """
    )

    @Test
    fun updateMultipleSetsWhereReturningDml() = assertExpression(
        "UPDATE x SET k = 5 SET m = 6 WHERE a = b RETURNING ALL OLD x.*",
        """
        (dml (operations (dml_op_list 
            (set (assignment (vr (id k (regular)) (unqualified)) (lit 5))) 
            (set (assignment (vr (id m (regular)) (unqualified)) (lit 6)))))
        (from (scan (vr (id x (regular)) (unqualified)) null null null))
        (where (eq (vr (id a (regular)) (unqualified)) (vr (id b (regular)) (unqualified))))
        (returning 
            (returning_expr 
                (returning_elem 
                    (all_old) 
                    (returning_column 
                        (path (vr (id x (regular)) (unqualified)) 
                        (path_unpivot)))))))
        """
    )

    @Test
    fun deleteDml() = assertExpression(
        "DELETE FROM y",
        """
          (dml
            (operations (dml_op_list (delete)))
            (from (scan (vr (id y (regular)) (unqualified)) null null null))
          )
        """
    )

    @Test
    fun deleteReturningDml() = assertExpression(
        "DELETE FROM y RETURNING MODIFIED NEW a",
        """
      (dml
        (operations (dml_op_list (delete)))
        (from (scan (vr (id y (regular)) (unqualified)) null null null))
        (returning 
            (returning_expr 
                (returning_elem 
                    (modified_new) 
                    (returning_column (vr (id a (regular)) (unqualified)))))))
        """
    )

    @Test
    fun deleteDmlAliased() = assertExpression(
        "DELETE FROM x AS y",
        """
          (dml
            (operations (dml_op_list (delete)))
            (from (scan (vr (id x (regular)) (unqualified)) (defnid y) null null))
          )
        """
    )

    @Test
    fun canParseADeleteQueryWithAPositionClause() = assertExpression(
        "DELETE FROM x AT y",
        """
            (dml
              (operations ( dml_op_list (delete)))
              (from (scan (vr (id x (regular)) (unqualified)) null (defnid y) null)))
        """
    )

    @Test
    fun canParseADeleteQueryWithAliasAndPositionClause() = assertExpression(
        "DELETE FROM x AS y AT z",
        """
            (dml
               (operations (dml_op_list (delete)))
               (from (scan (vr (id x (regular)) (unqualified)) (defnid y) (defnid z) null)))
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
                        (path (vr (id x (regular)) (unqualified)) (path_expr (lit "n") (regular)))
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
                            (vr (id x (regular)) (unqualified))
                            (path_expr (lit "n") (regular))
                            (path_expr (lit "m") (regular)))
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
                            (vr (id x (regular)) (unqualified))
                            (path_expr (lit "n") (regular))
                            (path_expr (lit "m") (regular)))
                        (defnid y)
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
                            (vr (id x (regular)) (unqualified))
                            (path_expr (lit "n") (regular))
                            (path_expr (lit "m") (regular)))
                        (defnid y)
                        (defnid z)
                        null)))
        """
    )

    // DDL
    // ****************************************
    @Test
    fun createTable() = assertExpression(
        "CREATE TABLE foo",
        "(ddl (create_table (id foo (regular)) null))"
    )

    @Test
    fun createTableWithColumn() = assertExpression(
        "CREATE TABLE foo (boo string)",
        """
            (ddl (create_table (id foo (regular))  
              (table_def
                (column_declaration (defnid boo) (string_type)))))
        """.trimIndent()
    )

    @Test
    fun createTableWithQuotedIdentifier() = assertExpression(
        "CREATE TABLE \"user\" (\"lastname\" string)",
        """
            (ddl (create_table (id user (delimited)) 
              (table_def
                (column_declaration (defnid lastname) (string_type)))))
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
                    (id
                        Customer
                        (regular)) 
                    (table_def
                        (column_declaration (defnid name) (string_type)
                            (column_constraint (defnid name_is_present) (column_notnull)))
                        (column_declaration (defnid age) (integer_type))
                        (column_declaration (defnid city) (string_type)
                            (column_constraint null (column_null)))
                        (column_declaration (defnid state) (string_type)
                            (column_constraint null (column_null))))))
        """.trimIndent()
    )

    @Test
    fun dropTable() = assertExpression(
        "DROP TABLE foo",
        "(ddl (drop_table (id foo (regular))))"
    )

    @Test
    fun dropTableWithQuotedIdentifier() = assertExpression(
        "DROP TABLE \"user\"",
        "(ddl (drop_table (id user (delimited))))"
    )

    @Test
    fun createIndex() = assertExpression(
        "CREATE INDEX ON foo (x, y.z)",
        """
        (ddl
          (create_index
            (id foo (regular))
            (vr (id x (regular)) (unqualified))
            (path (vr (id y (regular)) (unqualified)) (path_expr (lit "z") (regular)))))
        """
    )

    @Test
    fun createIndexWithQuotedIdentifiers() = assertExpression(
        "CREATE INDEX ON \"user\" (\"group\")",
        """
        (ddl
          (create_index
            (id user (delimited))
            (vr (id group (delimited)) (unqualified))))
        """
    )

    @Test
    fun dropIndex() = assertExpression(
        "DROP INDEX bar ON foo",
        "(ddl (drop_index (table (id foo (regular))) (keys (id bar (regular)))))"
    )

    @Test
    fun dropIndexWithQuotedIdentifiers() = assertExpression(
        "DROP INDEX \"bar\" ON \"foo\"",
        "(ddl (drop_index (table (id foo (delimited))) (keys (id bar (delimited)))))"
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
                        (vr (id foo (regular)) (unqualified))
                        null
                        null
                        null)))
            (select
                (project
                    (project_star))
                (from
                    (scan
                        (vr (id bar (regular)) (unqualified))
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
                vr("a"),
                vr("b")
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
                vr("a"),
                vr("b")
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
                vr("a"),
                vr("b")
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
                vr("a"),
                vr("b")
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
                vr("a"),
                vr("b")
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
                vr("a"),
                vr("b")
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
                vr("a"),
                vr("b")
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
                vr("a"),
                vr("b")
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
                vr("a"),
                vr("b")
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
                this.vr("a"),
                this.vr("b")
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
                this.vr("a"),
                this.vr("b")
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
                this.vr("a"),
                this.vr("b")
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
                this.vr("a"),
                this.vr("b")
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
                this.vr("a"),
                this.vr("b")
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
                this.vr("a"),
                this.vr("b")
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
                this.vr("a"),
                this.vr("b")
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
                this.vr("a"),
                this.vr("b")
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
                this.vr("a"),
                this.vr("b")
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

    private val projectX = PartiqlAst.build { projectList(projectExpr(vr("x"))) }

    @Test
    fun selectFromLetTest() = assertExpression("SELECT x FROM table1 LET 1 AS A") {
        select(
            project = projectX,
            from = scan(vr("table1")),
            fromLet = let(letBinding(lit(ionInt(1)), defnid("A")))
        )
    }

    @Test
    fun selectFromLetTwoBindingsTest() = assertExpression("SELECT x FROM table1 LET 1 AS A, 2 AS B") {
        select(
            project = projectX,
            from = scan(vr("table1")),
            fromLet = let(letBinding(lit(ionInt(1)), defnid("A")), letBinding(lit(ionInt(2)), defnid("B")))
        )
    }

    @Test
    fun selectFromLetTableBindingTest() = assertExpression("SELECT x FROM table1 LET table1 AS A") {
        select(
            project = projectX,
            from = scan(vr("table1")),
            fromLet = let(letBinding(vr("table1"), defnid("A")))
        )
    }

    @Test
    fun selectFromLetFunctionBindingTest() = assertExpression("SELECT x FROM table1 LET foo() AS A") {
        select(
            project = projectX,
            from = scan(vr("table1")),
            fromLet = let(letBinding(call(defnid("foo"), emptyList()), defnid("A")))
        )
    }

    @Test
    fun selectFromLetFunctionWithLiteralsTest() = assertExpression(
        "SELECT x FROM table1 LET foo(42, 'bar') AS A"
    ) {
        select(
            project = projectX,
            from = scan(vr("table1")),
            fromLet = let(letBinding(call(defnid("foo"), listOf(lit(ionInt(42)), lit(ionString("bar")))), defnid("A")))
        )
    }

    @Test
    fun selectFromLetFunctionWithVariablesTest() = assertExpression(
        "SELECT x FROM table1 LET foo(table1) AS A"
    ) {
        select(
            project = projectX,
            from = scan(vr("table1")),
            fromLet = let(letBinding(call(defnid("foo"), listOf(vr("table1"))), defnid("A")))
        )
    }

    // ****************************************
    // OFFSET clause parsing
    // ****************************************

    private fun buildProject(project: String) = PartiqlAst.build { projectList(projectExpr(vr(project))) }

    private fun buildLit(lit: String) = PartiqlAst.Expr.Lit(loadSingleElement(lit))

    @Test
    fun selectOffsetTest() = assertExpression("SELECT x FROM a OFFSET 5") {
        select(
            project = buildProject("x"),
            from = scan(vr("a")),
            offset = buildLit("5")
        )
    }

    @Test
    fun selectLimitOffsetTest() = assertExpression("SELECT x FROM a LIMIT 7 OFFSET 5") {
        select(
            project = buildProject("x"),
            from = scan(vr("a")),
            limit = buildLit("7"),
            offset = buildLit("5")
        )
    }

    @Test
    fun selectWhereLimitOffsetTest() = assertExpression("SELECT x FROM a WHERE y = 10 LIMIT 7 OFFSET 5") {
        select(
            project = buildProject("x"),
            from = scan(vr("a")),
            where = PartiqlAst.Expr.Eq(listOf(vr("y"), buildLit("10"))),
            limit = buildLit("7"),
            offset = buildLit("5")
        )
    }

    @Test
    fun selectOrderbyLimitOffsetTest() = assertExpression("SELECT x FROM a ORDER BY y DESC LIMIT 10 OFFSET 5") {
        select(
            project = buildProject("x"),
            from = scan(vr("a")),
            order = PartiqlAst.OrderBy(listOf(PartiqlAst.SortSpec(vr("y"), PartiqlAst.OrderingSpec.Desc(), null))),
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
                    project = projectList(projectExpr(vr("baz"))),
                    from = scan(vr("bar"))
                )
            )
        )
    }

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
