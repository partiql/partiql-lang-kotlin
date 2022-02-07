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

import org.junit.*
import org.junit.Assert.*
import java.time.ZoneOffset

class CompileOptionsTest {
    private fun assertDefault(actual: CompileOptions) {
        assertEquals(UndefinedVariableBehavior.ERROR, actual.undefinedVariable)
        assertEquals(ZoneOffset.UTC, actual.defaultTimezoneOffset)
    }

    @Test
    fun default() = assertDefault(CompileOptions.standard())

    @Test
    fun emptyKotlinBuilder() = assertDefault(CompileOptions.build {})

    @Test
    fun emptyJavaBuilder() = assertDefault(CompileOptions.builder().build())

    @Test
    fun changingUndefinedVariable() {
        val compileOptions = CompileOptions.builder().undefinedVariable(UndefinedVariableBehavior.MISSING).build()

        assertEquals(UndefinedVariableBehavior.MISSING, compileOptions.undefinedVariable)
    }

    @Test
    fun testSettingDefaultTimezoneOffset() {
        val otherTimezoneOffset = ZoneOffset.ofHours(5)
        val compileOptions = CompileOptions.builder().defaultTimezoneOffset(otherTimezoneOffset).build()

        assertEquals(otherTimezoneOffset, compileOptions.defaultTimezoneOffset)
    }
}
