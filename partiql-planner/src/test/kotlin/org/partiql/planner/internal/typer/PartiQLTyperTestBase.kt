package org.partiql.planner.internal.typer

import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.DynamicTest
import org.partiql.errors.ProblemCallback
import org.partiql.parser.PartiQLParser
import org.partiql.plan.Statement
import org.partiql.plan.debug.PlanPrinter
import org.partiql.planner.PartiQLPlanner
import org.partiql.planner.catalog.Catalog
import org.partiql.planner.catalog.Catalogs
import org.partiql.planner.catalog.Namespace
import org.partiql.planner.catalog.Session
import org.partiql.planner.internal.PlanningProblemDetails
import org.partiql.planner.test.PartiQLTest
import org.partiql.planner.test.PartiQLTestProvider
import org.partiql.planner.util.ProblemCollector
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

    val inputs = PartiQLTestProvider().apply { load() }

    val testingPipeline: ((String, Catalog, ProblemCallback) -> PartiQLPlanner.Result) = { query, catalog, collector ->
        val parser = PartiQLParser.default()
        val ast = parser.parse(query).root
        val planner = PartiQLPlanner.builder()
            .catalogs(Catalogs.of(catalog))
            .build()
        val session = Session.builder()
            .namespace(Namespace.of(catalog.getName()))
            .build()
        planner.plan(ast, session, collector)
    }

    /**
     * Build a ConnectorMetadata instance from the list of types.
     */
    private fun buildCatalog(catalog: String, types: List<StaticType>): Catalog {
        return Catalog.builder()
            .name(catalog)
            .apply {
                // define all bindings
                types.forEachIndexed { i, t ->
                    createTable(
                        name = "t${i + 1}",
                        schema = PType.fromStaticType(t)
                    )
                }
            }
            .build()
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
                    val catalog = buildCatalog(testName, types)
                    val displayName = "$group | $testName | $types"
                    val statement = test.statement
                    // Assert
                    DynamicTest.dynamicTest(displayName) {
                        val pc = ProblemCollector()
                        if (key is TestResult.Success) {
                            val result = testingPipeline(statement, catalog, pc)
                            val root = (result.plan.statement as Statement.Query).root
                            val actualType = root.type
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
                            val result = testingPipeline(statement, catalog, pc)
                            val root = (result.plan.statement as Statement.Query).root
                            val actualType = root.type
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
