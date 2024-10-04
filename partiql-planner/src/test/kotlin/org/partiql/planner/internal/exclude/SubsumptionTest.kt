package org.partiql.planner.internal.exclude

import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.parser.PartiQLParser
import org.partiql.plan.ExcludePath
import org.partiql.plan.ExcludeStep
import org.partiql.plan.Operation
import org.partiql.plan.rel.RelExclude
import org.partiql.plan.rel.RelProject
import org.partiql.plan.rex.RexSelect
import org.partiql.plan.rex.RexVar
import org.partiql.planner.PartiQLPlanner
import org.partiql.plugins.memory.MemoryCatalog
import org.partiql.spi.catalog.Session
import java.util.stream.Stream
import kotlin.test.assertEquals

class SubsumptionTest {

    companion object {

        private val planner = PartiQLPlanner.standard()
        private val parser = PartiQLParser.standard()
        private val catalog = MemoryCatalog.builder().name("default").build()
    }

    private fun getExcludeClause(statement: Operation): RelExclude {
        val queryExpr = (statement as Operation.Query).getRex()
        val relProject = (queryExpr as RexSelect).getInput() as RelProject
        return (relProject.getInput()) as RelExclude
    }

    private fun testExcludeExprSubsumption(tc: SubsumptionTC) {
        val text = "SELECT * EXCLUDE ${tc.excludeExprStr} FROM <<>> AS s, <<>> AS t;"
        val statement = parser.parse(text).root
        val session = Session.builder().catalog("default").catalogs(catalog).build()
        val plan = planner.plan(statement, session).plan
        val excludeClause = getExcludeClause(plan.getOperation()).getPaths()
        assertEquals(tc.expectedExcludeExprs, excludeClause)
    }

    data class SubsumptionTC(val excludeExprStr: String, val expectedExcludeExprs: List<ExcludePath>)

    @ParameterizedTest
    @ArgumentsSource(ExcludeSubsumptionTests::class)
    @Execution(ExecutionMode.CONCURRENT)
    fun subsumptionTests(tc: SubsumptionTC) = testExcludeExprSubsumption(tc)

