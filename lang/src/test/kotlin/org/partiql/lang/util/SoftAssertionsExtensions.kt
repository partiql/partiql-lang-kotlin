package org.partiql.lang.util

import org.assertj.core.api.SoftAssertions
import org.partiql.lang.SqlException
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.errors.PropertyType
import org.partiql.lang.errors.PropertyValue
import org.partiql.lang.errors.PropertyValueMap

/**
 * Validates [errorCode] and [expectedValues] for given exception [ex].
 *
 * @param errorCode expected errorCode
 * @param ex actual exception thrown by test
 * @param expectedValues expected values for errorContext
 */
internal fun <T : SqlException> SoftAssertions.checkErrorAndErrorContext(errorCode: ErrorCode, ex: T, expectedValues: Map<Property, Any>) {

    this.assertThat(ex.errorCode).isEqualTo(errorCode)

    val errorContext = ex.errorContext

    correctContextKeys(errorCode, errorContext)
    correctContextValues(errorCode, errorContext, expectedValues)
}

/**
 * Checks that [errorContext] contains keys for each [Property] found in [errorCode]
 *
 * @param errorCode for the exception thrown by the test
 * @param errorContext errorContext that was part of the exception thrown by the test
 */
private fun SoftAssertions.correctContextKeys(errorCode: ErrorCode, errorContext: PropertyValueMap?): Unit =
    errorCode.getProperties().forEach {
        assertThat(errorContext!!.hasProperty(it))
            .withFailMessage("Error Context does not contain $it")
            .isTrue
    }

/**
 * Check that for the given [errorCode], [errorContext] thrown by the test, it contains all the [expected] values
 * specified by the test.
 *
 * @param errorCode for the exception thrown by the test
 * @param errorContext errorContext that was part of the exception thrown by the test
 * @param expected expected values for errorContext
 */
private fun SoftAssertions.correctContextValues(errorCode: ErrorCode, errorContext: PropertyValueMap?, expected: Map<Property, Any>) {

    assertThat(errorCode.getProperties().containsAll(expected.keys))
        .withFailMessage(
            "Actual errorCode must contain these properties: " +
                "${expected.keys.joinToString(", ")} but contained only: " +
                errorCode.getProperties().joinToString(", ")
        )
        .isTrue

    val unexpectedProperties = errorCode.getProperties().filter { p -> !expected.containsKey(p) }
    if (unexpectedProperties.any()) {
        fail("Unexpected properties found in error code: ${unexpectedProperties.joinToString(", ")}")
    }

    if (errorContext == null) return
    expected.forEach { entry ->
        val actualPropertyValue: PropertyValue? = errorContext[entry.key]
        assertThat(errorContext.hasProperty(entry.key))
            .withFailMessage("Error Context does not contain ${entry.key}")
            .isTrue

        val message by lazy {
            "Expected property ${entry.key} to have value '${entry.value}' " +
                "but found value '$actualPropertyValue'"
        }

        val propertyValue: Any? = actualPropertyValue?.run {
            when (entry.key.propertyType) {
                PropertyType.LONG_CLASS -> longValue()
                PropertyType.STRING_CLASS -> stringValue()
                PropertyType.INTEGER_CLASS -> integerValue()
                PropertyType.TOKEN_CLASS -> tokenTypeValue()
                PropertyType.ION_VALUE_CLASS -> ionValue()
            }
        }

        assertThat(propertyValue)
            .withFailMessage(message)
            .isEqualTo(entry.value)
    }
}
