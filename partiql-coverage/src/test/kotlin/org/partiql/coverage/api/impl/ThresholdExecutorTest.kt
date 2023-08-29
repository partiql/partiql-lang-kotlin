/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.partiql.coverage.api.impl

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

/**
 * Tests to make sure we will throw exceptions.
 */
class ThresholdExecutorTest {
    /**
     * This fail has 38 branches found, 23 branches hit. Therefore, calculated threshold is: 23/38 ~= 0.60526315789
     */
    private val sampleLcovFile = ThresholdExecutorTest::class.java.classLoader.getResource("lcov/threshold.info")!!

    @Test
    fun testFailingBranch() {
        val threshold = 0.9
        val exception = assertThrows<ThresholdException> {
            ThresholdExecutor.execute(threshold, sampleLcovFile.path, ThresholdException.ThresholdType.BRANCH)
        }
        assertEquals(threshold, exception.minimum)
        assertEquals("0.605", String.format("%.3f", exception.actual))
    }

    @Test
    fun testBreachingSmall() {
        val threshold = 0.606
        val exception = assertThrows<ThresholdException> {
            ThresholdExecutor.execute(threshold, sampleLcovFile.path, ThresholdException.ThresholdType.BRANCH)
        }
        assertEquals(threshold, exception.minimum)
        assertEquals("0.605", String.format("%.3f", exception.actual))
    }

    @Test
    fun testMatchingBranch() = assertDoesNotThrow {
        ThresholdExecutor.execute(0.605, sampleLcovFile.path, ThresholdException.ThresholdType.BRANCH)
    }

    @Test
    fun testAboveThreshold() = assertDoesNotThrow {
        ThresholdExecutor.execute(0.6, sampleLcovFile.path, ThresholdException.ThresholdType.BRANCH)
    }
}
