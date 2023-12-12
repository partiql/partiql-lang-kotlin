package org.partiql.lang.eval.internal.exclude

import com.amazon.ionelement.api.emptyMetaContainer
import org.junit.Assert.assertEquals
import org.junit.jupiter.api.fail
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.annotations.ExperimentalPartiQLCompilerPipeline
import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.planner.PartiQLPlanner
import org.partiql.lang.planner.PartiQLPlannerBuilder
import org.partiql.lang.syntax.PartiQLParserBuilder
import org.partiql.lang.util.ArgumentsProviderBase
import org.partiql.pig.runtime.LongPrimitive

@OptIn(ExperimentalPartiQLCompilerPipeline::class)
class CompiledExcludeExprTest {
    private val parser = PartiQLParserBuilder.standard().build()
    private val planner = PartiQLPlannerBuilder.standard().build()

    private fun getExcludeClause(statement: PartiqlPhysical.Statement): PartiqlPhysical.Bexpr.ExcludeClause {
        val queryExpr = (statement as PartiqlPhysical.Statement.Query).expr
        val bindingsToValueQuery = (queryExpr as PartiqlPhysical.Expr.BindingsToValues).query
        return (bindingsToValueQuery as PartiqlPhysical.Bexpr.ExcludeClause)
    }

    private fun testExcludeExprSubsumption(tc: SubsumptionTC) {
        val statement = parser.parseAstStatement("SELECT * EXCLUDE ${tc.excludeExprStr} FROM <<>> AS s, <<>> AS t")
        val physicalPlan = when (val planningResult = planner.plan(statement)) {
            is PartiQLPlanner.Result.Success -> planningResult.plan
            is PartiQLPlanner.Result.Error -> fail("Expected no errors but found ${planningResult.problems}")
        }
        val excludeClause = getExcludeClause(physicalPlan.stmt)
        val actualExcludeExprs = compileExcludeClause(excludeClause)
        assertEquals(tc.expectedExcludeExprs, actualExcludeExprs)
    }

    internal data class SubsumptionTC(val excludeExprStr: String, val expectedExcludeExprs: List<CompiledExcludeExpr>)

    @ParameterizedTest
    @ArgumentsSource(ExcludeSubsumptionTests::class)
    internal fun subsumptionTests(tc: SubsumptionTC) = testExcludeExprSubsumption(tc)

    internal class ExcludeSubsumptionTests : ArgumentsProviderBase() {
        private fun caseSensitiveId(id: String): PartiqlPhysical.Identifier {
            return PartiqlPhysical.build { identifier(name = id, case = caseSensitive(emptyMetaContainer())) }
        }
        private fun caseInsensitiveId(id: String): PartiqlPhysical.Identifier {
            return PartiqlPhysical.build { identifier(name = id, case = caseInsensitive(emptyMetaContainer())) }
        }
        private fun exTupleAttr(id: PartiqlPhysical.Identifier): PartiqlPhysical.ExcludeStep {
            return PartiqlPhysical.ExcludeStep.ExcludeTupleAttr(id)
        }
        private fun exTupleWildcard(): PartiqlPhysical.ExcludeStep {
            return PartiqlPhysical.ExcludeStep.ExcludeTupleWildcard()
        }
        private fun exCollIndex(i: Int): PartiqlPhysical.ExcludeStep {
            return PartiqlPhysical.ExcludeStep.ExcludeCollectionIndex(index = LongPrimitive(i.toLong(), emptyMetaContainer()))
        }
        private fun exCollWildcard(): PartiqlPhysical.ExcludeStep {
            return PartiqlPhysical.ExcludeStep.ExcludeCollectionWildcard()
        }

