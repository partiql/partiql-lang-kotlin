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

package org.partiql.lang.eval

import org.partiql.lang.*
import org.partiql.lang.ast.*
import org.partiql.lang.errors.*
import org.partiql.lang.syntax.*
import org.partiql.lang.util.*
import org.junit.*
import org.partiql.lang.ast.passes.MetaStrippingRewriter
import kotlin.reflect.*
import kotlin.test.*

abstract class EvaluatorTestBase : TestBase() {

    /**
     * creates a [ExprValue] from the IonValue represented by this String. Assumes the string represents a single
     * IonValue
     */
    private fun String.toExprValue(): ExprValue = valueFactory.newFromIonText(this)

    protected fun Map<String, String>.toBindings(): Bindings<ExprValue> =
        Bindings.ofMap(mapValues { it.value.toExprValue() })

    protected fun Map<String, String>.toSession() = EvaluationSession.build { globals(this@toSession.toBindings()) }

    fun voidEval(source: String, compileOptions: CompileOptions = CompileOptions.standard(), session: EvaluationSession = EvaluationSession.standard()) {
        // force materialization
        eval(source, compileOptions, session).ionValue
    }

    /**
     * Assert that the result of the evaluation of source is the same as expected given optionally given an
     * [EvaluationSession] and [CompileOptions].
     *
     * This function also passes the [ExprNode] AST returned from the parser through (de)serialization steps through
     * s-exp AST version V0 and ensures that the deserialized forms of that are equivalent to each other.
     *
     * @param source query source to be tested
     * @param expected expected result
     * @param session [EvaluationSession] used for evaluation
     * @param block function literal with receiver used to plug in custom assertions
     */
    protected fun assertEval(source: String,
                             expected: String,
                             session: EvaluationSession = EvaluationSession.standard(),
                             compileOptions: CompileOptions = CompileOptions.standard(),
                             block: AssertExprValue.() -> Unit = { }) {

        val expectedIon = ion.singleValue(expected)
        val parser = SqlParser(ion)
        val originalExprNode = parser.parseExprNode(source)
        val deserializer = AstDeserializerBuilder(ion).build()

        fun evalAndAssert(exprNodeToEvaluate: ExprNode, message: String) {
            val result = eval(exprNodeToEvaluate, compileOptions, session)
            AssertExprValue(result, message = "Evaluated '$source' with evaluator ($message)")
                .apply { assertIonValue(expectedIon) }.run(block)
        }

        fun serializeRoundTripEvalAndAssert(astVersion: AstVersion) {
            val sexpAST = AstSerializer.serialize(originalExprNode, astVersion, ion)
            val deserializedExprNode = deserializer.deserialize(sexpAST, astVersion)
            assertEquals(MetaStrippingRewriter.stripMetas(originalExprNode), MetaStrippingRewriter.stripMetas(deserializedExprNode),
                    "ExprNode deserialized from s-exp $astVersion AST must match the ExprNode returned by the parser")

            // This step should only fail if there is a bug in the equality check that causes two
            // dissimilar ASTs to be considered equal.
            evalAndAssert(deserializedExprNode, "AST was round-tripped through $astVersion (de)serialization")
        }

        // Evaluate the ExprNodes originally obtained from the parser
        evalAndAssert(originalExprNode, "AST originated from parser")

        // round-trip and evaluate each of the [AstVersion]s.
        AstVersion.values().forEach { serializeRoundTripEvalAndAssert(it) }

        assertBaseRewrite(source, originalExprNode)
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
                                      session: EvaluationSession = EvaluationSession.standard(),
                                      compileOptions: CompileOptions = CompileOptions.standard()) {

        val parser = SqlParser(ion)
        val deserializer = AstDeserializerBuilder(ion).build()

        val originalExprNode = parser.parseExprNode(source)

        // Evaluate the ExprNodes originally obtained from the parser

        fun evalAndAssertIsMissing(exprNodeToEvaluate: ExprNode, message: String) {
            val result = eval(exprNodeToEvaluate, compileOptions, session)
            assertEquals(ExprValueType.MISSING, result.type, message)
        }

        // Also send the serializer through V0 sexp AST and evaluate them again
        // to be sure they still work after being deserialized
        fun serializeRoundTripEvalAndAssertIsMissing(astVersion: AstVersion) {
            val sexp = AstSerializer.serialize(originalExprNode, astVersion, ion)
            val sexpAST = deserializer.deserialize(sexp, astVersion)
            assertEquals(originalExprNode, sexpAST, "ExprNode deserialized from s-exp $astVersion AST must match the ExprNode returned by the parser")

            // This step should only fail if there is a bug in the equality check that causes two
            // dissimilar ASTs to be considered equal.
            val result = eval(sexpAST, compileOptions, session)
            assertEquals(ExprValueType.MISSING, result.type, "Evaluating AST created from deseriailzed $astVersion s-exp AST must result in missing")
        }

        evalAndAssertIsMissing(originalExprNode, "AST originated from parser")
        AstVersion.values().forEach { serializeRoundTripEvalAndAssertIsMissing(it) }

        assertBaseRewrite(source, originalExprNode)
    }

    protected fun assertExprNodeToPIGRoundTrip(exprNode: ExprNode) {
        val roundTrippedExprNode = MetaStrippingRewriter.stripMetas(exprNode).toAstStatement().toExprNode(ion)

        assertEquals(
            MetaStrippingRewriter.stripMetas(exprNode),
            roundTrippedExprNode,
            "ExprNode resulting from round trip to partiql_ast and back should be equivalent.")
    }


