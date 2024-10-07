package org.partiql.planner

import com.amazon.ionelement.api.loadSingleElement
import org.partiql.parser.PartiQLParser
import org.partiql.plan.Statement
import org.partiql.plan.debug.PlanPrinter
import org.partiql.planner.catalog.Catalog
import org.partiql.planner.catalog.Name
import org.partiql.planner.catalog.Session
import org.partiql.planner.internal.TestCatalog
import org.partiql.planner.test.Test
import org.partiql.planner.util.ProblemCollector
import org.partiql.plugins.local.toStaticType
import org.partiql.types.PType

/**
 * This is a port of the original assertion logic from [org.partiql.planner.internal.typer.PlanTyperTestsPorted].
 *
 * @see Success
 * @see org.partiql.planner.internal.typer.PlanTyperTestsPorted.TestCase.SuccessTestCase
 * @see Failure
 * @see org.partiql.planner.internal.typer.PlanTyperTestsPorted.TestCase.ErrorTestCase
 */
abstract class TyperTest(
    private val _name: String,
    open var expectedType: PType?,
    var statement: String,
    var catalog: String,
    var directory: List<String>,
) : Test {

    val parser = PartiQLParser.default()
    val planner = PartiQLPlanner.builder().signal().build()

    override fun getName(): String {
        return _name
    }

    override fun toString(): String {
        return getName()
    }

    val catalogs: List<Catalog> by lazy {
        // Make a map from catalog name to tables.
        val inputStream = this::class.java.getResourceAsStream("/resource_path.txt")!!
        val map = mutableMapOf<String, MutableList<Pair<Name, PType>>>()
        inputStream.reader().readLines().forEach { path ->
            if (path.startsWith("catalogs/default")) {
                val schema = this::class.java.getResourceAsStream("/$path")!!
                val ion = loadSingleElement(schema.reader().readText())
                val staticType = ion.toStaticType()
                val steps = path.substring(0, path.length - 4).split('/').drop(2) // drop the catalogs/default
                val catalogName = steps.first()
                // args
                val name = Name.of(steps.drop(1))
                val ptype = PType.fromStaticType(staticType)
                if (map.containsKey(catalogName)) {
                    map[catalogName]!!.add(name to ptype)
                } else {
                    map[catalogName] = mutableListOf(name to ptype)
                }
            }
        }
        // Make a catalogs list
        map.map { (catalog, tables) ->
            TestCatalog.builder()
                .name(catalog)
                .apply {
                    for ((name, schema) in tables) {
                        createTable(name, schema)
                    }
                }
                .build()
        }
    }

    /**
     * This represents a successful test case.
     *
     * The serialization of a test case looks as follows:
     * ```
     * test::{
     *   name:"Test #4",
     *   type:"type",
     *   body:{
     *     statement:"b",
     *     session:{
     *       catalog:"b",
     *       cwd:[
     *         "b"
     *       ]
     *     },
     *     status:SUCCESS,
     *     expected:(
     *     ROW
     *       "b"
     *       (
     *       ROW
     *         "b"
     *         INTEGER
     *       )
     *       "c"
     *       INTEGER
     *     )
     *   }
     * }
     * ```
     * @see org.partiql.planner.internal.typer.PlanTyperTestsPorted.TestCase.SuccessTestCase
     */
    class Success(
        name: String,
        expectedType: PType,
        statement: String,
        catalog: String,
        directory: List<String>,
    ) : TyperTest(name, expectedType, statement, catalog, directory) {

        override fun assert() {
            val session = Session.builder()
                .catalog(catalog)
                .catalogs(*catalogs.toTypedArray())
                .namespace(directory)
                .build()

            val collector = ProblemCollector()
            val ast = parser.parse(statement).root
            val plan = planner.plan(ast, session, collector).plan
            when (val statement = plan.statement) {
                is Statement.Query -> {
                    assert(collector.problems.isEmpty()) {
                        buildString {
                            appendLine(collector.problems.toString())
                            appendLine()
                            PlanPrinter.append(this, plan)
                        }
                    }
                    val actual = statement.root.type
                    assert(expectedType == actual) {
                        buildString {
                            appendLine()
                            appendLine("Name: ${getName()}")
                            appendLine("Expect: $expectedType")
                            appendLine("Actual: $actual")
                            appendLine("Statement: $statement")
                            appendLine()
                            PlanPrinter.append(this, plan)
                        }
                    }
                }
            }
        }
    }

    /**
     * This represents an error test case.
     *
     * The serialization of an error test case looks as follows:
     * ```
     * test::{
     *   name:"Pets should not be accessible #1",
     *   type:"type",
     *   body:{
     *     statement:"SELECT * FROM pets",
     *     session:{
     *       catalog:"pql",
     *       cwd:[
     *       ]
     *     },
     *     status:FAILURE,
     *     assertProblemExists:(
     *     ERROR
     *       '''Variable pets does not exist in the database environment and is not an attribute of the following in-scope variables [].'''
     *     )
     *   }
     * }
     * ```
     * The `assertProblemExists` is optional.
     *
     * @see org.partiql.planner.internal.typer.PlanTyperTestsPorted.TestCase.ErrorTestCase
     */
    class Failure(
        name: String,
        expectedType: PType?,
        private val assertion: TyperTestBuilder.ProblemAssertion?,
        statement: String,
        catalog: String,
        directory: List<String>,
    ) : TyperTest(name, expectedType, statement, catalog, directory) {
        override fun assert() {
            val session = Session.builder()
                .catalog(catalog)
                .catalogs(*catalogs.toTypedArray())
                .namespace(directory)
                .build()
            val collector = ProblemCollector()
            val ast = parser.parse(statement).root
            val plan = planner.plan(ast, session, collector).plan

            when (val statement = plan.statement) {
                is Statement.Query -> {
                    assert(collector.problems.isNotEmpty()) {
                        buildString {
                            appendLine("Expected to find problems, but none were found.")
                            appendLine()
                            PlanPrinter.append(this, plan)
                        }
                    }
                    if (expectedType != null) {
                        assert(expectedType == statement.root.type) {
                            buildString {
                                appendLine()
                                appendLine("Name: ${getName()}")
                                appendLine("Expect: $expectedType")
                                appendLine("Actual: ${statement.root.type}")
                                appendLine("Statement: $statement")
                                appendLine()
                                PlanPrinter.append(this, plan)
                            }
                        }
                    }
                    assert(collector.problems.isNotEmpty()) {
                        "Expected to find problems, but none were found."
                    }
                    if (assertion != null) {
                        val problemFound = collector.problems.any {
                            it.details.severity.name == assertion.severity && it.details.message == assertion.message
                        }
                        assert(problemFound) {
                            "Could not find problem: ${assertion.severity}: ${assertion.message}."
                        }
                    }
                }
            }
        }
    }
}
