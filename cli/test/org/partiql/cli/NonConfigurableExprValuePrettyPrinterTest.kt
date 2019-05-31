package org.partiql.cli

import com.amazon.ion.system.*
import junitparams.*
import org.junit.*
import org.junit.runner.*
import org.partiql.lang.*
import org.partiql.lang.eval.*
import java.io.*
import java.nio.charset.*
import kotlin.test.*


@RunWith(JUnitParamsRunner::class)
class NonConfigurableExprValuePrettyPrinterTest {

    private val ion = IonSystemBuilder.standard().build()
    private val compiler = CompilerPipeline.standard(ion)

    private val output = ByteArrayOutputStream()
    private val prettyPrinter = NonConfigurableExprValuePrettyPrinter(output)
    
    private fun evalQuery(q: String) = compiler.compile(q).eval(EvaluationSession.standard())
    private fun prettyPrint(v: ExprValue): String {
        prettyPrinter.prettyPrint(v)
        return output.toString("UTF-8")
    }

    fun unknownExamples()
        = arrayOf(
        "missing" to "MISSING",
        "MISSING" to "MISSING",

        "null" to "NULL",
        "NULL" to "NULL",
        "`null`" to "NULL"
    ).map { listOf(it.first, it.second) }
    
    fun examples() 
        = arrayOf(
            // Bool
            "true" to "true",
            "false" to "false",
            "`true`" to "true",

            // Int
            "1" to "1",
            "`1`" to "1",

            // Decimal
            "1.0" to "1.0",
            "`1.0`" to "1.0",

            // String 
            "'some text'" to "'some text'",
            "`\"some text\"`" to "'some text'",

            // Ion Literals
            "`1e0`" to "`1e0`",
            "`2019T`" to "`2019T`",
            "`symbol`" to "`symbol`",
            "`{{\"clob value\"}}`" to "`{{\"clob value\"}}`",
            "`{{VG8gaW5maW5pdHkuLi4gYW5kIGJleW9uZCE=}}`" to "`{{VG8gaW5maW5pdHkuLi4gYW5kIGJleW9uZCE=}}`",
            "`(sym 1 2 3)`" to "`(sym 1 2 3)`",

             // List
            "[]" to "[]",

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
            "<<>>" to "<<>>",

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
            "{}" to "{}",

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


    @Test
    @Parameters(method = "unknownExamples")
    fun testUnknown(expression: String, expected: String) {

        val value = evalQuery(expression)
        val actual = prettyPrint(value)

        assertEquals(expected, actual)
        assertEquals(value.type, evalQuery(actual).type)
    }
    
    @Test
    @Parameters(method = "examples")
    fun test(expression: String, expected: String) {
        val value = evalQuery(expression)
        val actual = prettyPrint(value)
        
        assertEquals(expected, actual)

        // the pretty print is valid PartiQL and represents the same value
        assertTrue(evalQuery("$expression = $actual").scalar.booleanValue()!!)
    }
}