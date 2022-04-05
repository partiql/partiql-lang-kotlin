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

// We don't need warnings about deprecated ExprNode.
@file:Suppress("DEPRECATION")

package org.partiql.lang.eval

import com.amazon.ion.IonType
import com.amazon.ion.IonValue
import org.partiql.lang.CUSTOM_TEST_TYPES
import org.partiql.lang.CompilerPipeline
import org.partiql.lang.ION
import org.partiql.lang.SqlException
import org.partiql.lang.TestBase
import org.partiql.lang.ast.AstDeserializerBuilder
import org.partiql.lang.ast.AstSerializer
import org.partiql.lang.ast.AstVersion
import org.partiql.lang.ast.ExprNode
import org.partiql.lang.ast.toAstStatement
import org.partiql.lang.ast.toExprNode
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.errors.ErrorBehaviorInPermissiveMode
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.PropertyValueMap
import org.partiql.lang.syntax.SqlParser
import org.partiql.lang.util.ConfigurableExprValueFormatter
import org.partiql.lang.util.asSequence
import org.partiql.lang.util.newFromIonText
import org.partiql.lang.util.softAssert
import org.partiql.lang.util.stripMetas
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
abstract class EvaluatorTestBase : TestBase() {

    /**
     * creates a [ExprValue] from the IonValue represented by this String. Assumes the string represents a single
     * IonValue
     */
    private fun String.toExprValue(): ExprValue = valueFactory.newFromIonText(this)

    private fun Map<String, String>.toBindings(): Bindings<ExprValue> =
        Bindings.ofMap(mapValues { it.value.toExprValue() })

    protected fun Map<String, String>.toSession() = EvaluationSession.build { globals(this@toSession.toBindings()) }

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
    protected fun assertEval(
        source: String,
        expected: String,
        session: EvaluationSession = EvaluationSession.standard(),
        compileOptions: CompileOptions = CompileOptions.standard(),
        compilerPipelineBuilderBlock: CompilerPipeline.Builder.() -> Unit = { },
        block: AssertExprValue.() -> Unit = { }
    ) {

        val expectedIon = ion.singleValue(expected)
        val parser = SqlParser(ion, CUSTOM_TEST_TYPES)
        val originalAst = parser.parseAstStatement(source)

        fun evalAndAssert(ast: PartiqlAst.Statement, message: String) {
            AssertExprValue(
                eval(ast, compileOptions, session, compilerPipelineBuilderBlock),
                message = "${compileOptions.typedOpBehavior} CAST in ${compileOptions.typingMode} typing mode, " +
                    "evaluated '$source' with evaluator ($message)"
            ).apply { assertIonValue(expectedIon) }.run(block)
        }

        // Evaluate the ast originally obtained from the parser
        evalAndAssert(originalAst, "AST originated from parser")

        assertAstRewriterBase(source, originalAst.toExprNode(ion))
    }

    /**
     * Assert that the result of the evaluation of source is a `MISSING` value, optionally given an
     * [EvaluationSession] and [CompileOptions].
     *
     * @param source query source to be tested
     * @param session [EvaluationSession] used for evaluation
     */
    protected fun assertEvalIsMissing(
        source: String,
        session: EvaluationSession = EvaluationSession.standard(),
        compileOptions: CompileOptions = CompileOptions.standard()
    ) {

        val parser = SqlParser(ion)
        val deserializer = AstDeserializerBuilder(ion).build()

        val originalAst = parser.parseAstStatement(source)

        // Evaluate the ast originally obtained from the parser

        fun evalAndAssertIsMissing(ast: PartiqlAst.Statement, message: String) {
            // LEGACY mode
            val result = eval(ast, compileOptions, session)
            assertEquals(ExprValueType.MISSING, result.type, "(LEGACY mode) $message")
            // PERMISSIVE mode
            val resultForPermissiveMode = eval(ast, CompileOptions.builder(compileOptions).typingMode(TypingMode.PERMISSIVE).build(), session)
            assertEquals(ExprValueType.MISSING, resultForPermissiveMode.type, "(PERMISSIVE mode) $message")
        }

        // Also send the serializer through V0 sexp AST and evaluate them again
        // to be sure they still work after being deserialized
        fun serializeRoundTripEvalAndAssertIsMissing(astVersion: AstVersion) {
            val sexp = AstSerializer.serialize(originalAst.toExprNode(ion), astVersion, ion)
            val sexpAST = deserializer.deserialize(sexp, astVersion)
            assertEquals(originalAst.toExprNode(ion), sexpAST, "ExprNode deserialized from s-exp $astVersion AST must match the ExprNode returned by the parser")

            // This step should only fail if there is a bug in the equality check that causes two
            // dissimilar ASTs to be considered equal.

            // LEGACY mode
            val result = eval(sexpAST.toAstStatement(), compileOptions, session)
            assertEquals(ExprValueType.MISSING, result.type, "(LEGACY mode) Evaluating AST created from deseriailzed $astVersion s-exp AST must result in missing")
            // PERMISSIVE mode
            val resultForPermissiveMode = eval(sexpAST.toAstStatement(), CompileOptions.builder(compileOptions).typingMode(TypingMode.PERMISSIVE).build(), session)
            assertEquals(ExprValueType.MISSING, resultForPermissiveMode.type, "(PERMISSIVE mode) Evaluating AST created from deseriailzed $astVersion s-exp AST must result in missing")
        }

        evalAndAssertIsMissing(originalAst, "AST originated from parser")
        AstVersion.values().forEach { serializeRoundTripEvalAndAssertIsMissing(it) }

        assertAstRewriterBase(source, originalAst.toExprNode(ion))
    }

