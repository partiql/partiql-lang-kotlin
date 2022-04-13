package org.partiql.lang.eval.test

import org.partiql.lang.CompilerPipeline
import org.partiql.lang.eval.EvaluationSession

/**
 * Allows evaluates [EvaluatorTestCase] or [EvaluatorErrorTestCase] to be run against a specific
 * compiler/pipeline.
 *
 * There is currently only one implementation of this interface: [AstEvaluatorTestAdapter]
 * which evaluates [EvaluatorTestCase] or [EvaluatorErrorTestCase] with the AST evaluator
 * (i.e [CompilerPipeline]/[org.partiql.lang.eval.EvaluatingCompiler] combination.
 *
 * Future implementations of [EvaluatorTestAdapter] will:
 *
 * - Tests the query planner and physical plan compiler instead.
 * - [EvaluatorTestAdapter] that delegates multiple other implementations, allowing multiple implementations to be
 * treated as one.
 */
interface EvaluatorTestAdapter {
    /**
     * Runs an [EvaluatorTestCase].  This is intended to be used by parameterized tests.
     *
     * Also runs additional assertions performed by [commonAssertions].
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
