package org.partiql.eval.internal.exclude

import org.junit.Assert.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.eval.internal.Compiler
import org.partiql.lang.util.ArgumentsProviderBase
import org.partiql.parser.PartiQLParser
import org.partiql.plan.Rel
import org.partiql.plan.Rex
import org.partiql.plan.Statement
import org.partiql.planner.PartiQLPlanner

class CompiledExcludeItemTest {
    private val planner = org.partiql.planner.PartiQLPlannerBuilder().build()
    private val parser = PartiQLParser.default()

    private fun getExcludeClause(statement: Statement): Rel.Op.Exclude {
        val queryExpr = (statement as Statement.Query).root
        val relProject = ((queryExpr.op as Rex.Op.Select).rel).op as Rel.Op.Project
        return ((relProject.input).op) as Rel.Op.Exclude
    }

    private fun testExcludeExprSubsumption(tc: SubsumptionTC) {
        val statement = parser.parse("SELECT * EXCLUDE ${tc.excludeExprStr} FROM <<>> AS s, <<>> AS t;").root
        val session = PartiQLPlanner.Session("q", "u")
        val plan = planner.plan(statement, session).plan
        val compiler = Compiler(plan, emptyMap())
        val excludeClause = getExcludeClause(plan.statement)
        val actualExcludeExprs = compiler.compileExcludeItems(excludeClause.items)
        assertEquals(tc.expectedExcludeExprs, actualExcludeExprs)
    }

    internal data class SubsumptionTC(val excludeExprStr: String, val expectedExcludeExprs: List<CompiledExcludeItem>)

    @ParameterizedTest
    @ArgumentsSource(ExcludeSubsumptionTests::class)
    internal fun subsumptionTests(tc: SubsumptionTC) = testExcludeExprSubsumption(tc)

