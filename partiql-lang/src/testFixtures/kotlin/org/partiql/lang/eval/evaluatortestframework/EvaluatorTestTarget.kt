package org.partiql.lang.eval.evaluatortestframework

/**
 * An indicator of which pipeline(s) each test case should be run against.  Useful when one pipeline supports
 * a feature that the other one doesn't.
 */
enum class EvaluatorTestTarget {
    /**
     * Run the test on all pipelines.
     *
     * Set this option when both pipelines support all features utilized in the test case.
     * */
    ALL_PIPELINES,

    /**
     * Run the test only on [org.partiql.lang.CompilerPipeline]. Set this when the test case covers features not yet
     * supported by [org.partiql.lang.compiler.PartiQLCompilerPipeline] or when testing features unique to the former.
     */
    COMPILER_PIPELINE,

    /**
     * Run the test on [org.partiql.lang.compiler.PartiQLCompilerPipeline]. Set this when the test case covers features not
     * supported by [org.partiql.lang.CompilerPipeline], or when testing features unique to the former.
     *
     * Since [org.partiql.lang.compiler.PartiQLCompilerPipeline] is deprecated and will be removed in favor of
     * [org.partiql.lang.compiler.PartiQLCompilerPipelineAsync], opt to use [PARTIQL_PIPELINE_ASYNC] or [ALL_PIPELINES].
     */
    PARTIQL_PIPELINE,

    /**
     * Run the test on [org.partiql.lang.compiler.PartiQLCompilerPipelineAsync]. Set this when the test case covers
     * features not supported by [org.partiql.lang.CompilerPipeline], or when testing features unique to the former.
     *
     * This is the async version of [PARTIQL_PIPELINE].
     */
    PARTIQL_PIPELINE_ASYNC
}
