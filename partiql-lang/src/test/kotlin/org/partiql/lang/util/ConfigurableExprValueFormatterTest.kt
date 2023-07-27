package org.partiql.lang.util

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import org.junit.Test
import org.junit.runner.RunWith
import org.partiql.lang.CompilerPipeline
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.syntax.PartiQLParserBuilder
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(JUnitParamsRunner::class)
class ConfigurableExprValueFormatterTest {

    private val parser = PartiQLParserBuilder.standard().build()
    private val compiler = CompilerPipeline.builder().sqlParser(parser).build()

    private val pretty = ConfigurableExprValueFormatter.pretty
    private val standard = ConfigurableExprValueFormatter.standard

    private fun evalQuery(q: String) = compiler.compile(q).eval(EvaluationSession.standard())

    private fun format(v: ExprValue, formatter: ConfigurableExprValueFormatter): String {
        val sb = StringBuilder()
        formatter.formatTo(v, sb)
        return sb.toString()
    }

    fun unknownExamples() = arrayOf(
        "missing" to "MISSING",
        "MISSING" to "MISSING",
        "null" to "NULL",
        "NULL" to "NULL",
        "`null`" to "NULL"
    ).map { listOf(it.first, it.second) }

    private fun baseExamples() = arrayOf(
        // Bool
        "true" to "true",
        "false" to "false",
        "`true`" to "true",

        // Int
        "1" to "1",
        "`1`" to "1",

        // Decimal
        "`-0.0`" to "-0.0",
        "-0.0" to "-0.0",
        "1.0" to "1.0",
        "`1.0`" to "1.0",

        // String
        "'some text'" to "'some text'",
        "`\"some text\"`" to "'some text'",

        // Ion Literals
        "`1e0`" to "`1e0`",
        "`symbol`" to "`symbol`",
        "`{{\"clob value\"}}`" to "`{{\"clob value\"}}`",
        "`{{VG8gaW5maW5pdHkuLi4gYW5kIGJleW9uZCE=}}`" to "`{{VG8gaW5maW5pdHkuLi4gYW5kIGJleW9uZCE=}}`",
        "`(sym 1 2 3)`" to "`(sym 1 2 3)`",

        // EmptyContainers
        "[]" to "[]",
        "<<>>" to "<<>>",

        // Date time
        "DATE '2021-02-28'" to "DATE '2021-02-28'",
        "TIME '00:00:00.00'" to "TIME '00:00:00.00'",
        "TIMESTAMP '2021-02-28 00:00:00'" to "TIMESTAMP '2021-02-28 00:00:00'",
        "TIMESTAMP '2021-02-28T00:00:00'" to "TIMESTAMP '2021-02-28 00:00:00'",
        "TIMESTAMP '2021-02-28T00:00:00Z'" to "TIMESTAMP '2021-02-28 00:00:00+00:00'",
        "`2019T`" to "TIMESTAMP '2019-01-01 00:00:00-00:00'",
        "{}" to "{}"
    ).map { listOf(it.first, it.second) }

    fun prettyExamples() = baseExamples() + arrayOf(
        // List
        "[1,2,3]" to """
                |[
                |  1,
                |  2,
                |  3
                |]""".trimMargin(),

        "`[1,2,3]`" to """
                |[
                |  1,
                |  2,
                |  3
                |]""".trimMargin(),

        "[1,2,[1]]" to """
                |[
                |  1,
                |  2,
                |  [
                |    1
                |  ]
                |]""".trimMargin(),

        // Bag
        "<<1,2,3>>" to """
                |<<
                |  1,
                |  2,
                |  3
                |>>""".trimMargin(),

        "<<1,2,<<1>> >>" to """
                |<<
                |  1,
                |  2,
                |  <<
                |    1
                |  >>
                |>>""".trimMargin(),

        // Struct
        "{'foo': 1, 'bar': 2}" to """
                |{
                |  'foo': 1,
                |  'bar': 2
                |}""".trimMargin(),

        "`{foo: 1, bar: 2,}`" to """
                |{
                |  'foo': 1,
                |  'bar': 2
                |}""".trimMargin(),

        "{'foo': 1, 'bar': 2, 'baz': {'a': 10}}" to """
                |{
                |  'foo': 1,
                |  'bar': 2,
                |  'baz': {
                |    'a': 10
                |  }
                |}
                """.trimMargin(),

        // Mixed containers
        "<<{'foo': 1, 'bar': [1,2,3], 'baz': {'books': <<>>}}>>" to """
                |<<
                |  {
                |    'foo': 1,
                |    'bar': [
                |      1,
                |      2,
                |      3
                |    ],
                |    'baz': {
                |      'books': <<>>
                |    }
                |  }
                |>>
                """.trimMargin()
    ).map { listOf(it.first, it.second) }

    fun standardExamples() = baseExamples() + arrayOf(
        // List
        "[1, 2, 3]",
        "[1, 2, [1]]",

        // Bag
        "<<1, 2, 3>>",
        "<<1, 2, <<1>>>>",

        // Struct
        "{'foo': 1, 'bar': 2}",
        "{'foo': 1, 'bar': 2, 'baz': {'a': 10}}",

        // Mixed containers
        "<<{'foo': 1, 'bar': [1, 2, 3], 'baz': {'books': <<>>}}>>"
    ).map { listOf(it, it) }

    private fun assertFormatter(expression: String, expected: String, formatter: ConfigurableExprValueFormatter) {
        val value = evalQuery(expression)
        val actual = format(value, formatter)

        assertEquals(expected, actual)

        // the pretty print is valid PartiQL and represents the same value
        assertTrue(evalQuery("$expression = $actual").scalar.booleanValue()!!)
    }

    private fun assertFormatterForUnknown(expression: String, expected: String, formatter: ConfigurableExprValueFormatter) {
        val value = evalQuery(expression)

        val actual = format(value, formatter)
        assertEquals(expected, actual)
        assertEquals(value.type, evalQuery(actual).type)
    }

    @Test
    @Parameters(method = "unknownExamples")
    fun testPrettyUnknown(expression: String, expected: String) =
        assertFormatterForUnknown(expression, expected, pretty)

    @Test
    @Parameters(method = "unknownExamples")
    fun testStandardUnknown(expression: String, expected: String) =
        assertFormatterForUnknown(expression, expected, standard)

    @Test
    @Parameters(method = "prettyExamples")
    fun testPretty(expression: String, expected: String) =
        assertFormatter(expression, expected, pretty)

    @Test
    @Parameters(method = "standardExamples")
    fun testStandard(expression: String, expected: String) =
        assertFormatter(expression, expected, standard)
}
