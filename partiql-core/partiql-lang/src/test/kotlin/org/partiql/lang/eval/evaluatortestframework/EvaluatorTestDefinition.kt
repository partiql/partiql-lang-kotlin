package org.partiql.lang.eval.evaluatortestframework

import org.partiql.lang.CompilerPipeline
import org.partiql.lang.eval.CompileOptions

/**
 * Defines fields common to both [EvaluatorTestCase] and [EvaluatorErrorTestCase]
 */
interface EvaluatorTestDefinition {
    /** The "group" of the tests--this only appears in the IDE's test runner and can be used to identify where in the
     * source code the test is defined.
     */
    val groupName: String?

    /**
     * The query to be evaluated.
     */
    val query: String

    /**
     * Expected result in the permissive mode. Default value is null.
     *
     * Since the expression with the error isn't always the top-most in the test case's query, value returned by the
     * query may not be `MISSING`, but rather it might be a container with `MISSING` somewhere in it.  Thus, we cannot
     * always assume the result will be `MISSING`.
     */
    val expectedPermissiveModeResult: String?

    /**
     * Include permissive mode test.
     */
    val implicitPermissiveModeTest: Boolean

    /**
     * Determines which pipeline this test should run against; the [CompilerPipeline],
     * [org.partiql.lang.compiler.PartiQLCompilerPipeline] or both.
     */
    val targetPipeline: EvaluatorTestTarget

    /**
     * Builder block for building [CompileOptions].
     */
    val compileOptionsBuilderBlock: CompileOptions.Builder.() -> Unit

    /**
     * Allows each test to configure its pipeline.
     */
    val compilerPipelineBuilderBlock: CompilerPipeline.Builder.() -> Unit
}
