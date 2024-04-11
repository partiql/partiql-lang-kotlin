package org.partiql.planner.internal.ddl

import org.junit.jupiter.api.Test
import org.partiql.parser.PartiQLParser
import org.partiql.plan.debug.PlanPrinter
import org.partiql.planner.PartiQLPlanner
import org.partiql.plugins.memory.MemoryConnector
import org.partiql.spi.connector.ConnectorSession
import org.partiql.types.StaticType
import java.util.Random

class DdlTests {

    private val parser = PartiQLParser.default()
    private val planner = PartiQLPlanner.default()

    private val catalogName = "TEST"
    private val metadata = MemoryConnector.Metadata.of(
        "global" to StaticType.INT2
    )
    private val connector = MemoryConnector(metadata)

    private val connectorSession = object : ConnectorSession {
        override fun getQueryId(): String = "q"
        override fun getUserId(): String = "u"
    }

    val plannerSession = PartiQLPlanner.Session(
        queryId = Random().nextInt().toString(),
        userId = "test-user",
        currentCatalog = catalogName,
        currentDirectory = listOf(),
        catalogs = mapOf(
            catalogName to metadata
        ),
    )

    @Test
    fun sanity() {
        val query = """
            CREATE TABLE my_catalog.my_schema.tbl(
                a INT2 PRIMARY KEY, 
                CHECK(a > 0)
            ) PARTITION BY (a)
            TBLPROPERTIES ('my_property1' = 'my_value1')
        """.trimIndent()
        val ast = parser.parse(query).root
        val plan = planner
            .plan(ast, plannerSession) {}
            .plan
        val res = buildString {
            PlanPrinter.append(this, plan)
        }
        println(res)
    }

    @Test
    fun sanity2() {
        val query = """
            CREATE TABLE andes."PROVIDER_NAME".TABLE_NAME_v3
                        (
                            "COLUMN_ONE" CHAR(20) COMMENT 'Comment one',
                            "COLUMN_TWO" CHAR(15) COMMENT 'Comment two',
                            "COLUMN_THREE" TIMESTAMP(0) COMMENT 'Comment three'
                        )
                        PARTITION BY (COLUMN_ONE)
                        TBLPROPERTIES (
                            'DATAPLANE' = 'Cairns',
                            'PARTITION_TYPE' = 'APPEND',
                            'CONTENT_TYPE' = 'application/parquet'
                        );
        """.trimIndent()
        val ast = parser.parse(query).root
        val plan = planner
            .plan(ast, plannerSession) {}
            .plan
        val res = buildString {
            PlanPrinter.append(this, plan)
        }
        println(res)
    }
}


data class Foo(
    @JvmField val a : Int,
    @JvmField val b : Int = 2
) {
    fun copy(a : Int) = copy(a, b)
}
