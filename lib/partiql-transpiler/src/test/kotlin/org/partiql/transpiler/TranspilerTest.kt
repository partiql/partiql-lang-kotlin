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
import org.partiql.types.BagType
import org.partiql.types.StaticType
import org.partiql.types.StructType
import org.partiql.types.TupleConstraint
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
                field("localdb_root", ionString(catalogUrl.path))
            )
        }
        const val CATALOG_AWS = "aws"
        const val CATALOG_B = "b"
        const val CATALOG_DB = "db"
        val DB_SCHEMA_MARKETS = listOf("markets")

        val TYPE_BOOL = StaticType.BOOL
        private val TYPE_AWS_DDB_PETS_ID = StaticType.INT
        private val TYPE_AWS_DDB_PETS_BREED = StaticType.STRING
        val TABLE_AWS_DDB_PETS = BagType(
            elementType = StructType(
                fields = mapOf(
                    "id" to TYPE_AWS_DDB_PETS_ID,
                    "breed" to TYPE_AWS_DDB_PETS_BREED
                ),
                contentClosed = true,
                constraints = setOf(
                    TupleConstraint.Open(false),
                    TupleConstraint.UniqueAttrs(true),
                    TupleConstraint.Ordered
                )
            )
        )
        val TABLE_AWS_DDB_B = BagType(
            StructType(
                fields = mapOf("identifier" to StaticType.STRING),
                contentClosed = true,
                constraints = setOf(
                    TupleConstraint.Open(false),
                    TupleConstraint.UniqueAttrs(true),
                    TupleConstraint.Ordered
                )
            )
        )
        val TABLE_AWS_B_B = BagType(
            StructType(
                fields = mapOf("identifier" to StaticType.INT),
                contentClosed = true,
                constraints = setOf(
                    TupleConstraint.Open(false),
                    TupleConstraint.UniqueAttrs(true),
                    TupleConstraint.Ordered
                )
            )
        )
        val TYPE_B_B_B_B_B = StaticType.INT
        private val TYPE_B_B_B_B = StructType(
            mapOf("b" to TYPE_B_B_B_B_B),
            contentClosed = true,
            constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true), TupleConstraint.Ordered)
        )
        val TYPE_B_B_B_C = StaticType.INT
        val TYPE_B_B_C = StaticType.INT
        val TYPE_B_B_B =
            StructType(
                fields = mapOf(
                    "b" to TYPE_B_B_B_B,
                    "c" to TYPE_B_B_B_C
                ),
                contentClosed = true,
                constraints = setOf(
                    TupleConstraint.Open(false),
                    TupleConstraint.UniqueAttrs(true),
                    TupleConstraint.Ordered
                )
            )
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
        val transpiler = Transpiler(target, context)
        //
        val result = transpiler.transpile(query)
        for (p in result.problems) {
            println("ERR! $p")
        }
        println(result.sql)
    }
}
