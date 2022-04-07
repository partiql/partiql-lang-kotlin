package org.partiql.lang.eval

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.util.ArgumentsProviderBase
import org.partiql.lang.util.propertyValueMapOf
import org.partiql.lang.util.to

class EvaluatingCompilerOffsetTests : EvaluatorTestBase() {
    private val session = mapOf("foo" to "[ { 'a': 1 }, { 'a': 2 }, { 'a': 3 }, { 'a': 4 }, { 'a': 5 } ]").toSession()

    class ArgsProviderValid : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            // OFFSET 0 should not affect results
            EvaluatorTestCase(
                "SELECT * FROM foo OFFSET 0",
                "<<{'a': 1}, {'a': 2}, {'a': 3}, {'a': 4}, {'a': 5}>>"
            ),
            // OFFSET 1 should skip first result
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
            ),
            // OFFSET with GROUP BY and HAVING
            EvaluatorTestCase(
                "SELECT * FROM foo GROUP BY a HAVING a > 2 LIMIT 1 OFFSET 1",
                "<<{'a': 4}>>"
            ),
            // OFFSET with PIVOT
            EvaluatorTestCase(
                """
                    PIVOT foo.a AT foo.b 
                    FROM <<{'a': 1, 'b':'I'}, {'a': 2, 'b':'II'}, {'a': 3, 'b':'III'}>> AS foo
                    LIMIT 1 OFFSET 1
                """.trimIndent(),
                "{'II': 2}"
            )
        )
    }

    @ParameterizedTest
    @ArgumentsSource(ArgsProviderValid::class)
    fun validTests(tc: EvaluatorTestCase) = runEvaluatorTestCase(tc, session)

    class ArgsProviderError : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            // OFFSET -1 should throw exception
            EvaluatorErrorTestCase(
                query = "select * from foo OFFSET -1",
                expectedErrorCode = ErrorCode.EVALUATOR_NEGATIVE_OFFSET,
                expectedErrorContext = propertyValueMapOf(1, 27)
            ),
            // OFFSET 1 - 2 should throw exception
            EvaluatorErrorTestCase(
                query = "select * from foo OFFSET 1 - 2",
                expectedErrorCode = ErrorCode.EVALUATOR_NEGATIVE_OFFSET,
                expectedErrorContext = propertyValueMapOf(1, 28)
            ),
            // non-integer value should throw exception
            EvaluatorErrorTestCase(
                query = "select * from foo OFFSET 'this won''t work'",
                expectedErrorCode = ErrorCode.EVALUATOR_NON_INT_OFFSET_VALUE,
                expectedErrorContext = propertyValueMapOf(1, 26, Property.ACTUAL_TYPE to "STRING")
            ),
            // non-integer value should throw exception
            EvaluatorErrorTestCase(
                query = "select * from foo OFFSET 2.5",
                expectedErrorCode = ErrorCode.EVALUATOR_NON_INT_OFFSET_VALUE,
                expectedErrorContext = propertyValueMapOf(1, 26, Property.ACTUAL_TYPE to "DECIMAL")
            ),
            // OFFSET value should not exceed Long type
            EvaluatorErrorTestCase(
                query = "select * from foo OFFSET ${Long.MAX_VALUE}0",
                expectedErrorCode = ErrorCode.SEMANTIC_LITERAL_INT_OVERFLOW,
                expectedErrorContext = propertyValueMapOf(1, 26)
            )
        )
    }

    @ParameterizedTest
    @ArgumentsSource(ArgsProviderError::class)
    fun errorTests(tc: EvaluatorErrorTestCase) = runEvaluatorErrorTestCase(
        tc.copy(excludeLegacySerializerAssertions = true),
        session
    )
}
