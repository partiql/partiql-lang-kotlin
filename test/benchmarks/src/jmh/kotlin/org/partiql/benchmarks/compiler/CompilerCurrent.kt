package org.partiql.benchmarks.compiler

import org.partiql.eval.PartiQLEngine
import org.partiql.eval.PartiQLResult
import org.partiql.planner.catalog.Session
import org.partiql.plugins.memory.MemoryCatalog
import org.partiql.plugins.memory.MemoryConnector
import org.partiql.spi.connector.ConnectorSession
import org.partiql.value.CollectionValue
import org.partiql.value.PartiQLValueExperimental

class CompilerCurrent : Compiler {

    private val parser = org.partiql.parser.PartiQLParser.builder().build()

    private val planner = org.partiql.planner.PartiQLPlanner.builder().build()

    private val evaluator = PartiQLEngine.builder().build()

    @OptIn(PartiQLValueExperimental::class)
    override fun compile(query: String): Iterable<Any> {
        val parseResult = parser.parse(query)
        val catalogName = "default"
        val catalog = MemoryCatalog.builder().name(catalogName)
        val connector = MemoryConnector(catalog.build())
        val connectorSession = object : ConnectorSession {
            override fun getQueryId(): String = "q"
            override fun getUserId(): String = "u"
        }
        val engineSession = PartiQLEngine.Session(mapOf(catalogName to connector))
        val plannerSession = Session.builder().catalog(catalogName).catalogs(catalogName to connector.getMetadata(connectorSession)).build()
        val planResult = planner.plan(parseResult.root, plannerSession)
        val statement = evaluator.prepare(planResult.plan, engineSession)
        val value = evaluator.execute(statement) as PartiQLResult.Value
        return value.value as CollectionValue<*>
    }
}