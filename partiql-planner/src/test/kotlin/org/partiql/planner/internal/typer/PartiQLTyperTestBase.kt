package org.partiql.planner.internal.typer

import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.DynamicTest
import org.partiql.errors.ProblemCallback
import org.partiql.parser.PartiQLParser
import org.partiql.plan.Statement
import org.partiql.plan.debug.PlanPrinter
import org.partiql.planner.PartiQLPlanner
import org.partiql.planner.catalog.Session
import org.partiql.planner.internal.PlanningProblemDetails
import org.partiql.planner.test.PartiQLTest
import org.partiql.planner.test.PartiQLTestProvider
import org.partiql.planner.util.ProblemCollector
import org.partiql.plugins.memory.MemoryCatalog
import org.partiql.plugins.memory.MemoryConnector
import org.partiql.plugins.memory.MemoryObject
import org.partiql.spi.BindingCase
import org.partiql.spi.BindingName
import org.partiql.spi.BindingPath
import org.partiql.spi.connector.ConnectorMetadata
import org.partiql.spi.connector.ConnectorSession
import org.partiql.types.PType
import org.partiql.types.PType.Kind
import org.partiql.types.StaticType
import org.partiql.value.PartiQLValueExperimental
import java.util.stream.Stream

abstract class PartiQLTyperTestBase {
    sealed class TestResult {
        data class Success(val expectedType: PType) : TestResult() {

            constructor(expectedType: StaticType) : this(SqlTypes.fromStaticType(expectedType))

            override fun toString(): String = "Success_$expectedType"
        }

        object Failure : TestResult() {
            override fun toString(): String = "Failure"
        }
    }

    companion object {

        public val parser = PartiQLParser.default()
        public val planner = PartiQLPlanner.standard()

        internal val session: ((String, ConnectorMetadata) -> Session) = { catalog, metadata ->
            Session.builder()
                .catalog(catalog)
                .catalogs(catalog to metadata)
                .build()
        }

        internal val connectorSession = object : ConnectorSession {
            override fun getQueryId(): String = "test"
            override fun getUserId(): String = "test"
        }
    }

    val inputs = PartiQLTestProvider().apply { load() }

    val testingPipeline: ((String, String, ConnectorMetadata, ProblemCallback) -> PartiQLPlanner.Result) = { query, catalog, metadata, collector ->
        val ast = parser.parse(query).root
        planner.plan(ast, session(catalog, metadata), collector)
    }

    /**
     * Build a ConnectorMetadata instance from the list of types.
     */
    @OptIn(PartiQLValueExperimental::class)
    private fun buildMetadata(catalog: String, types: List<StaticType>): ConnectorMetadata {
        val cat = MemoryCatalog.builder().name(catalog).build()
        val connector = MemoryConnector(cat)

        // define all bindings
        types.forEachIndexed { i, t ->
            val binding = BindingPath(listOf(BindingName("t${i + 1}", BindingCase.SENSITIVE)))
            val obj = MemoryObject(t)
            cat.insert(binding, obj)
        }
        return connector.getMetadata(connectorSession)
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
                    val metadata = buildMetadata(testName, types)
                    val displayName = "$group | $testName | $types"
                    val statement = test.statement
                    // Assert
                    DynamicTest.dynamicTest(displayName) {
                        val pc = ProblemCollector()
                        if (key is TestResult.Success) {
                            val result = testingPipeline(statement, testName, metadata, pc)
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
                            val result = testingPipeline(statement, testName, metadata, pc)
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
