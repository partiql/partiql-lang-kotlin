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

package org.partiql.lang

import com.amazon.ion.system.IonSystemBuilder
import org.partiql.lang.ast.*
import org.partiql.lang.ast.passes.*
import org.partiql.lang.errors.*
import junitparams.JUnitParamsRunner
import org.junit.Assert
import org.junit.runner.RunWith
import java.util.*

import org.partiql.lang.errors.Property.*
import org.partiql.lang.eval.*
import org.partiql.lang.util.*
import org.assertj.core.api.*
import com.amazon.ion.*
import kotlin.reflect.*
import kotlin.test.*


@RunWith(JUnitParamsRunner::class)
abstract class TestBase : Assert() {
    val ion: IonSystem = IonSystemBuilder.standard().build()
    val valueFactory = ExprValueFactory.standard(ion)

    val defaultRewriter = AstRewriterBase()

    protected fun anyToExprValue(value: Any) = when (value) {
        is String    -> valueFactory.newString(value)
        is Int       -> valueFactory.newInt(value)
        is Decimal   -> valueFactory.newDecimal(value)
        is Timestamp -> valueFactory.newTimestamp(value)
        else         ->
            error("Can't convert receiver to ExprValue (please extend this function to support the receiver's data type).")
    }

    inner class AssertExprValue(val exprValue: ExprValue,
                                val bindingsTransform: Bindings.() -> Bindings = { this },
                                val message: String? = null) {
        fun assertBindings(predicate: Bindings.() -> Boolean) =
            assertTrue(
                exprValue.bindings.bindingsTransform().predicate()
            )

        fun assertBinding(name: String, predicate: ExprValue.() -> Boolean) = assertBindings {
            get(BindingName(name, BindingCase.SENSITIVE))?.predicate() ?: false
        }

        fun assertNoBinding(name: String) = assertBindings { get(BindingName(name, BindingCase.INSENSITIVE)) == null }

        fun assertIonValue(expected: IonValue) {
            assertEquals(message, expected, exprValue.ionValue)
        }

        fun assertIterator(expected: Collection<IonValue>) {
            val actual = ArrayList<IonValue>()
            exprValue.asSequence().map { it.ionValue }.toCollection(actual)
            assertEquals(expected, actual)
        }
    }


    protected fun assertRewrite(originalSql: String, exprNode: ExprNode) {
        val clonedAst = defaultRewriter.rewriteExprNode(exprNode)
        assertEquals(exprNode, clonedAst,
            "AST returned from default AstRewriterBase should match the original AST.\n" +
            "AST originating from the query: $originalSql\n")
    }

    protected fun assertSexpEquals(
        expectedValue: IonValue,
        actualValue: IonValue,
        message: String = ""
    ) {
        if(!expectedValue.equals(actualValue)) {
            fail(
                "Expected and actual values do not match: $message\n" +
                "Expected:\n${SexpAstPrettyPrinter.format(expectedValue)}\n" +
                "Actual:\n${SexpAstPrettyPrinter.format(actualValue)}"
            )
        }
    }

