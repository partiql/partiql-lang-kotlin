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

import org.junit.Test
import kotlin.test.assertEquals

class EvaluationSessionTest {
    private fun assertDefault(block: () -> EvaluationSession) {
        val session = block.invoke()

        assertEquals(Bindings.empty(), session.globals)
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
        val block: () -> EvaluationSession = { EvaluationSession.build { globals(globals) } }
        val session = block.invoke()

        assertEquals(globals, session.globals)
    }

    @Test
    fun canSetContextVariables() {
        val session = EvaluationSession.build {
            withContextVariable("meaning", 42)
            withContextVariable("captain", "Picard")
        }
        assertEquals(2, session.context.size)
        assertEquals(42, session.context["meaning"])
        assertEquals("Picard", session.context["captain"])
    }
}
