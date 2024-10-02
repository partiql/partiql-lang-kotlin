package org.partiql.planner

import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.DynamicContainer.dynamicContainer
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.partiql.parser.PartiQLParser
import org.partiql.planner.internal.TestCatalog
import org.partiql.planner.test.PartiQLTest
import org.partiql.planner.test.PartiQLTestProvider
import org.partiql.planner.util.ProblemCollector
import org.partiql.spi.catalog.Catalog
import org.partiql.spi.catalog.Name
import org.partiql.spi.catalog.Session
import org.partiql.types.BagType
import org.partiql.types.PType
import org.partiql.types.StaticType
import org.partiql.types.StructType
import org.partiql.types.TupleConstraint
import java.io.File
import java.nio.file.Path
import java.util.stream.Stream
import kotlin.io.path.toPath
import kotlin.test.assertEquals

// Prevent Unintentional break of the plan
// We currently don't have a good way to assert on the result plan
// so we assert on having the partiql text.
// The input text and the normalized partiql text should produce identical plan.
// I.e.,
// if the input text is `SELECT a,b,c FROM T`
// the produced plan will be identical as the normalized query:
// `SELECT "T"['a'] AS "a", "T"['b'] AS "b", "T"['c'] AS "c" FROM "default"."T" AS "T";`
class PlanTest {
    val root: Path = this::class.java.getResource("/outputs")!!.toURI().toPath()

    val input = PartiQLTestProvider().apply { load() }

    val type = BagType(
        StructType(
            listOf(
                StructType.Field("a", StaticType.BOOL),
                StructType.Field("b", StaticType.INT4),
                StructType.Field("c", StaticType.STRING),
                StructType.Field(
                    "d",
                    StructType(
                        listOf(StructType.Field("e", StaticType.STRING)),
                        contentClosed = true,
                        emptyList(),
                        setOf(TupleConstraint.Open(false)),
                        emptyMap()
                    )
                ),
                StructType.Field("x", StaticType.ANY),
                StructType.Field("z", StaticType.STRING),
                StructType.Field("v", StaticType.STRING),
            ),
            contentClosed = true,
            emptyList(),
            setOf(TupleConstraint.Open(false)),
            emptyMap()
        )
    )

    val pipeline: (PartiQLTest, Boolean) -> PartiQLPlanner.Result = { test, isSignalMode ->
        val session = Session.builder()
            .catalog("default")
            .catalogs(buildCatalog("default"))
            .namespace("SCHEMA")
            .build()
        val problemCollector = ProblemCollector()
        val ast = PartiQLParser.standard().parse(test.statement).root
        val planner = PartiQLPlanner.builder().signal(isSignalMode).build()
        planner.plan(ast, session, problemCollector)
    }

    private fun buildCatalog(catalogName: String): Catalog {
        return TestCatalog.builder()
            .name(catalogName)
            .createTable(Name.of("SCHEMA", "T"), PType.fromStaticType(type))
            .build()
    }

    @TestFactory
    fun factory(): Stream<DynamicNode> {
        val r = root.toFile()
        return r
            .listFiles { f -> f.isDirectory }!!
            .mapNotNull { load(r, it) }
            .stream()
    }

    private fun load(parent: File, file: File): DynamicNode? = when {
        file.isDirectory -> loadD(parent, file)
        file.extension == "sql" -> loadF(parent, file)
        else -> null
    }

    private fun loadD(parent: File, file: File): DynamicContainer {
        val name = file.name
        val children = file.listFiles()!!.map { load(file, it) }
        return dynamicContainer(name, children)
    }

    private fun loadF(parent: File, file: File): DynamicContainer {
        val group = parent.name
        val tests = parse(group, file)

        val children = tests.map { test ->
            // Prepare
            val displayName = test.key.toString()

            // Assert
            DynamicTest.dynamicTest(displayName) {
                val input = input[test.key] ?: error("no test cases")
                val originalQuery = input
                val normalizedQuery = test
                listOf(true, false).forEach { isSignal ->
                    val inputPlan = pipeline.invoke(originalQuery, isSignal).plan
                    val outputPlan = pipeline.invoke(normalizedQuery, isSignal).plan
                    assertPlanEqual(inputPlan, outputPlan)
                }
            }
        }
        return dynamicContainer(file.nameWithoutExtension, children)
    }

    private fun assertPlanEqual(inputPlan: org.partiql.plan.Plan, outputPlan: org.partiql.plan.Plan) {
        val iStatement = inputPlan.getOperation()
        val oStatement = outputPlan.getOperation()
        assertEquals(iStatement, oStatement)
        // assert(inputPlan.isEquaivalentTo(outputPlan)) {
        //     buildString {
        //         this.appendLine("expect plan equivalence")
        //         PlanPrinter.append(this, inputPlan)
        //         PlanPrinter.append(this, outputPlan)
        //     }
        // }
    }

    private fun parse(group: String, file: File): List<PartiQLTest> {
        val tests = mutableListOf<PartiQLTest>()
        var name = ""
        val statement = StringBuilder()
        for (line in file.readLines()) {
            // start of test
            if (line.startsWith("--#[") and line.endsWith("]")) {
                name = line.substring(4, line.length - 1)
                statement.clear()
            }
            if (name.isNotEmpty() && line.isNotBlank()) {
                // accumulating test statement
                statement.appendLine(line)
            } else {
                // skip these lines
                continue
            }
            // Finish & Reset
            if (line.endsWith(";")) {
                val key = PartiQLTest.Key(group, name)
                tests.add(PartiQLTest(key, statement.toString()))
                name = ""
                statement.clear()
            }
        }
        return tests
    }
}
