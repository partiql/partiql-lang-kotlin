package org.partiql.lang.eval.builtins

import com.amazon.ion.Timestamp
import org.junit.Test
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.eval.Bindings
import org.partiql.lang.eval.Environment
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.EvaluatorTestBase
import org.partiql.lang.eval.RequiredArgs
import org.partiql.lang.eval.call
import org.partiql.lang.eval.timestampValue
import org.partiql.lang.util.timestampValue
import org.partiql.lang.util.to

class UtcNowEvaluationTest : EvaluatorTestBase() {
    private val env = Environment.standard()

    @Test fun utcnow1() = assertEval("utcnow()",
                                     "1970-01-01T00:00:00.000Z",
                                     session = EvaluationSession.build { now(Timestamp.forMillis(0, 0)) })

    @Test fun utcnow2() = assertEval("utcnow()",
                                     "1970-01-01T00:00:01.000Z",
                                     session = EvaluationSession.build { now(Timestamp.forMillis(1_000, 0)) })

    @Test fun utcnowWithDifferentOffset() {
        val fiveMinutesInMillis = 5L * 60 * 1_000
        val now = Timestamp.forMillis(fiveMinutesInMillis, 1) // 1970-01-01T00:06:01.000+00:01
        val session = EvaluationSession.build { now(now) }

        assertEval("utcnow()", "1970-01-01T00:05:00.000Z", session = session)
    }

    @Test
    fun utcNowDefaultSession() {
        val actual = createUtcNow(valueFactory).call(env, RequiredArgs(listOf())).ionValue.timestampValue()

        assertEquals("utcNow is not the session now", env.session.now, actual)
        assertEquals("utcNow is not at the zero offset", 0, actual.localOffset)
    }

    @Test
    fun utcNowPassedInSession() {
        val now = Timestamp.forMillis(10, 0)

        val env = Environment(locals = Bindings.empty(),
                              session = EvaluationSession.build { now(now) })

        val actual = createUtcNow(valueFactory).call(env, RequiredArgs(listOf())).ionValue.timestampValue()

        assertEquals(now, actual)
        assertEquals(10, actual.millis)
        assertEquals("utcNow is not at the zero offset", 0, actual.localOffset)
    }

    @Test
    fun utcNowPassedInSessionWithNonUtcOffset() {
        val utcMillis = 10L * 24 * 60 * 60 * 1_000 // 1970-01-10T00:00:00.000Z
        val localOffset = 5

        val now = Timestamp.forMillis(utcMillis, localOffset) // 1970-01-10T00:05:00.000+00:05

        val env = Environment(locals = Bindings.empty(),
                              session = EvaluationSession.build { now(now) })

        val actual = createUtcNow(valueFactory).call(env, RequiredArgs(listOf())).timestampValue()

        assertEquals(utcMillis, actual.millis)
        assertEquals("utcNow is not at the zero offset", 0, actual.localOffset)
    }

    @Test
    fun utcNowPassedInArguments() =
        checkInputThrowingEvaluationException(
            input = "utcnow('')",
            errorCode = ErrorCode.EVALUATOR_INCORRECT_NUMBER_OF_ARGUMENTS_TO_FUNC_CALL,
            expectErrorContextValues = mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 1L,
                Property.FUNCTION_NAME to "utcnow",
                Property.EXPECTED_ARITY_MIN to 0,
                Property.EXPECTED_ARITY_MAX to 0,
                Property.ACTUAL_ARITY to 1
            )
        )
}
