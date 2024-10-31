package org.partiql.planner.internal.exclude

import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.parser.V1PartiQLParser
import org.partiql.plan.Exclusion
import org.partiql.plan.Operation
import org.partiql.plan.builder.PlanFactory
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
        private val parser = V1PartiQLParser.standard()
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
        val excludeClause = getExcludeClause(plan.getOperation()).getExclusions()
        assertEquals(tc.expectedExcludeExprs, excludeClause)
    }

    data class SubsumptionTC(val excludeExprStr: String, val expectedExcludeExprs: List<Exclusion>)

    @ParameterizedTest
    @ArgumentsSource(ExcludeSubsumptionTests::class)
    @Execution(ExecutionMode.CONCURRENT)
    fun subsumptionTests(tc: SubsumptionTC) = testExcludeExprSubsumption(tc)

    internal class ExcludeSubsumptionTests : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
            return parameters.map { Arguments.of(it) }.stream()
        }

        private val factory = PlanFactory.STANDARD

        private fun rexOpVar(depth: Int, offset: Int): RexVar = factory.rexVar(depth, offset)

        private val parameters = listOf(
            SubsumptionTC(
                "s.a, t.a", // different variables
                listOf(
                    Exclusion(
                        variable = rexOpVar(0, 0),
                        items = listOf(Exclusion.structSymbol("a")),
                    ),
                    Exclusion(
                        variable = rexOpVar(0, 1),
                        items = listOf(Exclusion.structSymbol("a")),
                    ),
                )
            ),
            SubsumptionTC(
                "t.a, t.b", // different items
                listOf(
                    Exclusion(
                        variable = rexOpVar(0, 1),
                        items = listOf(
                            Exclusion.structSymbol("a"),
                            Exclusion.structSymbol("b"),
                        )
                    ),
                )
            ),
            SubsumptionTC(
                "s.a, t.a, t.b, s.b", // different variables and items
                listOf(
                    Exclusion(
                        variable = rexOpVar(0, 0),
                        items = listOf(
                            Exclusion.structSymbol("a"),
                            Exclusion.structSymbol("b"),
                        )
                    ),
                    Exclusion(
                        variable = rexOpVar(0, 1),
                        items = listOf(
                            Exclusion.structSymbol("a"),
                            Exclusion.structSymbol("b"),
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
                    Exclusion(
                        variable = rexOpVar(0, 1),
                        items = listOf(
                            Exclusion.structSymbol("a"),
                            Exclusion.structSymbol("b"),
                            Exclusion.structSymbol(
                                symbol = "c",
                                children = listOf(Exclusion.structWildCard()),
                            ),
                            Exclusion.structSymbol(
                                symbol = "d",
                                children = listOf(
                                    Exclusion.structWildCard(
                                        children = listOf(
                                            Exclusion.structSymbol(
                                                symbol = "e", children = listOf(Exclusion.structSymbol("f"))
                                            ),
                                        ),
                                    ),
                                ),
                            ),
                            Exclusion.structSymbol(
                                symbol = "g", children = listOf(Exclusion.collWildcard())
                            ),
                            Exclusion.structSymbol(
                                symbol = "h", children = listOf(Exclusion.collIndex(1))
                            ),
                            Exclusion.structSymbol(
                                symbol = "i", children = listOf(Exclusion.structKey("j"))
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
                    Exclusion(
                        variable = rexOpVar(0, 1),
                        items = listOf(
                            Exclusion.structSymbol("a"),
                            Exclusion.structSymbol("b"),
                            Exclusion.structSymbol(
                                symbol = "c",
                                children = listOf(
                                    Exclusion.structWildCard(
                                        children = listOf(
                                            Exclusion.structSymbol("c1")
                                        )
                                    )
                                )
                            ),
                            Exclusion.structSymbol(
                                symbol = "d",
                                children = listOf(
                                    Exclusion.structWildCard(
                                        children = listOf(Exclusion.structSymbol("d1")),
                                    )
                                )
                            ),
                            Exclusion.structSymbol(
                                symbol = "e", children = listOf(Exclusion.collIndex(1))
                            ),
                            Exclusion.structSymbol(
                                symbol = "f", children = listOf(Exclusion.collIndex(1))
                            ),
                            Exclusion.structSymbol(
                                symbol = "g", children = listOf(Exclusion.collWildcard())
                            ),
                            Exclusion.structSymbol(
                                symbol = "h", children = listOf(Exclusion.collWildcard())
                            ),
                            Exclusion.structKey("i"),
                            Exclusion.structKey("j"),
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
                    Exclusion(
                        variable = rexOpVar(0, 1),
                        items = listOf(
                            Exclusion.structSymbol(
                                symbol = "a",
                                children = listOf(
                                    Exclusion.structWildCard(
                                        children = emptyList()
                                    )
                                )
                            ),
                            Exclusion.structKey(
                                key = "b", children = listOf(Exclusion.structWildCard())
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
                    Exclusion(
                        variable = rexOpVar(0, 1),
                        items = listOf(
                            Exclusion.structSymbol(
                                symbol = "a",
                                children = listOf(Exclusion.collWildcard()),
                            ),
                            Exclusion.structSymbol(
                                symbol = "b",
                                children = listOf(
                                    Exclusion.structKey(
                                        key = "b1",
                                        children = listOf(Exclusion.collWildcard()),
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
                    Exclusion(
                        variable = rexOpVar(0, 1),
                        items = listOf(
                            Exclusion.structSymbol(symbol = "foo"),
                            Exclusion.structKey(
                                key = "bAr",
                                children = listOf(Exclusion.structSymbol(symbol = "baz")),
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
                    Exclusion(
                        variable = rexOpVar(0, 1),
                        items = listOf(
                            Exclusion.structSymbol(
                                symbol = "a",
                                children = listOf(
                                    Exclusion.collWildcard(
                                        children = listOf(Exclusion.structSymbol("a2"))
                                    ),
                                ),
                            ),
                            Exclusion.structSymbol(
                                symbol = "b",
                                children = listOf(
                                    Exclusion.collWildcard(
                                        children = listOf(
                                            Exclusion.structSymbol(
                                                symbol = "b2",
                                                children = listOf(Exclusion.structSymbol("b3")),
                                            ),
                                        )
                                    ),
                                ),
                            ),
                            Exclusion.structSymbol(
                                symbol = "c",
                                children = listOf(
                                    Exclusion.collWildcard(
                                        children = listOf(Exclusion.structSymbol("c2"))
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
                    Exclusion(
                        variable = rexOpVar(0, 1),
                        items = listOf(
                            Exclusion.structSymbol(
                                symbol = "a",
                                children = listOf(
                                    Exclusion.structWildCard(
                                        children = listOf(Exclusion.structSymbol("a1"))
                                    ),
                                ),
                            ),
                            Exclusion.structSymbol(
                                symbol = "b",
                                children = listOf(
                                    Exclusion.structSymbol(
                                        symbol = "b1",
                                        children = listOf(
                                            Exclusion.structWildCard(
                                                children = listOf(
                                                    Exclusion.structWildCard(
                                                        children = listOf(
                                                            Exclusion.structSymbol("b3")
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
                    Exclusion(
                        variable = rexOpVar(0, 1),
                        items = listOf(
                            Exclusion.structSymbol(
                                symbol = "a",
                                children = listOf(
                                    Exclusion.structSymbol(
                                        symbol = "bar",
                                        children = listOf(
                                            Exclusion.structSymbol("a1")
                                        ),
                                    ),
                                ),
                            ),
                            Exclusion.structSymbol(
                                symbol = "b",
                                children = listOf(
                                    Exclusion.structSymbol(
                                        symbol = "b1",
                                        children = listOf(
                                            Exclusion.structSymbol(
                                                symbol = "bar",
                                                children = listOf(
                                                    Exclusion.structWildCard(
                                                        children = listOf(
                                                            Exclusion.structSymbol("b3")
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
                """-- case sensitive and case-insensitive variables
                t.a, "t".a,     -- t.a subsumes
                "t".b, t.b,     -- t.b subsumes
                t."c", t.c,     -- t.c subsumes
                t.d, t."d",     -- t.d subsumes
                "t".e,          -- included under resolved variable 1
                "t"."f"         -- included under resolved variable 1
                """,
                listOf(
                    Exclusion(
                        variable = rexOpVar(0, 1),
                        items = listOf(
                            Exclusion.structSymbol(symbol = "a"),
                            Exclusion.structSymbol(symbol = "b"),
                            Exclusion.structSymbol(symbol = "c"),
                            Exclusion.structSymbol(symbol = "d"),
                            Exclusion.structSymbol(symbol = "e"),
                            Exclusion.structKey("f")
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
                    Exclusion(
                        variable = rexOpVar(0, 1),
                        items = listOf(
                            Exclusion.structSymbol(
                                symbol = "a",
                                children = listOf(
                                    Exclusion.structWildCard(
                                        children = listOf(
                                            Exclusion.structSymbol("a1"),
                                            Exclusion.structSymbol("a2"),
                                        )
                                    )
                                )
                            ),
                            Exclusion.structSymbol(
                                symbol = "b",
                                children = listOf(
                                    Exclusion.structWildCard(
                                        children = listOf(Exclusion.structSymbol("b2"))
                                    ),
                                    Exclusion.structSymbol(
                                        symbol = "b1",
                                        children = listOf(
                                            Exclusion.structSymbol("foo")
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
