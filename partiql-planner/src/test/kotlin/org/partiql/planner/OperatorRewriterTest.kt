package org.partiql.planner

import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.parser.PartiQLParser
import org.partiql.plan.Action
import org.partiql.plan.Operator
import org.partiql.plan.OperatorRewriter
import org.partiql.plan.Plan
import org.partiql.plan.rel.RelProject
import org.partiql.plan.rex.RexLit
import org.partiql.plan.rex.RexStruct
import org.partiql.planner.internal.TestCatalog
import org.partiql.spi.catalog.Name
import org.partiql.spi.catalog.Session
import org.partiql.spi.value.Datum
import org.partiql.types.BagType
import org.partiql.types.StaticType
import org.partiql.types.StructType
import org.partiql.types.TupleConstraint
import org.partiql.types.fromStaticType

/**
 * Tests for [OperatorRewriter] by planning an original query, applying a rewriter, then comparing
 * the plan against a target query's plan.
 *
 * 1. Plan the [SuccessTestCase.originalQuery]
 * 2. Apply the per-case [SuccessTestCase.rewriter] to the original plan's root rex
 * 3. Plan the [SuccessTestCase.targetQuery]
 * 4. Assert equivalence via [PlanEquivalenceOperatorVisitor]
 */
/**
 * An operator rewriter that adds a validation step.
 */

internal class OperatorRewriterTest {

    data class SuccessTestCase(
        val name: String,
        val originalQuery: String,
        val targetQuery: String,
        val rewriter: RewriterWithValidation<Void?>,
        val catalog: String = "default",
        val catalogPath: List<String> = emptyList(),
    ) {
        override fun toString(): String = name
    }

    companion object {

        private val parser = PartiQLParser.standard()
        private val planner = PartiQLPlanner.builder().build()

        private val schema = fromStaticType(
            BagType(
                StructType(
                    listOf(
                        StructType.Field("a", StaticType.BOOL),
                        StructType.Field("b", StaticType.INT4),
                    ),
                    contentClosed = true,
                    emptyList(),
                    setOf(TupleConstraint.Open(false)),
                    emptyMap()
                )
            )
        )

        private val catalogs = listOf(
            TestCatalog.builder()
                .name("default")
                .createTable(Name.of("SCHEMA", "T"), schema)
                .build()
        )

        private fun plan(query: String, catalog: String, catalogPath: List<String>): Plan {
            val session = Session.builder()
                .catalog(catalog)
                .catalogs(*catalogs.toTypedArray())
                .namespace(catalogPath)
                .build()
            val parseResult = parser.parse(query)
            val ast = parseResult.statements[0]
            return planner.plan(ast, session).plan
        }

        abstract class RewriterWithValidation<C> : OperatorRewriter<C>() {
            abstract fun validate(actual: Plan, expected: Plan): Boolean
        }

        /**
         * Rewriter that renames output struct field keys to col_0, col_1, ...
         *
         * In a SQL SELECT, the plan produces a RexStruct whose Field keys are RexLit strings
         * holding the column names. This rewriter replaces those keys with sequential names.
         */
        private class ColumnRenamingRewriter : RewriterWithValidation<Void?>() {
            override fun validate(actual: Plan, expected: Plan): Boolean {
                val originalAction = actual.action as Action.Query

                // Apply the rewriter
                val rewrittenRex = this.visitRex(originalAction.rex, null)
                // Find the first RelProject in the rewritten tree
                val rewrittenProject = findFirstRelProject(rewrittenRex)
                val actualNames = extractColumnNames(rewrittenProject!!)

                // Plan the target query and extract column names from its first RelProject
                val targetAction = expected.action as Action.Query
                val targetProject = findFirstRelProject(targetAction.rex)
                val expectedNames = extractColumnNames(targetProject!!)
                // TODO [PlanEquivalenceOperatorVisitor] is not fully implemented to compare plan.
                return actualNames == expectedNames
            }

            override fun visitStruct(rex: RexStruct, ctx: Void?): Operator {
                val fields = rex.getFields()
                val renamedFields = fields.mapIndexed { i, field ->
                    val newKey = RexLit.create(Datum.string("col_$i"))
                    val newValue = visitRex(field.getValue(), ctx)
                    RexStruct.field(newKey, newValue)
                }
                val newStruct = RexStruct.create(renamedFields)
                newStruct.setType(rex.getType())
                return newStruct
            }

            /**
             * Finds the first [RelProject] by walking the operator tree depth-first.
             */
            private fun findFirstRelProject(operator: Operator): RelProject? {
                if (operator is RelProject) return operator
                for (operand in operator.getOperands()) {
                    for (child in operand) {
                        val found = findFirstRelProject(child)
                        if (found != null) return found
                    }
                }
                return null
            }

            /**
             * Extracts column names from the projections struct of a [RelProject].
             *
             * In a SQL SELECT, the projections list contains a single [RexStruct] whose [RexStruct.Field] keys
             * are [RexLit] strings holding the column names.
             */
            private fun extractColumnNames(project: RelProject): List<String> {
                return project.getProjections()
                    .filterIsInstance<RexStruct>()
                    .flatMap { it.getFields() }
                    .map { field ->
                        val key = field.getKey() as RexLit
                        key.getDatum().getString()
                    }
            }
        }

        @JvmStatic
        fun visitWithCases() = listOf(
            SuccessTestCase(
                name = "Column renaming rewriter renames with `WITH` statement",
                originalQuery = """
                    WITH x AS (SELECT * FROM T AS t)
                    SELECT a, b FROM x
                """.trimIndent(),
                targetQuery = """
                    WITH x AS (SELECT * FROM T AS t)
                    SELECT a AS col_0, b as col_1 FROM x
                """.trimIndent(),
                rewriter = ColumnRenamingRewriter(),
            ),
            SuccessTestCase(
                name = "Column renaming rewriter renames without WITH statement",
                originalQuery = """
                    SELECT a, b FROM T
                """.trimIndent(),
                targetQuery = """
                    SELECT a AS col_0, b as col_1 FROM T
                """.trimIndent(),
                rewriter = ColumnRenamingRewriter(),
            )
        )
    }

    private fun runTest(tc: SuccessTestCase) {
        val o = plan(tc.originalQuery, tc.catalog, tc.catalogPath)
        val t = plan(tc.targetQuery, tc.catalog, tc.catalogPath)
        assert(tc.rewriter.validate(o, t), { tc.name })
    }

    @ParameterizedTest
    @MethodSource("visitWithCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun testVisitWith(tc: SuccessTestCase) = runTest(tc)
}