    protected fun assertExprEquals(expected: ExprValue, actual: ExprValue) {
        if(!expected.exprEquals(actual)) {
            println("Expected ionValue: ${ConfigurableExprValueFormatter.pretty.format(expected)} ")
            println("Actual ionValue  : ${ConfigurableExprValueFormatter.pretty.format(actual)} ")
            fail("Expected and actual ExprValue instances are not equivalent")
        }
    }

    /**
     * Evaluates [expected] and [source] and asserts that the resulting [ExprValue]s
     * are equivalent using PartiQL's equivalence rules. This differs from `assertEval`
     * in that the [ExprValue]s are not converted to Ion before comparison.
     * This function should be used for any result involving `BAG` and `MISSING`
     * since Ion has no representation for these values.
     *
     * @param source query source to be tested
     * @param expected expected result
     * @param session [EvaluationSession] used for evaluation
     * @param block function literal with receiver used to plug in custom assertions
     */
    protected fun assertEvalExprValue(source: String,
                             expected: String,
                             session: EvaluationSession = EvaluationSession.standard(),
                             compileOptions: CompileOptions = CompileOptions.standard()) {
        val parser = SqlParser(ion)
        val originalExprNode = parser.parseExprNode(source)
        val expectedExprNode = parser.parseExprNode(expected)

        val originalExprValue = eval(originalExprNode, compileOptions, session)
        val expectedExprValue = eval(expectedExprNode, compileOptions, session)
        assertExprEquals(expectedExprValue, originalExprValue)
    }

    /**
     * Evaluates a source query given a [EvaluationSession]
     *
     * @param source query source to be evaluated
     * @param session [EvaluationSession] used for evaluation
     */
    protected fun eval(source: String,
                       compileOptions: CompileOptions = CompileOptions.standard(),
                       session: EvaluationSession = EvaluationSession.standard()): ExprValue {

        val p = SqlParser(ion)

        val ast = p.parseExprNode(source)
        return eval(ast, compileOptions, session)
    }

    /**
     * Evaluates an [ExprNode] given a [EvaluationSession]
     *
     * @param exprNode The [ExprNode] instance to be evaluated.
     * @param session [EvaluationSession] used for evaluation
     */
    protected fun eval(exprNode: ExprNode,
                       compileOptions: CompileOptions = CompileOptions.standard(),
                       session: EvaluationSession = EvaluationSession.standard()): ExprValue {

        // "Sneak" in this little assertion to test that every ExprNode AST that passes through
        // this function can be round-tripped to `partiql_ast` and back.
        assertExprNodeToPIGRoundTrip(exprNode)

        val pipeline = CompilerPipeline.build(ion) { compileOptions(compileOptions) }
        return pipeline.compile(exprNode).eval(session)
    }

    /**
     *  Asserts that [func] throws an [SqlException] with the specified message, line and column number
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

                if(metadata != null) {
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
                                                        errorCode: ErrorCode? = null,
                                                        expectErrorContextValues: Map<Property, Any>,
                                                        cause: KClass<out Throwable>? = null) {
        checkInputThrowingEvaluationException(
            input,
            EvaluationSession.standard(),
            errorCode,
            expectErrorContextValues,
            cause)
    }

    protected fun checkInputThrowingEvaluationException(input: String,
                                                        session: EvaluationSession,
                                                        errorCode: ErrorCode? = null,
                                                        expectErrorContextValues: Map<Property, Any>,
                                                        cause: KClass<out Throwable>? = null) {
        softAssert {
            try {
                val result = eval(input, session = session).ionValue;
                fail("Expected EvaluationException but there was no Exception.  " +
                     "The unepxected result was: \n${result.toPrettyString()}")
            }
            catch (e: EvaluationException) {
                if (cause != null) assertThat(e).hasRootCauseExactlyInstanceOf(cause.java)
                checkErrorAndErrorContext(errorCode, e, expectErrorContextValues)
            }
            catch (e: Exception) {
                fail("Expected EvaluationException but a different exception was thrown:\n\t  $e")
            }
        }
    }

    protected fun checkInputThrowingEvaluationException(tc: EvaluatorErrorTestCase, session: EvaluationSession) {
        softAssert {
            try {
                val result = eval(tc.sqlUnderTest, session = session).ionValue;
                fail("Expected EvaluationException but there was no Exception.  " +
                     "The unepxected result was: \n${result.toPrettyString()}")
            }
            catch (e: EvaluationException) {
                if (tc.cause != null) assertThat(e).hasRootCauseExactlyInstanceOf(tc.cause.java)
                checkErrorAndErrorContext(tc.errorCode, e, tc.expectErrorContextValues)
            }
            catch (e: Exception) {
                fail("Expected EvaluationException but a different exception was thrown:\n\t  $e")
            }
        }
    }

    protected fun runTestCase(tc: EvaluatorTestCase, session: EvaluationSession) {
        fun showTestCase() {
            println("Query under test  : ${tc.sqlUnderTest}")
            println("Expected value    : ${tc.expectedSql}")
            println()
        }

        val expected = try {
            eval(tc.expectedSql, compileOptions = tc.compOptions.options)
        }
        catch(e: Throwable) {
            showTestCase()
            println(e)
            fail("Exception while attempting to evaluate the expected value, see standard output")
            throw e
        }

        val actual = try {
            eval(tc.sqlUnderTest, session = session, compileOptions = tc.compOptions.options)
        }
        catch(e: Throwable) {
            showTestCase()
            println(e)
            fail("Exception while attempting to evaluate the under test, see standard output")
            throw e
        }

        if(!expected.exprEquals(actual)) {
            showTestCase()
            println("Expected ionValue : ${expected.ionValue}")
            println("Actual ionValue   : ${actual.ionValue}")

            fail("Expected and actual ExprValue instances are not equivalent")
        }
    }
}
