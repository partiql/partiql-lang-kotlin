package org.partiql.benchmarks.compiler

import org.partiql_v1_0_0_perf_1.value.CollectionValue
import org.partiql_v1_0_0_perf_1.eval.PartiQLEngine
import org.partiql_v1_0_0_perf_1.eval.PartiQLResult
import org.partiql_v1_0_0_perf_1.parser.PartiQLParser
import org.partiql_v1_0_0_perf_1.planner.PartiQLPlanner
import org.partiql_v1_0_0_perf_1.plugins.memory.MemoryCatalog
import org.partiql_v1_0_0_perf_1.plugins.memory.MemoryConnector
import org.partiql_v1_0_0_perf_1.spi.connector.ConnectorSession
import org.partiql_v1_0_0_perf_1.value.PartiQLValueExperimental

class CompilerV1_0_0_Perf_1 : Compiler {

    private val parser = PartiQLParser.builder().build()

    private val planner = PartiQLPlanner.builder().build()

    private val evaluator = PartiQLEngine.builder().build()

    @OptIn(PartiQLValueExperimental::class, org.partiql.value.PartiQLValueExperimental::class)
    override fun compile(query: String): Iterable<Any> {
        val parseResult = parser.parse(query)
        val catalogName = "default"
        val catalog = MemoryCatalog.builder().name(catalogName)
        val connector = MemoryConnector(catalog.build())
        val queryId = "q"
        val userId = "u"
        val connectorSession = object : ConnectorSession {
            override fun getQueryId(): String = queryId
            override fun getUserId(): String = userId
        }
        val engineSession = PartiQLEngine.Session(mapOf(catalogName to connector))
        val plannerSession = PartiQLPlanner.Session(queryId, userId, catalogName, catalogs = mapOf(catalogName to connector.getMetadata(connectorSession)))
        val planResult = planner.plan(parseResult.root, plannerSession)
        val statement = evaluator.prepare(planResult.plan, engineSession)
        val value = evaluator.execute(statement) as PartiQLResult.Value
        return value.value as CollectionValue<*>
    }
}