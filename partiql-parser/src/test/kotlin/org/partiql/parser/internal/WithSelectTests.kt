package org.partiql.parser.internal

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource

class WithSelectTests {

    @ParameterizedTest
    @ArgumentsSource(SuccessTestCases::class)
    fun success(tc: PTestDef) {
        tc.assert()
    }

    @ParameterizedTest
    @ArgumentsSource(FailureTestCases::class)
    fun failure(tc: PTestDef) {
        tc.assert()
    }

    object SuccessTestCases : ArgumentsProviderBase() {
        override fun getParameters(): List<ParserTestCaseSimple> {
            val m = mapOf(
                "Simplest test" to "WITH a AS (SELECT * FROM t) SELECT * FROM a, a",
                "Simplest test delimited" to "WITH \"a\" AS (SELECT * FROM t) SELECT * FROM a, a",
                "Multiple withs" to "WITH a AS (SELECT * FROM t), b AS (SELECT * FROM r) SELECT * FROM a, b",
                "Simplest with column name" to "WITH a (b) AS (SELECT * FROM t) SELECT * FROM a, a",
                "Simplest with column names" to "WITH a (b, c, d) AS (SELECT * FROM t) SELECT * FROM a, a",
                "Target with union" to "WITH a AS (SELECT * FROM t UNION r) SELECT * FROM a, a",
                "Target with join" to "WITH a AS (SELECT * FROM t, r) SELECT * FROM a, a",
                "Target with join and limit" to "WITH a AS (SELECT * FROM t, r LIMIT 5) SELECT * FROM a, a LIMIT 3",
                "Union on the WITH" to "WITH a AS (SELECT * FROM t UNION r) SELECT * FROM a UNION SELECT * FROM r",
            ).entries.map { ParserTestCaseSimple(it.key, it.value, isValid = true) }
            return m
        }
    }

    object FailureTestCases : ArgumentsProviderBase() {
        override fun getParameters(): List<ParserTestCaseSimple> {
            val m = mapOf(
                "No query body" to "WITH a AS (SELECT * FROM t)",
                "No columns with parenthesis" to "WITH a () AS (SELECT * FROM t) SELECT * FROM a",
                "No parenthesis on with target" to "WITH a AS SELECT * FROM t SELECT * FROM a",
                "Unsupported path index" to "WITH a.b[0] AS (SELECT * FROM t) SELECT * FROM a, a",
                "Unsupported path index" to "WITH a['b'] AS (SELECT * FROM t) SELECT * FROM a, a",
                "Qualified identifiers" to "WITH a.b AS (SELECT * FROM t), b.c AS (SELECT * FROM r) SELECT * FROM a.b, b.c",
                "Qualified identifiers delimited" to "WITH \"a\".\"b\" AS (SELECT * FROM t), b.c AS (SELECT * FROM r) SELECT * FROM a.b, b.c",
                "With target is scalar" to "WITH a AS (5) SELECT * FROM a",
                "With target is bag" to "WITH a AS (<<5>>) SELECT * FROM a",
                "With target is bag with no paren" to "WITH a AS <<5>> SELECT * FROM a",
            ).entries.map { ParserTestCaseSimple(it.key, it.value, isValid = false) }
            return m
        }
    }
}
