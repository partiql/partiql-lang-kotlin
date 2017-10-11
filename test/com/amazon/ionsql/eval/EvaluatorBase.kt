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

    private fun Map<String, String>.toBindings(): Bindings = Bindings.over { key -> this[key]?.toExprValue() }

    val evaluator = EvaluatingCompiler(ion)

    fun voidEval(source: String, bindingsMap: Map<String, String>) {
        voidEval(source, bindingsMap.toBindings())
    }

    fun voidEval(source: String, bindings: Bindings = Bindings.empty()) {
        // force materialization
        evalWithBindings(source, bindings).ionValue
    }

    /**
     * Assert that the evaluation of source is the same as expected given a binding map
     *
     * @param source query source to be tested
     * @param expected expected result
     * @param bindingsMap map with all bindings used for evaluation, assumes the map values are string representations of
     *                 single IonValue's
     * @param block function literal with receiver used to plug in custom assertions
     */
    protected fun assertEval(source: String,
                             expected: String,
                             bindingsMap: Map<String, String>,
                             block: AssertExprValue.() -> Unit = { }) {

        assertEval(source,
                   expected,
                   bindingsMap.toBindings(),
                   block)
    }

    /**
     * Assert that the evaluation of source is the same as expected given a [Bindings]
     *
     * @param source query source to be tested
     * @param expected expected result
     * @param bindings [Bindings] used for evaluation
     * @param block function literal with receiver used to plug in custom assertions
     */
    protected fun assertEval(source: String,
                             expected: String,
                             bindings: Bindings = Bindings.empty(),
                             block: AssertExprValue.() -> Unit = { }) {

        val expectedIon = literal(expected)
        val exprValue = evalWithBindings(source, bindings)

        AssertExprValue(exprValue).apply { assertIonValue(expectedIon) }
                                  .run(block)
    }

    /**
     * Evaluates a source query given a [Bindings]
     *
     * @param source query source to be evaluated
     * @param bindings [Bindings] used for evaluation
     */
    protected fun evalWithBindings(source: String, bindings: Bindings = Bindings.empty()): ExprValue {
        return evaluator.compile(source)
                        .eval(bindings)
    }

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
}
