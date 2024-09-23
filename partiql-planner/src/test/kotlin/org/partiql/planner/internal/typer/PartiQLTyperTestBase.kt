package org.partiql.planner.internal.typer

import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.DynamicTest
import org.partiql.errors.ProblemCallback
import org.partiql.parser.PartiQLParser
import org.partiql.plan.v1.Statement
import org.partiql.planner.PartiQLPlanner
import org.partiql.planner.internal.PlanningProblemDetails
import org.partiql.planner.test.PartiQLTest
import org.partiql.planner.test.PartiQLTestProvider
import org.partiql.planner.util.PlanPrinter
import org.partiql.planner.util.ProblemCollector
import org.partiql.plugins.memory.MemoryCatalog
import org.partiql.plugins.memory.MemoryTable
import org.partiql.spi.catalog.Catalog
import org.partiql.spi.catalog.Name
import org.partiql.spi.catalog.Session
import org.partiql.types.PType
import org.partiql.types.PType.Kind
import org.partiql.types.StaticType
import java.util.stream.Stream

abstract class PartiQLTyperTestBase {
    sealed class TestResult {
        data class Success(val expectedType: PType) : TestResult() {

            constructor(expectedType: StaticType) : this(PType.fromStaticType(expectedType))

            override fun toString(): String = "Success_$expectedType"
        }

        object Failure : TestResult() {
            override fun toString(): String = "Failure"
        }
    }

    companion object {

        public val parser = PartiQLParser.standard()
        public val planner = PartiQLPlanner.standard()

        internal val session: ((String, Catalog) -> Session) = { catalog, metadata ->
            Session.builder()
                .catalog(catalog)
                .catalogs(metadata)
                .build()
        }
    }

    val inputs = PartiQLTestProvider().apply { load() }

    val testingPipeline: ((String, String, Catalog, ProblemCallback) -> PartiQLPlanner.Result) =
        { query, catalog, metadata, collector ->
            val ast = parser.parse(query).root
            planner.plan(ast, session(catalog, metadata), collector)
        }

    /**
     * Build a ConnectorMetadata instance from the list of types.
     */
    private fun buildCatalog(name: String, types: List<StaticType>): Catalog {
        val catalog = MemoryCatalog.builder().name(name)
        // define all bindings
        types.forEachIndexed { i, t ->
            val tableName = Name.of("t${i + 1}")
            val tableSchema = PType.fromStaticType(t)
            val table = MemoryTable.empty(tableName, tableSchema)
            catalog.define(table)
        }
        return catalog.build()
    }

    fun testGen(
        testCategory: String,
        tests: List<PartiQLTest>,
        argsMap: Map<TestResult, Set<List<StaticType>>>,
    ): Stream<DynamicContainer> {

        return tests.map { test ->
            val group = test.statement
            val children = argsMap.flatMap { (key, value) ->
                value.mapIndexed { index: Int, types: List<StaticType> ->
                    val testName = "${testCategory}_${key}_$index"
                    val metadata = buildCatalog(testName, types)
                    val displayName = "$group | $testName | $types"
                    val statement = test.statement
                    // Assert
                    DynamicTest.dynamicTest(displayName) {
                        val pc = ProblemCollector()
                        if (key is TestResult.Success) {
                            val result = testingPipeline(statement, testName, metadata, pc)
                            val root = (result.plan.getStatement() as Statement.Query).getRoot()
                            val actualType = root.getType()
                            assert(actualType == key.expectedType) {
                                buildString {
                                    this.appendLine("expected Type is : ${key.expectedType}")
                                    this.appendLine("actual Type is : $actualType")
                                    PlanPrinter.append(this, result.plan)
                                }
                            }
                            // We need to allow for the testing of null/missing
                            val problemsWithoutNullMissing = pc.problems.filterNot {
                                it.details is PlanningProblemDetails.ExpressionAlwaysReturnsNullOrMissing
                            }
                            assert(problemsWithoutNullMissing.isEmpty()) {
                                buildString {
                                    this.appendLine("expected success Test case to have no problem")
                                    this.appendLine("actual problems are: ")
                                    pc.problems.forEach {
                                        this.appendLine(it)
                                    }
                                    PlanPrinter.append(this, result.plan)
                                }
                            }
                        } else {
                            val result = testingPipeline(statement, testName, metadata, pc)
                            val root = (result.plan.getStatement() as Statement.Query).getRoot()
                            val actualType = root.getType()
                            assert(actualType.kind == Kind.DYNAMIC) {
                                buildString {
                                    this.appendLine("expected Type is : DYNAMIC")
                                    this.appendLine("actual Type is : $actualType")
                                    PlanPrinter.append(this, result.plan)
                                }
                            }
                            assert(pc.problems.isNotEmpty()) {
                                buildString {
                                    this.appendLine("expected success Test case to have problems")
                                    this.appendLine("but received no problems")
                                    PlanPrinter.append(this, result.plan)
                                }
                            }
                        }
                    }
                }
            }
            DynamicContainer.dynamicContainer(group, children)
        }.stream()
    }
}
