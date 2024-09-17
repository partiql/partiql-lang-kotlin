package org.partiql.planner.internal.ddl

import org.junit.jupiter.api.Test
import org.partiql.parser.PartiQLParser
import org.partiql.plan.DdlOp
import org.partiql.plan.Statement
import org.partiql.plan.debug.PlanPrinter
import org.partiql.planner.PartiQLPlanner
import org.partiql.planner.PartiQLPlannerBuilder
import org.partiql.plugins.memory.MemoryCatalog
import org.partiql.plugins.memory.MemoryConnector
import org.partiql.spi.connector.ConnectorSession
import java.util.Random

class DDLTests {

    private val parser = PartiQLParser.default()
    private val planner = PartiQLPlannerBuilder().build()

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

    // Table Level Check constraint is nuked
//    @Test
//    fun sanity() {
//        val query = """
//            CREATE TABLE my_catalog.my_schema.tbl(
//                a INT2 PRIMARY KEY,
//                b INT2,
//                CHECK(a != b)
//            )
//        """.trimIndent()
//        val ast = parser.parse(query).root
//        val plan = planner
//            .plan(ast, plannerSession) {}
//            .plan
//        val res = buildString {
//            PlanPrinter.append(this, plan)
//        }
//        println(res)
//
//        val staticType =
//            ((plan.statement as Statement.DDL).op as DdlOp.CreateTable).shape
//        val expected = BagType(
//            StructType(
//                fields = listOf(
//                    StructType.Field("a", StaticType.INT2),
//                    StructType.Field("b", StaticType.INT2.asNullable()),
//                ),
//                contentClosed = true,
//                primaryKeyFields = emptyList(),
//                constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true)),
//                metas = mapOf(
//                    "check_constraints" to ionListOf(ionString("a <> b")),
//                )
//            ),
//            metas = mapOf(),
//            constraints = setOf(CollectionConstraint.PrimaryKey(setOf("a")))
//        )
//        assertEquals(expected, staticType)
//    }

    @Test
    fun sanity2() {
        val query = """
            CREATE TABLE foo.bar.Tbl_v2 (
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

    @Test
    fun sanity3() {
        val query = """
            CREATE TABLE foo.bar.my_table_V1 (
                ATTr1 VARCHAR(3),
                PRIMARY KEY (ATTr1)
            )
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
    fun sanity4() {
        val query = """
            CREATE TABLE foo.bar.my_table_V1 (
                ATTR1 VARCHAR(3),
                PRIMARY KEY ("ATTR1")
            )
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
    fun sanity5() {
        val query = """
            CREATE TABLE foo.bar.my_table_V1 (
                "ATTR1" VARCHAR(3),
                PRIMARY KEY ("ATTR1")
            )
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

    // The support for table level check constraint is nuked
//    @Test
//    fun sanity6() {
//        val query = """
//            CREATE TABLE foo.bar.my_table_V1 (
//                attr1 VARCHAR(3),
//                attr2 INT2,
//                attr3 STRUCT<attr4: INT>,
//                CHECK(attr3.attr4 >= 0)
//            )
//        """.trimIndent()
//
//        val ast = parser.parse(query).root
//        val plan = planner
//            .plan(ast, plannerSession) {}
//            .plan
//        val res = buildString {
//            PlanPrinter.append(this, plan)
//        }
//        println(res)
//
//        val staticType =
//            ((plan.statement as Statement.DDL).op as DdlOp.CreateTable).shape
//
//        println(staticType)
//    }

    @Test
    fun sanity7() {
        val query = """
            CREATE TABLE foo (
                A INT2,
                PRIMARY KEY(A) 
            )
        """.trimIndent()

        val ast = parser.parse(query).root
        val plan = planner
            .plan(ast, plannerSession) {}
            .plan
        val res = buildString {
            PlanPrinter.append(this, plan)
        }
        println(res)

        val staticType =
            ((plan.statement as Statement.DDL).op as DdlOp.CreateTable).shape

        println(staticType)
    }

    @Test
    fun sanity8() {
        val query = """
            ALTER TABLE FOO 
                CHANGE COLUMN foo TYPE INT2,
                CHANGE COLUMN foo COMMENT 'foo comment',
                CHANGE COLUMN foo SET NOT NULL,
                ADD COLUMN bar OPTIONAL INT2 NOT NULL COMMENT 'bla'
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
