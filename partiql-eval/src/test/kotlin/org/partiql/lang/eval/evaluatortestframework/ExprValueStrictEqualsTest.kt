package org.partiql.lang.eval.evaluatortestframework

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.CompilerPipeline
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.util.ArgumentsProviderBase

class ExprValueStrictEqualsTest {
    data class EqTest(val q1: String, val q2: String, val equals: Boolean)

    val pipeline = CompilerPipeline.builder().build()
    val session = EvaluationSession.standard()

    @ParameterizedTest
    @ArgumentsSource(StrictEqualCases::class)
    fun strictEqualsTests(eqTest: EqTest) {
        val v1 = pipeline.compile(eqTest.q1).eval(session)
        val v2 = pipeline.compile(eqTest.q2).eval(session)

        Assertions.assertEquals(eqTest.equals, v1.strictEquals(v2))
    }

    class StrictEqualCases : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            // MISSING
            EqTest("MISSING", "MISSING", true),
            EqTest("MISSING", "1", false),
            // NULL
            EqTest("NULL", "NULL", true),
            EqTest("`null.int`", "`null.int`", true),
            EqTest("`null.list`", "`null.list`", true),
            EqTest("NULL", "1", false),
            // BOOL
            EqTest("TRUE", "TRUE", true),
            EqTest("FALSE", "FALSE", true),
            EqTest("TRUE", "FALSE", false),
            // INT
            EqTest("1", "1", true),
            EqTest("1", "2", false),
            // FLOAT
            EqTest("`1e0`", "`1e0`", true),
            EqTest("`1e0`", "`0e0`", false),
            // DECIMAL
            EqTest("1.0", "1.0", true),
            EqTest("1.", "1.0", true),
            // DATE
            EqTest("DATE '2022-11-22'", "DATE '2022-11-22'", true),
            EqTest("DATE '2022-11-22'", "DATE '2022-11-23'", false),
            // TIMESTAMP
            EqTest("`2007-02-23T12:14:33.079-08:00`", "`2007-02-23T12:14:33.079-08:00`", true),
            EqTest("`2007-02-23`", "`2007-02-24`", false),
            EqTest("`2007-02-23T12:14:33.079-08:00`", "`2007-02-23T12:14:33.079-08:30`", false),
            // TIME
            EqTest("TIME '01:23:45'", "TIME '01:23:45'", true),
            EqTest("TIME '01:23:45'", "TIME '01:23:47'", false),
            // SYMBOL
            EqTest("`1`", "`1`", true),
            EqTest("`1`", "`2`", false),
            // STRING
            EqTest("'1'", "'1'", true),
            EqTest("'1'", "'2'", false),
            // CLOB
            EqTest("`{{ \"This is a CLOB of text.\" }}`", "`{{ \"This is a CLOB of text.\" }}`", true),
            EqTest("`{{ \"This is a CLOB of text.\" }}`", "`{{ \"This is another CLOB of text.\" }}`", false),
            // BLOB
            EqTest("`{{ dHdvIHBhZGRpbmcgY2hhcmFjdGVycw== }}`", "`{{ dHdvIHBhZGRpbmcgY2hhcmFjdGVycw== }}`", true),
            EqTest("`{{ dHdvIHBhZGRpbmcgY2hhcmFjdGVycw== }}`", "`{{ dqdvIHBhZGRpbmcgY2hhcmFjdGVycw== }}`", false),
            // LIST
            EqTest("[1,'2',TRUE]", "[1,'2',TRUE]", true),
            EqTest("[1,'2',<<1,'2',TRUE>>]", "[1,'2',<<1, '2', TRUE>>]", true),
            EqTest("[{'a': 1, 'b': 2}, {'a': 3, 'b': 4}]", "[{'b': 2, 'a': 1}, {'b': 4, 'a': 3}]", true),
            EqTest("[1,'2',TRUE]", "[1,'2',FALSE]", false),
            EqTest("[1,'2',<<1,'2',TRUE>>]", "[1,'2',<<1, '2', FALSE>>]", false),
            EqTest("[{'a': 1, 'b': 2}, {'a': 3, 'b': 4}]", "[{'b': 2, 'a': 1}, {'b': 4, 'a': NULL}]", false),
            // SEXP
            EqTest("sexp(1,'2',TRUE)", "sexp(1,'2',TRUE)", true),
            EqTest("sexp(1,'2',<<1,'2',TRUE>>)", "sexp(1,'2',<<1, '2', TRUE>>)", true),
            EqTest("sexp({'a': 1, 'b': 2}, {'a': 3, 'b': 4})", "sexp({'b': 2, 'a': 1}, {'b': 4, 'a': 3})", true),
            EqTest("sexp(1,'2',TRUE)", "sexp(1,'2',FALSE)", false),
            EqTest("sexp(1,'2',<<1,'2',TRUE>>)", "sexp(1,'2',<<1, '2', FALSE>>)", false),
            EqTest("sexp({'a': 1, 'b': 2}, {'a': 3, 'b': 4})", "sexp({'b': 2, 'a': 1}, {'b': 4, 'a': NULL})", false),
            // BAG
            EqTest("<<1,2,1>>", "<<1,1,2>>", true),
            EqTest("<<'1','2','1'>>", "<<'1','1','2'>>", true),
            EqTest("<<1,'2',TRUE>>", "<<1,'2',TRUE>>", true),
            EqTest("<<TRUE,1,'2'>>", "<<TRUE,1,'2'>>", true),
            EqTest("<<1,'2',[1,'2',TRUE]>>", "<<[1,'2',TRUE],1,'2'>>", true),
            EqTest("<<{'a': 1, 'b': 2}, {'a': 3, 'b': 4}>>", "<<{'b': 4, 'a': 3}, {'b': 2, 'a': 1}>>", true),
            EqTest("<<1,'2',TRUE>>", "<<1,'2',FALSE>>", false),
            EqTest("<<TRUE,1,'2'>>", "<<FALSE,1,'2'>>", false),
            EqTest("<<1,'2',[1,'2',TRUE]>>", "<<[1,'2',FALSE],1,'2'>>", false),
            EqTest("<<{'a': 1, 'b': 2}, {'a': 3, 'b': 4}>>", "<<{'b': 4, 'a': 3}, {'b': 2, 'a': NULL}>>", false),
            // STRUCT
            EqTest("{'a': 1, 'b': 2}", "{'a': 1, 'b': 2}", true),
            EqTest("{'a': 1, 'b': 2}", "{'b': 2, 'a': 1}", true),
            EqTest("{'a': 1, 'b': 2, 'c': 1}", "{'c': 1, 'a': 1, 'b': 2}", true),
            EqTest("{'a': <<1 ,2>>, 'b': 2}", "{'b': 2, 'a': <<2, 1>>}", true),
            EqTest("{'X': 2}", "{`X`: 2}", true),
            EqTest("{'a': 1, 'b': 2}", "{'a': 1, 'b': NULL}", false),
            EqTest("{'a': 1, 'b': 2}", "{'b': 2, 'a': NULL}", false),
            EqTest("{'a': <<1 ,2>>, 'b': 2}", "{'b': 2, 'a': <<2, NULL>>}", false),
            EqTest("{'X': 2}", "{`X`: 2.}", false),
            // Data type mismatch
            EqTest("MISSING", "NULL", false),
            EqTest("`null.int`", "`null.string`", false),
            EqTest("1", "1.", false),
            EqTest("1", "`1e0`", false),
            EqTest("1.", "`1e0`", false),
            EqTest("DATE '2022-11-22'", "`2022-11-22`", false),
            EqTest("`1`", "'1'", false),
            EqTest("`{{ \"dHdvIHBhZGRpbmcgY2hhcmFjdGVycw==\" }}`", "`{{ dHdvIHBhZGRpbmcgY2hhcmFjdGVycw== }}`", false),
            EqTest("[]", "<<>>", false),
            EqTest("[]", "`()`", false),
            EqTest("[]", "{}", false),
            EqTest("<<>>", "`()`", false),
            EqTest("<<>>", "{}", false),
            EqTest("`()`", "{}", false),
        )
    }
}
