package org.partiql.lang.eval.evaluatortestframework

/**
 * The implementation of this interface passed to the constructor of [PipelineEvaluatorTestAdapter].  Determines
 * which pipeline (either [org.partiql.lang.CompilerPipeline] or [org.partiql.lang.planner.PlannerPipeline]) will be
 * tested.
 */
internal interface PipelineFactory {
    val pipelineName: String
    val target: EvaluatorTestTarget

    fun createPipeline(
        evaluatorTestDefinition: EvaluatorTestDefinition,
        forcePermissiveMode: Boolean = false
    ): AbstractPipeline
}
