package org.partiql.benchmarks.compiler

import org.partiql_v1_0_0_perf_1.lang.CompilerPipeline
import org.partiql_v1_0_0_perf_1.lang.eval.EvaluationSession
import org.partiql_v1_0_0_perf_1.lang.eval.PartiQLResult

class CompilerLegacy : Compiler {

    private val compiler = CompilerPipeline.builder().build()

    override fun compile(query: String): Iterable<Any> {
        val result = compiler.compile(query)
        val session = EvaluationSession.standard()
        val evaluationResult = result.evaluate(session)
        val value = evaluationResult as PartiQLResult.Value
        return value.value
    }
}