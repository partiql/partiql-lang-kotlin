package org.partiql.planner.internal.exclude

import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.parser.PartiQLParser
import org.partiql.plan.v1.Statement
import org.partiql.plan.v1.builder.PlanFactory
import org.partiql.plan.v1.operator.rel.RelExclude
import org.partiql.plan.v1.operator.rel.RelExcludePath
import org.partiql.plan.v1.operator.rel.RelExcludeStep
import org.partiql.plan.v1.operator.rel.RelProject
import org.partiql.plan.v1.operator.rex.RexSelect
import org.partiql.plan.v1.operator.rex.RexVar
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

    private fun getExcludeClause(statement: Statement): RelExclude {
        val queryExpr = (statement as Statement.Query).getRoot()
        val relProject = (queryExpr as RexSelect).getInput() as RelProject
        return (relProject.getInput()) as RelExclude
    }

    private fun testExcludeExprSubsumption(tc: SubsumptionTC) {
        val text = "SELECT * EXCLUDE ${tc.excludeExprStr} FROM <<>> AS s, <<>> AS t;"
        val statement = parser.parse(text).root
        val session = Session.builder().catalog("default").catalogs(catalog).build()
        val plan = planner.plan(statement, session).plan
        val excludeClause = getExcludeClause(plan.getStatement()).getPaths()
        assertEquals(tc.expectedExcludeExprs, excludeClause)
    }

    data class SubsumptionTC(val excludeExprStr: String, val expectedExcludeExprs: List<RelExcludePath>)

    @ParameterizedTest
    @ArgumentsSource(ExcludeSubsumptionTests::class)
    @Execution(ExecutionMode.CONCURRENT)
    fun subsumptionTests(tc: SubsumptionTC) = testExcludeExprSubsumption(tc)

    internal class ExcludeSubsumptionTests : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
            return parameters.map { Arguments.of(it) }.stream()
        }

        private val factory = PlanFactory.STANDARD

        private fun rexOpVar(binding: Int, position: Int): RexVar = factory.rexVar(binding, position)

        private val parameters = listOf(
            SubsumptionTC(
                "s.a, t.a", // different roots
                listOf(
                    RelExcludePath.of(
                        root = rexOpVar(0, 0), steps = listOf(RelExcludeStep.symbol("a"))
                    ),
                    RelExcludePath.of(
                        root = rexOpVar(0, 1), steps = listOf(RelExcludeStep.symbol("a"))
                    ),
                )
            ),
            SubsumptionTC(
                "t.a, t.b", // different steps
                listOf(
                    RelExcludePath.of(
                        root = rexOpVar(0, 1),
                        steps = listOf(
                            RelExcludeStep.symbol("a"),
                            RelExcludeStep.symbol("b"),
                        )
                    ),
                )
            ),
            SubsumptionTC(
                "s.a, t.a, t.b, s.b", // different roots and steps
                listOf(
                    RelExcludePath.of(
                        root = rexOpVar(0, 0),
                        steps = listOf(
                            RelExcludeStep.symbol("a"),
                            RelExcludeStep.symbol("b"),
                        )
                    ),
                    RelExcludePath.of(
                        root = rexOpVar(0, 1),
                        steps = listOf(
                            RelExcludeStep.symbol("a"),
                            RelExcludeStep.symbol("b"),
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
                    RelExcludePath.of(
                        root = rexOpVar(0, 1),
                        steps = listOf(
                            RelExcludeStep.symbol("a"),
                            RelExcludeStep.symbol("b"),
                            RelExcludeStep.symbol(
                                symbol = "c",
                                substeps = listOf(RelExcludeStep.struct()),
                            ),
                            RelExcludeStep.symbol(
                                symbol = "d",
                                substeps = listOf(
                                    RelExcludeStep.struct(
                                        substeps = listOf(
                                            RelExcludeStep.symbol(
                                                symbol = "e", substeps = listOf(RelExcludeStep.symbol("f"))
                                            ),
                                        ),
                                    ),
                                ),
                            ),
                            RelExcludeStep.symbol(
                                symbol = "g", substeps = listOf(RelExcludeStep.collection())
                            ),
                            RelExcludeStep.symbol(
                                symbol = "h", substeps = listOf(RelExcludeStep.index(1))
                            ),
                            RelExcludeStep.symbol(
                                symbol = "i", substeps = listOf(RelExcludeStep.key("j"))
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
                    RelExcludePath.of(
                        root = rexOpVar(0, 1),
                        steps = listOf(
                            RelExcludeStep.symbol("a"),
                            RelExcludeStep.symbol("b"),
                            RelExcludeStep.symbol(
                                symbol = "c",
                                substeps = listOf(
                                    RelExcludeStep.struct(
                                        substeps = listOf(
                                            RelExcludeStep.symbol("c1")
                                        )
                                    )
                                )
                            ),
                            RelExcludeStep.symbol(
                                symbol = "d",
                                substeps = listOf(
                                    RelExcludeStep.struct(
                                        substeps = listOf(RelExcludeStep.symbol("d1")),
                                    )
                                )
                            ),
                            RelExcludeStep.symbol(
                                symbol = "e", substeps = listOf(RelExcludeStep.index(1))
                            ),
                            RelExcludeStep.symbol(
                                symbol = "f", substeps = listOf(RelExcludeStep.index(1))
                            ),
                            RelExcludeStep.symbol(
                                symbol = "g", substeps = listOf(RelExcludeStep.collection())
                            ),
                            RelExcludeStep.symbol(
                                symbol = "h", substeps = listOf(RelExcludeStep.collection())
                            ),
                            RelExcludeStep.key("i"),
                            RelExcludeStep.key("j"),
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
                    RelExcludePath.of(
                        root = rexOpVar(0, 1),
                        steps = listOf(
                            RelExcludeStep.symbol(
                                symbol = "a",
                                substeps = listOf(
                                    RelExcludeStep.struct(
                                        substeps = emptyList()
                                    )
                                )
                            ),
                            RelExcludeStep.key(
                                key = "b", substeps = listOf(RelExcludeStep.struct())
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
                    RelExcludePath.of(
                        root = rexOpVar(0, 1),
                        steps = listOf(
                            RelExcludeStep.symbol(
                                symbol = "a",
                                substeps = listOf(RelExcludeStep.collection()),
                            ),
                            RelExcludeStep.symbol(
                                symbol = "b",
                                substeps = listOf(
                                    RelExcludeStep.key(
                                        key = "b1",
                                        substeps = listOf(RelExcludeStep.collection()),
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
                    RelExcludePath.of(
                        root = rexOpVar(0, 1),
                        steps = listOf(
                            RelExcludeStep.symbol(symbol = "foo"),
                            RelExcludeStep.key(
                                key = "bAr",
                                substeps = listOf(RelExcludeStep.symbol(symbol = "baz")),
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
                    RelExcludePath.of(
                        root = rexOpVar(0, 1),
                        steps = listOf(
                            RelExcludeStep.symbol(
                                symbol = "a",
                                substeps = listOf(
                                    RelExcludeStep.collection(
                                        substeps = listOf(RelExcludeStep.symbol("a2"))
                                    ),
                                ),
                            ),
                            RelExcludeStep.symbol(
                                symbol = "b",
                                substeps = listOf(
                                    RelExcludeStep.collection(
                                        substeps = listOf(
                                            RelExcludeStep.symbol(
                                                symbol = "b2",
                                                substeps = listOf(RelExcludeStep.symbol("b3")),
                                            ),
                                        )
                                    ),
                                ),
                            ),
                            RelExcludeStep.symbol(
                                symbol = "c",
                                substeps = listOf(
                                    RelExcludeStep.collection(
                                        substeps = listOf(RelExcludeStep.symbol("c2"))
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
                    RelExcludePath.of(
                        root = rexOpVar(0, 1),
                        steps = listOf(
                            RelExcludeStep.symbol(
                                symbol = "a",
                                substeps = listOf(
                                    RelExcludeStep.struct(
                                        substeps = listOf(RelExcludeStep.symbol("a1"))
                                    ),
                                ),
                            ),
                            RelExcludeStep.symbol(
                                symbol = "b",
                                substeps = listOf(
                                    RelExcludeStep.symbol(
                                        symbol = "b1",
                                        substeps = listOf(
                                            RelExcludeStep.struct(
                                                substeps = listOf(
                                                    RelExcludeStep.struct(
                                                        substeps = listOf(
                                                            RelExcludeStep.symbol("b3")
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
                    RelExcludePath.of(
                        root = rexOpVar(0, 1),
                        steps = listOf(
                            RelExcludeStep.symbol(
                                symbol = "a",
                                substeps = listOf(
                                    RelExcludeStep.symbol(
                                        symbol = "bar",
                                        substeps = listOf(
                                            RelExcludeStep.symbol("a1")
                                        ),
                                    ),
                                ),
                            ),
                            RelExcludeStep.symbol(
                                symbol = "b",
                                substeps = listOf(
                                    RelExcludeStep.symbol(
                                        symbol = "b1",
                                        substeps = listOf(
                                            RelExcludeStep.symbol(
                                                symbol = "bar",
                                                substeps = listOf(
                                                    RelExcludeStep.struct(
                                                        substeps = listOf(
                                                            RelExcludeStep.symbol("b3")
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
                    RelExcludePath.of(
                        root = rexOpVar(0, 1),
                        steps = listOf(
                            RelExcludeStep.symbol(symbol = "a"),
                            RelExcludeStep.symbol(symbol = "b"),
                            RelExcludeStep.symbol(symbol = "c"),
                            RelExcludeStep.symbol(symbol = "d"),
                            RelExcludeStep.symbol(symbol = "e"),
                            RelExcludeStep.key("f")
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
                    RelExcludePath.of(
                        root = rexOpVar(0, 1),
                        steps = listOf(
                            RelExcludeStep.symbol(
                                symbol = "a",
                                substeps = listOf(
                                    RelExcludeStep.struct(
                                        substeps = listOf(
                                            RelExcludeStep.symbol("a1"),
                                            RelExcludeStep.symbol("a2"),
                                        )
                                    )
                                )
                            ),
                            RelExcludeStep.symbol(
                                symbol = "b",
                                substeps = listOf(
                                    RelExcludeStep.struct(
                                        substeps = listOf(RelExcludeStep.symbol("b2"))
                                    ),
                                    RelExcludeStep.symbol(
                                        symbol = "b1",
                                        substeps = listOf(
                                            RelExcludeStep.symbol("foo")
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
