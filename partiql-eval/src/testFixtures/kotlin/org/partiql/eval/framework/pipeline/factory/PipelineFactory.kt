package org.partiql.eval.framework.pipeline.factory

import org.partiql.eval.framework.EvaluatorTestTarget
import org.partiql.eval.framework.pipeline.AbstractPipeline
import org.partiql.eval.framework.testcase.EvaluatorTestDefinition
import org.partiql.lang.eval.EvaluationSession

/**
 * The implementation of this interface is passed to the constructor of [PipelineEvaluatorTestAdapter].  Determines
 * which pipeline (either [org.partiql.lang.CompilerPipeline] or [org.partiql.lang.compiler.PartiQLCompilerPipeline]) will be
 * tested.
 */
interface PipelineFactory {
    val pipelineName: String
    val target: EvaluatorTestTarget

    fun createPipeline(
        evaluatorTestDefinition: EvaluatorTestDefinition,
        session: EvaluationSession,
        forcePermissiveMode: Boolean = false
    ): AbstractPipeline
}
