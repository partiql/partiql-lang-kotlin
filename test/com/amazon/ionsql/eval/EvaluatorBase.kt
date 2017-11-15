/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql.eval

import com.amazon.ionsql.*
import com.amazon.ionsql.errors.*
import com.amazon.ionsql.util.*
import org.junit.*
import kotlin.reflect.*

abstract class EvaluatorBase : Base() {

    /**
     * creates a [ExprValue] from the IonValue represented by this String. Assumes the string represents a single
     * IonValue
     */
    private fun String.toExprValue(): ExprValue = literal(this).exprValue()

    protected fun Map<String, String>.toBindings(): Bindings = Bindings.over { key -> this[key]?.toExprValue() }

    protected fun Map<String, String>.toSession() = EvaluationSession.build { globals(this@toSession.toBindings()) }

    fun voidEval(source: String, compileOptions: CompileOptions = CompileOptions.default()) {

        // force materialization
        eval(source, compileOptions).ionValue
    }

    /**
     * Assert that the result of the evaluation of source is the same as expected given optionally given an
     * [EvaluationSession] and [CompileOptions].
     *
     * @param source query source to be tested
     * @param expected expected result
     * @param session [EvaluationSession] used for evaluation
     * @param block function literal with receiver used to plug in custom assertions
     */
    protected fun assertEval(source: String,
                             expected: String,
                             session: EvaluationSession = EvaluationSession.default(),
                             compileOptions: CompileOptions = CompileOptions.default(),
                             block: AssertExprValue.() -> Unit = { }) {

        val expectedIon = literal(expected)
        val exprValue = eval(source, compileOptions, session)

        AssertExprValue(exprValue).apply { assertIonValue(expectedIon) }
            .run(block)
    }

    /**
     * Assert that the result of the evaluation of source is a `MISSING` value, optionally given an
     * [EvaluationSession] and [CompileOptions].
     *
     * @param source query source to be tested
     * @param expected expected result
     * @param session [EvaluationSession] used for evaluation
     * @param block function literal with receiver used to plug in custom assertions
     */
    protected fun assertEvalIsMissing(source: String,
                             session: EvaluationSession = EvaluationSession.default(),
                             compileOptions: CompileOptions = CompileOptions.default()) {

        val exprValue = eval(source, compileOptions, session)
        assertEquals(ExprValueType.MISSING, exprValue.type)
    }

    /**
     * Evaluates a source query given a [EvaluationSession]
     *
     * @param source query source to be evaluated
     * @param session [EvaluationSession] used for evaluation
     */
    protected fun eval(source: String,
                       compileOptions: CompileOptions,
                       session: EvaluationSession = EvaluationSession.default()): ExprValue {

        val e = EvaluatingCompiler(ion, compileOptions = compileOptions)
        return e.compile(source).eval(session)
    }
    /**
     *  Asserts that [func] throws an [IonSqlException] with the specified message, line and column number
     */
    protected fun assertThrows(message: String,
                               metadata: NodeMetadata? = null,
                               internal: Boolean = false,
                               cause: KClass<out Throwable>? = null,
                               func: () -> Unit) {
        try {
            func()
            Assert.fail("didn't throw")
        }
        catch (e: EvaluationException) {
            softAssert {
                assertThat(e.message).`as`("error message").isEqualTo(message)
                assertThat(e.internal).isEqualTo(internal)

                if (cause != null) assertThat(e).hasRootCauseExactlyInstanceOf(cause.java)

                if(metadata != null){
                    assertThat(e.errorContext!![Property.LINE_NUMBER]!!.longValue()).`as`("line number").isEqualTo(metadata.line)
                    assertThat(e.errorContext!![Property.COLUMN_NUMBER]!!.longValue()).`as`("column number").isEqualTo(metadata.column)
                }
                else {
                    assertThat(e.errorContext).isNull()
                }
            }
        }
    }

    protected fun checkInputThrowingEvaluationException(input: String,
                                                        errorCode: ErrorCode,
                                                        expectErrorContextValues: Map<Property, Any>) {
        try {
            voidEval(input)
            fail("Expected EvaluationException but there was no Exception")
        } catch (pex: EvaluationException) {
            checkErrorAndErrorContext(errorCode, pex, expectErrorContextValues)
        } catch (ex: Exception) {
            fail("Expected EvaluationException but a different exception was thrown \n\t  $ex")
        }
    }
}
