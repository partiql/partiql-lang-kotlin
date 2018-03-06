package com.amazon.ionsql.eval.builtins

import com.amazon.ion.*
import com.amazon.ion.system.*
import com.amazon.ionsql.eval.*
import com.amazon.ionsql.util.*
import org.assertj.core.api.Assertions.*
import org.junit.*
import org.junit.Assert.*

class BuiltinFunctionFactoryTest {
    private val ion = IonSystemBuilder.standard().build()
    private val bindings = Bindings.empty()
    private val env = Environment(locals = bindings,
                                  current = bindings,
                                  session = EvaluationSession.builder().build(),
                                  registers = RegisterBank(0))

    private val factory = BuiltinFunctionFactory(ion)

    @Test
    fun existsWithNonEmptyCollectionReturnsTrue() {
        val value = factory.exists().call(env, listOf(ion.newList(ion.newInt(1)).exprValue()))
        assertTrue(value.ionValue.booleanValue()!!)
    }

    @Test
    fun existsWithEmptyCollectionReturnsFalse() {
        val value = factory.exists().call(env, listOf(ion.newList().exprValue()))
        assertFalse(value.ionValue.booleanValue()!!)
    }

    @Test
    fun utcNowDefaultSession() {
        val actual = factory.utcNow().call(env, listOf()).ionValue.timestampValue()

        assertEquals("utcNow is not the session now", env.session.now, actual)
        assertEquals("utcNow is not at the zero offset", 0, actual.localOffset)
    }

    @Test
    fun utcNowPassedInSession() {
        val now = Timestamp.forMillis(10, 0)

        val env = Environment(locals = bindings,
                              current = bindings,
                              session = EvaluationSession.build { now(now) },
                              registers = RegisterBank(0))

        val actual = factory.utcNow().call(env, listOf()).ionValue.timestampValue()

        assertEquals(now, actual)
        assertEquals(10, actual.millis)
        assertEquals("utcNow is not at the zero offset", 0, actual.localOffset)
    }

    @Test
    fun utcNowPassedInSessionWithNonUtcOffset() {
        val utcMillis = 10L * 24 * 60 * 60 * 1_000 // 1970-01-10T00:00:00.000Z
        val localOffset = 5

        val now = Timestamp.forMillis(utcMillis, localOffset) // 1970-01-10T00:05:00.000+00:05

        val env = Environment(locals = bindings,
                              current = bindings,
                              session = EvaluationSession.build { now(now) },
                              registers = RegisterBank(0))

        val actual = factory.utcNow().call(env, listOf()).timestampValue()

        assertEquals(utcMillis, actual.millis)
        assertEquals("utcNow is not at the zero offset", 0, actual.localOffset)
    }

    @Test
    fun utcNowPassedInArguments() {
        assertThatThrownBy { factory.utcNow().call(env, listOf("".exprValue(ion))) }
            .hasMessage("utcnow() takes no arguments")
            .isExactlyInstanceOf(EvaluationException::class.java)
    }
}