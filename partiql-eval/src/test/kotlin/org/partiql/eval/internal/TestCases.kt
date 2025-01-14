package org.partiql.eval.internal

import org.partiql.eval.Mode
import org.partiql.eval.PTestCase
import org.partiql.eval.compiler.PartiQLCompiler
import org.partiql.parser.PartiQLParser
import org.partiql.plan.Plan
import org.partiql.planner.PartiQLPlanner
import org.partiql.spi.catalog.Catalog
import org.partiql.spi.catalog.Name
import org.partiql.spi.catalog.Session
import org.partiql.spi.catalog.Table
import org.partiql.spi.value.Datum
import org.partiql.spi.value.DatumReader
import org.partiql.spi.value.ValueUtils
import org.partiql.types.StaticType
import org.partiql.types.fromStaticType
import org.partiql.value.PartiQLValue
import kotlin.test.assertEquals

/**
 * @property value is a serialized Ion value.
 */
class Global(
    val name: String,
    val value: String,
    val type: StaticType = StaticType.ANY,
)

public class SuccessTestCase(
    val input: String,
    val expected: Datum,
    val mode: Mode = Mode.PERMISSIVE(),
    val globals: List<Global> = emptyList(),
    val jvmEquality: Boolean = false
) : PTestCase {

    constructor(
        input: String,
        expected: PartiQLValue,
        mode: Mode = Mode.PERMISSIVE(),
        globals: List<Global> = emptyList(),
    ) : this(input, ValueUtils.newDatum(expected), mode, globals)

    private val compiler = PartiQLCompiler.standard()
    private val parser = PartiQLParser.standard()
    private val planner = PartiQLPlanner.standard()

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
                        schema = fromStaticType(it.type),
                        datum = DatumReader.ion(it.value.byteInputStream()).next()!!
                    )
                    define(table)
                }
            }
            .build()
        val session = Session.builder()
            .catalog("memory")
            .catalogs(catalog)
            .build()
        val plan = planner.plan(statement, session).plan
        val result = DatumMaterialize.materialize(compiler.prepare(plan, mode).execute())
        val comparison = when (jvmEquality) {
            true -> expected == result
            false -> Datum.comparator().compare(expected, result) == 0
        }
        assert(comparison) {
            comparisonString(expected, result, plan)
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
        return input
    }
}

public class FailureTestCase(
    val input: String,
    val mode: Mode = Mode.STRICT(), // default to run in STRICT mode
    val globals: List<Global> = emptyList(),
) : PTestCase {
    private val compiler = PartiQLCompiler.standard()
    private val parser = PartiQLParser.standard()
    private val planner = PartiQLPlanner.standard()

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
                        schema = fromStaticType(it.type),
                        datum = DatumReader.ion(it.value.byteInputStream()).next()!!
                    )
                    define(table)
                }
            }
            .build()
        val session = Session.builder()
            .catalog("memory")
            .catalogs(catalog)
            .build()
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
    }
}
