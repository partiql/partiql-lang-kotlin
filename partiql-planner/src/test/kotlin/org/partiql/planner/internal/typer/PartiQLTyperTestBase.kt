package org.partiql.planner.internal.typer

import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.DynamicTest
import org.partiql.parser.V1PartiQLParser
import org.partiql.plan.Operation
import org.partiql.planner.PartiQLPlanner
import org.partiql.planner.test.PartiQLTest
import org.partiql.planner.test.PartiQLTestProvider
import org.partiql.planner.util.PErrorCollector
import org.partiql.planner.util.PlanPrinter
import org.partiql.spi.Context
import org.partiql.spi.catalog.Catalog
import org.partiql.spi.catalog.Name
import org.partiql.spi.catalog.Session
import org.partiql.spi.catalog.Table
import org.partiql.spi.errors.PError
import org.partiql.spi.errors.PErrorListener
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

        public val parser = V1PartiQLParser.standard()
        public val planner = PartiQLPlanner.standard()

        internal val session: ((String, Catalog) -> Session) = { catalog, metadata ->
            Session.builder()
                .catalog(catalog)
                .catalogs(metadata)
                .build()
        }
    }

    val inputs = PartiQLTestProvider().apply { load() }

    private val testingPipeline: ((String, String, Catalog, PErrorListener) -> PartiQLPlanner.Result) =
        { query, catalog, metadata, collector ->
            val parseResult = parser.parse(query)
            if (parseResult.statements.size != 1) {
                throw IllegalArgumentException("Only single statement is supported for testing")
            }
            val ast = parseResult.statements[0]
            val config = Context.of(collector)
            planner.plan(ast, session(catalog, metadata), config)
        }

    /**
     * Build a ConnectorMetadata instance from the list of types.
     */
    private fun buildCatalog(name: String, types: List<StaticType>): Catalog {
        val catalog = Catalog.builder().name(name)
        // define all bindings
        types.forEachIndexed { i, t ->
            val tableName = Name.of("t${i + 1}")
            val tableSchema = PType.fromStaticType(t)
            val table = Table.empty(tableName, tableSchema)
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
                        val pc = PErrorCollector()
                        if (key is TestResult.Success) {
                            val result = testingPipeline(statement, testName, metadata, pc)
                            val query = result.plan.getOperation() as Operation.Query
                            val actualType = query.getType().getPType()
                            assert(actualType == key.expectedType) {
                                buildString {
                                    this.appendLine("expected Type is : ${key.expectedType}")
                                    this.appendLine("actual Type is : $actualType")
                                    PlanPrinter.append(this, result.plan)
                                }
                            }
                            // We need to allow for the testing of null/missing
                            val problemsWithoutNullMissing = pc.problems.filterNot {
                                it.code() in setOf(
                                    PError.PATH_INDEX_NEVER_SUCCEEDS,
                                    PError.PATH_SYMBOL_NEVER_SUCCEEDS,
                                    PError.PATH_KEY_NEVER_SUCCEEDS,
                                    PError.ALWAYS_MISSING
                                )
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
                            val query = result.plan.getOperation() as Operation.Query
                            val actualType = query.getType().getPType()
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
