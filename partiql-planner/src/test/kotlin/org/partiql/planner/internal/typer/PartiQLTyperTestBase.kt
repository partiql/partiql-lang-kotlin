package org.partiql.planner.typer

import com.amazon.ionelement.api.ionString
import com.amazon.ionelement.api.ionStructOf
import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.DynamicTest
import org.partiql.errors.Problem
import org.partiql.errors.ProblemCallback
import org.partiql.errors.ProblemSeverity
import org.partiql.parser.PartiQLParserBuilder
import org.partiql.plan.Statement
import org.partiql.plan.debug.PlanPrinter
import org.partiql.planner.PartiQLPlanner
import org.partiql.planner.PartiQLPlannerBuilder
import org.partiql.planner.test.PartiQLTest
import org.partiql.planner.test.PartiQLTestProvider
import org.partiql.plugins.memory.MemoryCatalog
import org.partiql.plugins.memory.MemoryPlugin
import org.partiql.types.StaticType
import java.util.Random
import java.util.stream.Stream

abstract class PartiQLTyperTestBase {
    sealed class TestResult {
        data class Success(val expectedType: StaticType) : TestResult() {
            override fun toString(): String = "Success_$expectedType"
        }

        object Failure : TestResult() {
            override fun toString(): String = "Failure"
        }
    }

    internal class ProblemCollector : ProblemCallback {
        private val problemList = mutableListOf<Problem>()

        val problems: List<Problem>
            get() = problemList

        val hasErrors: Boolean
            get() = problemList.any { it.details.severity == ProblemSeverity.ERROR }

        val hasWarnings: Boolean
            get() = problemList.any { it.details.severity == ProblemSeverity.WARNING }

        override fun invoke(problem: Problem) {
            problemList.add(problem)
        }
    }

    companion object {
        internal val session: ((String) -> PartiQLPlanner.Session) = { catalog ->
            PartiQLPlanner.Session(
                queryId = Random().nextInt().toString(),
                userId = "test-user",
                currentCatalog = catalog,
                catalogConfig = mapOf(
                    catalog to ionStructOf(
                        "connector_name" to ionString("memory")
                    )
                )
            )
        }
    }

    val inputs = PartiQLTestProvider().apply { load() }

    val testingPipeline: ((String, String, MemoryCatalog.Provider, ProblemCallback) -> PartiQLPlanner.Result) = { query, catalog, catalogProvider, collector ->
        val ast = PartiQLParserBuilder.standard().build().parse(query).root
        val planner = PartiQLPlannerBuilder().plugins(listOf(MemoryPlugin(catalogProvider))).build()
        planner.plan(ast, session(catalog), collector)
    }

    fun testGen(
        testCategory: String,
        tests: List<PartiQLTest>,
        argsMap: Map<TestResult, Set<List<StaticType>>>,
    ): Stream<DynamicContainer> {
        val catalogProvider = MemoryCatalog.Provider()

        return tests.map { test ->
            val group = test.statement
            val children = argsMap.flatMap { (key, value) ->
                value.mapIndexed { index: Int, types: List<StaticType> ->
                    val testName = "${testCategory}_${key}_$index"
                    catalogProvider[testName] = MemoryCatalog.of(
                        *(
                            types.mapIndexed { i, t ->
                                "t${i + 1}" to t
                            }.toTypedArray()
                            )
                    )
                    val displayName = "$group | $testName | $types"
                    val statement = test.statement
                    // Assert
                    DynamicTest.dynamicTest(displayName) {
                        val pc = ProblemCollector()
                        if (key is TestResult.Success) {
                            val result = testingPipeline(statement, testName, catalogProvider, pc)
                            val root = (result.plan.statement as Statement.Query).root
                            val actualType = root.type
                            assert(actualType == key.expectedType) {
                                buildString {
                                    this.appendLine("expected Type is : ${key.expectedType}")
                                    this.appendLine("actual Type is : $actualType")
                                    PlanPrinter.append(this, result.plan)
                                }
                            }
                            assert(pc.problems.isEmpty()) {
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
                            val result = testingPipeline(statement, testName, catalogProvider, pc)
                            val root = (result.plan.statement as Statement.Query).root
                            val actualType = root.type
                            assert(actualType == StaticType.MISSING) {
                                buildString {
                                    this.appendLine(" expected Type is : MISSING")
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
