/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql

import com.amazon.ion.IonSystem
import com.amazon.ion.IonValue
import com.amazon.ion.system.IonSystemBuilder
import com.amazon.ionsql.errors.*
import junitparams.JUnitParamsRunner
import org.junit.Assert
import org.junit.runner.RunWith
import java.util.*

import com.amazon.ionsql.errors.Property.*
import com.amazon.ionsql.eval.*


@RunWith(JUnitParamsRunner::class)
abstract class Base : Assert() {
    val ion: IonSystem = IonSystemBuilder.standard().build()

    fun literal(text: String): IonValue = ion.singleValue(text)

    inner class AssertExprValue(val exprValue: ExprValue,
                                val bindingsTransform: Bindings.() -> Bindings = { this }) {

        fun assertBindings(predicate: Bindings.() -> Boolean) =
            assertTrue(
                exprValue.bindings.bindingsTransform().predicate()
            )

        fun assertBinding(name: String, predicate: ExprValue.() -> Boolean) = assertBindings {
            get(name)?.predicate() ?: false
        }

        fun assertNoBinding(name: String) = assertBindings { get(name) == null }

        fun assertIonValue(expected: IonValue) {
            assertEquals(expected, exprValue.ionValue)
        }

        fun assertIterator(expected: Collection<IonValue>) {
            val actual = ArrayList<IonValue>()
            exprValue.asSequence().map { it.ionValue }.toCollection(actual)
            assertEquals(expected, actual)
        }
    }

    /**
     * Validates [errorCode] and [expectedValues] for given exception [ex].
     *
     * @param errorCode expected errorCode
     * @param ex actual exception thrown by test
     * @param expectedValues expected values for errorContext
     */
    protected fun <T : IonSqlException> checkErrorAndErrorContext(errorCode: ErrorCode, ex: T, expectedValues: Map<Property, Any>) {
        assertEquals(errorCode, ex.errorCode)
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
    protected fun correctContextKeys(errorCode: ErrorCode, errorContext: PropertyValueMap?): Unit =
        errorCode.getProperties().forEach { assertTrue("Error Context does not contain $it", errorContext!!.hasProperty(it)) }


    /**
     * Check that for the given [errorCode], [errorContext] thrown by the test, it contains all the [expected] values
     * specified by the test.
     *
     * @param errorCode for the exception thrown by the test
     * @param errorContext errorContext that was part of the exception thrown by the test
     * @param expected expected values for errorContext
     */
    protected fun correctContextValues(errorCode: ErrorCode, errorContext: PropertyValueMap?, expected: Map<Property, Any>) {

        assertTrue("Expected parameter must contain all Properties for the error code",
                   errorCode.getProperties().containsAll(expected.keys))


        val unexpectedProperties = errorCode.getProperties().filter { p -> !expected.containsKey(p) }
        if(unexpectedProperties.any()) {
            fail("Unexpected properties found in error code: ${unexpectedProperties.joinToString(", ")}")
        }


        expected.forEach { entry ->
            assertTrue("Error Context does not contain ${entry.key}", errorContext!!.hasProperty(entry.key))

            when (entry.key) {
                LINE_NUMBER,
                COLUMN_NUMBER -> assertEquals("$entry", entry.value, errorContext[entry.key]?.longValue())
                TOKEN_STRING,
                CAST_TO,
                KEYWORD,
                TIMESTAMP_FORMAT_PATTERN -> assertEquals("$entry", entry.value, errorContext[entry.key]?.stringValue())
                TOKEN_TYPE,
                EXPECTED_TOKEN_TYPE_1_OF_2,
                EXPECTED_TOKEN_TYPE_2_OF_2,
                EXPECTED_TOKEN_TYPE -> assertEquals("$entry", entry.value, errorContext[entry.key]?.tokenTypeValue())
                TOKEN_VALUE -> assertEquals("$entry", entry.value, errorContext[entry.key]?.ionValue())
                EXPECTED_ARITY_MIN,
                EXPECTED_ARITY_MAX -> assertEquals("$entry", entry.value, errorContext[entry.key]?.integerValue())
            }
        }
    }
}
