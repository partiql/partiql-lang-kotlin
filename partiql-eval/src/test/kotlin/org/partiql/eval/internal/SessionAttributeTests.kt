package org.partiql.eval.internal

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.eval.Mode
import org.partiql.eval.PartiQLVM
import org.partiql.eval.compiler.PartiQLCompiler
import org.partiql.eval.internal.compiler.StandardCompiler
import org.partiql.eval.internal.vm.StandardVM
import org.partiql.parser.PartiQLParser
import org.partiql.plan.Plan
import org.partiql.planner.PartiQLPlanner
import org.partiql.spi.catalog.Catalog
import org.partiql.spi.catalog.Session
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Runtime evaluation of SQL session attributes.
 *
 * `CURRENT_USER` is the planning session identity. [Session.Builder] defaults that identity to
 * `"unknown"` when it is not set. `CURRENT_DATE` is pinned once when execution starts.
 */
class SessionAttributeTests {

    @ParameterizedTest
    @MethodSource("sessionAttributeCases")
    fun sessionAttributes(tc: SuccessTestCase) = tc.run()

    @Test
    fun currentDateIsPinnedForOneStatementExecution() {
        var ticks = 0
        val clock = {
            ticks += 1
            LocalDate.of(2020, 1, ticks)
        }
        val plan = plan("SELECT CURRENT_DATE AS a, CURRENT_DATE AS b FROM << 0 >>")
        val statement = StandardCompiler(clock).prepare(plan, Mode.PERMISSIVE())

        val first = DatumMaterialize.materialize(statement.execute())
        val second = DatumMaterialize.materialize(statement.execute())

        assertEquals(LocalDate.of(2020, 1, 1), dateField(first, "a"))
        assertEquals(LocalDate.of(2020, 1, 1), dateField(first, "b"))
        assertEquals(LocalDate.of(2020, 1, 2), dateField(second, "a"))
        assertEquals(LocalDate.of(2020, 1, 2), dateField(second, "b"))
        assertEquals(2, ticks)
    }

    @Test
    fun currentUserAndCurrentDateProjection() {
        val before = LocalDate.now()
        val query = "SELECT CURRENT_USER AS u, CURRENT_DATE AS d FROM << 0 >>"
        val parseResult = PartiQLParser.standard().parse(query)
        val statement = parseResult.statements.single()
        val catalog = Catalog.builder().name("memory").build()
        val session = Session.builder()
            .catalog("memory")
            .catalogs(catalog)
            .identity("alice")
            .build()
        val plan = PartiQLPlanner.standard().plan(statement, session).plan
        val after = LocalDate.now()

        val result = DatumMaterialize.materialize(
            PartiQLCompiler.standard().prepare(plan, Mode.PERMISSIVE()).execute()
        )
        val row = result.iterator().next()
        assertEquals("alice", row.fields.asSequence().first { it.name == "u" }.value.string)
        val d = row.fields.asSequence().first { it.name == "d" }.value
        assertEquals(PType.date(), d.type)
        assertTrue(!d.localDate.isBefore(before) && !d.localDate.isAfter(after))
    }

    @Test
    fun currentDateIsPinnedForOneVmExecution() {
        var ticks = 0
        val clock = {
            ticks += 1
            LocalDate.of(2020, 1, ticks)
        }
        val current = session()
        val planned = refPlanner.plan(parse("SELECT VALUE CURRENT_DATE FROM << 1, 2, 3 >>"), current)
        val execPlan = PartiQLCompiler.standard().compile(planned.plan, Mode.PERMISSIVE())
        val catalogs = buildExecutionCatalogs(planned.symbols, current)
        val vm: PartiQLVM = StandardVM(clock)

        val result = DatumMaterialize.materialize(vm.execute(execPlan, catalogs))
        val dates = result.map { it.localDate }

        assertEquals(listOf(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 1)), dates)
        assertEquals(1, ticks)
    }

    private fun dateField(result: Datum, name: String): LocalDate {
        val row = result.iterator().next()
        val field = row.fields.asSequence().first { it.name == name }
        return field.value.localDate
    }

    companion object {
        private val parser = PartiQLParser.standard()
        private val planner = PartiQLPlanner.standard()
        private val refPlanner = PartiQLPlanner.builder().useRefs().build()

        private fun session(): Session {
            val catalog = Catalog.builder().name("memory").build()
            return Session.builder().catalog("memory").catalogs(catalog).build()
        }

        private fun parse(query: String) = parser.parse(query).statements.single()

        private fun plan(query: String): Plan = planner.plan(parse(query), session()).plan

        @JvmStatic
        fun sessionAttributeCases(): List<SuccessTestCase> = listOf(
            SuccessTestCase(
                name = "CURRENT_USER default identity",
                input = "CURRENT_USER",
                expected = Datum.string("unknown"),
            ),
            SuccessTestCase(
                name = "CURRENT_USER session identity",
                input = "CURRENT_USER",
                expected = Datum.string("alice"),
                identity = "alice",
            ),
            SuccessTestCase(
                name = "CURRENT_USER concatenation",
                input = "CURRENT_USER || '!'",
                expected = Datum.string("alice!"),
                identity = "alice",
            ),
            SuccessTestCase(
                name = "CURRENT_USER filter",
                input = "SELECT VALUE x FROM << 'a', 'b' >> AS x WHERE CURRENT_USER = 'alice'",
                expected = Datum.bagVararg(Datum.string("a"), Datum.string("b")),
                identity = "alice",
            ),
            SuccessTestCase(
                name = "CURRENT_USER filter misses",
                input = "SELECT VALUE x FROM << 'a', 'b' >> AS x WHERE CURRENT_USER = 'alice'",
                expected = Datum.bagVararg(),
                identity = "bob",
            ),
            SuccessTestCase(
                name = "CURRENT_DATE",
                input = "CURRENT_DATE = CURRENT_DATE",
                expected = Datum.bool(true),
            ),
            SuccessTestCase(
                name = "CURRENT_DATE is the same on every row",
                input = "SELECT VALUE CURRENT_DATE = CURRENT_DATE FROM << 1, 2, 3 >>",
                expected = Datum.bagVararg(Datum.bool(true), Datum.bool(true), Datum.bool(true)),
            ),
        )
    }
}
