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

import org.partiql.lang.CompilerPipeline
import org.partiql.lang.TestBase
import org.partiql.lang.ast.AstDeserializerBuilder
import org.partiql.lang.ast.AstSerializer
import org.partiql.lang.ast.AstVersion
import org.partiql.lang.ast.ExprNode
import com.amazon.ion.IonType
import com.amazon.ion.IonValue
import org.partiql.lang.CUSTOM_TEST_TYPES_MAP
import org.partiql.lang.SqlException
import org.partiql.lang.ast.passes.MetaStrippingRewriter
import org.partiql.lang.ast.toAstStatement
import org.partiql.lang.ast.toExprNode
import org.partiql.lang.checkErrorAndErrorContext
import org.partiql.lang.errors.ErrorBehaviorInPermissiveMode
import org.partiql.lang.errors.ErrorCategory
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.syntax.SqlParser
import org.partiql.lang.util.ConfigurableExprValueFormatter
import org.partiql.lang.util.asSequence
import org.partiql.lang.util.newFromIonText
import org.partiql.lang.util.softAssert
import kotlin.reflect.KClass
import kotlin.test.assertEquals

/**
 * This class is being deprecated because it is becoming unmaintainable but can't be removed yet.
 *
 * New tests should use JUnit5's parameterized testing as much as possible.
 *
 * When code re-use among test classes is needed, please prefer making your new functions top-level and accessible
 * from any test class.
 *
 * As we parameterize PartiQL's other tests, we should migrate them away from using this base class as well.
 */
@Deprecated("This class and everything in it should be considered deprecated.")
abstract class EvaluatorTestBase : TestBase() {

    /**
     * creates a [ExprValue] from the IonValue represented by this String. Assumes the string represents a single
     * IonValue
     */
    private fun String.toExprValue(): ExprValue = valueFactory.newFromIonText(this)

    protected fun Map<String, String>.toBindings(): Bindings<ExprValue> =
        Bindings.ofMap(mapValues { it.value.toExprValue() })

    protected fun Map<String, String>.toSession() = EvaluationSession.build { globals(this@toSession.toBindings()) }