        override fun getParameters(): List<Any> = listOf(
            SubsumptionTC(
                "s.a, t.a, t.b, s.b",
                listOf(
                    CompiledExcludeExpr(
                        root = 0,
                        leaves = mutableSetOf(ExcludeLeaf(exTupleAttr(caseInsensitiveId("a"))), ExcludeLeaf(exTupleAttr(caseInsensitiveId("b")))),
                        branches = mutableSetOf()
                    ),
                    CompiledExcludeExpr(
                        root = 1,
                        leaves = mutableSetOf(ExcludeLeaf(exTupleAttr(caseInsensitiveId("a"))), ExcludeLeaf(exTupleAttr(caseInsensitiveId("b")))),
                        branches = mutableSetOf()
                    ),
                )
            ),
            SubsumptionTC(
                "t.a, t.b",
                listOf(
                    CompiledExcludeExpr(
                        root = 1,
                        leaves = mutableSetOf(ExcludeLeaf(exTupleAttr(caseInsensitiveId("a"))), ExcludeLeaf(exTupleAttr(caseInsensitiveId("b")))),
                        branches = mutableSetOf()
                    ),
                )
            ),
            SubsumptionTC(
                "t.a, t.b, t.a, t.b, t.b", // duplicates subsumed
                listOf(
                    CompiledExcludeExpr(
                        root = 1,
                        leaves = mutableSetOf(ExcludeLeaf(exTupleAttr(caseInsensitiveId("a"))), ExcludeLeaf(exTupleAttr(caseInsensitiveId("b")))),
                        branches = mutableSetOf()
                    ),
                )
            ),
            SubsumptionTC(
                "t.a, t.b, t.*", // tuple wildcard subsumes tuple attr
                listOf(
                    CompiledExcludeExpr(
                        root = 1,
                        leaves = mutableSetOf(ExcludeLeaf(exTupleWildcard())),
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
                    CompiledExcludeExpr(
                        root = 1,
                        leaves = mutableSetOf(ExcludeLeaf(exTupleAttr(caseInsensitiveId("a"))), ExcludeLeaf(exTupleAttr(caseInsensitiveId("c")))),
                        branches = mutableSetOf(
                            ExcludeBranch(
                                step = exTupleAttr(caseInsensitiveId("b")),
                                leaves = mutableSetOf(ExcludeLeaf(exTupleAttr(caseInsensitiveId("b1")))),
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
                    CompiledExcludeExpr(
                        root = 1,
                        leaves = mutableSetOf(ExcludeLeaf(exTupleAttr(caseInsensitiveId("a"))), ExcludeLeaf(exTupleAttr(caseInsensitiveId("b")))),
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
                    CompiledExcludeExpr(
                        root = 1,
                        leaves = mutableSetOf(),
                        branches = mutableSetOf(
                            ExcludeBranch(
                                step = exTupleAttr(caseInsensitiveId("a")),
                                leaves = mutableSetOf(ExcludeLeaf(exCollWildcard())),
                                branches = mutableSetOf()
                            ),
                            ExcludeBranch(
                                step = exTupleAttr(caseInsensitiveId("b")),
                                leaves = mutableSetOf(ExcludeLeaf(exCollWildcard())),
                                branches = mutableSetOf()
                            ),
                            ExcludeBranch(
                                step = exTupleAttr(caseInsensitiveId("c")),
                                leaves = mutableSetOf(ExcludeLeaf(exCollWildcard())),
                                branches = mutableSetOf()
                            ),
                            ExcludeBranch(
                                step = exTupleAttr(caseInsensitiveId("d")),
                                leaves = mutableSetOf(ExcludeLeaf(exCollWildcard())),
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
                    CompiledExcludeExpr(
                        root = 1,
                        leaves = mutableSetOf(),
                        branches = mutableSetOf(
                            ExcludeBranch(
                                step = exTupleAttr(caseInsensitiveId("a")),
                                leaves = mutableSetOf(ExcludeLeaf(exCollIndex(1))),
                                branches = mutableSetOf(),
                            ),
                            ExcludeBranch(
                                step = exTupleAttr(caseInsensitiveId("b")),
                                leaves = mutableSetOf(ExcludeLeaf(exCollIndex(1))),
                                branches = mutableSetOf(),
                            ),
                            ExcludeBranch(
                                step = exTupleAttr(caseInsensitiveId("c")),
                                leaves = mutableSetOf(ExcludeLeaf(exCollWildcard())),
                                branches = mutableSetOf(),
                            ),
                            ExcludeBranch(
                                step = exTupleAttr(caseInsensitiveId("d")),
                                leaves = mutableSetOf(ExcludeLeaf(exCollWildcard())),
                                branches = mutableSetOf(),
                            ),
                            ExcludeBranch(
                                step = exTupleAttr(caseInsensitiveId("e")),
                                leaves = mutableSetOf(ExcludeLeaf(exCollIndex(1))),
                                branches = mutableSetOf(
                                    ExcludeBranch(
                                        step = exCollWildcard(),
                                        leaves = mutableSetOf(ExcludeLeaf(exTupleAttr(caseInsensitiveId("e1")))),
                                        branches = mutableSetOf(),
                                    )
                                ),
                            ),
                            ExcludeBranch(
                                step = exTupleAttr(caseInsensitiveId("f")),
                                leaves = mutableSetOf(ExcludeLeaf(exCollIndex(1))),
                                branches = mutableSetOf(
                                    ExcludeBranch(
                                        step = exCollWildcard(),
                                        leaves = mutableSetOf(ExcludeLeaf(exTupleAttr(caseInsensitiveId("f1")))),
                                        branches = mutableSetOf(),
                                    )
                                ),
                            ),
                            ExcludeBranch(
                                step = exTupleAttr(caseInsensitiveId("g")),
                                leaves = mutableSetOf(ExcludeLeaf(exCollWildcard())),
                                branches = mutableSetOf(),
                            ),
                            ExcludeBranch(
                                step = exTupleAttr(caseInsensitiveId("h")),
                                leaves = mutableSetOf(ExcludeLeaf(exCollWildcard())),
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
                    CompiledExcludeExpr(
                        root = 1,
                        leaves = mutableSetOf(
                            ExcludeLeaf(exTupleAttr(caseInsensitiveId("a"))),
                            ExcludeLeaf(exTupleAttr(caseInsensitiveId("b"))),
                            ExcludeLeaf(exTupleAttr(caseInsensitiveId("c"))),
                            ExcludeLeaf(exTupleAttr(caseInsensitiveId("d"))),
                            ExcludeLeaf(exTupleAttr(caseSensitiveId("c"))),
                            ExcludeLeaf(exTupleAttr(caseSensitiveId("d")))
                        ),
                        branches = mutableSetOf(),
                    ),
                )
            ),
        )
    }
}
