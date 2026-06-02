package org.partiql.eval.internal

import org.partiql.eval.Mode
import org.partiql.eval.PTestCase
import org.partiql.eval.PartiQLVM
import org.partiql.eval.compiler.PartiQLCompiler
import org.partiql.parser.PartiQLParser
import org.partiql.plan.Plan
import org.partiql.planner.PartiQLPlanner
import org.partiql.spi.catalog.Catalog
import org.partiql.spi.catalog.Name
import org.partiql.spi.catalog.Session
import org.partiql.spi.catalog.Table
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum
import org.partiql.spi.value.DatumReader
import org.partiql.types.StaticType
import org.partiql.types.fromStaticType
import kotlin.test.assertEquals

/**
 * @property value is a serialized Ion value.
 */
class Global(
    val name: String,
    val value: Datum,
    val type: PType = PType.dynamic(),
) {
    constructor(
        name: String,
        value: String,
        type: StaticType = StaticType.ANY,
    ) : this(name, DatumReader.ion(value.byteInputStream()).next()!!, fromStaticType(type))
}

public class SuccessTestCase(
    val name: String,
    val input: String,
    val expected: Datum,
    val mode: Mode = Mode.PERMISSIVE(),
    val globals: List<Global> = emptyList(),
    val jvmEquality: Boolean = false
) : PTestCase {

    constructor(
        input: String,
        expected: Datum,
        mode: Mode = Mode.PERMISSIVE(),
        globals: List<Global> = emptyList(),
        jvmEquality: Boolean = false
    ) : this("no_name", input, expected, mode, globals, jvmEquality)

    private val compiler = PartiQLCompiler.standard()
    private val parser = PartiQLParser.standard()
    private val planner = PartiQLPlanner.standard()
    private val refPlanner = PartiQLPlanner.builder().useRefs().build()
    private val vm = PartiQLVM.standard()

    override fun run() {
        val parseResult = parser.parse(input)
        assertEquals(1, parseResult.statements.size)
        val statement = parseResult.statements[0]
        val catalog = Catalog.builder()
            .name("memory")
            .apply {
                globals.forEach {
                    val table = Table.standard(
                        name = Name.of(it.name),
                        schema = it.type,
                        datum = it.value
                    )
                    define(table)
                }
            }
            .build()
        val session = Session.builder()
            .catalog("memory")
            .catalogs(catalog)
            .build()
        // Old path
        val plan = planner.plan(statement, session).plan
        val result = DatumMaterialize.materialize(compiler.prepare(plan, mode).execute())
        val comparison = when (jvmEquality) {
            true -> expected == result
            false -> Datum.comparator(true, true).compare(expected, result) == 0
        }
        assert(comparison) {
            comparisonString(expected, result, plan)
        }
        // New VM path — execute through the thread-safe path and assert same result
        val refResult = refPlanner.plan(statement, session)
        val execPlan = compiler.compile(refResult.plan, mode)
        val catalogs = buildExecutionCatalogs(refResult.symbols, session)
        val vmResult = DatumMaterialize.materialize(vm.execute(execPlan, catalogs))
        val vmComparison = when (jvmEquality) {
            true -> expected == vmResult
            false -> Datum.comparator(true, true).compare(expected, vmResult) == 0
        }
        assert(vmComparison) {
            buildString {
                appendLine("[VM PATH]")
                appendLine(comparisonString(expected, vmResult, refResult.plan))
            }
        }
    }

    private fun comparisonString(expected: Datum, actual: Datum, plan: Plan): String {
        return buildString {
            // TODO pretty-print V1 plans!
            appendLine(plan)
            // TODO: Add DatumWriter
            appendLine("Expected : $expected")
            appendLine("Actual   : $actual")
        }
    }

    override fun toString(): String {
        return "$name ($mode): $input"
    }
}

public class FailureTestCase(
    val name: String,
    val input: String,
    val mode: Mode = Mode.STRICT(), // default to run in STRICT mode
    val globals: List<Global> = emptyList(),
) : PTestCase {

    constructor(
        input: String,
        mode: Mode = Mode.STRICT(),
        globals: List<Global> = emptyList()
    ) : this("no_name", input, mode, globals)

    private val compiler = PartiQLCompiler.standard()
    private val parser = PartiQLParser.standard()
    private val planner = PartiQLPlanner.standard()
    private val refPlanner = PartiQLPlanner.builder().useRefs().build()
    private val vm = PartiQLVM.standard()

    override fun run() {
        val parseResult = parser.parse(input)
        assertEquals(1, parseResult.statements.size)
        val statement = parseResult.statements[0]
        val catalog = Catalog.builder()
            .name("memory")
            .apply {
                globals.forEach {
                    val table = Table.standard(
                        name = Name.of(it.name),
                        schema = it.type,
                        datum = it.value
                    )
                    define(table)
                }
            }
            .build()
        val session = Session.builder()
            .catalog("memory")
            .catalogs(catalog)
            .build()
        // Old path
        var thrown: Throwable? = null
        val plan = planner.plan(statement, session).plan
        val actual: Datum = try {
            DatumMaterialize.materialize(compiler.prepare(plan, mode).execute())
        } catch (t: Throwable) {
            thrown = t
            Datum.nullValue()
        }
        if (thrown == null) {
            val message = buildString {
                appendLine("Expected error to be thrown but none was thrown.")
                appendLine("Actual Result: $actual")
            }
            error(message)
        }
        // New VM path — also expect failure
        var vmThrown: Throwable? = null
        try {
            val refResult = refPlanner.plan(statement, session)
            val execPlan = compiler.compile(refResult.plan, mode)
            val catalogs = buildExecutionCatalogs(refResult.symbols, session)
            DatumMaterialize.materialize(vm.execute(execPlan, catalogs))
        } catch (t: Throwable) {
            vmThrown = t
        }
        if (vmThrown == null) {
            error("[VM PATH] Expected error to be thrown but none was thrown for: $input")
        }
    }

    override fun toString(): String {
        return "$name ($mode): $input"
    }
}