    fun voidEval(source: String,
                 compileOptions: CompileOptions = CompileOptions.standard(),
                 session: EvaluationSession = EvaluationSession.standard(),
                 compilerPipelineBuilderBlock: CompilerPipeline.Builder.() -> Unit = {  }) {
        // force materialization
        eval(source, compileOptions, session, compilerPipelineBuilderBlock).ionValue
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
     * @param compilerPipelineBuilderBlock any additional configuration to the pipeline after the options are set.
     * @param block function literal with receiver used to plug in custom assertions
     */
    protected fun assertEval(source: String,
                             expected: String,
                             session: EvaluationSession = EvaluationSession.standard(),
                             compileOptions: CompileOptions = CompileOptions.standard(),
                             compilerPipelineBuilderBlock: CompilerPipeline.Builder.() -> Unit = {  },
                             block: AssertExprValue.() -> Unit = { }) {

        val expectedIon = ion.singleValue(expected)
        val parser = SqlParser(ion, CUSTOM_TEST_TYPES_MAP)
        val originalExprNode = parser.parseExprNode(source)

        fun evalAndAssert(exprNodeToEvaluate: ExprNode, message: String) {
            AssertExprValue(eval(exprNodeToEvaluate, compileOptions, session, compilerPipelineBuilderBlock),
                message =  "${compileOptions.typedOpBehavior} CAST in ${compileOptions.typingMode} typing mode, " +
                    "evaluated '$source' with evaluator ($message)").apply { assertIonValue(expectedIon) }.run(block)
        }

        // Evaluate the ExprNodes originally obtained from the parser
        evalAndAssert(originalExprNode, "AST originated from parser")

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
            // LEGACY mode
            val result = eval(exprNodeToEvaluate, compileOptions, session)
            assertEquals(ExprValueType.MISSING, result.type, "(LEGACY mode) $message")
            // PERMISSIVE mode
            val resultForPermissiveMode = eval(exprNodeToEvaluate, CompileOptions.builder(compileOptions).typingMode(TypingMode.PERMISSIVE).build(), session)
            assertEquals(ExprValueType.MISSING, resultForPermissiveMode.type, "(PERMISSIVE mode) $message")
        }

        // Also send the serializer through V0 sexp AST and evaluate them again
        // to be sure they still work after being deserialized
        fun serializeRoundTripEvalAndAssertIsMissing(astVersion: AstVersion) {
            val sexp = AstSerializer.serialize(originalExprNode, astVersion, ion)
            val sexpAST = deserializer.deserialize(sexp, astVersion)
            assertEquals(originalExprNode, sexpAST, "ExprNode deserialized from s-exp $astVersion AST must match the ExprNode returned by the parser")

            // This step should only fail if there is a bug in the equality check that causes two
            // dissimilar ASTs to be considered equal.

            // LEGACY mode
            val result = eval(sexpAST, compileOptions, session)
            assertEquals(ExprValueType.MISSING, result.type, "(LEGACY mode) Evaluating AST created from deseriailzed $astVersion s-exp AST must result in missing")
            // PERMISSIVE mode
            val resultForPermissiveMode = eval(sexpAST, CompileOptions.builder(compileOptions).typingMode(TypingMode.PERMISSIVE).build(), session)
            assertEquals(ExprValueType.MISSING, resultForPermissiveMode.type, "(PERMISSIVE mode) Evaluating AST created from deseriailzed $astVersion s-exp AST must result in missing")
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


    protected fun assertExprEquals(expected: ExprValue, actual: ExprValue, message: String) {
        // exprEquals consider NULL and MISSING to be equivalent so we also check types here
        val isActuallyEquivalent = expected.type == actual.type && expected.exprEquals(actual)

        if(!isActuallyEquivalent) {
            println("Expected ionValue: ${ConfigurableExprValueFormatter.pretty.format(expected)} ")
            println("Actual ionValue  : ${ConfigurableExprValueFormatter.pretty.format(actual)} ")
            fail("$message Expected and actual ExprValue instances are not equivalent")
        }
    }

    /**
     * Evaluates [expectedLegacyModeResult] and [sqlUnderTest] and asserts that the resulting [ExprValue]s
     * are equivalent using PartiQL's equivalence rules. This differs from `assertEval`
     * in that the [ExprValue]s are not converted to Ion before comparison.
     * This function should be used for any result involving `BAG` and `MISSING`
     * since Ion has no representation for these values.
     *
     * @param sqlUnderTest query source to be tested
     * @param expectedLegacyModeResult expected result for legacy mode.
     * @param session [EvaluationSession] used for evaluation
     * @param expectedPermissiveModeResult expected result for permissive mode.  Defaults to [expectedLegacyModeResult].
     */
    protected fun assertEvalExprValue(
        sqlUnderTest: String,
        expectedLegacyModeResult: String,
        session: EvaluationSession = EvaluationSession.standard(),
        compileOptions: CompileOptions = CompileOptions.standard(),
        expectedPermissiveModeResult: String = expectedLegacyModeResult
    ) {
        // LegacyMode
        val originalExprValue = eval(sqlUnderTest, compileOptions, session)
        val expectedExprValue = eval(expectedLegacyModeResult, compileOptions, session)
        assertExprEquals(expectedExprValue, originalExprValue, "(LEGACY mode)")

        // PERMISSIVE mode
        val permissiveMode = CompileOptions.builder(compileOptions).typingMode(TypingMode.PERMISSIVE).build()

        val originalExprValueForPermissiveMode = eval(sqlUnderTest, permissiveMode, session)
        val expectedExprValueForPermissiveMode = eval(expectedPermissiveModeResult, permissiveMode, session)
        assertExprEquals(expectedExprValueForPermissiveMode, originalExprValueForPermissiveMode, "(PERMISSIVE mode)")
    }

    /**
     * Evaluates a source query given a [EvaluationSession]
     *
     * @param source query source to be evaluated
     * @param session [EvaluationSession] used for evaluation
     * @param compilerPipelineBuilderBlock any additional configuration to the pipeline after the options are set.
     */
    protected fun eval(source: String,
                       compileOptions: CompileOptions = CompileOptions.standard(),
                       session: EvaluationSession = EvaluationSession.standard(),
                       compilerPipelineBuilderBlock: CompilerPipeline.Builder.() -> Unit = { }): ExprValue {

        val p = SqlParser(ion, CUSTOM_TEST_TYPES_MAP)

        val ast = p.parseExprNode(source)
        return eval(ast, compileOptions, session, compilerPipelineBuilderBlock)
    }

    /**
     * Evaluates a source query given a [EvaluationSession] with default [CompileOptions] for [TypingMode.PERMISSIVE]
     *
     * The provided (or default) [compileOptions] are modified to have the [TypingMode] as [TypingMode.PERMISSIVE]
     * @param source query source to be evaluated
     * @param session [EvaluationSession] used for evaluation
     * @param compilerPipelineBuilderBlock any additional configuration to the pipeline after the options are set.
     */
    protected fun evalForPermissiveMode(source: String,
                                        compileOptions: CompileOptions = CompileOptions.standard(),
                                        session: EvaluationSession = EvaluationSession.standard(),
                                        compilerPipelineBuilderBlock: CompilerPipeline.Builder.() -> Unit = { }): ExprValue {

        val p = SqlParser(ion)

        val ast = p.parseExprNode(source)
        return eval(
            ast,
            CompileOptions.builder(compileOptions).typingMode(TypingMode.PERMISSIVE).build(),
            session,
            compilerPipelineBuilderBlock
        )
    }
    /**
     * Evaluates an [ExprNode] given a [EvaluationSession]
     *
     * @param exprNode The [ExprNode] instance to be evaluated.
     * @param session [EvaluationSession] used for evaluation
     * @param compilerPipelineBuilderBlock any additional configuration to the pipeline after the options are set.
     */
    protected fun eval(exprNode: ExprNode,
                       compileOptions: CompileOptions = CompileOptions.standard(),
                       session: EvaluationSession = EvaluationSession.standard(),
                       compilerPipelineBuilderBlock: CompilerPipeline.Builder.() -> Unit = {  } ): ExprValue {

        // "Sneak" in this little assertion to test that every ExprNode AST that passes through
        // this function can be round-tripped to `partiql_ast` and back.
        assertExprNodeToPIGRoundTrip(exprNode)


        val pipeline = CompilerPipeline.builder(ion).apply {
            compileOptions(compileOptions)
            compilerPipelineBuilderBlock()
        }

        return pipeline.build().compile(exprNode).eval(session)
    }

    private fun assertEvalThrows(query: String,
                                 message: String,
                                 metadata: NodeMetadata? = null,
                                 internal: Boolean = false,
                                 cause: KClass<out Throwable>? = null,
                                 session: EvaluationSession = EvaluationSession.standard(),
                                 typingMode: TypingMode = TypingMode.LEGACY): EvaluationException {

        val compileOptions = when (typingMode) {
            TypingMode.LEGACY -> CompileOptions.standard()
            TypingMode.PERMISSIVE -> CompileOptions.build { typingMode(TypingMode.PERMISSIVE) }
        }

        try {
            voidEval(query, session = session, compileOptions = compileOptions)
            fail("didn't throw")
        }
        catch (e: EvaluationException) {
            softAssert {
                if (typingMode == TypingMode.LEGACY) {
                    assertThat(e.message).`as`("error message").isEqualTo(message)
                    assertThat(e.internal).isEqualTo(internal)
                }

                if (cause != null) assertThat(e).hasRootCauseExactlyInstanceOf(cause.java)

                if(metadata != null) {
                    assertThat(e.errorContext!![Property.LINE_NUMBER]!!.longValue()).`as`("line number").isEqualTo(metadata.line)
                    assertThat(e.errorContext!![Property.COLUMN_NUMBER]!!.longValue()).`as`("column number").isEqualTo(metadata.column)
                }
                else {
                    assertThat(e.errorContext).isNull()
                }
            }
            return e
        }
        throw Exception("This should be unreachable.")
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
            fail("didn't throw")
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

    /**
     *  Asserts that [query] throws an [EvaluationException] or [SqlException] with the specified message, line and column number.
     *  Asserts that the [query] throws or returns missing in the [TypingMode.PERMISSIVE] mode depending on the [ErrorCode.errorBehaviorInPermissiveMode]
     *  It also verifies the behavior of error in [TypingMode.PERMISSIVE] mode.
     *  This should be used to ensure that the query is tested for both [TypingMode.LEGACY] and [TypingMode.PERMISSIVE]
     */
    protected fun assertThrows(query: String,
                               message: String,
                               metadata: NodeMetadata? = null,
                               expectedPermissiveModeResult: String? = null,
                               internal: Boolean = false,
                               cause: KClass<out Throwable>? = null,
                               session: EvaluationSession = EvaluationSession.standard()) {

        val exception = assertEvalThrows(query, message, metadata, internal, cause, session = session, typingMode = TypingMode.LEGACY)

        when (exception.errorCode.errorBehaviorInPermissiveMode) {
            ErrorBehaviorInPermissiveMode.THROW_EXCEPTION -> {
                assertNull("An expectedPermissiveModeResult must not be specified when ErrorCode.errorBehaviorInPermissiveMode is set to ErrorBehaviorInPermissiveMode.THROW_EXCEPTION", expectedPermissiveModeResult)
                val e = assertEvalThrows(query, message, metadata, internal, cause, session = session, typingMode = TypingMode.PERMISSIVE)
                assertEquals(exception.errorCode, e.errorCode)
            }
            // Return MISSING
            ErrorBehaviorInPermissiveMode.RETURN_MISSING -> {
                assertNotNull("Required non null expectedPermissiveModeResult when ErrorCode.errorBehaviorInPermissiveMode is set to ErrorBehaviorInPermissiveMode.RETURN_MISSING", expectedPermissiveModeResult)
                val originalExprValueForPermissiveMode = evalForPermissiveMode(query, session = session)
                val expectedExprValueForPermissiveMode = evalForPermissiveMode(expectedPermissiveModeResult!!, session = session)
                assertExprEquals(expectedExprValueForPermissiveMode, originalExprValueForPermissiveMode, "(PERMISSIVE mode)")
            }
        }
    }

    /**
     *  Asserts that [func] throws an [SqlException], line and column number in [TypingMode.PERMISSIVE] mode
     */
    protected fun assertThrowsInPermissiveMode(errorCode: ErrorCode,
                                               metadata: NodeMetadata? = null,
                                               cause: KClass<out Throwable>? = null,
                                               func: () -> Unit) {
        try {
            func()
            fail("didn't throw")
        }
        catch (e: SqlException) {
            softAssert {
                if(metadata != null) {
                    assertThat(e.errorContext!![Property.LINE_NUMBER]!!.longValue()).`as`("line number").isEqualTo(metadata.line)
                    assertThat(e.errorContext!![Property.COLUMN_NUMBER]!!.longValue()).`as`("column number").isEqualTo(metadata.column)

                    if (cause != null) assertThat(e).hasRootCauseExactlyInstanceOf(cause.java)
                }
                assertEquals(errorCode, e.errorCode, "Error codes should be same")

            }
        }
    }

    protected fun checkInputThrowingEvaluationException(input: String,
                                                        errorCode: ErrorCode? = null,
                                                        expectErrorContextValues: Map<Property, Any>,
                                                        cause: KClass<out Throwable>? = null,
                                                        expectedPermissiveModeResult: String? = null) {
        checkInputThrowingEvaluationException(
            input,
            EvaluationSession.standard(),
            errorCode,
            expectErrorContextValues,
            cause,
            expectedPermissiveModeResult)
    }

    protected fun checkInputThrowingEvaluationException(input: String,
                                                        session: EvaluationSession,
                                                        errorCode: ErrorCode? = null,
                                                        expectErrorContextValues: Map<Property, Any>,
                                                        cause: KClass<out Throwable>? = null,
                                                        expectedPermissiveModeResult: String? = null) {
        softAssert {
            try {
                val result = eval(input, session = session).ionValue;
                fail("Expected SqlException but there was no Exception.  " +
                     "The unexpected result was: \n${result.toPrettyString()}")
            }
            catch (e: SqlException) {
                if (cause != null) assertThat(e).hasRootCauseExactlyInstanceOf(cause.java)
                checkErrorAndErrorContext(errorCode, e, expectErrorContextValues)
                //Error thrown in LEGACY MODE needs to be checked in PERMISSIVE MODE
                when (e.errorCode.errorBehaviorInPermissiveMode) {
                    ErrorBehaviorInPermissiveMode.THROW_EXCEPTION -> {
                        assertNull("An expectedPermissiveModeResult must not be specified when ErrorCode.errorBehaviorInPermissiveMode is set to ErrorBehaviorInPermissiveMode.THROW_EXCEPTION", expectedPermissiveModeResult)
                        assertThrowsInPermissiveMode(e.errorCode) {
                            voidEval(input, session = session, compileOptions = CompileOptions.build { typingMode(TypingMode.PERMISSIVE) })
                        }
                    }
                    // Return MISSING
                    ErrorBehaviorInPermissiveMode.RETURN_MISSING -> {
                        assertNotNull("Required non null expectedPermissiveModeResult when ErrorCode.errorBehaviorInPermissiveMode is set to ErrorBehaviorInPermissiveMode.RETURN_MISSING", expectedPermissiveModeResult)
                        val originalExprValueForPermissiveMode = evalForPermissiveMode(input, session = session)
                        val expectedExprValueForPermissiveMode = evalForPermissiveMode(expectedPermissiveModeResult!!, session = session)
                        assertExprEquals(expectedExprValueForPermissiveMode, originalExprValueForPermissiveMode, "(PERMISSIVE mode)")
                    }
                }
            }
            catch (e: Exception) {
                fail("Expected SqlException but a different exception was thrown:\n\t  $e")
            }
        }
    }

    protected fun checkInputThrowingEvaluationException(tc: EvaluatorErrorTestCase, session: EvaluationSession) {
        softAssert {
            try {
                val result = eval(tc.sqlUnderTest, compileOptions = tc.compOptions.options, session = session).ionValue;
                fail("Expected EvaluationException but there was no Exception.  " +
                     "The unepxected result was: \n${result.toPrettyString()}")
            }
            catch (e: EvaluationException) {
                if (tc.cause != null) assertThat(e).hasRootCauseExactlyInstanceOf(tc.cause.java)
                checkErrorAndErrorContext(tc.errorCode, e, tc.expectErrorContextValues)
                //Error thrown in LEGACY MODE needs to be checked in PERMISSIVE MODE
                when (e.errorCode.errorBehaviorInPermissiveMode) {
                    ErrorBehaviorInPermissiveMode.THROW_EXCEPTION -> {
                        assertNull("An EvaluatorErrorTestCase.expectedPermissiveModeResult must not be specified when ErrorCode.errorBehaviorInPermissiveMode is set to ErrorBehaviorInPermissiveMode.THROW_EXCEPTION", tc.expectedPermissiveModeResult)
                        assertThrowsInPermissiveMode(e.errorCode) {
                            voidEval(tc.sqlUnderTest, session = session, compileOptions = CompileOptions.build { typingMode(TypingMode.PERMISSIVE) })
                        }
                    }
                    // Return MISSING
                    ErrorBehaviorInPermissiveMode.RETURN_MISSING -> {
                        if (tc.errorCode?.errorCategory() != ErrorCategory.SEMANTIC.toString()) {
                            assertNotNull("Required non null expectedPermissiveModeResult when ErrorCode.errorBehaviorInPermissiveMode is set to ErrorBehaviorInPermissiveMode.RETURN_MISSING", tc.expectedPermissiveModeResult)
                            val originalExprValueForPermissiveMode = evalForPermissiveMode(tc.sqlUnderTest, session = session)
                            val expectedExprValueForPermissiveMode = evalForPermissiveMode(tc.expectedPermissiveModeResult!!, session = session)
                            assertExprEquals(expectedExprValueForPermissiveMode, originalExprValueForPermissiveMode, "(PERMISSIVE mode)")
                        }
                    }
                }
            }
            catch (e: Exception) {
                fail("Expected EvaluationException but a different exception was thrown:\n\t  $e")
            }
        }
    }

    /**
     * Runs each test case twice--once without altering the [CompileOptions] (intended for [TypingMode.LEGACY], which
     * is the current default), and once while forcing [TypingMode.PERMISSIVE].  Used for cases where we expect the
     * result to be the same in both modes.
     */
    protected fun runTestCaseInLegacyAndPermissiveModes(tc: EvaluatorTestCase, session: EvaluationSession) {
        // LEGACY mode
        runTestCase(tc, session, "compile options unaltered")

        // PERMISSIVE mode
        runTestCase(tc, session, "compile options forced to PERMISSIVE mode") { compileOptions ->
            CompileOptions.build(compileOptions) { typingMode(TypingMode.PERMISSIVE) }
        }
    }

    /**
     * Runs the give test case once with the specified [session].
     *
     * If specified, [compileOptionsMutator] will be invoked allow the compilation options to be mutated.
     * This feature can be used multiple times consecutively to allow repetition of the same test case
     * under different [CompileOptions].
     *
     * If non-null, [message] will be dumped to the console before test failure to aid in the identification
     * and debugging of failed tests.
     */
    protected fun runTestCase(
        tc: EvaluatorTestCase,
        session: EvaluationSession,
        message: String? = null,
        compilerPipelineBuilderBlock: CompilerPipeline.Builder.() -> Unit = { },
        compileOptionsMutator: ((CompileOptions) -> CompileOptions) = { it }
    ) {
        val msg = message?.let { "($it)" } ?: ""

        val co = compileOptionsMutator(tc.compOptions.options)

        fun showTestCase() {
            println(listOfNotNull(message, tc.groupName).joinToString(" : "))
            println("Query under test  : ${tc.sqlUnderTest}")
            println("Expected value    : ${tc.expectedSql}")
            println()
        }

        val expected = try {
            eval(
                source = tc.expectedSql,
                compilerPipelineBuilderBlock = compilerPipelineBuilderBlock,
                compileOptions = co)
        } catch (e: Throwable) {
            showTestCase()
            e.printStackTrace()
            fail("Exception while attempting to evaluate the expected value, see standard output $msg")
            throw e
        }

        val actual = try {
            eval(
                source = tc.sqlUnderTest,
                compilerPipelineBuilderBlock = compilerPipelineBuilderBlock,
                session = session,
                compileOptions = co)
        } catch (e: Throwable) {
            showTestCase()
            e.printStackTrace()
            fail("Exception while attempting to evaluate the under test, see standard output $msg")
            throw e
        }

        if (!expected.exprEquals(actual)) {
            showTestCase()
            println("Expected : $expected")
            println("Actual   : $actual")

            fail("Expected and actual ExprValue instances are not equivalent $msg")
        }
    }
}


internal fun IonValue.removeAnnotations() {
    when(this.type) {
        // Remove $partiql_missing annotation from NULL for assertions
        IonType.NULL -> this.removeTypeAnnotation(MISSING_ANNOTATION)
        IonType.DATAGRAM,
        IonType.SEXP,
        IonType.STRUCT,
        IonType.LIST -> {
            // Remove $partiql_bag annotation from LIST for assertions
            if (this.type == IonType.LIST) {
                this.removeTypeAnnotation(BAG_ANNOTATION)
            }
            // Recursively remove annotations
            this.asSequence().forEach {
                it.removeAnnotations()
            }
        }
        else -> { /* ok to do nothing. */ }
    }
}

internal fun IonValue.cloneAndRemoveAnnotations() = this.clone().apply {
    removeAnnotations()
    makeReadOnly()
}