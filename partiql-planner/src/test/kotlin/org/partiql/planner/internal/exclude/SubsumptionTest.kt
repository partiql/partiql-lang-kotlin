package org.partiql.planner.internal.exclude

import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.parser.PartiQLParser
import org.partiql.plan.Rel
import org.partiql.plan.Rex
import org.partiql.plan.Statement
import org.partiql.plan.relOpExcludePath
import org.partiql.plan.relOpExcludeStep
import org.partiql.plan.relOpExcludeTypeCollIndex
import org.partiql.plan.relOpExcludeTypeCollWildcard
import org.partiql.plan.relOpExcludeTypeStructKey
import org.partiql.plan.relOpExcludeTypeStructSymbol
import org.partiql.plan.relOpExcludeTypeStructWildcard
import org.partiql.plan.rexOpVar
import org.partiql.planner.PartiQLPlanner
import org.partiql.plugins.memory.MemoryConnector
import org.partiql.spi.catalog.Session
import java.util.stream.Stream
import kotlin.test.assertEquals

class SubsumptionTest {

    companion object {

        private val planner = PartiQLPlanner.standard()
        private val parser = PartiQLParser.default()
        private val catalog = MemoryConnector.builder().name("default").build().getCatalog()
    }

    private fun getExcludeClause(statement: Statement): Rel.Op.Exclude {
        val queryExpr = (statement as Statement.Query).root
        val relProject = ((queryExpr.op as Rex.Op.Select).rel).op as Rel.Op.Project
        return ((relProject.input).op) as Rel.Op.Exclude
    }

    private fun testExcludeExprSubsumption(tc: SubsumptionTC) {
        val text = "SELECT * EXCLUDE ${tc.excludeExprStr} FROM <<>> AS s, <<>> AS t;"
        val statement = parser.parse(text).root
        val session = Session.builder()
            .catalog("default")
            .catalogs(catalog)
            .build()
        val plan = planner.plan(statement, session).plan
        val excludeClause = getExcludeClause(plan.statement).paths
        assertEquals(tc.expectedExcludeExprs, excludeClause)
    }

    data class SubsumptionTC(val excludeExprStr: String, val expectedExcludeExprs: List<Rel.Op.Exclude.Path>)

    @ParameterizedTest
    @ArgumentsSource(ExcludeSubsumptionTests::class)
    @Execution(ExecutionMode.CONCURRENT)
    fun subsumptionTests(tc: SubsumptionTC) = testExcludeExprSubsumption(tc)