    internal class ExcludeSubsumptionTests : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
            return parameters.map { Arguments.of(it) }.stream()
        }

        private val factory = org.partiql.plan.builder.PlanFactory.STANDARD

        private fun rexOpVar(binding: Int, position: Int): RexVar = factory.rexVar(binding, position)

        private val parameters = listOf(
            SubsumptionTC(
                "s.a, t.a", // different roots
                listOf(
                    ExcludePath.of(
                        root = rexOpVar(0, 0), steps = listOf(org.partiql.plan.ExcludeStep.symbol("a"))
                    ),
                    ExcludePath.of(
                        root = rexOpVar(0, 1), steps = listOf(org.partiql.plan.ExcludeStep.symbol("a"))
                    ),
                )
            ),
            SubsumptionTC(
                "t.a, t.b", // different steps
                listOf(
                    ExcludePath.of(
                        root = rexOpVar(0, 1),
                        steps = listOf(
                            org.partiql.plan.ExcludeStep.symbol("a"),
                            org.partiql.plan.ExcludeStep.symbol("b"),
                        )
                    ),
                )
            ),
            SubsumptionTC(
                "s.a, t.a, t.b, s.b", // different roots and steps
                listOf(
                    ExcludePath.of(
                        root = rexOpVar(0, 0),
                        steps = listOf(
                            ExcludeStep.symbol("a"),
                            ExcludeStep.symbol("b"),
                        )
                    ),
                    ExcludePath.of(
                        root = rexOpVar(0, 1),
                        steps = listOf(
                            ExcludeStep.symbol("a"),
                            ExcludeStep.symbol("b"),
                        )
                    )
                )
            ),
            SubsumptionTC(
                """-- duplicates subsumed
                t.a, t.a,
                t.b, t.b, t.b,
                t.c.*, t.c.*,
                t.d.*.e.f, t.d.*.e.f,
                t.g[*], t.g[*],
                t.h[1], t.h[1],
                t.i['j'], t.i."j"
                """.trimIndent(),
                listOf(
                    ExcludePath.of(
                        root = rexOpVar(0, 1),
                        steps = listOf(
                            ExcludeStep.symbol("a"),
                            ExcludeStep.symbol("b"),
                            ExcludeStep.symbol(
                                symbol = "c",
                                substeps = listOf(ExcludeStep.struct()),
                            ),
                            ExcludeStep.symbol(
                                symbol = "d",
                                substeps = listOf(
                                    ExcludeStep.struct(
                                        substeps = listOf(
                                            ExcludeStep.symbol(
                                                symbol = "e", substeps = listOf(ExcludeStep.symbol("f"))
                                            ),
                                        ),
                                    ),
                                ),
                            ),
                            ExcludeStep.symbol(
                                symbol = "g", substeps = listOf(ExcludeStep.collection())
                            ),
                            ExcludeStep.symbol(
                                symbol = "h", substeps = listOf(ExcludeStep.index(1))
                            ),
                            ExcludeStep.symbol(
                                symbol = "i", substeps = listOf(ExcludeStep.key("j"))
                            ),
                        )
                    ),
                )
            ),
            SubsumptionTC(
                """-- keep shorter exclude path
                t.a, t.a.a1,
                t.b.b1, t.b,
                t.c.*.c1, t.c.*.c1[*],
                t.d.*.d1[*], t.d.*.d1,
                t.e[1], t.e[1][*],
                t.f[1][*], t.f[1],
                t.g[*], t.g[*][1],
                t.h[*][1], t.h[*],
                t."i", t."i".*,
                t."j".*, t."j"
                """.trimIndent(),
                listOf(
                    ExcludePath.of(
                        root = rexOpVar(0, 1),
                        steps = listOf(
                            ExcludeStep.symbol("a"),
                            ExcludeStep.symbol("b"),
                            ExcludeStep.symbol(
                                symbol = "c",
                                substeps = listOf(
                                    ExcludeStep.struct(
                                        substeps = listOf(
                                            ExcludeStep.symbol("c1")
                                        )
                                    )
                                )
                            ),
                            ExcludeStep.symbol(
                                symbol = "d",
                                substeps = listOf(
                                    ExcludeStep.struct(
                                        substeps = listOf(ExcludeStep.symbol("d1")),
                                    )
                                )
                            ),
                            ExcludeStep.symbol(
                                symbol = "e", substeps = listOf(ExcludeStep.index(1))
                            ),
                            ExcludeStep.symbol(
                                symbol = "f", substeps = listOf(ExcludeStep.index(1))
                            ),
                            ExcludeStep.symbol(
                                symbol = "g", substeps = listOf(ExcludeStep.collection())
                            ),
                            ExcludeStep.symbol(
                                symbol = "h", substeps = listOf(ExcludeStep.collection())
                            ),
                            ExcludeStep.key("i"),
                            ExcludeStep.key("j"),
                        )
                    ),
                )
            ),
            SubsumptionTC(
                """-- struct wildcard subsumes struct symbol and struct key
                t.a.a1, t.a.a2[*].a3.*, t.a.*,              -- t.a.* subsumes
                t."b".*, t."b"."b1"[1], t."b".b2."b3"[*]    -- t."b".* subsumes
                """,
                listOf(
                    ExcludePath.of(
                        root = rexOpVar(0, 1),
                        steps = listOf(
                            ExcludeStep.symbol(
                                symbol = "a",
                                substeps = listOf(
                                    ExcludeStep.struct(
                                        substeps = emptyList()
                                    )
                                )
                            ),
                            ExcludeStep.key(
                                key = "b", substeps = listOf(ExcludeStep.struct())
                            ),
                        )
                    ),
                )
            ),
            SubsumptionTC(
                """-- collection wildcard subsumes collection index
                t.a[*], t.a[1], t.a[2][3].a4,               -- t.a[*] subsumes
                t.b."b1"[*], t.b."b1"[2], t.b."b1"[3][*]    -- t.b."b1"[*] subsumes
                """,
                listOf(
                    ExcludePath.of(
                        root = rexOpVar(0, 1),
                        steps = listOf(
                            ExcludeStep.symbol(
                                symbol = "a",
                                substeps = listOf(ExcludeStep.collection()),
                            ),
                            ExcludeStep.symbol(
                                symbol = "b",
                                substeps = listOf(
                                    ExcludeStep.key(
                                        key = "b1",
                                        substeps = listOf(ExcludeStep.collection()),
                                    ),
                                ),
                            ),
                        )
                    ),
                )
            ),
            SubsumptionTC(
                """-- struct symbol subsumes struct key
                t.foo, t."fOo",     -- t.foo subsumes
                t."bAr"."bAz", t."bAr".baz      -- t."bAr".baz subsumes
                """,
                listOf(
                    ExcludePath.of(
                        root = rexOpVar(0, 1),
                        steps = listOf(
                            ExcludeStep.symbol(symbol = "foo"),
                            ExcludeStep.key(
                                key = "bAr",
                                substeps = listOf(ExcludeStep.symbol(symbol = "baz")),
                            ),
                        ),
                    ),
                )
            ),
            SubsumptionTC(
                """-- collection wildcard subsumes collection index when tail subsumes
                t.a[*].a2, t.a[1].a2,    -- t.a[*].a2 subsumes
                t.b[1].b2.b3, t.b[*].b2.b3,    -- t.b[*].b2.b3 subsumes
                t.c[*].c2, t.c[1].c2.*[*].c3    -- t.c[*].c2 subsumes
                """,
                listOf(
                    ExcludePath.of(
                        root = rexOpVar(0, 1),
                        steps = listOf(
                            ExcludeStep.symbol(
                                symbol = "a",
                                substeps = listOf(
                                    ExcludeStep.collection(
                                        substeps = listOf(ExcludeStep.symbol("a2"))
                                    ),
                                ),
                            ),
                            ExcludeStep.symbol(
                                symbol = "b",
                                substeps = listOf(
                                    ExcludeStep.collection(
                                        substeps = listOf(
                                            ExcludeStep.symbol(
                                                symbol = "b2",
                                                substeps = listOf(ExcludeStep.symbol("b3")),
                                            ),
                                        )
                                    ),
                                ),
                            ),
                            ExcludeStep.symbol(
                                symbol = "c",
                                substeps = listOf(
                                    ExcludeStep.collection(
                                        substeps = listOf(ExcludeStep.symbol("c2"))
                                    ),
                                ),
                            ),
                        )
                    ),
                )
            ),
            SubsumptionTC(
                """-- struct wildcard subsumes struct symbol and key when tail subsumes
                t.a.*.a1, t.a.foo.a1, t.a.bar.a1, t.a.bat.a1.a2[*], t.a."baz".a1,    -- t.a.*.a1 subsumes
                t.b.b1.foo.b2.b3, t.b.b1.bar.b2.b3.b4, t.b.b1."bat".b2.b3, t.b.b1.*.*.b3    -- t.b.b1.*.*.b3 subsumes
                """,
                listOf(
                    ExcludePath.of(
                        root = rexOpVar(0, 1),
                        steps = listOf(
                            ExcludeStep.symbol(
                                symbol = "a",
                                substeps = listOf(
                                    ExcludeStep.struct(
                                        substeps = listOf(ExcludeStep.symbol("a1"))
                                    ),
                                ),
                            ),
                            ExcludeStep.symbol(
                                symbol = "b",
                                substeps = listOf(
                                    ExcludeStep.symbol(
                                        symbol = "b1",
                                        substeps = listOf(
                                            ExcludeStep.struct(
                                                substeps = listOf(
                                                    ExcludeStep.struct(
                                                        substeps = listOf(
                                                            ExcludeStep.symbol("b3")
                                                        )
                                                    )
                                                )
                                            )
                                        )
                                    ),
                                ),
                            )
                        ),
                    )
                ),
            ),
            SubsumptionTC(
                """-- struct symbol subsumes struct key when tail subsumes
                t.a.bar.a1, t.a."bAr".a1, t.a."bAR".a1.a2,  -- t.a.bar.a1 subsumes
                t.b."B1"."Bar".b2.b3, t.b.b1."Bar".b2.b3, t.b.b1."Bar".*.b3, t.b.b1.bar.*.b3 -- t.b.b1.bar.*.b3 subsumes
                """,
                listOf(
                    ExcludePath.of(
                        root = rexOpVar(0, 1),
                        steps = listOf(
                            ExcludeStep.symbol(
                                symbol = "a",
                                substeps = listOf(
                                    ExcludeStep.symbol(
                                        symbol = "bar",
                                        substeps = listOf(
                                            ExcludeStep.symbol("a1")
                                        ),
                                    ),
                                ),
                            ),
                            ExcludeStep.symbol(
                                symbol = "b",
                                substeps = listOf(
                                    ExcludeStep.symbol(
                                        symbol = "b1",
                                        substeps = listOf(
                                            ExcludeStep.symbol(
                                                symbol = "bar",
                                                substeps = listOf(
                                                    ExcludeStep.struct(
                                                        substeps = listOf(
                                                            ExcludeStep.symbol("b3")
                                                        )
                                                    )
                                                )
                                            )
                                        )
                                    ),
                                ),
                            ),
                        )
                    ),
                )
            ),
            SubsumptionTC(
                """-- case sensitive and case-insensitive roots
                t.a, "t".a,     -- t.a subsumes
                "t".b, t.b,     -- t.b subsumes
                t."c", t.c,     -- t.c subsumes
                t.d, t."d",     -- t.d subsumes
                "t".e,          -- included under resolved variable 1
                "t"."f"         -- included under resolved variable 1
                """,
                listOf(
                    ExcludePath.of(
                        root = rexOpVar(0, 1),
                        steps = listOf(
                            ExcludeStep.symbol(symbol = "a"),
                            ExcludeStep.symbol(symbol = "b"),
                            ExcludeStep.symbol(symbol = "c"),
                            ExcludeStep.symbol(symbol = "d"),
                            ExcludeStep.symbol(symbol = "e"),
                            ExcludeStep.key("f")
                        )
                    ),
                )
            ),
            SubsumptionTC(
                """-- no subsumption rules apply
                t.a.*.a1, t.a.*.a2,
                t.b.*.b2, t.b.b1.foo
                """,
                listOf(
                    ExcludePath.of(
                        root = rexOpVar(0, 1),
                        steps = listOf(
                            ExcludeStep.symbol(
                                symbol = "a",
                                substeps = listOf(
                                    ExcludeStep.struct(
                                        substeps = listOf(
                                            ExcludeStep.symbol("a1"),
                                            ExcludeStep.symbol("a2"),
                                        )
                                    )
                                )
                            ),
                            ExcludeStep.symbol(
                                symbol = "b",
                                substeps = listOf(
                                    ExcludeStep.struct(
                                        substeps = listOf(ExcludeStep.symbol("b2"))
                                    ),
                                    ExcludeStep.symbol(
                                        symbol = "b1",
                                        substeps = listOf(
                                            ExcludeStep.symbol("foo")
                                        )
                                    )
                                ),
                            )
                        ),
                    )
                ),
            )
        )
    }
}