    /**
     * Validates [errorCode] and [expectedValues] for given exception [ex].
     *
     * @param errorCode expected errorCode
     * @param ex actual exception thrown by test
     * @param expectedValues expected values for errorContext
     */
    fun <T : SqlException> SoftAssertions.checkErrorAndErrorContext(errorCode: ErrorCode?, ex: T, expectedValues: Map<Property, Any>) {
        if(ex.errorCode == null && errorCode != null) {
            fail("Expected an error code but exception error code was null, message was: ${ex.message}")
        } else {
            this.assertThat(ex.errorCode).isEqualTo(errorCode)
        }
        val errorContext = ex.errorContext

        if(errorCode != null) {
            correctContextKeys(errorCode, errorContext)
            correctContextValues(errorCode, errorContext, expectedValues)
        }
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
     * Asserts that the specified [block] throws an [SqlException] and its [expectedErrorCode] matches the expected value.
     */
    protected fun assertThrowsSqlException(expectedErrorCode: ErrorCode, block: () -> Unit) {
               try {
            block()
            fail("Expected EvaluationException but there was no Exception")
        }
        catch (e: SqlException) {
            assertEquals("The expected error code did not match the actual error code", expectedErrorCode, e.errorCode)
        }
        catch (e: Exception) {
            fail("Expected EvaluationException but a different exception was thrown \n\t  $e")
        }
    }

    /**
     * Asserts that the specified [block] throws an [EvaluationException] and its [errorCode] and
     * [expectErrorContextValues] match the expected values.
     */
    protected fun assertThrowsEvaluationException(errorCode: ErrorCode? = null,
                                                  expectErrorContextValues: Map<Property, Any>,
                                                  cause: KClass<out Throwable>? = null,
                                                  block: () -> Unit) {
        softAssert {
            try {
                block()
                fail("Expected EvaluationException but there was no Exception")
            }
            catch (e: EvaluationException) {
                if (cause != null) assertThat(e).hasRootCauseExactlyInstanceOf(cause.java)
                checkErrorAndErrorContext(errorCode, e, expectErrorContextValues)
            }
            catch (e: Exception) {
                fail("Expected EvaluationException but a different exception was thrown \n\t  $e")
            }
        }
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
            .withFailMessage("Actual errorCode must contain these properties: " +
                             "${expected.keys.joinToString(", ")} but contained only: " +
                             errorCode.getProperties().joinToString(", "))
            .isTrue

        val unexpectedProperties = errorCode.getProperties().filter { p -> !expected.containsKey(p) }
        if(unexpectedProperties.any()) {
            fail("Unexpected properties found in error code: ${unexpectedProperties.joinToString(", ")}")
        }

        if(errorContext == null) return
        expected.forEach { entry ->
            val actualPropertyValue: PropertyValue? = errorContext[entry.key]
            assertThat(errorContext.hasProperty(entry.key))
                .withFailMessage("Error Context does not contain ${entry.key}")
                .isTrue

            val message by lazy {
                "Expected property ${entry.key} to have value '${entry.value}' " +
                "but found value '${actualPropertyValue.toString()}'"
            }

            when (entry.key) {
                LINE_NUMBER,
                COLUMN_NUMBER
                    -> assertThat(actualPropertyValue?.longValue()).withFailMessage(message).isEqualTo(entry.value)
                TOKEN_STRING,
                CAST_TO,
                CAST_FROM,
                KEYWORD,
                TIMESTAMP_STRING,
                TIMESTAMP_FORMAT_PATTERN,
                BINDING_NAME,
                LIKE_ESCAPE,
                LIKE_PATTERN,
                LIKE_VALUE,
                BINDING_NAME_MATCHES,
                FUNCTION_NAME,
                TIMESTAMP_FORMAT_PATTERN_FIELDS,
                EXPECTED_ARGUMENT_TYPES,
                ACTUAL_ARGUMENT_TYPES,
                FEATURE_NAME
                    -> assertThat(actualPropertyValue?.stringValue()).withFailMessage(message).isEqualTo(entry.value)
                TOKEN_TYPE,
                EXPECTED_TOKEN_TYPE_1_OF_2,
                EXPECTED_TOKEN_TYPE_2_OF_2,
                EXPECTED_TOKEN_TYPE
                    -> assertThat(actualPropertyValue?.tokenTypeValue()).withFailMessage(message).isEqualTo(entry.value)
                TOKEN_VALUE
                    -> assertThat(errorContext[entry.key]?.ionValue()).withFailMessage(message).isEqualTo(entry.value)
                EXPECTED_ARITY_MIN,
                EXPECTED_ARITY_MAX,
                ACTUAL_ARITY
                    -> assertThat(actualPropertyValue?.integerValue()).withFailMessage(message).isEqualTo(entry.value)
            }
        }
    }

    protected fun sourceLocationProperties(lineNum: Long, colNum: Long) =
        mapOf(Property.LINE_NUMBER to lineNum, Property.COLUMN_NUMBER to colNum)
}