    internal class ExcludeSubsumptionTests : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            SubsumptionTC(
                "s.a, t.a, t.b, s.b",
                listOf(
                    CompiledExcludeItem(
                        root = 0,
                        leaves = mutableSetOf(ExcludeLeaf(ExcludeStep.StructField("a", ExcludeFieldCase.INSENSITIVE)), ExcludeLeaf(ExcludeStep.StructField("b", ExcludeFieldCase.INSENSITIVE))),
                        branches = mutableSetOf()
                    ),
                    CompiledExcludeItem(
                        root = 1,
                        leaves = mutableSetOf(ExcludeLeaf(ExcludeStep.StructField("a", ExcludeFieldCase.INSENSITIVE)), ExcludeLeaf(ExcludeStep.StructField("b", ExcludeFieldCase.INSENSITIVE))),
                        branches = mutableSetOf()
                    ),
                )
            ),
            SubsumptionTC(
                "t.a, t.b",
                listOf(
                    CompiledExcludeItem(
                        root = 1,
                        leaves = mutableSetOf(ExcludeLeaf(ExcludeStep.StructField("a", ExcludeFieldCase.INSENSITIVE)), ExcludeLeaf(ExcludeStep.StructField("b", ExcludeFieldCase.INSENSITIVE))),
                        branches = mutableSetOf()
                    ),
                )
            ),
            SubsumptionTC(
                "t.a, t.b, t.a, t.b, t.b", // duplicates subsumed
                listOf(
                    CompiledExcludeItem(
                        root = 1,
                        leaves = mutableSetOf(ExcludeLeaf(ExcludeStep.StructField("a", ExcludeFieldCase.INSENSITIVE)), ExcludeLeaf(ExcludeStep.StructField("b", ExcludeFieldCase.INSENSITIVE))),
                        branches = mutableSetOf()
                    ),
                )
            ),
            SubsumptionTC(
                "t.a, t.b, t.*", // struct wildcard subsumes struct attr
                listOf(
                    CompiledExcludeItem(
                        root = 1,
                        leaves = mutableSetOf(ExcludeLeaf(ExcludeStep.StructWildcard)),
                        branches = mutableSetOf()
                    ),
                )
            ),
            SubsumptionTC( // removal at earlier step subsumes
                """
                t.a, t.a.a1,        -- t.a.a1 subsumed
                t.b.b1.b2, t.b.b1,  -- t.b.b1.b2 subsumed
                t.c, t.c.c1[2].c3[*].* -- t.c.c1[2].c3[*].* subsumed
                """,
                listOf(
                    CompiledExcludeItem(
                        root = 1,
                        leaves = mutableSetOf(ExcludeLeaf(ExcludeStep.StructField("a", ExcludeFieldCase.INSENSITIVE)), ExcludeLeaf(ExcludeStep.StructField("c", ExcludeFieldCase.INSENSITIVE))),
                        branches = mutableSetOf(
                            ExcludeBranch(
                                step = ExcludeStep.StructField("b", ExcludeFieldCase.INSENSITIVE),
                                leaves = mutableSetOf(ExcludeLeaf(ExcludeStep.StructField("b1", ExcludeFieldCase.INSENSITIVE))),
                                branches = mutableSetOf()
                            )
                        )
                    )
                )
            ),
            SubsumptionTC( // exclude collection index
                """
                t.a, t.a[1],
                t.b[1], t.b
                """,
                listOf(
                    CompiledExcludeItem(
                        root = 1,
                        leaves = mutableSetOf(ExcludeLeaf(ExcludeStep.StructField("a", ExcludeFieldCase.INSENSITIVE)), ExcludeLeaf(ExcludeStep.StructField("b", ExcludeFieldCase.INSENSITIVE))),
                        branches = mutableSetOf()
                    )
                )
            ),
            SubsumptionTC( // exclude collection index, collection wildcard
                """
                t.a[*], t.a[1],
                t.b[1], t.b[*],
                t.c[*], t.c[1].c1,
                t.d[1].d1, t.d[*]
                """,
                listOf(
                    CompiledExcludeItem(
                        root = 1,
                        leaves = mutableSetOf(),
                        branches = mutableSetOf(
                            ExcludeBranch(
                                step = ExcludeStep.StructField("a", ExcludeFieldCase.INSENSITIVE),
                                leaves = mutableSetOf(ExcludeLeaf(ExcludeStep.CollWildcard)),
                                branches = mutableSetOf()
                            ),
                            ExcludeBranch(
                                step = ExcludeStep.StructField("b", ExcludeFieldCase.INSENSITIVE),
                                leaves = mutableSetOf(ExcludeLeaf(ExcludeStep.CollWildcard)),
                                branches = mutableSetOf()
                            ),
                            ExcludeBranch(
                                step = ExcludeStep.StructField("c", ExcludeFieldCase.INSENSITIVE),
                                leaves = mutableSetOf(ExcludeLeaf(ExcludeStep.CollWildcard)),
                                branches = mutableSetOf()
                            ),
                            ExcludeBranch(
                                step = ExcludeStep.StructField("d", ExcludeFieldCase.INSENSITIVE),
                                leaves = mutableSetOf(ExcludeLeaf(ExcludeStep.CollWildcard)),
                                branches = mutableSetOf()
                            )
                        )
                    )
                )
            ),
            SubsumptionTC(
                """
                t.a[1].a1, t.a[1],
                t.b[1], t.b[1].b1,
                t.c[*], t.c[*].c1,
                t.d[*].d1, t.d[*],
                t.e[1], t.e[*].e1,  -- keep both
                t.f[*].f1, t.f[1],  -- keep both
                t.g[*], t.g[1].e1,
                t.h[1].f1, t.h[*]
                """,
                listOf(
                    CompiledExcludeItem(
                        root = 1,
                        leaves = mutableSetOf(),
                        branches = mutableSetOf(
                            ExcludeBranch(
                                step = ExcludeStep.StructField("a", ExcludeFieldCase.INSENSITIVE),
                                leaves = mutableSetOf(ExcludeLeaf(ExcludeStep.CollIndex(1))),
                                branches = mutableSetOf(),
                            ),
                            ExcludeBranch(
                                step = ExcludeStep.StructField("b", ExcludeFieldCase.INSENSITIVE),
                                leaves = mutableSetOf(ExcludeLeaf(ExcludeStep.CollIndex(1))),
                                branches = mutableSetOf(),
                            ),
                            ExcludeBranch(
                                step = ExcludeStep.StructField("c", ExcludeFieldCase.INSENSITIVE),
                                leaves = mutableSetOf(ExcludeLeaf(ExcludeStep.CollWildcard)),
                                branches = mutableSetOf(),
                            ),
                            ExcludeBranch(
                                step = ExcludeStep.StructField("d", ExcludeFieldCase.INSENSITIVE),
                                leaves = mutableSetOf(ExcludeLeaf(ExcludeStep.CollWildcard)),
                                branches = mutableSetOf(),
                            ),
                            ExcludeBranch(
                                step = ExcludeStep.StructField("e", ExcludeFieldCase.INSENSITIVE),
                                leaves = mutableSetOf(ExcludeLeaf(ExcludeStep.CollIndex(1))),
                                branches = mutableSetOf(
                                    ExcludeBranch(
                                        step = ExcludeStep.CollWildcard,
                                        leaves = mutableSetOf(ExcludeLeaf(ExcludeStep.StructField("e1", ExcludeFieldCase.INSENSITIVE))),
                                        branches = mutableSetOf(),
                                    )
                                ),
                            ),
                            ExcludeBranch(
                                step = ExcludeStep.StructField("f", ExcludeFieldCase.INSENSITIVE),
                                leaves = mutableSetOf(ExcludeLeaf(ExcludeStep.CollIndex(1))),
                                branches = mutableSetOf(
                                    ExcludeBranch(
                                        step = ExcludeStep.CollWildcard,
                                        leaves = mutableSetOf(ExcludeLeaf(ExcludeStep.StructField("f1", ExcludeFieldCase.INSENSITIVE))),
                                        branches = mutableSetOf(),
                                    )
                                ),
                            ),
                            ExcludeBranch(
                                step = ExcludeStep.StructField("g", ExcludeFieldCase.INSENSITIVE),
                                leaves = mutableSetOf(ExcludeLeaf(ExcludeStep.CollWildcard)),
                                branches = mutableSetOf(),
                            ),
                            ExcludeBranch(
                                step = ExcludeStep.StructField("h", ExcludeFieldCase.INSENSITIVE),
                                leaves = mutableSetOf(ExcludeLeaf(ExcludeStep.CollWildcard)),
                                branches = mutableSetOf(),
                            ),
                        ),
                    )
                )
            ),
            SubsumptionTC( // case sensitive
                """
                t.a, "t".a,
                "t".b, t.b,
                t."c", t.c,
                t.d, t."d"
                """,
                listOf(
                    CompiledExcludeItem(
                        root = 1,
                        leaves = mutableSetOf(
                            ExcludeLeaf(ExcludeStep.StructField("a", ExcludeFieldCase.INSENSITIVE)),
                            ExcludeLeaf(ExcludeStep.StructField("b", ExcludeFieldCase.INSENSITIVE)),
                            ExcludeLeaf(ExcludeStep.StructField("c", ExcludeFieldCase.INSENSITIVE)),
                            ExcludeLeaf(ExcludeStep.StructField("d", ExcludeFieldCase.INSENSITIVE)),
                            ExcludeLeaf(ExcludeStep.StructField("c", ExcludeFieldCase.SENSITIVE)),
                            ExcludeLeaf(ExcludeStep.StructField("d", ExcludeFieldCase.SENSITIVE)),
                        ),
                        branches = mutableSetOf(),
                    ),
                )
            ),
        )
    }
}
