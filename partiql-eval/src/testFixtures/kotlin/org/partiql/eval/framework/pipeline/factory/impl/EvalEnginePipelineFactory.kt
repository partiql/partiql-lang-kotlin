package org.partiql.eval.framework.pipeline.factory.impl

import org.partiql.eval.ION
import org.partiql.eval.PartiQLEngine
import org.partiql.eval.PartiQLResult
import org.partiql.eval.framework.EvaluatorTestTarget
import org.partiql.eval.framework.pipeline.AbstractPipeline
import org.partiql.eval.framework.pipeline.factory.PipelineFactory
import org.partiql.eval.framework.testcase.EvaluatorTestDefinition
import org.partiql.eval.plugin.MemoryCatalog
import org.partiql.eval.plugin.MemoryConnector
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.TypingMode
import org.partiql.parser.PartiQLParserBuilder
import org.partiql.planner.PartiQLPlanner
import org.partiql.planner.PartiQLPlannerBuilder
import org.partiql.spi.connector.ConnectorSession
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.io.PartiQLValueIonWriterBuilder
import java.io.ByteArrayOutputStream

class EvalEnginePipelineFactory : PipelineFactory {
    override val pipelineName: String = "Eval Engine Pipeline"
    override val target: EvaluatorTestTarget = EvaluatorTestTarget.EVAL_ENGINE_PIPELINE

    @OptIn(PartiQLValueExperimental::class)
    override fun createPipeline(
        evaluatorTestDefinition: EvaluatorTestDefinition,
        session: EvaluationSession,
        forcePermissiveMode: Boolean
    ): AbstractPipeline {
        // Construct a legacy CompilerPipeline
        val legacyPipeline = evaluatorTestDefinition.createCompilerPipeline(forcePermissiveMode)
        val co = legacyPipeline.compileOptions

        val parser = PartiQLParserBuilder().build()
        val planner = PartiQLPlannerBuilder().build()
        val engine = PartiQLEngine.builder().build()
        val catalog = MemoryCatalog.builder().name("default").binding(session.globals).build()

        val connector = MemoryConnector(catalog)
        val plannerSession = PartiQLPlanner.Session(
            queryId = "query",
            userId = "user",
            currentCatalog = "default",
            catalogs = mapOf(
                "default" to connector.getMetadata(object : ConnectorSession {
                    override fun getQueryId(): String = "query"
                    override fun getUserId(): String = "user"
                })
            )
        )

        val mode = when (co.typingMode) {
            TypingMode.LEGACY -> PartiQLEngine.Mode.STRICT
            TypingMode.PERMISSIVE -> PartiQLEngine.Mode.PERMISSIVE
        }

        val engineSession = PartiQLEngine.Session(
            catalogs = mutableMapOf(
                "default" to connector
            ),
            mode = mode
        )

        return object : AbstractPipeline {
            override val typingMode: TypingMode = co.typingMode

            override fun evaluate(query: String): ExprValue {
                val statement = parser.parse(evaluatorTestDefinition.query).root
                val plan = planner.plan(statement, plannerSession).plan
                val executable = engine.prepare(plan, engineSession)
                val partiqlValue = when (val res = engine.execute(executable)) {
                    is PartiQLResult.Error -> throw res.cause
                    is PartiQLResult.Value -> res.value
                }
                val bufferForPartiQL = ByteArrayOutputStream()
                val writer = PartiQLValueIonWriterBuilder.standardIonTextBuilder().build(bufferForPartiQL)
                writer.append(partiqlValue)
                val ionText = bufferForPartiQL.toString()

                return ExprValue.of(ION.singleValue(ionText))
            }
        }
    }
}
