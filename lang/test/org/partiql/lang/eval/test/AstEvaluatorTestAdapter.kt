package org.partiql.lang.eval.test

import org.junit.Assert
import org.junit.jupiter.api.assertThrows
import org.partiql.lang.CompilerPipeline
import org.partiql.lang.ION
import org.partiql.lang.SqlException
import org.partiql.lang.errors.ErrorBehaviorInPermissiveMode
import org.partiql.lang.eval.CompileOptions
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExpectedResultFormat
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.TypingMode
import org.partiql.lang.eval.cloneAndRemoveBagAndMissingAnnotations
import org.partiql.lang.eval.exprEquals
import org.partiql.lang.util.ConfigurableExprValueFormatter
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

private fun EvaluatorTestDefinition.createPipeline(forcePermissiveMode: Boolean = false): CompilerPipeline {
    val compileOptions = CompileOptions.build(this@createPipeline.compileOptionsBuilderBlock).let { co ->
        if (forcePermissiveMode) {
            CompileOptions.build(co) {
                typingMode(TypingMode.PERMISSIVE)
            }
        } else {
            co
        }
    }

    return CompilerPipeline.build(ION) {
        compileOptions(compileOptions)
        this@createPipeline.compilerPipelineBuilderBlock(this)
    }
}

class AstEvaluatorTestAdapter : EvaluatorTestAdapter {

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

        val pipeline = tc.createPipeline()

        val actualResult = try {
            pipeline.compile(tc.query).eval(session)
        } catch (e: Throwable) {
            showTestCase()
            e.printStackTrace()
            Assert.fail("Exception while attempting to evaluate the under test, see standard output $assertionMessage")
            throw e
        }

        val expectedResult =
            when (pipeline.compileOptions.typingMode) {
                TypingMode.LEGACY -> tc.expectedResult
                TypingMode.PERMISSIVE -> tc.expectedPermissiveModeResult
            }

        when (tc.expectedResultFormat) {
            ExpectedResultFormat.ION, ExpectedResultFormat.ION_WITHOUT_BAG_AND_MISSING_ANNOTATIONS -> {
                val expectedIonResult = ION.singleValue(expectedResult)
                val actualIonResult = actualResult.ionValue.let {
                    if (tc.expectedResultFormat == ExpectedResultFormat.ION_WITHOUT_BAG_AND_MISSING_ANNOTATIONS)
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
            ExpectedResultFormat.PARTIQL -> {
                val expected = try {
                    pipeline.compile(tc.query).eval(session)
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
            ExpectedResultFormat.STRING -> {
                assertEquals(expectedResult, actualResult.toString(), "Actual result must match expected (string equality)")
            }
        }.let { }
        tc.extraResultAssertions(actualResult)
    }

    /** Runs an [EvaluatorErrorTestCase] once. */
    private fun privateRunEvaluatorErrorTestCase(
        tc: EvaluatorErrorTestCase,
        session: EvaluationSession,
    ) {
        val compilerPipeline = tc.createPipeline()

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
                val permissiveModePipeline = tc.createPipeline(forcePermissiveMode = true)

                val expectedExprValueForPermissiveMode = permissiveModePipeline
                    .compile(tc.expectedPermissiveModeResult!!).eval(session)

                val actualReturnValueForPermissiveMode = permissiveModePipeline
                    .compile(tc.query).eval(session)

                assertExprEquals(
                    expectedExprValueForPermissiveMode,
                    actualReturnValueForPermissiveMode,
                    "(PERMISSIVE mode)"
                )
            }
        }
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
