package org.partiql.lang.eval

import org.junit.Assert
import org.junit.jupiter.api.assertThrows
import org.partiql.lang.CUSTOM_TEST_TYPES
import org.partiql.lang.CompilerPipeline
import org.partiql.lang.ION
import org.partiql.lang.SqlException
import org.partiql.lang.ast.AstDeserializerBuilder
import org.partiql.lang.ast.toAstStatement
import org.partiql.lang.ast.toExprNode
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.errors.ErrorBehaviorInPermissiveMode
import org.partiql.lang.syntax.SqlParser
import org.partiql.lang.util.ConfigurableExprValueFormatter
import org.partiql.lang.util.stripMetas
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * Implementation of this interface allow for different PartiQL compilers to be tested.
 *
 * - [AstEvaluatorTestHarness] evaluates [EvaluatorTestCase] or [EvaluatorErrorTestCase] with the AST evaluator
 * (i.e [CompilerPipeline]/[EvaluatingCompiler] combination.
 *
 * Future implementations of [EvaluatorTestHarness] will:
 *
 * - Tests the query planner and physical plan compiler instead.
 * - [EvaluatorTestHarness] that delegates multiple other implementations, allowing multiple implementations to be
 * treated as one.
 */
interface EvaluatorTestHarness {
    /**
     * Runs an [EvaluatorTestCase].  This is intended to be used by parameterized tests.
     *
     * Also runs additional assertions performed by [commonAssertions].
     *
     * @see [EvaluatorTestCase].
     */
    fun runEvaluatorTestCase(tc: EvaluatorTestCase, session: EvaluationSession)

    /**
     * Evaluates a source query given a [EvaluationSession]
     *
     * @param source query source to be evaluated
     * @param session [EvaluationSession] used for evaluation
     * @param compilerPipelineBuilderBlock any additional configuration to the pipeline after the options are set.
     */
    fun eval(
        source: String,
        compileOptions: CompileOptions = CompileOptions.standard(),
        session: EvaluationSession = EvaluationSession.standard(),
        compilerPipelineBuilderBlock: CompilerPipeline.Builder.() -> Unit = { }
    ): ExprValue

    /**
     * Runs an [EvaluatorErrorTestCase].  This should be the normal entry point for parameterized error test cases.
     *
     * If the [EvaluatorErrorTestCase.expectedErrorCode] has [ErrorBehaviorInPermissiveMode.RETURN_MISSING] set,
     * evaluates and asserts no [SqlException] was thrown and the return value is equal to
     * [EvaluatorErrorTestCase.expectedPermissiveModeResult] (PartiQL equivalence.)  Otherwise, the error assertions
     * are the same as [TypingMode.LEGACY]
     */
    fun runEvaluatorErrorTestCase(tc: EvaluatorErrorTestCase, session: EvaluationSession)
}

class AstEvaluatorTestHarness : EvaluatorTestHarness {

    @Suppress("DEPRECATION")
    private val defaultRewriter = org.partiql.lang.ast.passes.AstRewriterBase()

    override fun runEvaluatorTestCase(tc: EvaluatorTestCase, session: EvaluationSession) {
        // DL TODO: delete me
        if (tc.implicitPermissiveModeTest) {
            val testOpts = CompileOptions.build { tc.compileOptionsBuilderBlock(this) }
            assertNotEquals(
                TypingMode.PERMISSIVE, testOpts.typingMode,
                "Setting TypingMode.PERMISSIVE when implicit permissive mode testing is enabled is redundant"
            )
        }

        // Compile options unmodified... This covers [TypingMode.LEGACY], unless the test explicitly
        // sets the typing mode.
        privateRunEvaluatorTestCase(tc, session, "compile options unaltered")

        // Unless the tests disable it, run again in permissive mode.
        if (tc.implicitPermissiveModeTest) {
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
        }

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
        val assertionMessage = message.let { "($it)" }

        fun showTestCase() {
            println(listOfNotNull(message, tc.groupName).joinToString(" : "))
            println("Query under test  : ${tc.query}")
            println("Expected value    : ${tc.expectedResult}")
            println()
        }

        val compileOptions = CompileOptions.build { tc.compileOptionsBuilderBlock(this) }

        val actualResult = try {
            eval(
                source = tc.query,
                compilerPipelineBuilderBlock = tc.compilerPipelineBuilderBlock,
                session = session,
                compileOptions = compileOptions
            )
        } catch (e: Throwable) {
            showTestCase()
            e.printStackTrace()
            Assert.fail("Exception while attempting to evaluate the under test, see standard output $assertionMessage")
            throw e
        }

        val expectedResult =
            when (compileOptions.typingMode) {
                TypingMode.LEGACY -> tc.expectedResult
                TypingMode.PERMISSIVE -> tc.expectedPermissiveModeResult
            }

        when (tc.expectedResultMode) {
            ExpectedResultMode.ION, ExpectedResultMode.ION_WITHOUT_BAG_AND_MISSING_ANNOTATIONS -> {
                val expectedIonResult = ION.singleValue(expectedResult)
                val actualIonResult = actualResult.ionValue.let {
                    if (tc.expectedResultMode == ExpectedResultMode.ION_WITHOUT_BAG_AND_MISSING_ANNOTATIONS)
                        it.cloneAndRemoveBagAndMissingAnnotations()
                    else
                        it
                }
                assertEquals(
                    expectedIonResult,
                    actualIonResult,
                    assertionMessage
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
                    Assert.fail("Exception while attempting to evaluate the expected value, see standard output $assertionMessage")
                    throw e
                }
                if (!expected.exprEquals(actualResult)) {
                    showTestCase()
                    println("Expected : $expected")
                    println("Actual   : $actualResult")

                    Assert.fail("Expected and actual ExprValue instances are not equivalent $assertionMessage")
                }
                Unit
            }
        }.let {}

        tc.extraResultAssertions(actualResult)
    }

    override fun eval(
        source: String,
        compileOptions: CompileOptions,
        session: EvaluationSession,
        compilerPipelineBuilderBlock: CompilerPipeline.Builder.() -> Unit
    ): ExprValue {
        val pipeline = CompilerPipeline.builder(ION).apply {
            customDataTypes(CUSTOM_TEST_TYPES)
            compileOptions(compileOptions)
            compilerPipelineBuilderBlock()
        }

        return pipeline.build().compile(source).eval(session)
    }

    /**
     * Parses the given query and then:
     *
     * - Tests `ExprNode` <-> `PartiqlAst` transformations.
     * - Tests [org.partiql.lang.ast.passes.AstRewriterBase].
     * - When [excludeLegacySerializerAssertions] is `false`, tests the legacy (de)serializers for the V0 AST.
     *
     * @param query The SQL under test.
     * @param excludeLegacySerializerAssertions Disables the legacy (de)serializers tests.  These are no longer
     * being updated to support new AST nodes and therefore tests that include new nodes will fail unless this is
     * set.
     */
    private fun commonAssertions(query: String, excludeLegacySerializerAssertions: Boolean) {
        val parser = SqlParser(ION, CUSTOM_TEST_TYPES)
        val ast = parser.parseAstStatement(query)

        assertPartiqlAstExprNodeRoundTrip(ast)

        val exprNode = ast.toExprNode(ION)

        assertAstRewriterBase(query, exprNode)

        if (!excludeLegacySerializerAssertions) {
            assertLegacySerializer(exprNode)
        }
    }

    @Suppress("DEPRECATION")
    private fun assertLegacySerializer(
        exprNode: org.partiql.lang.ast.ExprNode
    ) {
        val deserializer = AstDeserializerBuilder(ION).build()
        org.partiql.lang.ast.AstVersion.values().forEach { astVersion ->
            val sexpRepresentation = org.partiql.lang.ast.AstSerializer.serialize(exprNode, astVersion, ION)
            val roundTrippedExprNode = deserializer.deserialize(sexpRepresentation, astVersion)
            assertEquals(
                exprNode.stripMetas(),
                roundTrippedExprNode.stripMetas(),
                "ExprNode deserialized from s-exp $astVersion AST must match the ExprNode returned by the parser"
            )
        }
    }

    private fun assertPartiqlAstExprNodeRoundTrip(ast: PartiqlAst.Statement) {
        val roundTrippedAst = ast.toExprNode(ION).toAstStatement()
        assertEquals(
            ast,
            roundTrippedAst,
            "PIG ast resulting from round trip to ExprNode and back should be equivalent."
        )
    }

    private fun assertAstRewriterBase(
        originalSql: String,
        @Suppress("DEPRECATION")
        exprNode: org.partiql.lang.ast.ExprNode
    ) {
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

    override fun runEvaluatorErrorTestCase(tc: EvaluatorErrorTestCase, session: EvaluationSession) {
        if (tc.implicitPermissiveModeTest) {
            val testOpts = CompileOptions.build { tc.compileOptionsBuilderBlock(this) }
            assertNotEquals(
                TypingMode.PERMISSIVE, testOpts.typingMode,
                "Setting TypingMode.PERMISSIVE when implicit permissive mode testing is enabled is redundant"
            )
        }

        // Run the query once with compile options unmodified.
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
                // The expected error code indicates that this error should also throw in permissive mode.
                Assert.assertNull(
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
            ErrorBehaviorInPermissiveMode.RETURN_MISSING -> {

                // The expected error code indicates that this error should continue, but the expression should
                // return missing.
                Assert.assertNotNull(
                    "Required non null expectedPermissiveModeResult when ErrorCode.errorBehaviorInPermissiveMode is " +
                        "set to ErrorBehaviorInPermissiveMode.RETURN_MISSING",
                    tc.expectedPermissiveModeResult
                )

                // DL TODO: can we convert this EvaluatorErrorTestCase into an EvaluatorTestCase and run it instead?

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
        return eval(
            source,
            CompileOptions.builder(compileOptions).typingMode(TypingMode.PERMISSIVE).build(),
            session,
            compilerPipelineBuilderBlock
        )
    }

    private fun assertExprEquals(expected: ExprValue, actual: ExprValue, message: String) {
        // exprEquals consider NULL and MISSING to be equivalent so we also check types here
        val isActuallyEquivalent = expected.type == actual.type && expected.exprEquals(actual)

        if (!isActuallyEquivalent) {
            println("Expected ionValue: ${ConfigurableExprValueFormatter.pretty.format(expected)} ")
            println("Actual ionValue  : ${ConfigurableExprValueFormatter.pretty.format(actual)} ")
            Assert.fail("$message Expected and actual ExprValue instances are not equivalent")
        }
    }
}