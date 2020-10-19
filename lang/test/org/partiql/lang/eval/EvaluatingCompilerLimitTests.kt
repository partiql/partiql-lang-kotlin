package org.partiql.lang.eval

import org.junit.Test
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property

class EvaluatingCompilerLimitTests : EvaluatorTestBase() {

    private val session = mapOf("foo" to "[ { a: 1 }, { a: 2 }, { a: 3 }, { a: 4 }, { a: 5 } ]").toSession()

    @Test
    fun `LIMIT 0 should return no results`() =
        assertEval(
            "SELECT * FROM foo LIMIT 0",
            "[]",
            session
        )

    @Test
    fun `LIMIT 1 should return first result`() =
        assertEval(
            "SELECT * FROM foo LIMIT 1",
            "[{a: 1}]",
            session
        )

    @Test
    fun `LIMIT 2 should return first two results`() =
        assertEval(
            "SELECT * FROM foo LIMIT 2",
            "[{a: 1}, {a: 2}]",
            session
        )

    @Test
    fun `LIMIT 2^31 should return all results`() =
        assertEval(
            "SELECT * FROM foo LIMIT ${Integer.MAX_VALUE}",
            "[ { a: 1 }, { a: 2 }, { a: 3 }, { a: 4 }, { a: 5 } ]",
            session
        )

    @Test
    fun `LIMIT 2^63 should return all results`() =
        assertEval(
            "SELECT * FROM foo LIMIT ${Long.MAX_VALUE}",
            "[ { a: 1 }, { a: 2 }, { a: 3 }, { a: 4 }, { a: 5 } ]",
            session
        )

    @Test
    fun `LIMIT -1 should throw exception`() =
        checkInputThrowingEvaluationException(
            """ select * from <<1>> limit -1 """,
            ErrorCode.EVALUATOR_NEGATIVE_LIMIT,
            sourceLocationProperties(1, 29))

    @Test
    fun `non-integer value should throw exception`() =
        checkInputThrowingEvaluationException(
            """ select * from <<1>> limit 'this won''t work' """,
            ErrorCode.EVALUATOR_NON_INT_LIMIT_VALUE,
            sourceLocationProperties(1, 28) + mapOf(Property.ACTUAL_TYPE to "STRING"))

    @Test
    fun `LIMIT applied after GROUP BY`() =
        assertEval(
            "SELECT g FROM `[{foo: 1, bar: 10}, {foo: 1, bar: 11}]` AS f GROUP BY f.foo GROUP AS g LIMIT 1",
            """[ { 'g': [ { 'f': { 'foo': 1, 'bar': 10 } }, { 'f': { 'foo': 1, 'bar': 11 } } ] } ]"""
        )
}