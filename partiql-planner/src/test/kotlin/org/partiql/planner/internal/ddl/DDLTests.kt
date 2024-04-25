package org.partiql.planner.internal.ddl

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.partiql.parser.PartiQLParser
import org.partiql.plan.debug.PlanPrinter
import org.partiql.planner.PartiQLPlanner
import org.partiql.plugins.memory.MemoryCatalog
import org.partiql.plugins.memory.MemoryConnector
import org.partiql.spi.connector.ConnectorSession
import java.util.Random

class DDLTests {

    private val parser = PartiQLParser.default()
    private val planner = PartiQLPlanner.default()

    private val catalogName = "TEST"
    private val catalog = MemoryCatalog.PartiQL().name(catalogName).build()
    private val connector = MemoryConnector(catalog)

    private val connectorSession = object : ConnectorSession {
        override fun getQueryId(): String = "q"
        override fun getUserId(): String = "u"
    }

    private val metadata = connector.getMetadata(connectorSession)

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
        assertThrows<NotImplementedError> {
            val query = """
            CREATE TABLE my_catalog.my_schema.tbl(
                a INT2 PRIMARY KEY, 
                CHECK(a != b)
            ) PARTITION BY (b)
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
    }

    @Test
    fun sanity2() {
        val query = """
            CREATE TABLE andes.myProvider.Tbl_v2 (
                "COLUMN_ONE" INT2 CHECK("COLUMN_ONE" > 0) COMMENT 'Comment one',
                "COLUMN_TWO" CHAR(15) COMMENT 'Comment two',
                "COLUMN_THREE" TIMESTAMP(0) COMMENT 'Comment three'
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