    internal class ExcludeSubsumptionTests : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
            return parameters.map { Arguments.of(it) }.stream()
        }

        private val parameters = listOf(
            SubsumptionTC(
                "s.a, t.a", // different roots
                listOf(
                    relOpExcludePath(
                        root = rexOpVar(0, 0),
                        steps = listOf(
                            relOpExcludeStep(
                                type = relOpExcludeTypeStructSymbol(
                                    symbol = "a"
                                ),
                                substeps = emptyList()
                            ),
                        )
                    ),
                    relOpExcludePath(
                        root = rexOpVar(0, 1),
                        steps = listOf(
                            relOpExcludeStep(
                                type = relOpExcludeTypeStructSymbol(
                                    symbol = "a"
                                ),
                                substeps = emptyList()
                            ),
                        )
                    ),
                )
            ),
            SubsumptionTC(
                "t.a, t.b", // different steps
                listOf(
                    relOpExcludePath(
                        root = rexOpVar(0, 1),
                        steps = listOf(
                            relOpExcludeStep(
                                type = relOpExcludeTypeStructSymbol(
                                    symbol = "a"
                                ),
                                substeps = emptyList()
                            ),
                            relOpExcludeStep(
                                type = relOpExcludeTypeStructSymbol(
                                    symbol = "b"
                                ),
                                substeps = emptyList()
                            )
                        )
                    ),
                )
            ),
            SubsumptionTC(
                "s.a, t.a, t.b, s.b", // different roots and steps
                listOf(
                    relOpExcludePath(
                        root = rexOpVar(0, 0),
                        steps = listOf(
                            relOpExcludeStep(
                                type = relOpExcludeTypeStructSymbol(symbol = "a"), substeps = emptyList()
                            ),
                            relOpExcludeStep(
                                type = relOpExcludeTypeStructSymbol(symbol = "b"), substeps = emptyList()
                            )
                        )
                    ),
                    relOpExcludePath(
                        root = rexOpVar(0, 1),
                        steps = listOf(
                            relOpExcludeStep(
                                type = relOpExcludeTypeStructSymbol(symbol = "a"), substeps = emptyList()
                            ),
                            relOpExcludeStep(
                                type = relOpExcludeTypeStructSymbol(symbol = "b"), substeps = emptyList()
                            )
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
                    relOpExcludePath(
                        root = rexOpVar(0, 1),
                        steps = listOf(
                            relOpExcludeStep(
                                type = relOpExcludeTypeStructSymbol(symbol = "a"), substeps = emptyList()
                            ),
                            relOpExcludeStep(
                                type = relOpExcludeTypeStructSymbol(symbol = "b"), substeps = emptyList()
                            ),
                            relOpExcludeStep(
                                type = relOpExcludeTypeStructSymbol(symbol = "c"),
                                substeps = listOf(
                                    relOpExcludeStep(
                                        type = relOpExcludeTypeStructWildcard(), substeps = emptyList()
                                    )
                                )
                            ),
                            relOpExcludeStep(
                                type = relOpExcludeTypeStructSymbol(symbol = "d"),
                                substeps = listOf(
                                    relOpExcludeStep(
                                        type = relOpExcludeTypeStructWildcard(),
                                        substeps = listOf(
                                            relOpExcludeStep(
                                                type = relOpExcludeTypeStructSymbol(symbol = "e"),
                                                substeps = listOf(
                                                    relOpExcludeStep(
                                                        type = relOpExcludeTypeStructSymbol(symbol = "f"),
                                                        substeps = emptyList()
                                                    )
                                                )
                                            )
                                        )
                                    )
                                )
                            ),
                            relOpExcludeStep(
                                type = relOpExcludeTypeStructSymbol(symbol = "g"),
                                substeps = listOf(
                                    relOpExcludeStep(
                                        type = relOpExcludeTypeCollWildcard(), substeps = emptyList()
                                    )
                                )
                            ),
                            relOpExcludeStep(
                                type = relOpExcludeTypeStructSymbol(symbol = "h"),
                                substeps = listOf(
                                    relOpExcludeStep(
                                        type = relOpExcludeTypeCollIndex(index = 1), substeps = emptyList()
                                    )
                                )
                            ),
                            relOpExcludeStep(
                                type = relOpExcludeTypeStructSymbol(symbol = "i"),
                                substeps = listOf(
                                    relOpExcludeStep(
                                        type = relOpExcludeTypeStructKey(key = "j"), substeps = emptyList()
                                    )
                                )
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
                    relOpExcludePath(
                        root = rexOpVar(0, 1),
                        steps = listOf(
                            relOpExcludeStep(
                                type = relOpExcludeTypeStructSymbol(symbol = "a"), substeps = emptyList()
                            ),
                            relOpExcludeStep(
                                type = relOpExcludeTypeStructSymbol(symbol = "b"), substeps = emptyList()
                            ),
                            relOpExcludeStep(
                                type = relOpExcludeTypeStructSymbol(symbol = "c"),
                                substeps = listOf(
                                    relOpExcludeStep(
                                        type = relOpExcludeTypeStructWildcard(),
                                        substeps = listOf(
                                            relOpExcludeStep(
                                                type = relOpExcludeTypeStructSymbol(symbol = "c1"),
                                                substeps = emptyList()
                                            )
                                        )
                                    )
                                )
                            ),
                            relOpExcludeStep(
                                type = relOpExcludeTypeStructSymbol(symbol = "d"),
                                substeps = listOf(
                                    relOpExcludeStep(
                                        type = relOpExcludeTypeStructWildcard(),
                                        substeps = listOf(
                                            relOpExcludeStep(
                                                type = relOpExcludeTypeStructSymbol(symbol = "d1"),
                                                substeps = emptyList()
                                            )
                                        )
                                    )
                                )
                            ),
                            relOpExcludeStep(
                                type = relOpExcludeTypeStructSymbol(symbol = "e"),
                                substeps = listOf(
                                    relOpExcludeStep(
                                        type = relOpExcludeTypeCollIndex(index = 1), substeps = emptyList()
                                    )
                                )
                            ),
                            relOpExcludeStep(
                                type = relOpExcludeTypeStructSymbol(symbol = "f"),
                                substeps = listOf(
                                    relOpExcludeStep(
                                        type = relOpExcludeTypeCollIndex(index = 1), substeps = emptyList()
                                    )
                                )
                            ),
                            relOpExcludeStep(
                                type = relOpExcludeTypeStructSymbol(symbol = "g"),
                                substeps = listOf(
                                    relOpExcludeStep(
                                        type = relOpExcludeTypeCollWildcard(), substeps = emptyList()
                                    )
                                )
                            ),
                            relOpExcludeStep(
                                type = relOpExcludeTypeStructSymbol(symbol = "h"),
                                substeps = listOf(
                                    relOpExcludeStep(
                                        type = relOpExcludeTypeCollWildcard(), substeps = emptyList()
                                    )
                                )
                            ),
                            relOpExcludeStep(
                                type = relOpExcludeTypeStructKey(key = "i"), substeps = emptyList()
                            ),
                            relOpExcludeStep(
                                type = relOpExcludeTypeStructKey(key = "j"), substeps = emptyList()
                            ),
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
                    relOpExcludePath(
                        root = rexOpVar(0, 1),
                        steps = listOf(
                            relOpExcludeStep(
                                type = relOpExcludeTypeStructSymbol(symbol = "a"),
                                substeps = listOf(
                                    relOpExcludeStep(
                                        type = relOpExcludeTypeStructWildcard(), substeps = emptyList()
                                    )
                                )
                            ),
                            relOpExcludeStep(
                                type = relOpExcludeTypeStructKey(key = "b"),
                                substeps = listOf(
                                    relOpExcludeStep(
                                        type = relOpExcludeTypeStructWildcard(), substeps = emptyList()
                                    )
                                )
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
                    relOpExcludePath(
                        root = rexOpVar(0, 1),
                        steps = listOf(
                            relOpExcludeStep(
                                type = relOpExcludeTypeStructSymbol(symbol = "a"),
                                substeps = listOf(
                                    relOpExcludeStep(
                                        type = relOpExcludeTypeCollWildcard(), substeps = emptyList()
                                    )
                                )
                            ),
                            relOpExcludeStep(
                                type = relOpExcludeTypeStructSymbol(symbol = "b"),
                                substeps = listOf(
                                    relOpExcludeStep(
                                        type = relOpExcludeTypeStructKey(key = "b1"),
                                        substeps = listOf(
                                            relOpExcludeStep(
                                                type = relOpExcludeTypeCollWildcard(), substeps = emptyList()
                                            )
                                        )
                                    )
                                )
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
                    relOpExcludePath(
                        root = rexOpVar(0, 1),
                        steps = listOf(
                            relOpExcludeStep(
                                type = relOpExcludeTypeStructSymbol(symbol = "foo"), substeps = emptyList()
                            ),
                            relOpExcludeStep(
                                type = relOpExcludeTypeStructKey(key = "bAr"),
                                substeps = listOf(
                                    relOpExcludeStep(
                                        type = relOpExcludeTypeStructSymbol(symbol = "baz"), substeps = emptyList()
                                    )
                                )
                            ),
                        )
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
                    relOpExcludePath(
                        root = rexOpVar(0, 1),
                        steps = listOf(
                            relOpExcludeStep(
                                type = relOpExcludeTypeStructSymbol(symbol = "a"),
                                substeps = listOf(
                                    relOpExcludeStep(
                                        type = relOpExcludeTypeCollWildcard(),
                                        substeps = listOf(
                                            relOpExcludeStep(
                                                type = relOpExcludeTypeStructSymbol(symbol = "a2"),
                                                substeps = emptyList()
                                            )
                                        )
                                    )
                                )
                            ),
                            relOpExcludeStep(
                                type = relOpExcludeTypeStructSymbol(symbol = "b"),
                                substeps = listOf(
                                    relOpExcludeStep(
                                        type = relOpExcludeTypeCollWildcard(),
                                        substeps = listOf(
                                            relOpExcludeStep(
                                                type = relOpExcludeTypeStructSymbol(symbol = "b2"),
                                                substeps = listOf(
                                                    relOpExcludeStep(
                                                        type = relOpExcludeTypeStructSymbol(symbol = "b3"),
                                                        substeps = emptyList()
                                                    )
                                                )
                                            )
                                        )
                                    )
                                )
                            ),
                            relOpExcludeStep(
                                type = relOpExcludeTypeStructSymbol(symbol = "c"),
                                substeps = listOf(
                                    relOpExcludeStep(
                                        type = relOpExcludeTypeCollWildcard(),
                                        substeps = listOf(
                                            relOpExcludeStep(
                                                type = relOpExcludeTypeStructSymbol(symbol = "c2"),
                                                substeps = emptyList()
                                            )
                                        )
                                    )
                                )
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
                    relOpExcludePath(
                        root = rexOpVar(0, 1),
                        steps = listOf(
                            relOpExcludeStep(
                                type = relOpExcludeTypeStructSymbol(symbol = "a"),
                                substeps = listOf(
                                    relOpExcludeStep(
                                        type = relOpExcludeTypeStructWildcard(),
                                        substeps = listOf(
                                            relOpExcludeStep(
                                                type = relOpExcludeTypeStructSymbol(symbol = "a1"),
                                                substeps = emptyList()
                                            )
                                        )
                                    )
                                )
                            ),
                            relOpExcludeStep(
                                type = relOpExcludeTypeStructSymbol(symbol = "b"),
                                substeps = listOf(
                                    relOpExcludeStep(
                                        type = relOpExcludeTypeStructSymbol(symbol = "b1"),
                                        substeps = listOf(
                                            relOpExcludeStep(
                                                type = relOpExcludeTypeStructWildcard(),
                                                substeps = listOf(
                                                    relOpExcludeStep(
                                                        type = relOpExcludeTypeStructWildcard(),
                                                        substeps = listOf(
                                                            relOpExcludeStep(
                                                                type = relOpExcludeTypeStructSymbol(symbol = "b3"),
                                                                substeps = emptyList()
                                                            )
                                                        )
                                                    )
                                                )
                                            )
                                        )
                                    )
                                )
                            ),
                        )
                    ),
                )
            ),
            SubsumptionTC(
                """-- struct symbol subsumes struct key when tail subsumes
                t.a.bar.a1, t.a."bAr".a1, t.a."bAR".a1.a2,  -- t.a.bar.a1 subsumes
                t.b."B1"."Bar".b2.b3, t.b.b1."Bar".b2.b3, t.b.b1."Bar".*.b3, t.b.b1.bar.*.b3 -- t.b.b1.bar.*.b3 subsumes
                """,
                listOf(
                    relOpExcludePath(
                        root = rexOpVar(0, 1),
                        steps = listOf(
                            relOpExcludeStep(
                                type = relOpExcludeTypeStructSymbol(symbol = "a"),
                                substeps = listOf(
                                    relOpExcludeStep(
                                        type = relOpExcludeTypeStructSymbol(symbol = "bar"),
                                        substeps = listOf(
                                            relOpExcludeStep(
                                                type = relOpExcludeTypeStructSymbol(symbol = "a1"),
                                                substeps = emptyList()
                                            )
                                        )
                                    )
                                )
                            ),
                            relOpExcludeStep(
                                type = relOpExcludeTypeStructSymbol(symbol = "b"),
                                substeps = listOf(
                                    relOpExcludeStep(
                                        type = relOpExcludeTypeStructSymbol(symbol = "b1"),
                                        substeps = listOf(
                                            relOpExcludeStep(
                                                type = relOpExcludeTypeStructSymbol(symbol = "bar"),
                                                substeps = listOf(
                                                    relOpExcludeStep(
                                                        type = relOpExcludeTypeStructWildcard(),
                                                        substeps = listOf(
                                                            relOpExcludeStep(
                                                                type = relOpExcludeTypeStructSymbol(symbol = "b3"),
                                                                substeps = emptyList()
                                                            )
                                                        )
                                                    )
                                                )
                                            )
                                        )
                                    )
                                )
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
                    relOpExcludePath(
                        root = rexOpVar(0, 1),
                        steps = listOf(
                            relOpExcludeStep(
                                type = relOpExcludeTypeStructSymbol(symbol = "a"), substeps = emptyList()
                            ),
                            relOpExcludeStep(
                                type = relOpExcludeTypeStructSymbol(symbol = "b"), substeps = emptyList()
                            ),
                            relOpExcludeStep(
                                type = relOpExcludeTypeStructSymbol(symbol = "c"), substeps = emptyList()
                            ),
                            relOpExcludeStep(
                                type = relOpExcludeTypeStructSymbol(symbol = "d"), substeps = emptyList()
                            ),
                            relOpExcludeStep(
                                type = relOpExcludeTypeStructSymbol(symbol = "e"), substeps = emptyList()
                            ),
                            relOpExcludeStep(
                                type = relOpExcludeTypeStructKey(key = "f"), substeps = emptyList()
                            ),
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
                    relOpExcludePath(
                        root = rexOpVar(0, 1),
                        steps = listOf(
                            relOpExcludeStep(
                                type = relOpExcludeTypeStructSymbol(symbol = "a"),
                                substeps = listOf(
                                    relOpExcludeStep(
                                        type = relOpExcludeTypeStructWildcard(),
                                        substeps = listOf(
                                            relOpExcludeStep(
                                                type = relOpExcludeTypeStructSymbol(symbol = "a1"),
                                                substeps = emptyList()
                                            ),
                                            relOpExcludeStep(
                                                type = relOpExcludeTypeStructSymbol(symbol = "a2"),
                                                substeps = emptyList()
                                            )
                                        )
                                    )
                                )
                            ),
                            relOpExcludeStep(
                                type = relOpExcludeTypeStructSymbol(symbol = "b"),
                                substeps = listOf(
                                    relOpExcludeStep(
                                        type = relOpExcludeTypeStructWildcard(),
                                        substeps = listOf(
                                            relOpExcludeStep(
                                                type = relOpExcludeTypeStructSymbol(symbol = "b2"),
                                                substeps = emptyList()
                                            )
                                        )
                                    ),
                                    relOpExcludeStep(
                                        type = relOpExcludeTypeStructSymbol(symbol = "b1"),
                                        substeps = listOf(
                                            relOpExcludeStep(
                                                type = relOpExcludeTypeStructSymbol(symbol = "foo"),
                                                substeps = emptyList()
                                            )
                                        )
                                    )
                                )
                            ),
                        )
                    ),
                )
            ),
        )
    }
}
