package org.partiql.planner.internal.typer

import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.parser.PartiQLParser
import org.partiql.plan.Action
import org.partiql.plan.Operators
import org.partiql.plan.Plan
import org.partiql.plan.rel.Rel
import org.partiql.plan.rel.RelExcept
import org.partiql.plan.rel.RelIntersect
import org.partiql.plan.rel.RelProject
import org.partiql.plan.rel.RelUnion
import org.partiql.plan.rex.RexSelect
import org.partiql.plan.rex.RexStruct
import org.partiql.planner.AssertingEquivalenceOperatorVisitor
import org.partiql.planner.PartiQLPlanner
import org.partiql.planner.plugins.local.LocalCatalog
import org.partiql.planner.util.PErrorCollector
import org.partiql.spi.Context
import org.partiql.spi.catalog.Session
import org.partiql.spi.types.PType
import org.partiql.spi.types.PTypeField
import org.partiql.spi.value.Datum
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.io.path.toPath

/**
 * Asserts that coerceRow's extractRowMember optimization produces direct field references
 * (RexCast wrapping RexLit/RexVar/etc.) rather than unnecessary RexPathKey nodes when coercing
 * struct-typed projections in set operations with disjoint but compatible field types.
 */
/* ktlint-disable standard:final-newline */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Suppress("ktlint:standard:indent")
internal class RelaxedSetOpTypeMatchingTest {

    private val root = this::class.java.getResource("/catalogs/default/pql")!!.toURI().toPath()
    private val ops = Operators.STANDARD

    @ParameterizedTest
    @MethodSource("allSetOperations")
    @Suppress("ktlint:standard:indent")
    fun testDirectSelectDecimalWithBigInt(setOp: String) {
        val query = """
            SELECT
                1.0 AS a
            FROM << 0 >>
            
            $setOp SELECT
                CAST(1 AS BIGINT) AS a
            FROM << 0 >>
        """.trimIndent()

        val expectedLeft = ops.struct(listOf(RexStruct.field(
            ops.lit(Datum.string("a")),
            ops.cast(
                ops.lit(Datum.decimal(BigDecimal.valueOf(1.0), 2, 1)),
                PType.decimal(20, 1)
            ),
        )))
        val expectedRight = ops.struct(listOf(RexStruct.field(
            ops.lit(Datum.string("a")),
            ops.cast(
                ops.cast(
                    ops.lit(Datum.integer(1)),
                    PType.bigint()
                ),
                PType.decimal(20, 1)
            ),
        )))

        assertSetOpPlan(expectedLeft, expectedRight, query)
    }

    @ParameterizedTest
    @MethodSource("allSetOperations")
    @Suppress("ktlint:standard:indent")
    fun testDirectSelectMultiMismatch(setOp: String) {
        val query = """
            SELECT
                CAST(1 AS BIGINT) AS a,
                DATE '2024-01-01' AS b,
                15 AS c
            FROM << 0 >>
            
            $setOp SELECT
                47.5 AS a,
                TIMESTAMP '2024-01-01 00:00:00' AS b,
                CAST(0 AS BIGINT) AS c
            FROM << 0 >>
        """.trimIndent()

        val expectedLeft = ops.struct(listOf(RexStruct.field(
            ops.lit(Datum.string("a")),
            ops.cast(
                ops.cast(
                    ops.lit(Datum.integer(1)),
                    PType.bigint()
                ),
                PType.decimal(20, 1)
            ),
        ), RexStruct.field(
            ops.lit(Datum.string("b")),
            ops.cast(
                ops.lit(Datum.date(LocalDate.parse("2024-01-01"))),
                PType.timestamp(6)
            ),
        ), RexStruct.field(
            ops.lit(Datum.string("c")),
            ops.cast(
                ops.lit(Datum.integer(15)),
                PType.bigint()
            ),
        )))
        val expectedRight = ops.struct(listOf(RexStruct.field(
            ops.lit(Datum.string("a")),
            ops.cast(
                ops.lit(Datum.decimal(BigDecimal.valueOf(47.5), 3, 1)),
                PType.decimal(20, 1)
            ),
        ), RexStruct.field(
            ops.lit(Datum.string("b")),
            ops.lit(Datum.timestamp(LocalDateTime.parse("2024-01-01T00:00:00"), 6)),
        ), RexStruct.field(
            ops.lit(Datum.string("c")),
            ops.cast(
                ops.lit(Datum.integer(0)),
                PType.bigint()
            ),
        )))

        assertSetOpPlan(expectedLeft, expectedRight, query)
    }

    @ParameterizedTest
    @MethodSource("allSetOperations")
    @Suppress("ktlint:standard:indent")
    fun testDirectSelectBag(setOp: String) {
        val query = """
            SELECT
                << CAST(1 AS BIGINT) >> AS a
            FROM << 0 >>
            
            $setOp SELECT
                << 1.5 >> AS a
            FROM << 0 >>
        """.trimIndent()

        val expectedLeft = ops.struct(listOf(RexStruct.field(
            ops.lit(Datum.string("a")),
            ops.cast(
                ops.bag(listOf(
                    ops.cast(
                        ops.lit(Datum.integer(1)),
                        PType.bigint()
                    )
                )),
                PType.bag(PType.decimal(20, 1))
            ),
        )))
        val expectedRight = ops.struct(listOf(RexStruct.field(
            ops.lit(Datum.string("a")),
            ops.cast(
                ops.bag(listOf(
                    ops.lit(Datum.decimal(BigDecimal.valueOf(1.5), 2, 1))
                )),
                PType.bag(PType.decimal(20, 1))
            ),
        )))

        assertSetOpPlan(expectedLeft, expectedRight, query)
    }

