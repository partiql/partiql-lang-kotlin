package org.partiql.lang.eval.evaluatortestframework

import org.partiql.lang.eval.EvaluationSession

/**
 * Allows evaluates [EvaluatorTestCase] or [EvaluatorErrorTestCase] to be run against a specific
 * compiler/pipeline or perform other testing activities.  (See implementations of this interface for examples.)
 */
interface EvaluatorTestAdapter {
    /**
     * Runs an [EvaluatorTestCase].  This is intended to be used by parameterized tests.
     *
     * @see [EvaluatorTestCase].
     */
    fun runEvaluatorTestCase(tc: EvaluatorTestCase, session: EvaluationSession)

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
