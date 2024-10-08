package org.partiql.spi.errors

import org.junit.jupiter.api.Test

class ErrorCodeTests {
    /**
     * This ensures that we don't accidentally duplicate error codes. It also ensures that we don't change error codes.
     * Whenever a new code is added to [ErrorCode], we should add an entry to the `manualEntries` map with the new code
     * and a unique value.
     *
     * This test will fail if we accidentally duplicate an error code or if we change an existing error code.
     *
     * If we ever need to delete an error code from our public API, we will need to update this test to make sure that
     * we never accidentally use the value that it previously used.
     */
    @Test
    fun testErrorCodesAreUnique() {
        val manualEntries = mapOf(
            "UNKNOWN" to 0,
            "INTERNAL_ERROR" to 1,
            "UNRECOGNIZED_TOKEN" to 2,
            "UNEXPECTED_TOKEN" to 3,
            "ALWAYS_MISSING" to 4,
        )

        // Preparation
        val reflectionEntries = ErrorCode::class.java.fields.filter { java.lang.reflect.Modifier.isStatic(it.modifiers) }.map {
            it.isAccessible = true
            it.name to it.get(null) as Int
        }.toMap()
        val reflectionValues = reflectionEntries.values
        val manualValues = manualEntries.values

        // Assert that all values are unique.
        val areUnique = reflectionValues.distinct().size == reflectionValues.size
        assert(areUnique)
        val areUnique2 = manualValues.distinct().size == manualValues.size
        assert(areUnique2)

        // Assert that manual entries values match the reflection values.
        assert(reflectionEntries == manualEntries) {
            buildString {
                appendLine("Reflection entries: $reflectionEntries")
                appendLine("Manual entries: $manualEntries")
            }
        }
    }
}