    @ParameterizedTest
    @MethodSource("allSetOperations")
    @Suppress("ktlint:standard:indent")
    fun testDirectSelectStruct(setOp: String) {
        val query = """
            SELECT
                { 'foo': CAST(1 AS BIGINT) } AS a
            FROM << 0 >>
            
            $setOp SELECT
                { 'foo': 1.5 } AS a
            FROM << 0 >>
        """.trimIndent()

        // The struct gets its members unpacked, casted, and re-assembled, rather than
        // the entire struct getting a path key expr on it for each field.
        val expectedLeft = ops.struct(listOf(RexStruct.field(
            ops.lit(Datum.string("a")),
            ops.struct(listOf(RexStruct.field(
                ops.lit(Datum.string("foo")),
                ops.cast(
                    ops.cast(
                        ops.lit(Datum.integer(1)),
                        PType.bigint()
                    ),
                    PType.decimal(20, 1)
                ),
            )))
        )))
        val expectedRight = ops.struct(listOf(RexStruct.field(
            ops.lit(Datum.string("a")),
            ops.struct(listOf(RexStruct.field(
                ops.lit(Datum.string("foo")),
                ops.cast(
                    ops.lit(Datum.decimal(BigDecimal.valueOf(1.5), 2, 1)),
                    PType.decimal(20, 1)
                ),
            )))
        )))

        assertSetOpPlan(expectedLeft, expectedRight, query)
    }

    @ParameterizedTest
    @MethodSource("allSetOperations")
    @Suppress("ktlint:standard:indent")
    fun testSelectStructValuedExpression(setOp: String) {
        val query = """
            SELECT
                the_struct AS a
            FROM << { 'the_struct': { 'foo': CAST(1 AS BIGINT) } } >>
            
            $setOp SELECT
                the_struct AS a
            FROM << { 'the_struct': { 'foo': 1.5 } } >>
        """.trimIndent()

        // For row-typed expressions that cannot be unpacked, planner generates
        // path key expression with the appropriate cast. E.g.
        // { foo: CAST(<...>.the_struct.foo AS ...) }
        val expectedLeft = ops.struct(listOf(RexStruct.field(
            ops.lit(Datum.string("a")),
            ops.struct(listOf(RexStruct.field(
                ops.lit(Datum.string("foo")),
                ops.cast(
                    ops.pathKey(
                        ops.pathKey(
                            ops.variable(
                                0, 0,
                                PType.row(PTypeField.of(
                                    "the_struct",
                                    PType.row(PTypeField.of("foo", PType.bigint())))
                                )
                            ),
                            ops.lit(Datum.string("the_struct"))
                        ),
                        ops.lit(Datum.string("foo"))
                    ),
                    PType.decimal(20, 1)
                ),
            )))
        )))
        val expectedRight = ops.struct(listOf(RexStruct.field(
            ops.lit(Datum.string("a")),
            ops.struct(listOf(RexStruct.field(
                ops.lit(Datum.string("foo")),
                ops.cast(
                    ops.pathKey(
                        ops.pathKey(
                            ops.variable(
                                0, 0,
                                PType.row(PTypeField.of(
                                    "the_struct",
                                    PType.row(PTypeField.of("foo", PType.decimal(2, 1))))
                                )
                            ),
                            ops.lit(Datum.string("the_struct"))
                        ),
                        ops.lit(Datum.string("foo"))
                    ),
                    PType.decimal(20, 1)
                ),
            )))
        )))

        assertSetOpPlan(expectedLeft, expectedRight, query)
    }

    private fun assertSetOpPlan(expectedLeft: RexStruct, expectedRight: RexStruct, query: String) {
        val plan = plan(query)
        val action = plan.action as Action.Query

        val leftFields = (action.rex as RexSelect).input.left().projections.single() as RexStruct
        val rightFields = (action.rex as RexSelect).input.right().projections.single() as RexStruct

        AssertingEquivalenceOperatorVisitor.assertEquals(expectedLeft, leftFields)
        AssertingEquivalenceOperatorVisitor.assertEquals(expectedRight, rightFields)
    }

    private fun Rel.left(): RelProject {
        return when (this) {
            is RelUnion -> this.left
            is RelIntersect -> this.left
            is RelExcept -> this.left
            else -> throw IllegalArgumentException("Expected a set operation, got ${this::class.java.name}")
        } as RelProject
    }

    private fun Rel.right(): RelProject {
        return when (this) {
            is RelUnion -> this.right
            is RelIntersect -> this.right
            is RelExcept -> this.right
            else -> throw IllegalArgumentException("Expected a set operation, got ${this::class.java.name}")
        } as RelProject
    }

    private fun plan(query: String): Plan {
        val parser = PartiQLParser.standard()
        val planner = PartiQLPlanner.standard()
        val session = Session.builder()
            .catalog("pql")
            .namespace("main")
            .catalogs(
                LocalCatalog.builder()
                    .name("pql")
                    .root(root)
                    .build()
            )
            .build()
        val ast = parser.parse(query).statements[0]
        val collector = PErrorCollector()
        return planner.plan(ast, session, Context.of(collector)).plan
    }

    private fun allSetOperations(): List<String> {
        // NOTE: OUTER UNION/EXCEPT/INTERSECT not supported
        return listOf(
            "UNION ALL",
            "UNION DISTINCT",
            "EXCEPT ALL",
            "EXCEPT DISTINCT",
            "INTERSECT ALL",
            "INTERSECT DISTINCT",
        )
    }
}
