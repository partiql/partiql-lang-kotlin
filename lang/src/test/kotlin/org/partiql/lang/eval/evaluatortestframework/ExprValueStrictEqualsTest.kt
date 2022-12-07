package org.partiql.lang.eval.evaluatortestframework

import com.amazon.ion.system.IonSystemBuilder
import org.junit.Assert
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.CompilerPipeline
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.util.ArgumentsProviderBase

class ExprValueStrictEqualsTest {
    data class TestCase(val q1: String, val q2: String, val equals: Boolean)

    val ion = IonSystemBuilder.standard().build()
    val pipeline = CompilerPipeline.builder(ion).build()
    val session = EvaluationSession.standard()

    @ParameterizedTest
    @ArgumentsSource(StrictEqualCases::class)
    fun strictEqualsTests(testCase: TestCase) {
        val v1 = pipeline.compile(testCase.q1).eval(session)
        val v2 = pipeline.compile(testCase.q2).eval(session)

        Assert.assertEquals(testCase.equals, v1.strictEquals(v2))
    }

    class StrictEqualCases : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            // MISSING
            TestCase("MISSING", "MISSING", true),
            TestCase("MISSING", "1", false),
            // NULL
            TestCase("NULL", "NULL", true),
            TestCase("`null.int`", "`null.int`", true),
            TestCase("`null.list`", "`null.list`", true),
            TestCase("NULL", "1", false),
            // BOOL
            TestCase("TRUE", "TRUE", true),
            TestCase("FALSE", "FALSE", true),
            TestCase("TRUE", "FALSE", false),
            // INT
            TestCase("1", "1", true),
            TestCase("1", "2", false),
            // FLOAT
            TestCase("`1e0`", "`1e0`", true),
            TestCase("`1e0`", "`0e0`", false),
            // DECIMAL
            TestCase("1.0", "1.0", true),
            TestCase("1.", "1.0", true),
            // DATE
            TestCase("DATE '2022-11-22'", "DATE '2022-11-22'", true),
            TestCase("DATE '2022-11-22'", "DATE '2022-11-23'", false),
            // TIMESTAMP
            TestCase("`2007-02-23T12:14:33.079-08:00`", "`2007-02-23T12:14:33.079-08:00`", true),
            TestCase("`2007-02-23`", "`2007-02-24`", false),
            TestCase("`2007-02-23T12:14:33.079-08:00`", "`2007-02-23T12:14:33.079-08:30`", false),
            // TIME
            TestCase("TIME '01:23:45'", "TIME '01:23:45'", true),
            TestCase("TIME '01:23:45'", "TIME '01:23:47'", false),
            // SYMBOL
            TestCase("`1`", "`1`", true),
            TestCase("`1`", "`2`", false),
            // STRING
            TestCase("'1'", "'1'", true),
            TestCase("'1'", "'2'", false),
            // CLOB
            TestCase("`{{ \"This is a CLOB of text.\" }}`", "`{{ \"This is a CLOB of text.\" }}`", true),
            TestCase("`{{ \"This is a CLOB of text.\" }}`", "`{{ \"This is another CLOB of text.\" }}`", false),
            // BLOB
            TestCase("`{{ dHdvIHBhZGRpbmcgY2hhcmFjdGVycw== }}`", "`{{ dHdvIHBhZGRpbmcgY2hhcmFjdGVycw== }}`", true),
            TestCase("`{{ dHdvIHBhZGRpbmcgY2hhcmFjdGVycw== }}`", "`{{ dqdvIHBhZGRpbmcgY2hhcmFjdGVycw== }}`", false),
            // LIST
            TestCase("[1,'2',TRUE]", "[1,'2',TRUE]", true),
            TestCase("[1,'2',<<1,'2',TRUE>>]", "[1,'2',<<1, '2', TRUE>>]", true),
            TestCase("[{'a': 1, 'b': 2}, {'a': 3, 'b': 4}]", "[{'b': 2, 'a': 1}, {'b': 4, 'a': 3}]", true),
            TestCase("[1,'2',TRUE]", "[1,'2',FALSE]", false),
            TestCase("[1,'2',<<1,'2',TRUE>>]", "[1,'2',<<1, '2', FALSE>>]", false),
            TestCase("[{'a': 1, 'b': 2}, {'a': 3, 'b': 4}]", "[{'b': 2, 'a': 1}, {'b': 4, 'a': NULL}]", false),
            // SEXP
            TestCase("sexp(1,'2',TRUE)", "sexp(1,'2',TRUE)", true),
            TestCase("sexp(1,'2',<<1,'2',TRUE>>)", "sexp(1,'2',<<1, '2', TRUE>>)", true),
            TestCase("sexp({'a': 1, 'b': 2}, {'a': 3, 'b': 4})", "sexp({'b': 2, 'a': 1}, {'b': 4, 'a': 3})", true),
            TestCase("sexp(1,'2',TRUE)", "sexp(1,'2',FALSE)", false),
            TestCase("sexp(1,'2',<<1,'2',TRUE>>)", "sexp(1,'2',<<1, '2', FALSE>>)", false),
            TestCase("sexp({'a': 1, 'b': 2}, {'a': 3, 'b': 4})", "sexp({'b': 2, 'a': 1}, {'b': 4, 'a': NULL})", false),
            // BAG
            TestCase("<<1,2,1>>", "<<1,1,2>>", true),
            TestCase("<<'1','2','1'>>", "<<'1','1','2'>>", true),
            TestCase("<<1,'2',TRUE>>", "<<1,'2',TRUE>>", true),
            TestCase("<<TRUE,1,'2'>>", "<<TRUE,1,'2'>>", true),
            TestCase("<<1,'2',[1,'2',TRUE]>>", "<<[1,'2',TRUE],1,'2'>>", true),
            TestCase("<<{'a': 1, 'b': 2}, {'a': 3, 'b': 4}>>", "<<{'b': 4, 'a': 3}, {'b': 2, 'a': 1}>>", true),
            TestCase("<<1,'2',TRUE>>", "<<1,'2',FALSE>>", false),
            TestCase("<<TRUE,1,'2'>>", "<<FALSE,1,'2'>>", false),
            TestCase("<<1,'2',[1,'2',TRUE]>>", "<<[1,'2',FALSE],1,'2'>>", false),
            TestCase("<<{'a': 1, 'b': 2}, {'a': 3, 'b': 4}>>", "<<{'b': 4, 'a': 3}, {'b': 2, 'a': NULL}>>", false),
            // STRUCT
            TestCase("{'a': 1, 'b': 2}", "{'a': 1, 'b': 2}", true),
            TestCase("{'a': 1, 'b': 2}", "{'b': 2, 'a': 1}", true),
            TestCase("{'a': 1, 'b': 2, 'c': 1}", "{'c': 1, 'a': 1, 'b': 2}", true),
            TestCase("{'a': <<1 ,2>>, 'b': 2}", "{'b': 2, 'a': <<2, 1>>}", true),
            TestCase("{'X': 2}", "{`X`: 2}", true),
            TestCase("{'a': 1, 'b': 2}", "{'a': 1, 'b': NULL}", false),
            TestCase("{'a': 1, 'b': 2}", "{'b': 2, 'a': NULL}", false),
            TestCase("{'a': <<1 ,2>>, 'b': 2}", "{'b': 2, 'a': <<2, NULL>>}", false),
            TestCase("{'X': 2}", "{`X`: 2.}", false),
            // Data type mismatch
            TestCase("MISSING", "NULL", false),
            TestCase("`null.int`", "`null.string`", false),
            TestCase("1", "1.", false),
            TestCase("1", "`1e0`", false),
            TestCase("1.", "`1e0`", false),
            TestCase("DATE '2022-11-22'", "`2022-11-22`", false),
            TestCase("`1`", "'1'", false),
            TestCase("`{{ \"dHdvIHBhZGRpbmcgY2hhcmFjdGVycw==\" }}`", "`{{ dHdvIHBhZGRpbmcgY2hhcmFjdGVycw== }}`", false),
            TestCase("[]", "<<>>", false),
            TestCase("[]", "`()`", false),
            TestCase("[]", "{}", false),
            TestCase("<<>>", "`()`", false),
            TestCase("<<>>", "{}", false),
            TestCase("`()`", "{}", false),
        )
    }
}
