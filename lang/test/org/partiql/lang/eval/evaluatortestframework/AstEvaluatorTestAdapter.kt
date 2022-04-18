package org.partiql.lang.eval.evaluatortestframework

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.partiql.lang.CompilerPipeline
import org.partiql.lang.ION
import org.partiql.lang.SqlException
import org.partiql.lang.errors.ErrorBehaviorInPermissiveMode
import org.partiql.lang.errors.PropertyValueMap
import org.partiql.lang.eval.CompileOptions
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.TypingMode
import org.partiql.lang.eval.cloneAndRemoveBagAndMissingAnnotations
import org.partiql.lang.eval.exprEquals
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

/** A generated and human readable description of this test case for display in assertion failure messages. */
fun EvaluatorTestCase.testDetails(note: String, actualResult: String? = null): String {
    val b = StringBuilder()
    b.appendLine("Note            : $note")
    b.appendLine("Group name      : $groupName")
    b.appendLine("Query           : $query")
    b.appendLine("Expected result : $expectedResult")
    if (actualResult != null) {
        b.appendLine("Actual result   : $actualResult")
    }
    b.appendLine("Result format   : $expectedResultFormat")

    return b.toString()
}

/** A generated and human readable description of this test case for display in assertion failure messages. */
fun EvaluatorErrorTestCase.testDetails(
    note: String,
    actualErrorCode: String? = null,
    actualErrorContext: PropertyValueMap? = null,
    actualPermissiveModeResult: String? = null,
    actualInternalFlag: Boolean? = null,
): String {
    val b = StringBuilder()
    b.appendLine("Note                           : $note")
    b.appendLine("Group name                     : $groupName")
    b.appendLine("Query                          : $query")
    b.appendLine("Expected error code            : $expectedErrorCode")
    if (actualErrorCode != null) {
        b.appendLine("Actual error code              : $actualErrorCode")
    }
    b.appendLine("Expected error context         : $expectedErrorContext")
    if (actualErrorContext != null) {
        b.appendLine("Actual error context           : $actualErrorContext")
    }
    b.appendLine("Expected internal flag         : $expectedInternalFlag")
    if (actualErrorContext != null) {
        b.appendLine("Actual internal flag           : $actualInternalFlag")
    }
    b.appendLine("Expected permissive mode result: $expectedPermissiveModeResult")
    if (actualPermissiveModeResult != null) {
        b.appendLine("Actual permissive mode result  : $actualPermissiveModeResult")
    }
    return b.toString()
}

private fun assertEquals(
    expected: Any?,
    actual: Any?,
    reason: EvaluatorTestFailureReason,
    detailsBlock: () -> String
) {
    if (expected != actual) {
        throw EvaluatorAssertionFailedError(reason, detailsBlock())
    }
}

private fun <T> assertDoesNotThrow(
    reason: EvaluatorTestFailureReason,
    detailsBlock: () -> String,
    block: () -> T
): T {
    try {
        return block()
    } catch (ex: Throwable) {
        throw EvaluatorAssertionFailedError(reason, detailsBlock(), ex.cause)
    }
}

private inline fun assertThrowsSqlException(
    reason: EvaluatorTestFailureReason,
    detailsBlock: () -> String,
    block: () -> Unit
): SqlException {
    try {
        block()
        // if we made it here, the test failed.
        throw EvaluatorAssertionFailedError(reason, detailsBlock())
    } catch (ex: SqlException) {
        return ex
    }
}

class AstEvaluatorTestAdapter : EvaluatorTestAdapter {

