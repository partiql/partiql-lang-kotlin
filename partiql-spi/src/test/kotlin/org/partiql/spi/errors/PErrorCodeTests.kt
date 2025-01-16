package org.partiql.spi.errors

import org.junit.jupiter.api.Test

class PErrorCodeTests {
    /**
     * This ensures that we don't accidentally duplicate error codes. It also ensures that we don't change error codes.
     * Whenever a new code is added to [PError], we should add an entry to the `manualEntries` map with the new code
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
            "INTERNAL_ERROR" to 1,
            "UNRECOGNIZED_TOKEN" to 2,
            "UNEXPECTED_TOKEN" to 3,
            "PATH_KEY_NEVER_SUCCEEDS" to 4,
            "PATH_SYMBOL_NEVER_SUCCEEDS" to 5,
            "PATH_INDEX_NEVER_SUCCEEDS" to 6,
            "FEATURE_NOT_SUPPORTED" to 7,
            "UNDEFINED_CAST" to 8,
            "FUNCTION_NOT_FOUND" to 9,
            "FUNCTION_TYPE_MISMATCH" to 10,
            "VAR_REF_NOT_FOUND" to 11,
            "VAR_REF_AMBIGUOUS" to 12,
            "TYPE_UNEXPECTED" to 13,
            "ALWAYS_MISSING" to 14,
            "INVALID_EXCLUDE_PATH" to 15,
            "CARDINALITY_VIOLATION" to 16,
            "NUMERIC_VALUE_OUT_OF_RANGE" to 17,
            "INVALID_CHAR_VALUE_FOR_CAST" to 18
        )

        // Preparation
        val reflectionEntries = PError::class.java.fields.filter {
            java.lang.reflect.Modifier.isStatic(it.modifiers) // && java.lang.reflect.Modifier.isPublic(it.modifiers)
        }.map {
            it.isAccessible = true
            it.name to it.get(null)
        }.filter {
            it.second is Int // Only grab the static final integers
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
