package org.partiql.lang.eval

import org.junit.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.util.ArgumentsProviderBase
import org.partiql.lang.util.to

class EvaluatingCompilerOffsetTests: EvaluatorTestBase() {
    private val session = mapOf("foo" to "[ { 'a': 1 }, { 'a': 2 }, { 'a': 3 }, { 'a': 4 }, { 'a': 5 } ]").toSession()

    class ArgsProviderValid: ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            // OFFSET 0 should not affect results
            EvaluatorTestCase(
                "SELECT * FROM foo OFFSET 0",
                "<<{'a': 1}, {'a': 2}, {'a': 3}, {'a': 4}, {'a': 5}>>"
            ),
            // OFFEST 1 should skip first result
            EvaluatorTestCase(
                "SELECT * FROM foo OFFSET 1",
                "<<{'a': 2}, {'a': 3}, {'a': 4}, {'a': 5}>>"
            ),
            // OFFSET 2 should skip first two results
            EvaluatorTestCase(
                "SELECT * FROM foo OFFSET 2",
                "<<{'a': 3}, {'a': 4}, {'a': 5}>>"
            ),
            // OFFSET 2^31 should return no results
            EvaluatorTestCase(
                "SELECT * FROM foo OFFSET ${Integer.MAX_VALUE}",
                "<<>>"
            ),
            // OFFSET 2^63 should return no results
            EvaluatorTestCase(
                "SELECT * FROM foo OFFSET ${Long.MAX_VALUE}",
                "<<>>"
            ),
            // LIMIT 1 and OFFSET 1 should return the second result
            EvaluatorTestCase(
                "SELECT * FROM foo LIMIT 1 OFFSET 1",
                "<<{'a': 2}>>"
            ),
            // LIMIT 10 and OFFSET 1 should skip first result
            EvaluatorTestCase(
                "SELECT * FROM foo LIMIT 10 OFFSET 1",
                "<<{'a': 2}, {'a': 3}, {'a': 4}, {'a': 5}>>"
            ),
            // LIMIT 10 and OFFSET 10 should return no result
            EvaluatorTestCase(
                "SELECT * FROM foo LIMIT 10 OFFSET 10",
                "<<>>"
            ),
            // LIMIT 2 and OFFSET 2 should return third and fourth results
            EvaluatorTestCase(
                "SELECT * FROM foo GROUP BY a LIMIT 2 OFFSET 2",
                "<<{'a': 3}, {'a': 4}>>"
            ),
            // LIMIT and OFFSET applied after GROUP BY
            EvaluatorTestCase(
                "SELECT * FROM foo GROUP BY a LIMIT 1 OFFSET 1",
                "<<{'a': 2}>>"
            ),
            // OFFSET value can be subtraction of 2 numbers
            EvaluatorTestCase(
                "SELECT * FROM foo OFFSET 2 - 1",
                "<<{'a': 2}, {'a': 3}, {'a': 4}, {'a': 5}>>"
            ),
            // OFFSET value can be addition of 2 numbers
            EvaluatorTestCase(
                "SELECT * FROM foo OFFSET 2 + 1",
                "<<{'a': 4}, {'a': 5}>>"
            ),
            // OFFSET value can be multiplication of 2 numbers
            EvaluatorTestCase(
                "SELECT * FROM foo OFFSET 2 * 1",
                "<<{'a': 3}, {'a': 4}, {'a': 5}>>"
            ),
            // OFFSET value can be division of 2 numbers
            EvaluatorTestCase(
                "SELECT * FROM foo OFFSET 4 / 2",
                "<<{'a': 3}, {'a': 4}, {'a': 5}>>"
            )
        )
    }

    @ParameterizedTest
    @ArgumentsSource(ArgsProviderValid::class)
    fun validTests(tc: EvaluatorTestCase) = runTestCase(tc, session)

    class ArgsProviderError : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            // OFFSET -1 should throw exception
            EvaluatorErrorTestCase(
                "select * from foo OFFSET -1",
                ErrorCode.EVALUATOR_NEGATIVE_OFFSET,
                mapOf(
                    Property.LINE_NUMBER to 1L,
                    Property.COLUMN_NUMBER to 27L
                )
            ),
            // OFFSET 1 - 2 should throw exception
            EvaluatorErrorTestCase(
                "select * from foo OFFSET 1 - 2",
                ErrorCode.EVALUATOR_NEGATIVE_OFFSET,
                mapOf(
                    Property.LINE_NUMBER to 1L,
                    Property.COLUMN_NUMBER to 28L
                )
            ),
            // non-integer value should throw exception
            EvaluatorErrorTestCase(
                "select * from foo OFFSET 'this won''t work'",
                ErrorCode.EVALUATOR_NON_INT_OFFSET_VALUE,
                mapOf(
                    Property.LINE_NUMBER to 1L,
                    Property.COLUMN_NUMBER to 26L,
                    Property.ACTUAL_TYPE to "STRING"
                )
            ),
            // non-integer value should throw exception
            EvaluatorErrorTestCase(
                "select * from foo OFFSET 2.5",
                ErrorCode.EVALUATOR_NON_INT_OFFSET_VALUE,
                mapOf(
                    Property.LINE_NUMBER to 1L,
                    Property.COLUMN_NUMBER to 26L,
                    Property.ACTUAL_TYPE to "DECIMAL"
                )
            )
        )
    }

    @ParameterizedTest
    @ArgumentsSource(ArgsProviderError::class)
    fun errorTests(tc: EvaluatorErrorTestCase) = checkInputThrowingEvaluationException(tc, session)
}