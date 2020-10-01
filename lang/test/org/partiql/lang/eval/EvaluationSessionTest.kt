/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

package org.partiql.lang.eval

import com.amazon.ion.*
import org.partiql.lang.util.*
import org.junit.*
import org.junit.Test
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
    fun default() = assertDefault { EvaluationSession.standard() }

    @Test
    fun emptyKotlinStyleBuilder() = assertDefault { EvaluationSession.build {} }

    @Test
    fun emptyJavaStyleBuilder() = assertDefault { EvaluationSession.builder().build() }

    @Test
    fun settingGlobals() {
        val globals = Bindings.empty<ExprValue>()
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