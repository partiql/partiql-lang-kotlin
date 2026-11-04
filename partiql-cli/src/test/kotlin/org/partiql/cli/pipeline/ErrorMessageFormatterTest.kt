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

package org.partiql.cli.pipeline

import org.junit.jupiter.api.Test
import org.partiql.spi.catalog.Identifier
import org.partiql.spi.errors.PError
import org.partiql.spi.errors.PErrorKind
import org.partiql.spi.errors.Severity
import kotlin.test.assertContains
import kotlin.test.assertEquals

class ErrorMessageFormatterTest {

    @Test
    fun `formats ambiguous function candidates`() {
        val error = PError(
            PError.FUNCTION_AMBIGUOUS,
            Severity.ERROR(),
            PErrorKind.SEMANTIC(),
            null,
            mapOf(
                "FN_ID" to Identifier.regular("demo", "echo"),
                "CANDIDATES" to listOf(
                    "scalar demo.echo",
                    "aggregate demo.echo",
                ),
            ),
        )

        val message = ErrorMessageFormatter.message(error)

        assertContains(message, "Ambiguous function")
        assertContains(message, "scalar demo.echo")
        assertContains(message, "aggregate demo.echo")
    }

    @Test
    fun `formats ambiguous function without candidates`() {
        listOf(
            mapOf<String, Any>("FN_ID" to Identifier.regular("demo", "echo")),
            mapOf<String, Any>(
                "FN_ID" to Identifier.regular("demo", "echo"),
                "CANDIDATES" to emptyList<String>(),
            ),
        ).forEach { properties ->
            val error = PError(
                PError.FUNCTION_AMBIGUOUS,
                Severity.ERROR(),
                PErrorKind.SEMANTIC(),
                null,
                properties,
            )

            assertEquals(
                "e: [semantic] Ambiguous function demo.echo.",
                ErrorMessageFormatter.message(error),
            )
        }
    }

    @Test
    fun `formats invalid window function`() {
        val error = PError(
            PError.INVALID_WINDOW_FUNCTION,
            Severity.ERROR(),
            PErrorKind.SEMANTIC(),
            null,
            mapOf("FN_NAME" to "PERCENT_RANK", "REASON" to "requires a window ORDER BY clause"),
        )

        assertEquals(
            "e: [semantic] Invalid window function PERCENT_RANK: requires a window ORDER BY clause.",
            ErrorMessageFormatter.message(error),
        )
    }
}