    protected fun assertExprEquals(expected: ExprValue, actual: ExprValue, message: String) {
        // exprEquals consider NULL and MISSING to be equivalent so we also check types here
        val isActuallyEquivalent = expected.type == actual.type && expected.exprEquals(actual)

        if (!isActuallyEquivalent) {
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
    protected fun eval(
        source: String,
        compileOptions: CompileOptions = CompileOptions.standard(),
        session: EvaluationSession = EvaluationSession.standard(),
        compilerPipelineBuilderBlock: CompilerPipeline.Builder.() -> Unit = { }
    ): ExprValue {
        val p = SqlParser(ion, CUSTOM_TEST_TYPES)
        val ast = p.parseAstStatement(source)
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
    protected fun evalForPermissiveMode(
        source: String,
        compileOptions: CompileOptions = CompileOptions.standard(),
        session: EvaluationSession = EvaluationSession.standard(),
        compilerPipelineBuilderBlock: CompilerPipeline.Builder.() -> Unit = { }
    ): ExprValue {

        val p = SqlParser(ion, CUSTOM_TEST_TYPES)

        val ast = p.parseAstStatement(source)
        return eval(
            ast,
            CompileOptions.builder(compileOptions).typingMode(TypingMode.PERMISSIVE).build(),
            session,
            compilerPipelineBuilderBlock
        )
    }

    /**
     * Evaluates an [PartiqlAst.Statement] given a [EvaluationSession]
     *
     * @param astStatement The [PartiqlAst.Statement] instance to be evaluated.
     * @param session [EvaluationSession] used for evaluation
     * @param compilerPipelineBuilderBlock any additional configuration to the pipeline after the options are set.
     */
    protected fun eval(
        astStatement: PartiqlAst.Statement,
        compileOptions: CompileOptions = CompileOptions.standard(),
        session: EvaluationSession = EvaluationSession.standard(),
        compilerPipelineBuilderBlock: CompilerPipeline.Builder.() -> Unit = { }
    ): ExprValue {

        // "Sneak" in this little assertion to test that every PIG ast that passes through
        // this function can be round-tripped to ExprNode and back.
        assertPartiqlAstExprNodeRoundTrip(astStatement)

        val pipeline = CompilerPipeline.builder(ion).apply {
            customDataTypes(CUSTOM_TEST_TYPES)
            compileOptions(compileOptions)
            compilerPipelineBuilderBlock()
        }

        return pipeline.build().compile(astStatement).eval(session)
    }

    private fun privateAssertThrows(
        query: String,
        expectedErrorCode: ErrorCode,
        expectedErrorContext: PropertyValueMap?,
        expectedInternal: Boolean?,
        session: EvaluationSession,
        excludeLegacySerializerAssertions: Boolean,
        compilerPipelineBuilderBlock: CompilerPipeline.Builder.() -> Unit,
        compileOptionsBlock: CompileOptions.Builder.() -> Unit,
        exceptionAssertBlock: (SqlException) -> Unit
    ) {
        val compilerPipeline = CompilerPipeline.build(ION) {
            customDataTypes(CUSTOM_TEST_TYPES)
            compilerPipelineBuilderBlock()
            compileOptions { compileOptionsBlock() }
        }

        val ex = org.junit.jupiter.api.assertThrows<SqlException>("test case should throw during evaluation") {
            // Note that an SqlException (usually a SemanticException or EvaluationException) might be thrown in
            // .compile OR in .eval.  We currently don't make a distinction, so tests cannot assert that certain
            // errors are compile-time and others are evaluation-time.  We really aught to create a way for tests to
            // indicate when the exception should be thrown.  This is undone.
            val expression = compilerPipeline.compile(query)
            expression.eval(session).ionValue
            // The call to .ionValue is important since query execution won't actually
            // begin otherwise.
        }

        assertEquals(expectedErrorCode, ex.errorCode, "Expected error code must match")
        if (expectedErrorContext != null) {
            assertEquals(expectedErrorContext, ex.errorContext, "Expected error context must match")
        }
        if (expectedInternal != null) {
            assertEquals(expectedInternal, ex.internal, "Expected internal flag must match")
        }

        exceptionAssertBlock(ex)

        commonAssertions(query, excludeLegacySerializerAssertions)
    }

    private fun commonAssertions(query: String, excludeLegacySerializerAssertions: Boolean) {
        val parser = SqlParser(ION, CUSTOM_TEST_TYPES)
        val ast = parser.parseAstStatement(query)

        assertPartiqlAstExprNodeRoundTrip(ast)

        val exprNode = ast.toExprNode(ion)

        assertAstRewriterBase(query, exprNode)

        if (!excludeLegacySerializerAssertions) {
            assertLegacySerializer(exprNode)
        }
    }

    @Suppress("DEPRECATION")
    private fun assertLegacySerializer(exprNode: ExprNode) {
        val deserializer = AstDeserializerBuilder(ion).build()
        AstVersion.values().forEach { astVersion ->
            val sexpRepresentation = AstSerializer.serialize(exprNode, astVersion, ion)
            val roundTrippedExprNode = deserializer.deserialize(sexpRepresentation, astVersion)
            assertEquals(
                exprNode.stripMetas(),
                roundTrippedExprNode.stripMetas(),
                "ExprNode deserialized from s-exp $astVersion AST must match the ExprNode returned by the parser"
            )
        }
    }

    private fun assertPartiqlAstExprNodeRoundTrip(ast: PartiqlAst.Statement) {
        val roundTrippedAst = ast.toExprNode(ion).toAstStatement()
        assertEquals(
            ast,
            roundTrippedAst,
            "PIG ast resulting from round trip to ExprNode and back should be equivalent."
        )
    }

    private fun assertAstRewriterBase(originalSql: String, exprNode: ExprNode) {
        val clonedAst = defaultRewriter.rewriteExprNode(exprNode)
        assertEquals(
            exprNode,
            clonedAst,
            "AST returned from default AstRewriterBase should match the original AST. SQL was: $originalSql"
        )
    }

    /**
     *  Asserts that [query] throws an [EvaluationException] or [SqlException] with the specified message, line and column number.
     *  Asserts that the [query] throws or returns missing in the [TypingMode.PERMISSIVE] mode depending on the [ErrorCode.errorBehaviorInPermissiveMode]
     *  It also verifies the behavior of error in [TypingMode.PERMISSIVE] mode.
     *  This should be used to ensure that the query is tested for both [TypingMode.LEGACY] and [TypingMode.PERMISSIVE]
     */
    protected fun assertThrows(
        query: String,
        expectedErrorCode: ErrorCode,
        expectedErrorContext: PropertyValueMap? = null,
        expectedPermissiveModeResult: String? = null,
        expectedInternal: Boolean? = null,
        excludeLegacySerializerAssertions: Boolean = false,
        session: EvaluationSession = EvaluationSession.standard(),
        compilerPipelineBuilderBlock: CompilerPipeline.Builder.() -> Unit = { },
        compileOptionsBuilderBlock: CompileOptions.Builder.() -> Unit = { },
        exceptionAssertBlock: (SqlException) -> Unit = { }
    ) {
        // Run the query once in legacy mode.
        privateAssertThrows(
            query = query,
            expectedErrorCode = expectedErrorCode,
            expectedErrorContext = expectedErrorContext,
            expectedInternal = expectedInternal,
            excludeLegacySerializerAssertions = excludeLegacySerializerAssertions,
            session = session,
            compilerPipelineBuilderBlock = compilerPipelineBuilderBlock,
            compileOptionsBlock = {
                compileOptionsBuilderBlock()
                typingMode(TypingMode.LEGACY)
            }
        ) { exception ->
            when (exception.errorCode.errorBehaviorInPermissiveMode) {
                ErrorBehaviorInPermissiveMode.THROW_EXCEPTION -> {
                    exceptionAssertBlock(exception)
                    // Reaching this point means: that the exception thrown in legacy mode indicates
                    // that an exception should also be thrown in permissive mode (unlike other exceptions
                    // which cause an expression's value to be changed to MISSING).

                    // In that case, we are going to re-run the test in permissive mode to ensure the same exception
                    // is thrown and verify that the expected permissive mode result is correct.

                    // But first, we check to ensure that the test case itself is valid.
                    assertNull(
                        "An expectedPermissiveModeResult must not be specified when " +
                            "ErrorCode.errorBehaviorInPermissiveMode is set to " +
                            "ErrorBehaviorInPermissiveMode.THROW_EXCEPTION",
                        expectedPermissiveModeResult
                    )

                    privateAssertThrows(
                        query = query,
                        expectedErrorCode = expectedErrorCode,
                        expectedErrorContext = expectedErrorContext,
                        expectedInternal = expectedInternal,
                        session = session,
                        excludeLegacySerializerAssertions = excludeLegacySerializerAssertions,
                        compilerPipelineBuilderBlock = compilerPipelineBuilderBlock,
                        compileOptionsBlock = {
                            compileOptionsBuilderBlock()
                            typingMode(TypingMode.PERMISSIVE)
                        }
                    ) { exception2 ->
                        exceptionAssertBlock(exception2)
                    }
                }
                // Return MISSING
                ErrorBehaviorInPermissiveMode.RETURN_MISSING -> {
                    assertNotNull(
                        "Required non null expectedPermissiveModeResult when ErrorCode.errorBehaviorInPermissiveMode is set to ErrorBehaviorInPermissiveMode.RETURN_MISSING",
                        expectedPermissiveModeResult
                    )
                    val originalExprValueForPermissiveMode = evalForPermissiveMode(
                        query,
                        session = session,
                        compileOptions = CompileOptions.build { compileOptionsBuilderBlock() },
                        compilerPipelineBuilderBlock = compilerPipelineBuilderBlock
                    )
                    val expectedExprValueForPermissiveMode =
                        evalForPermissiveMode(expectedPermissiveModeResult!!, session = session)
                    assertExprEquals(
                        expectedExprValueForPermissiveMode,
                        originalExprValueForPermissiveMode,
                        "(PERMISSIVE mode)"
                    )
                }
            }
        }
    }

    protected fun assertThrows(tc: EvaluatorErrorTestCase, session: EvaluationSession) {
        assertThrows(
            query = tc.query,
            expectedErrorCode = tc.errorCode,
            expectedErrorContext = tc.expectErrorContext,
            excludeLegacySerializerAssertions = tc.excludeLegacySerializerAssertions,
            session = session,
            expectedPermissiveModeResult = tc.expectedPermissiveModeResult,
            compileOptionsBuilderBlock = tc.compileOptionsBuilderBlock,
            compilerPipelineBuilderBlock = tc.compilerPipelineBuilderBlock
        )
    }

    /**
     *  Asserts that [func] throws an [SqlException], line and column number in [TypingMode.PERMISSIVE] mode
     */
    private fun assertThrowsInPermissiveMode(
        expectedErrorCode: ErrorCode,
        expectedErrorContext: PropertyValueMap? = null,
        cause: KClass<out Throwable>? = null,
        func: () -> Unit
    ) {
        try {
            func()
            fail("didn't throw")
        } catch (e: SqlException) {
            softAssert {
                assertThat(e.errorCode).`as`("error code").isEqualTo(expectedErrorCode)
                if (expectedErrorContext != null) {
                    assertThat(e.errorContext).`as`("error context").isEqualTo(expectedErrorContext)
                }
                if (cause != null) assertThat(e).hasRootCauseExactlyInstanceOf(cause.java)
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
        runTestCase(
            tc.copy(
                compileOptionsBuilderBlock = {
                    tc.compileOptionsBuilderBlock(this)
                    typingMode(TypingMode.PERMISSIVE)
                }
            ),
            session,
            "compile options forced to PERMISSIVE mode"
        )
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
        message: String? = null
    ) {
        val msg = message?.let { "($it)" } ?: ""

        fun showTestCase() {
            println(listOfNotNull(message, tc.groupName).joinToString(" : "))
            println("Query under test  : ${tc.sqlUnderTest}")
            println("Expected value    : ${tc.expectedSql}")
            println()
        }

        val expected = try {
            eval(
                source = tc.expectedSql,
                compilerPipelineBuilderBlock = tc.compilerPipelineBuilderBlock,
                compileOptions = CompileOptions.build {
                    tc.compileOptionsBuilderBlock(this)
                }
            )
        } catch (e: Throwable) {
            showTestCase()
            e.printStackTrace()
            fail("Exception while attempting to evaluate the expected value, see standard output $msg")
            throw e
        }

        val actual = try {
            eval(
                source = tc.sqlUnderTest,
                compilerPipelineBuilderBlock = tc.compilerPipelineBuilderBlock,
                session = session,
                compileOptions = CompileOptions.build {
                    tc.compileOptionsBuilderBlock(this)
                }
            )
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

internal fun IonValue.removePartiqlAnnotations() {
    when (this.type) {
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
                it.removePartiqlAnnotations()
            }
        }
        // IonType.TIMESTAMP -> this.removeTypeAnnotation(DATE_ANNOTATION)
        else -> { /* ok to do nothing. */ }
    }
}

internal fun IonValue.cloneAndRemoveAnnotations() = this.clone().apply {
    removePartiqlAnnotations()
    makeReadOnly()
}
