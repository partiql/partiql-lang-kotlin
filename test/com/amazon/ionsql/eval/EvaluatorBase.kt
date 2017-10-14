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

    val evaluator = EvaluatingCompiler(ion)

    fun voidEval(source: String, bindingsMap: Map<String, String>) {
        voidEval(source, bindingsMap.toBindings())
    }

    fun voidEval(source: String, bindings: Bindings = Bindings.empty()) {
        // force materialization
        evalWithBindings(source).ionValue
    }

    /**
     * Assert that the evaluation of source is the same as expected given a [Bindings]
     *
     * @param source query source to be tested
     * @param expected expected result
     * @param session [EvaluationSession] used for evaluation
     * @param block function literal with receiver used to plug in custom assertions
     */
    protected fun assertEval(source: String,
                             expected: String,
                             session: EvaluationSession = EvaluationSession.default(),
                             block: AssertExprValue.() -> Unit = { }) {

        val expectedIon = literal(expected)
        val exprValue = evalWithBindings(source, session)

        AssertExprValue(exprValue).apply { assertIonValue(expectedIon) }
                                  .run(block)
    }

    /**
     * Evaluates a source query given a [Bindings]
     *
     * @param source query source to be evaluated
     * @param session [EvaluationSession] used for evaluation
     */
    protected fun evalWithBindings(source: String,
                                   session: EvaluationSession = EvaluationSession.default()): ExprValue =
        evaluator.compile(source).eval(session)

    /**
     *  Asserts that [f] throws an [IonSqlException] with the specified message, line and column number
     */
    protected fun assertThrows(message: String,
                             metadata: NodeMetadata,
                             cause: KClass<out Throwable>? = null,
                             f: () -> Unit) {
        try {
            f()
            Assert.fail("didn't throw")
        }
        catch (e: IonSqlException) {
            softAssert {
                assertThat(e.message).`as`("error message").isEqualTo(message)

                if (cause != null) assertThat(e).hasRootCauseExactlyInstanceOf(cause.java)

                assertThat(e.errorContext!![Property.LINE_NUMBER]!!.longValue()).`as`("line number").isEqualTo(metadata.line)
                assertThat(e.errorContext!![Property.COLUMN_NUMBER]!!.longValue()).`as`("column number").isEqualTo(metadata.column)
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
