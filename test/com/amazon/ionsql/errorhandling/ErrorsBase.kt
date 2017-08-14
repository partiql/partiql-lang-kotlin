package com.amazon.ionsql.errorhandling

import com.amazon.ionsql.Base
import com.amazon.ionsql.IonSqlException

open class ErrorsBase : Base() {


    /**
     * Validates [errorCode] and [expectedValues] for given exception [ex].
     *
     * @param errorCode expected errorCode
     * @param ex actual exception thrown by test
     * @param expectedValues expected values for errorContext
     * @param strict if `true` [expectedValues] must contain exactly all keys and values as the exception's errorContext,
     *        if `false` [expectedValues] must contain a **subset** of keys and their values as the exception's errorContext.
     */
    protected fun <T : IonSqlException> checkErrorAndErrorContext(errorCode: ErrorCode, ex: T, expectedValues: Map<Property, Any>, strict: Boolean) {
        assertEquals(errorCode, ex.getErrorCode())
        val errorContext = ex.getErrorContext()
        correctContextKeys(errorCode, errorContext)
        correctContextValues(errorCode, errorContext, expectedValues, strict)
    }

    /**
     * Checks that [errorContext] contains keys for each [Property] found in [errorCode]
     *
     * @param errorCode for the exception thrown by the test
     * @param errorContext errorContext that was part of the exception thrown by the test
     */
    protected fun correctContextKeys(errorCode: ErrorCode, errorContext: PropertyBag?): Unit =
        errorCode.getProperties().forEach { assertTrue("Error Context does not contain $it", errorContext!!.hasProperty(it)) }


    /**
     * Check that for the given [errorCode], [errorContext] thrown by the test, it contains all the [expected] values
     * specified by the test.
     *
     * @param errorCode for the exception thrown by the test
     * @param errorContext errorContext that was part of the exception thrown by the test
     * @param expected expected values for errorContext
     * @strict if `true` [expected] must contain exactly all keys and values as the exception's errorContext,
     *        if `false` [expected] must contain a **subset** of keys and their values as the exception's errorContext.
     *
     */
    protected fun correctContextValues(errorCode: ErrorCode, errorContext: PropertyBag?, expected: Map<Property, Any>, strict: Boolean) {
        if (strict)
            assertTrue("Strict mode requires expected param to contain all Properties for the error code",
                errorCode.getProperties().containsAll(expected.keys))

        expected.forEach { entry ->
            assertTrue("Error Context does not contain ${entry.key}", errorContext!!.hasProperty(entry.key))
            assertEquals("$entry", entry.value, errorContext.getProperty(entry.key, entry.key.type))
        }
    }

}