    override fun runEvaluatorTestCase(tc: EvaluatorTestCase, session: EvaluationSession) {
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
     */
    private fun privateRunEvaluatorTestCase(
        tc: EvaluatorTestCase,
        session: EvaluationSession,
        note: String,
    ) {
        val pipeline = tc.createPipeline()

        val actualExprValueResult = assertDoesNotThrow(
            EvaluatorTestFailureReason.FAILED_TO_EVALUATE_QUERY,
            { tc.testDetails(note) }
        ) {
            pipeline.compile(tc.query).eval(session)
        }

        val (expectedResult, unexpectedResultErrorCode) =
            when (pipeline.compileOptions.typingMode) {
                TypingMode.LEGACY -> tc.expectedResult to EvaluatorTestFailureReason.UNEXPECTED_QUERY_RESULT
                TypingMode.PERMISSIVE -> tc.expectedPermissiveModeResult to EvaluatorTestFailureReason.UNEXPECTED_PERMISSIVE_MODE_RESULT
            }

        when (tc.expectedResultFormat) {
            ExpectedResultFormat.ION, ExpectedResultFormat.ION_WITHOUT_BAG_AND_MISSING_ANNOTATIONS -> {
                val expectedIonResult = assertDoesNotThrow(
                    EvaluatorTestFailureReason.FAILED_TO_PARSE_ION_EXPECTED_RESULT,
                    { tc.testDetails(note) }
                ) {
                    ION.singleValue(expectedResult)
                }

                val actualIonResult = actualExprValueResult.ionValue.let {
                    if (tc.expectedResultFormat == ExpectedResultFormat.ION_WITHOUT_BAG_AND_MISSING_ANNOTATIONS)
                        it.cloneAndRemoveBagAndMissingAnnotations()
                    else
                        it
                }
                assertEquals(
                    expectedIonResult,
                    actualIonResult,
                    unexpectedResultErrorCode
                ) { tc.testDetails(actualIonResult.toString()) }
            }
            ExpectedResultFormat.PARTIQL -> {
                val expectedExprValueResult = assertDoesNotThrow(
                    EvaluatorTestFailureReason.FAILED_TO_EVALUATE_PARTIQL_EXPECTED_RESULT,
                    { tc.testDetails(note) }
                ) {
                    pipeline.compile(expectedResult).eval(session)
                }

                if (!expectedExprValueResult.exprEquals(actualExprValueResult)) {
                    throw EvaluatorAssertionFailedError(
                        EvaluatorTestFailureReason.UNEXPECTED_QUERY_RESULT,
                        tc.testDetails(actualExprValueResult.toString())
                    )
                }
                Unit
            }
            ExpectedResultFormat.STRING -> {
                val actualResultString = actualExprValueResult.toString()
                assertEquals(
                    expectedResult,
                    actualResultString,
                    EvaluatorTestFailureReason.UNEXPECTED_QUERY_RESULT,
                ) { tc.testDetails(actualResultString) }
            }
        }.let { }
        tc.extraResultAssertions(actualExprValueResult)
    }

    /** Runs an [EvaluatorErrorTestCase] once. */
    private fun privateRunEvaluatorErrorTestCase(
        tc: EvaluatorErrorTestCase,
        session: EvaluationSession,
        note: String
    ) {
        val compilerPipeline = tc.createPipeline()

        val ex = assertThrowsSqlException(
            EvaluatorTestFailureReason.EXPECTED_SQL_EXCEPTION_BUT_THERE_WAS_NONE,
            { tc.testDetails(note) }
        ) {

            // Note that an SqlException (usually a SemanticException or EvaluationException) might be thrown in
            // .compile OR in .eval.  We currently don't make a distinction, so tests cannot assert that certain
            // errors are compile-time and others are evaluation-time.  We really aught to create a way for tests to
            // indicate when the exception should be thrown.  This is undone.
            val expression = compilerPipeline.compile(tc.query)

            // The call to .ionValue is important since query execution won't actually begin otherwise.
            expression.eval(session).ionValue
        }

        assertEquals(
            tc.expectedErrorCode,
            ex.errorCode,
            EvaluatorTestFailureReason.UNEXPECTED_ERROR_CODE
        ) { tc.testDetails(note, actualErrorCode = ex.errorCode.toString()) }

        if (tc.expectedErrorContext != null) {
            assertEquals(
                tc.expectedErrorContext,
                ex.errorContext,
                EvaluatorTestFailureReason.UNEXPECTED_ERROR_CONTEXT
            ) { tc.testDetails(note, actualErrorContext = ex.errorContext) }
        }

        if (tc.expectedInternalFlag != null) {
            assertEquals(
                tc.expectedInternalFlag,
                ex.internal,
                EvaluatorTestFailureReason.UNEXPECTED_INTERNAL_FLAG
            ) { tc.testDetails(note, actualInternalFlag = ex.internal) }
        }

        tc.additionalExceptionAssertBlock(ex)
    }

    override fun runEvaluatorErrorTestCase(tc: EvaluatorErrorTestCase, session: EvaluationSession) {
        if (tc.implicitPermissiveModeTest) {
            val testOpts = CompileOptions.build { tc.compileOptionsBuilderBlock(this) }
            assertNotEquals(
                TypingMode.PERMISSIVE,
                testOpts.typingMode,
                "Setting TypingMode.PERMISSIVE when implicit permissive mode testing is enabled is redundant"
            )
        }

        // Run the query once with compile options unmodified.
        privateRunEvaluatorErrorTestCase(
            tc = tc.copy(
                compileOptionsBuilderBlock = {
                    tc.compileOptionsBuilderBlock(this)
                    typingMode(TypingMode.LEGACY)
                }
            ),
            session = session,
            note = "Typing mode forced to LEGACY"
        )

        when (tc.expectedErrorCode.errorBehaviorInPermissiveMode) {
            ErrorBehaviorInPermissiveMode.THROW_EXCEPTION -> {
                // The expected error code indicates that this error should also throw in permissive mode.
                assertNull(
                    tc.expectedPermissiveModeResult,
                    "An expectedPermissiveModeResult must not be specified when " +
                        "ErrorCode.errorBehaviorInPermissiveMode is set to THROW_EXCEPTION"
                )

                // Run the query once in permissive mode.
                privateRunEvaluatorErrorTestCase(
                    tc.copy(
                        compileOptionsBuilderBlock = {
                            tc.compileOptionsBuilderBlock(this)
                            typingMode(TypingMode.PERMISSIVE)
                        }
                    ),
                    session,
                    note = "Typing mode forced to PERMISSIVE"
                )
            }
            ErrorBehaviorInPermissiveMode.RETURN_MISSING -> {

                // The expected error code indicates that this error should continue, but the expression should
                // return missing.
                assertNotNull(
                    tc.expectedPermissiveModeResult,
                    "Required non null expectedPermissiveModeResult when ErrorCode.errorBehaviorInPermissiveMode is " +
                        "set to ErrorBehaviorInPermissiveMode.RETURN_MISSING"
                )

                // Compute the expected return value
                val permissiveModePipeline = tc.createPipeline(forcePermissiveMode = true)

                val expectedExprValueForPermissiveMode =
                    assertDoesNotThrow(
                        EvaluatorTestFailureReason.FAILED_TO_EVALUATE_PARTIQL_EXPECTED_RESULT,
                        { tc.testDetails("Evaluating expected permissive mode result") }
                    ) {
                        permissiveModePipeline.compile(tc.expectedPermissiveModeResult!!).eval(session)
                    }

                val actualReturnValueForPermissiveMode =
                    assertDoesNotThrow(
                        EvaluatorTestFailureReason.FAILED_TO_EVALUATE_QUERY,
                        {
                            tc.testDetails(
                                "PERMISSIVE typing mode forced.  Query should throw in LEGACY mode but not in PERMISSIVE",
                                tc.expectedPermissiveModeResult!!
                            )
                        }
                    ) {
                        permissiveModePipeline.compile(tc.query).eval(session)
                    }

                if (!expectedExprValueForPermissiveMode.exprEquals(actualReturnValueForPermissiveMode)) {
                    throw EvaluatorAssertionFailedError(
                        EvaluatorTestFailureReason.UNEXPECTED_PERMISSIVE_MODE_RESULT,
                        tc.testDetails(
                            "PERMISSIVE typing mode forced.",
                            actualPermissiveModeResult = actualReturnValueForPermissiveMode.toString()
                        )
                    )
                }
            }
        }
    }
}
