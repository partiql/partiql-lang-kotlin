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
import org.junit.jupiter.api.assertThrows
import org.partiql.lang.CUSTOM_TEST_TYPES
import org.partiql.lang.CompilerPipeline
import org.partiql.lang.ION
import org.partiql.lang.SqlException
import org.partiql.lang.TestBase
import org.partiql.lang.ast.AstDeserializerBuilder
import org.partiql.lang.ast.AstSerializer
import org.partiql.lang.ast.AstVersion
import org.partiql.lang.ast.ExprNode
import org.partiql.lang.ast.passes.AstRewriterBase
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
import org.partiql.lang.util.stripMetas
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

    private val defaultRewriter = AstRewriterBase()

    /**
     * creates a [ExprValue] from the IonValue represented by this String. Assumes the string represents a single
     * IonValue
     */
    private fun String.toExprValue(): ExprValue = valueFactory.newFromIonText(this)

    private fun Map<String, String>.toBindings(): Bindings<ExprValue> =
        Bindings.ofMap(mapValues { it.value.toExprValue() })

    protected fun Map<String, String>.toSession() = EvaluationSession.build { globals(this@toSession.toBindings()) }

    /**
     * Constructor style override of [runEvaluatorTestCase].  Constructs an [EvaluatorTestCase]
     * and runs it.  This is intended to be used by non-parameterized tests.
     *
     * @see [EvaluatorTestCase].
     */
    protected fun runEvaluatorTestCase(
        source: String,
        expected: String,
        session: EvaluationSession = EvaluationSession.standard(),
        excludeLegacySerializerAssertions: Boolean = false,
        compileOptionsBuilderBlock: CompileOptions.Builder.() -> Unit = { },
        compilerPipelineBuilderBlock: CompilerPipeline.Builder.() -> Unit = { },
        block: (ExprValue) -> Unit = { }
    ) {
        val tc = EvaluatorTestCase(
            query = source,
            expectedResult = expected,
            expectedResultMode = ExpectedResultMode.ION_WITHOUT_BAG_AND_MISSING_ANNOTATIONS,
            excludeLegacySerializerAssertions = excludeLegacySerializerAssertions,
            compileOptionsBuilderBlock = compileOptionsBuilderBlock,
            compilerPipelineBuilderBlock = compilerPipelineBuilderBlock,
            extraResultAssertions = block
        )
        runEvaluatorTestCase(tc, session)
    }

    /**
     * Runs an [EvaluatorTestCase].  This is intended to be used by parameterized tests.
     *
     * @see [EvaluatorTestCase].
     */
    protected fun runEvaluatorTestCase(tc: EvaluatorTestCase, session: EvaluationSession) {
//        val testOpts = CompileOptions.build { tc.compileOptionsBuilderBlock(this) }
//        assertNotEquals(TypingMode.PERMISSIVE, testOpts.typingMode)

        // LEGACY mode
        privateRunEvaluatorTestCase(tc, session, "compile options unaltered")

        // PERMISSIVE mode
        privateRunEvaluatorTestCase(
            tc.copy(
                compileOptionsBuilderBlock = {
                    tc.compileOptionsBuilderBlock(this)
                    typingMode(TypingMode.PERMISSIVE)
                }
            ),
            session,
            "compile options forced to PERMISSIVE mode"
        )

        commonAssertions(tc.query, tc.excludeLegacySerializerAssertions)
    }

    /**
     * Runs the give test case once with the specified [session].
     *
     * If non-null, [message] will be dumped to the console before test failure to aid in the identification
     * and debugging of failed tests.
     */
    private fun privateRunEvaluatorTestCase(
        tc: EvaluatorTestCase,
        session: EvaluationSession,
        message: String
    ) {
        val msg = message.let { "($it)" }

        fun showTestCase() {
            println(listOfNotNull(message, tc.groupName).joinToString(" : "))
            println("Query under test  : ${tc.query}")
            println("Expected value    : ${tc.expectedResult}")
            println()
        }

        val compileOptions = CompileOptions.build { tc.compileOptionsBuilderBlock(this) }

        val actual = try {
            eval(
                source = tc.query,
                compilerPipelineBuilderBlock = tc.compilerPipelineBuilderBlock,
                session = session,
                compileOptions = compileOptions
            )
        } catch (e: Throwable) {
            showTestCase()
            e.printStackTrace()
            fail("Exception while attempting to evaluate the under test, see standard output $msg")
            throw e
        }

        val expectedResult =
            when (compileOptions.typingMode) {
                TypingMode.LEGACY -> tc.expectedResult
                TypingMode.PERMISSIVE -> tc.expectedPermissiveModeResult
            }

        when (tc.expectedResultMode) {
            ExpectedResultMode.ION_WITHOUT_BAG_AND_MISSING_ANNOTATIONS -> {
                assertEquals(
                    ION.singleValue(expectedResult),
                    actual.ionValue.cloneAndRemoveBagAndMissingAnnotations(),
                    "$message: ${compileOptions.typedOpBehavior} CAST in ${compileOptions.typingMode} typing mode, " +
                        "evaluated '${tc.query}' with evaluator"
                )
            }
            ExpectedResultMode.PARTIQL -> {
                val expected = try {
                    eval(
                        source = expectedResult,
                        compilerPipelineBuilderBlock = tc.compilerPipelineBuilderBlock,
                        compileOptions = compileOptions,
                        session = session
                    )
                } catch (e: Throwable) {
                    showTestCase()
                    e.printStackTrace()
                    fail("Exception while attempting to evaluate the expected value, see standard output $msg")
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

        tc.extraResultAssertions(actual)
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
        val pipeline = CompilerPipeline.builder(ion).apply {
            customDataTypes(CUSTOM_TEST_TYPES)
            compileOptions(compileOptions)
            compilerPipelineBuilderBlock()
        }

        return pipeline.build().compile(astStatement).eval(session)
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

    /** Runs an [EvaluatorErrorTestCase] once. */
    private fun privateRunEvaluatorErrorTestCase(
        tc: EvaluatorErrorTestCase,
        session: EvaluationSession,
    ) {
//        val testOpts = CompileOptions.build { tc.compileOptionsBuilderBlock(this) }
//        assertNotEquals(TypingMode.PERMISSIVE, testOpts.typingMode)

        val compilerPipeline = CompilerPipeline.build(ION) {
            customDataTypes(CUSTOM_TEST_TYPES)
            tc.compilerPipelineBuilderBlock(this)
            compileOptions { tc.compileOptionsBuilderBlock(this) }
        }

        val ex = assertThrows<SqlException>("test case should throw during evaluation") {
            // Note that an SqlException (usually a SemanticException or EvaluationException) might be thrown in
            // .compile OR in .eval.  We currently don't make a distinction, so tests cannot assert that certain
            // errors are compile-time and others are evaluation-time.  We really aught to create a way for tests to
            // indicate when the exception should be thrown.  This is undone.
            val expression = compilerPipeline.compile(tc.query)
            expression.eval(session).ionValue
            // The call to .ionValue is important since query execution won't actually
            // begin otherwise.
        }

        assertEquals(tc.expectedErrorCode, ex.errorCode, "Expected error code must match")
        if (tc.expectedErrorContext != null) {
            assertEquals(tc.expectedErrorContext, ex.errorContext, "Expected error context must match")
        }
        if (tc.expectedInternalFlag != null) {
            assertEquals(tc.expectedInternalFlag, ex.internal, "Expected internal flag must match")
        }

        tc.additionalExceptionAssertBlock(ex)
    }

    /** See the other overload of this function.  This overload is for non-parameterized tests. */
    protected fun runEvaluatorErrorTestCase(
        query: String,
        expectedErrorCode: ErrorCode,
        expectedErrorContext: PropertyValueMap? = null,
        expectedPermissiveModeResult: String? = null,
        expectedInternalFlag: Boolean? = null,
        excludeLegacySerializerAssertions: Boolean = false,
        compilerPipelineBuilderBlock: CompilerPipeline.Builder.() -> Unit = { },
        compileOptionsBuilderBlock: CompileOptions.Builder.() -> Unit = { },
        addtionalExceptionAssertBlock: (SqlException) -> Unit = { },
        session: EvaluationSession = EvaluationSession.standard()
    ) {
        val tc = EvaluatorErrorTestCase(
            query = query,
            expectedErrorCode = expectedErrorCode,
            expectedErrorContext = expectedErrorContext,
            expectedInternalFlag = expectedInternalFlag,
            expectedPermissiveModeResult = expectedPermissiveModeResult,
            excludeLegacySerializerAssertions = excludeLegacySerializerAssertions,
            compileOptionsBuilderBlock = compileOptionsBuilderBlock,
            compilerPipelineBuilderBlock = compilerPipelineBuilderBlock,
            additionalExceptionAssertBlock = addtionalExceptionAssertBlock
        )

        runEvaluatorErrorTestCase(tc, session)
    }

    /**
     * Runs an [EvaluatorErrorTestCase].  This should be the normal entry point for all error test cases.
     **
     * If the [EvaluatorErrorTestCase.expectedErrorCode] has [ErrorBehaviorInPermissiveMode.RETURN_MISSING] set,
     * evaluates and asserts no [SqlException] was thrown and the return value is equal to
     * [EvaluatorErrorTestCase.expectedPermissiveModeResult] (PartiQL equivalence.)  Otherwise, the error assertions
     * are the same as [TypingMode.LEGACY]
     */
    protected fun runEvaluatorErrorTestCase(tc: EvaluatorErrorTestCase, session: EvaluationSession) {
        // Run the query once in legacy mode.
        privateRunEvaluatorErrorTestCase(
            tc.copy(
                compileOptionsBuilderBlock = {
                    tc.compileOptionsBuilderBlock(this)
                    typingMode(TypingMode.LEGACY)
                }
            ),
            session
        )
        when (tc.expectedErrorCode.errorBehaviorInPermissiveMode) {
            ErrorBehaviorInPermissiveMode.THROW_EXCEPTION -> {
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
                    tc.expectedPermissiveModeResult
                )

                // Run the query once in permissive mode.
                privateRunEvaluatorErrorTestCase(
                    tc.copy(
                        compileOptionsBuilderBlock = {
                            tc.compileOptionsBuilderBlock(this)
                            typingMode(TypingMode.PERMISSIVE)
                        }
                    ),
                    session
                )
            }
            // Return MISSING
            ErrorBehaviorInPermissiveMode.RETURN_MISSING -> {
                assertNotNull(
                    "Required non null expectedPermissiveModeResult when ErrorCode.errorBehaviorInPermissiveMode is " +
                        "set to ErrorBehaviorInPermissiveMode.RETURN_MISSING",
                    tc.expectedPermissiveModeResult
                )

                // Compute the expected return value
                val expectedExprValueForPermissiveMode = evalInPermissiveMode(
                    tc.expectedPermissiveModeResult!!,
                    session = session
                )

                val actualReturnValueForPermissiveMode = evalInPermissiveMode(
                    tc.query,
                    session = session,
                    compileOptions = CompileOptions.build { tc.compileOptionsBuilderBlock(this) },
                    compilerPipelineBuilderBlock = tc.compilerPipelineBuilderBlock
                )
                assertExprEquals(
                    expectedExprValueForPermissiveMode,
                    actualReturnValueForPermissiveMode,
                    "(PERMISSIVE mode)"
                )
            }
        }

        commonAssertions(tc.query, tc.excludeLegacySerializerAssertions)
    }

    /**
     * Evaluates a source query given a [EvaluationSession] with default [CompileOptions] for [TypingMode.PERMISSIVE]
     *
     * The provided (or default) [compileOptions] are modified to have the [TypingMode] as [TypingMode.PERMISSIVE]
     * @param source query source to be evaluated
     * @param session [EvaluationSession] used for evaluation
     * @param compilerPipelineBuilderBlock any additional configuration to the pipeline after the options are set.
     */
    private fun evalInPermissiveMode(
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
}

internal fun IonValue.removeBagAndMissingAnnotations() {
    when (this.type) {
        // Remove $partiql_missing annotation from NULL for assertions
        IonType.NULL -> this.removeTypeAnnotation(MISSING_ANNOTATION)
        // Recurse into all container types.
        IonType.DATAGRAM, IonType.SEXP, IonType.STRUCT, IonType.LIST -> {
            // Remove $partiql_bag annotation from LIST for assertions
            if (this.type == IonType.LIST) {
                this.removeTypeAnnotation(BAG_ANNOTATION)
            }
            // Recursively remove annotations
            this.asSequence().forEach {
                it.removeBagAndMissingAnnotations()
            }
        }
        else -> { /* ok to do nothing. */ }
    }
}

/**
 * Clones and removes $partiql_bag and $partiql_missing annotations from the clone and any child values.
 *
 * There are many tests which were created before these annotations were present and thus do not include them
 * in their expected values.  This function provides an alternative to having to go and update all of them.
 * This is tech debt of the unhappy variety:  all of those test cases should really be updated and this function
 * should be deleted.
 *
 * NOTE: this function does not remove $partiql_date annotations ever!  There are tests that depend on this too.
 * $partiql_date however, was added AFTER this function was created, and so no test cases needed to remove that
 * annotation.
 */
internal fun IonValue.cloneAndRemoveBagAndMissingAnnotations() = this.clone().apply {
    removeBagAndMissingAnnotations()
    makeReadOnly()
}
