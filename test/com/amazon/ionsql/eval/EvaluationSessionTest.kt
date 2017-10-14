package com.amazon.ionsql.eval

import com.amazon.ion.*
import com.amazon.ionsql.util.*
import org.junit.*
import kotlin.test.*

class EvaluationSessionTest {
    private fun assertDefault(block: () -> EvaluationSession) {
        assertEquals(Bindings.empty(), block.invoke().globals)
        assertNow(block)
    }

    private fun assertNow(block: () -> EvaluationSession) {
        val beforeNow = Timestamp.nowZ()
        val session = block.invoke()
        val afterNow = Timestamp.nowZ()

        softAssert {
            // can't assert the defaulting `now`
            assertThat(session.now).isNotNull()
            assertThat(session.now.dateValue()).isBeforeOrEqualsTo(afterNow.dateValue())
            assertThat(session.now.dateValue()).isAfterOrEqualsTo(beforeNow.dateValue())
        }
    }

    @Test
    fun default() = assertDefault { EvaluationSession.default() }

    @Test
    fun emptyKotlinStyleBuilder() = assertDefault { EvaluationSession.build {} }

    @Test
    fun emptyJavaStyleBuilder() = assertDefault { EvaluationSession.builder().build() }

    @Test
    fun settingGlobals() {
        val globals = Bindings.over { _ -> null }

        val block: () -> EvaluationSession = { EvaluationSession.build { globals(globals) }}

        assertEquals(globals, block.invoke().globals)
        assertNow(block)
    }

    @Test
    fun settingNow() {
        val now = Timestamp.forMillis(10, 0)
        val session =  EvaluationSession.build { now(now) }

        assertEquals(Bindings.empty(), session.globals)
        assertEquals(now, session.now)
    }

    @Test
    fun settingNowNonZeroOffset() {
        val now = Timestamp.forMillis(10, 10)
        val utcNow = now.withLocalOffset(0)

        val session =  EvaluationSession.build { now(now) }

        assertEquals(Bindings.empty(), session.globals)
        assertEquals(utcNow, session.now)
    }
}