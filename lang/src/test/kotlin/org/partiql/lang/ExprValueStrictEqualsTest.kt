package org.partiql.lang

import com.amazon.ion.system.IonSystemBuilder
import org.junit.Assert
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.util.ArgumentsProviderBase

class ExprValueStrictEqualsTest {
    data class TestCase(val q1: String, val q2: String)

    val ion = IonSystemBuilder.standard().build()
    val pipeline = CompilerPipeline.builder(ion).build()
    val session = EvaluationSession.standard()

    @ParameterizedTest
    @ArgumentsSource(EqualCases::class)
    fun equalityTests(testCase: TestCase) {
        val v1 = pipeline.compile(testCase.q1).eval(session)
        val v2 = pipeline.compile(testCase.q2).eval(session)

        Assert.assertEquals(true, v1.strictEquals(v2))
    }

    class EqualCases : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            // MISSING
            TestCase("MISSING", "MISSING"),
            // NULL
            TestCase("NULL", "NULL"),
            TestCase("`null.int`", "`null.int`"),
            TestCase("`null.list`", "`null.list`"),
            // BOOL
            TestCase("TRUE", "TRUE"),
            TestCase("FALSE", "FALSE"),
            // INT
            TestCase("1", "1"),
            // FLOAT
            TestCase("`1e0`", "`1e0`"),
            // DECIMAL
            TestCase("1.0", "1.0"),
            // DATE
            TestCase("DATE '2022-11-22'", "DATE '2022-11-22'"),
            // TIMESTAMP
            TestCase("`2007-02-23T12:14:33.079-08:00`", "`2007-02-23T12:14:33.079-08:00`"),
            // TIME
            TestCase("TIME '01:23:45'", "TIME '01:23:45'"),
            // SYMBOL
            TestCase("`1`", "`1`"),
            // STRING
            TestCase("'1'", "'1'"),
            // CLOB
            TestCase("`{{ \"This is a CLOB of text.\" }}`", "`{{ \"This is a CLOB of text.\" }}`"),
            // BLOB
            TestCase("`{{ dHdvIHBhZGRpbmcgY2hhcmFjdGVycw== }}`", "`{{ dHdvIHBhZGRpbmcgY2hhcmFjdGVycw== }}`"),
            // LIST
            TestCase("[1,'2',TRUE]", "[1,'2',TRUE]"),
            TestCase("[1,'2',<<1,'2',TRUE>>]", "[1,'2',<<1, '2', TRUE>>]"),
            TestCase("[{'a': 1, 'b': 2}, {'a': 3, 'b': 4}]", "[{'b': 2, 'a': 1}, {'b': 4, 'a': 3}]"),
            // SEXP
            TestCase("sexp(1,'2',TRUE)", "sexp(1,'2',TRUE)"),
            TestCase("sexp(1,'2',<<1,'2',TRUE>>)", "sexp(1,'2',<<1, '2', TRUE>>)"),
            TestCase("sexp({'a': 1, 'b': 2}, {'a': 3, 'b': 4})", "sexp({'b': 2, 'a': 1}, {'b': 4, 'a': 3})"),
            // BAG
            TestCase("<<1,'2',TRUE>>", "<<1,'2',TRUE>>"),
            TestCase("<<TRUE,1,'2'>>", "<<TRUE,1,'2'>>"),
            TestCase("<<1,'2',[1,'2',TRUE]>>", "<<[1,'2',TRUE],1,'2'>>"),
            TestCase("<<{'a': 1, 'b': 2}, {'a': 3, 'b': 4}>>", "<<{'b': 4, 'a': 3}, {'b': 2, 'a': 1}>>"),
            // STRUCT
            TestCase("{'a': 1, 'b': 2}", "{'a': 1, 'b': 2}"),
            TestCase("{'a': 1, 'b': 2}", "{'b': 2, 'a': 1}"),
            TestCase("{'a': <<1 ,2>>, 'b': 2}", "{'b': 2, 'a': <<2, 1>>}"),
            TestCase("{'X': 2}", "{`X`: 2}"),
        )
    }
}
