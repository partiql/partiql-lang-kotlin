package org.partiql.lang.eval.evaluatortestframework

import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.partiql.lang.ION
import org.partiql.lang.errors.ErrorBehaviorInPermissiveMode
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.TypingMode
import org.partiql.lang.eval.cloneAndRemoveBagAndMissingAnnotations
import org.partiql.lang.eval.exprEquals
import org.partiql.lang.eval.toIonValue

internal class PipelineEvaluatorTestAdapter(
    private val pipelineFactory: PipelineFactory
) : EvaluatorTestAdapter {

    override fun runEvaluatorTestCase(tc: EvaluatorTestCase, session: EvaluationSession) {
        // Skip execution of this test case if it does not apply to the pipeline supplied by pipelineFactory.
        if (tc.targetPipeline != EvaluatorTestTarget.ALL_PIPELINES && pipelineFactory.target != tc.targetPipeline) {
            return
        }
        checkRedundantPermissiveMode(tc)

        // Compile options unmodified... This covers [TypingMode.LEGACY], unless the test explicitly
        // sets the typing mode.
        privateRunEvaluatorTestCase(tc, session, "${pipelineFactory.pipelineName} (compile options unaltered)")

        // Unless the test disables it, run again in permissive mode.
        if (tc.implicitPermissiveModeTest) {
            privateRunEvaluatorTestCase(
                tc.copy(
                    compileOptionsBuilderBlock = {
                        tc.compileOptionsBuilderBlock(this)
                        typingMode(TypingMode.PERMISSIVE)
                    }
                ),
                session,
                "${pipelineFactory.pipelineName} (compile options forced to PERMISSIVE mode)"
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
        val pipeline = pipelineFactory.createPipeline(tc, session)

        val actualExprValueResult: ExprValue = assertDoesNotThrow(
            EvaluatorTestFailureReason.FAILED_TO_EVALUATE_QUERY,
            { tc.testDetails(note = note) }
        ) {
            pipeline.evaluate(tc.query)
        }

        val (expectedResult, unexpectedResultErrorCode) =
            when (pipeline.typingMode) {
                TypingMode.LEGACY -> tc.expectedResult to EvaluatorTestFailureReason.UNEXPECTED_QUERY_RESULT
                TypingMode.PERMISSIVE -> tc.expectedPermissiveModeResult to EvaluatorTestFailureReason.UNEXPECTED_PERMISSIVE_MODE_RESULT
            }

        when (tc.expectedResultFormat) {
            ExpectedResultFormat.ION, ExpectedResultFormat.ION_WITHOUT_BAG_AND_MISSING_ANNOTATIONS -> {
                val expectedIonResult = assertDoesNotThrow(
                    EvaluatorTestFailureReason.FAILED_TO_PARSE_ION_EXPECTED_RESULT,
                    { tc.testDetails(note = note) }
                ) {
                    ION.singleValue(expectedResult)
                }

                val actualIonResult = actualExprValueResult.toIonValue(ION).let {
                    if (tc.expectedResultFormat == ExpectedResultFormat.ION_WITHOUT_BAG_AND_MISSING_ANNOTATIONS)
                        it.cloneAndRemoveBagAndMissingAnnotations()
                    else
                        it
                }
                assertEquals(
                    expectedIonResult,
                    actualIonResult,
                    unexpectedResultErrorCode
                ) { tc.testDetails(note = note, actualResult = actualIonResult.toString()) }
            }
            ExpectedResultFormat.PARTIQL -> {
                val expectedExprValueResult = assertDoesNotThrow(
                    EvaluatorTestFailureReason.FAILED_TO_EVALUATE_PARTIQL_EXPECTED_RESULT,
                    { tc.testDetails(note = note) }
                ) {
                    pipeline.evaluate(expectedResult)
                }

                if (!expectedExprValueResult.exprEquals(actualExprValueResult)) {

                    println("I am here")

                    throw EvaluatorAssertionFailedError(
                        EvaluatorTestFailureReason.UNEXPECTED_QUERY_RESULT,
                        tc.testDetails(note = note, actualResult = actualExprValueResult.toString())
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
                ) { tc.testDetails(note = note, actualResult = actualResultString) }
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
        val pipeline = pipelineFactory.createPipeline(tc, session)

        val ex = assertThrowsSqlException(
            EvaluatorTestFailureReason.EXPECTED_SQL_EXCEPTION_BUT_THERE_WAS_NONE,
            { tc.testDetails(note = note) }
        ) {

            // Note that an SqlException (usually a SemanticException or EvaluationException) might be thrown in
            // .compile OR in .eval.  We currently don't make a distinction, so tests cannot assert that certain
            // errors are compile-time and others are evaluation-time.  We really aught to create a way for tests to
            // indicate when the exception should be thrown.  This is undone.
            // The call to .ionValue below is important since query execution won't actually begin otherwise.
            pipeline.evaluate(tc.query).toIonValue(ION)
        }

        assertEquals(
            tc.expectedErrorCode,
            ex.errorCode,
            EvaluatorTestFailureReason.UNEXPECTED_ERROR_CODE
        ) { tc.testDetails(note = note, actualErrorCode = ex.errorCode) }

        if (tc.expectedErrorContext != null) {
            assertEquals(
                tc.expectedErrorContext,
                ex.errorContext,
                EvaluatorTestFailureReason.UNEXPECTED_ERROR_CONTEXT
            ) { tc.testDetails(note = note, actualErrorContext = ex.errorContext) }
        }

        if (tc.expectedInternalFlag != null) {
            assertEquals(
                tc.expectedInternalFlag,
                ex.internal,
                EvaluatorTestFailureReason.UNEXPECTED_INTERNAL_FLAG
            ) { tc.testDetails(note = note, actualInternalFlag = ex.internal) }
        }

        tc.additionalExceptionAssertBlock(ex)
    }

    override fun runEvaluatorErrorTestCase(tc: EvaluatorErrorTestCase, session: EvaluationSession) {
        // Skip execution of this test case if it does not apply to the pipeline supplied by pipelineFactory.
        if (tc.targetPipeline != EvaluatorTestTarget.ALL_PIPELINES && pipelineFactory.target != tc.targetPipeline) {
            return
        }

        checkRedundantPermissiveMode(tc)

        // Run the query once with compile options unmodified.
        privateRunEvaluatorErrorTestCase(
            tc = tc.copy(
                compileOptionsBuilderBlock = {
                    tc.compileOptionsBuilderBlock(this)
                    typingMode(TypingMode.LEGACY)
                }
            ),
            session = session,
            note = "${pipelineFactory.pipelineName} (Typing mode forced to LEGACY)"
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
                    note = "${pipelineFactory.pipelineName} (typing mode forced to PERMISSIVE)"
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
                val permissiveModePipeline = pipelineFactory.createPipeline(evaluatorTestDefinition = tc, session, forcePermissiveMode = true)

                val expectedExprValueForPermissiveMode =
                    assertDoesNotThrow(
                        EvaluatorTestFailureReason.FAILED_TO_EVALUATE_PARTIQL_EXPECTED_RESULT,
                        { tc.testDetails(note = "Evaluating expected permissive mode result") }
                    ) {
                        permissiveModePipeline.evaluate(tc.expectedPermissiveModeResult!!)
                    }

                val actualReturnValueForPermissiveMode =
                    assertDoesNotThrow(
                        EvaluatorTestFailureReason.FAILED_TO_EVALUATE_QUERY,
                        {
                            tc.testDetails(
                                note = "PERMISSIVE typing mode forced.  Query should throw in LEGACY mode but not in PERMISSIVE",
                            )
                        }
                    ) {
                        permissiveModePipeline.evaluate(tc.query)
                    }

                if (!expectedExprValueForPermissiveMode.exprEquals(actualReturnValueForPermissiveMode)) {
                    throw EvaluatorAssertionFailedError(
                        EvaluatorTestFailureReason.UNEXPECTED_PERMISSIVE_MODE_RESULT,
                        tc.testDetails(
                            note = "PERMISSIVE typing mode forced.",
                            actualPermissiveModeResult = actualReturnValueForPermissiveMode.toString()
                        )
                    )
                }
            }
        }
    }

    private fun checkRedundantPermissiveMode(tc: EvaluatorTestDefinition) {
        if (tc.implicitPermissiveModeTest) {
            val pipeline = pipelineFactory.createPipeline(tc, EvaluationSession.standard())
            assertNotEquals(
                TypingMode.PERMISSIVE,
                pipeline.typingMode,
                "Setting TypingMode.PERMISSIVE when implicit permissive mode testing is enabled is redundant"
            )
        }
    }
}
