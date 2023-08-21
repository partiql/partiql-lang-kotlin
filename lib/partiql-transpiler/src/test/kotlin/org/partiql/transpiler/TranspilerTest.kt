package org.partiql.transpiler

import com.amazon.ionelement.api.field
import com.amazon.ionelement.api.ionString
import com.amazon.ionelement.api.ionStructOf
import org.junit.jupiter.api.Test
import org.partiql.annotations.ExperimentalPartiQLSchemaInferencer
import org.partiql.lang.errors.ProblemCollector
import org.partiql.lang.planner.transforms.PartiQLSchemaInferencer
import org.partiql.lang.planner.transforms.PlannerSession
import org.partiql.plugins.mockdb.LocalPlugin
import org.partiql.transpiler.targets.PartiQLTarget
import java.net.URL
import java.time.Instant

@OptIn(ExperimentalPartiQLSchemaInferencer::class)
class TranspilerTest {

    /**
     * COPIED FROM PartiQLSchemaInferencerTests
     */
    companion object {
        private val PLUGINS = listOf(LocalPlugin())

        private const val USER_ID = "TEST_USER"
        private val CATALOG_MAP = listOf("aws", "b", "db").associateWith { catalogName ->
            val catalogUrl: URL =
                TranspilerTest::class.java.classLoader.getResource("catalogs/$catalogName")
                    ?: error("Couldn't be found")
            ionStructOf(
                field("connector_name", ionString("localdb")),
                // field("localdb_root", ionString(catalogUrl.path))
            )
        }
    }

    //
    private fun ctx(queryId: String, catalog: String, path: List<String> = emptyList()): PartiQLSchemaInferencer.Context {
        val session = PlannerSession(
            queryId,
            USER_ID,
            catalog,
            path,
            CATALOG_MAP,
            Instant.now(),
        )
        val collector = ProblemCollector()
        return PartiQLSchemaInferencer.Context(session, PLUGINS, collector)
    }

    @Test
    fun transpile() {
        val query = "SELECT id, breed FROM pets"
        val target = PartiQLTarget
        val context = ctx("test-query", "aws", listOf("ddb"))
        val transpiler = PartiQLTranspiler(target, context)
        //
        val result = transpiler.transpile(query)
        for (p in result.problems) {
            println("ERR! $p")
        }
        println(result.sql)
    }
}
