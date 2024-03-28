package org.partiql.planner

import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.DynamicContainer.dynamicContainer
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.partiql.parser.PartiQLParser
import org.partiql.plan.PartiQLPlan
import org.partiql.plan.PlanNode
import org.partiql.plan.debug.PlanPrinter
import org.partiql.planner.test.PartiQLTest
import org.partiql.planner.test.PartiQLTestProvider
import org.partiql.planner.util.PlanNodeEquivalentVisitor
import org.partiql.planner.util.ProblemCollector
import org.partiql.plugins.memory.MemoryCatalog
import org.partiql.plugins.memory.MemoryConnector
import org.partiql.plugins.memory.MemoryObject
import org.partiql.spi.BindingCase
import org.partiql.spi.BindingName
import org.partiql.spi.BindingPath
import org.partiql.spi.connector.ConnectorMetadata
import org.partiql.spi.connector.ConnectorSession
import org.partiql.types.BagType
import org.partiql.types.StaticType
import org.partiql.types.StructType
import org.partiql.types.TupleConstraint
import java.io.File
import java.nio.file.Path
import java.time.Instant
import java.util.stream.Stream
import kotlin.io.path.toPath

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

    val connectorSession = object : ConnectorSession {
        override fun getQueryId(): String = "query-id"
        override fun getUserId(): String = "user-id"
    }

    val pipeline: (PartiQLTest, PartiQLPlanner.Session.MissingOpBehavior) -> PartiQLPlanner.Result = { test, missingOpBehaivor ->
        val session = PartiQLPlanner.Session(
            queryId = test.key.toString(),
            userId = "user_id",
            currentCatalog = "default",
            currentDirectory = listOf("SCHEMA"),
            catalogs = mapOf("default" to buildMetadata("default")),
            instant = Instant.now(),
            missingOpBehavior = missingOpBehaivor
        )
        val problemCollector = ProblemCollector()
        val ast = PartiQLParser.default().parse(test.statement).root
        val planner = PartiQLPlanner.default()
        planner.plan(ast, session, problemCollector)
    }

    fun buildMetadata(catalogName: String): ConnectorMetadata {
        val catalog = MemoryCatalog.PartiQL().name(catalogName).build()
        // Insert binding
        val name = BindingPath(
            listOf(
                BindingName("SCHEMA", BindingCase.INSENSITIVE),
                BindingName("T", BindingCase.INSENSITIVE),
            )
        )
        val obj = MemoryObject(type)
        catalog.insert(name, obj)
        return MemoryConnector(catalog).getMetadata(connectorSession)
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

                PartiQLPlanner.Session.MissingOpBehavior.values().forEach { missingOpBehavior ->
                    val inputPlan = pipeline.invoke(input, missingOpBehavior).plan
                    val outputPlan = pipeline.invoke(test, missingOpBehavior).plan
                    assertPlanEqual(inputPlan, outputPlan)
                }
            }
        }
        return dynamicContainer(file.nameWithoutExtension, children)
    }

    private fun assertPlanEqual(inputPlan: PartiQLPlan, outputPlan: PartiQLPlan) {
        assert(inputPlan.isEquaivalentTo(outputPlan)) {
            buildString {
                this.appendLine("expect plan equivalence")
                PlanPrinter.append(this, inputPlan)
                PlanPrinter.append(this, outputPlan)
            }
        }
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

    private fun PlanNode.isEquaivalentTo(other: PlanNode): Boolean =
        PlanNodeEquivalentVisitor().visit(this, other)
}
