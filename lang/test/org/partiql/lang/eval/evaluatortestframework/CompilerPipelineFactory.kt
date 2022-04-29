package org.partiql.lang.eval.evaluatortestframework

import org.partiql.lang.CompilerPipeline
import org.partiql.lang.ION
import org.partiql.lang.eval.CompileOptions
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.TypingMode

internal class CompilerPipelineFactory : PipelineFactory {
    override val pipelineName: String
        get() = "CompilerPipeline (AST Evaluator)"

    override val target: EvaluatorTestTarget
        get() = EvaluatorTestTarget.COMPILER_PIPELINE

    override fun createPipeline(
        evaluatorTestDefinition: EvaluatorTestDefinition,
        session: EvaluationSession,
        forcePermissiveMode: Boolean
    ): AbstractPipeline {
        val concretePipeline = evaluatorTestDefinition.createCompilerPipeline(forcePermissiveMode)

        return object : AbstractPipeline {
            override val typingMode: TypingMode
                get() = concretePipeline.compileOptions.typingMode

            override fun evaluate(query: String): ExprValue =
                concretePipeline.compile(query).eval(session)
        }
    }
}

internal fun EvaluatorTestDefinition.createCompilerPipeline(forcePermissiveMode: Boolean): CompilerPipeline {

    val compileOptions = CompileOptions.build(compileOptionsBuilderBlock).let { co ->
        if (forcePermissiveMode) {
            CompileOptions.build(co) {
                typingMode(TypingMode.PERMISSIVE)
            }
        } else {
            co
        }
    }

    val concretePipeline = CompilerPipeline.build(ION) {
        compileOptions(compileOptions)
        this@createCompilerPipeline.compilerPipelineBuilderBlock(this)
    }
    return concretePipeline
}
