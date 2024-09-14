
package org.partiql.lang.planner.transforms

import com.amazon.ionelement.api.ionBool
import com.amazon.ionelement.api.ionSymbol
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.domains.PartiqlLogicalResolved
import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.errors.ProblemCollector
import org.partiql.lang.util.ArgumentsProviderBase

class LogicalResolvedToDefaultPartiQLPhysicalVisitorTransformTestsPass {
    data class BexprTestCase(val input: PartiqlLogicalResolved.Bexpr, val expected: PartiqlPhysical.Bexpr)

    @ParameterizedTest
    @ArgumentsSource(ArgumentsForToPhysicalTests::class)
    fun `relational operators`(tc: BexprTestCase) {
        val problemHandler = ProblemCollector()
        assertEquals(tc.expected, LogicalResolvedToDefaultPhysicalVisitorTransform(problemHandler).transformBexpr(tc.input))
        assertEquals(0, problemHandler.problems.size)
    }

    class ArgumentsForToPhysicalTests : ArgumentsProviderBase() {
        override fun getParameters() = listOf(
            BexprTestCase(
                PartiqlLogicalResolved.build {
                    scan(
                        expr = globalId("foo"),
                        asDecl = varDecl(0),
                        atDecl = varDecl(1),
                        byDecl = varDecl(2)
                    )
                },
                PartiqlPhysical.build {
                    scan(
                        i = DEFAULT_IMPL,
                        expr = globalId("foo"),
                        asDecl = varDecl(0),
                        atDecl = varDecl(1),
                        byDecl = varDecl(2)
                    )
                }
            ),
            BexprTestCase(
                PartiqlLogicalResolved.build {
                    filter(
                        predicate = lit(ionBool(true)),
                        source = scan(
                            expr = globalId("foo"),
                            asDecl = varDecl(0),
                            atDecl = varDecl(1),
                            byDecl = varDecl(2)
                        )
                    )
                },
                PartiqlPhysical.build {
                    filter(
                        i = DEFAULT_IMPL,
                        predicate = lit(ionBool(true)),
                        source = scan(
                            i = DEFAULT_IMPL,
                            expr = globalId("foo"),
                            asDecl = varDecl(0),
                            atDecl = varDecl(1),
                            byDecl = varDecl(2)
                        )
                    )
                }
            ),
            BexprTestCase(
                PartiqlLogicalResolved.build {
                    unpivot(
                        expr = globalId("foo"),
                        asDecl = varDecl(0),
                        atDecl = varDecl(1),
                        byDecl = varDecl(2)
                    )
                },
                PartiqlPhysical.build {
                    unpivot(
                        i = DEFAULT_IMPL,
                        expr = globalId("foo"),
                        asDecl = varDecl(0),
                        atDecl = varDecl(1),
                        byDecl = varDecl(2)
                    )
                }
            ),
            BexprTestCase(
                PartiqlLogicalResolved.build {
                    sort(
                        source = scan(
                            expr = globalId("foo"),
                            asDecl = varDecl(0),
                            atDecl = varDecl(1),
                            byDecl = varDecl(2)
                        ),
                        sortSpecs = listOf(
                            sortSpec(
                                globalId("foo"),
                                asc(),
                                nullsLast()
                            )
                        )
                    )
                },
                PartiqlPhysical.build {
                    sort(
                        i = DEFAULT_IMPL,
                        source = scan(
                            i = DEFAULT_IMPL,
                            expr = globalId("foo"),
                            asDecl = varDecl(0),
                            atDecl = varDecl(1),
                            byDecl = varDecl(2)
                        ),
                        sortSpecs = listOf(
                            sortSpec(
                                globalId("foo"),
                                asc(),
                                nullsLast()
                            )
                        )
                    )
                }
            ),

            // TODO: Test for window function, remove from experimental once https://github.com/partiql/partiql-docs/issues/31 is resolved and a RFC is approved
            BexprTestCase(
                PartiqlLogicalResolved.build {
                    window(
                        source = scan(
                            expr = globalId("foo"),
                            asDecl = varDecl(0)
                        ),
                        windowSpecification = over(
                            windowPartitionList(
                                path(
                                    localId(0),
                                    listOf(pathExpr(lit(ionSymbol("a")), PartiqlLogicalResolved.CaseSensitivity.CaseInsensitive()))
                                )
                            ),
                            windowSortSpecList(
                                sortSpec(
                                    path(
                                        localId(0),
                                        listOf(pathExpr(lit(ionSymbol("b")), PartiqlLogicalResolved.CaseSensitivity.CaseInsensitive()))
                                    )
                                )
                            )
                        ),
                        // This is a hack. At this point the window operator should have at most one Window Expression.
                        windowExpressionList0 = windowExpression(
                            varDecl(1),
                            "lag",
                            path(
                                localId(0),
                                listOf(
                                    pathExpr(lit(ionSymbol("b")), PartiqlLogicalResolved.CaseSensitivity.CaseInsensitive())
                                )
                            )
                        )
                    )
                },
                PartiqlPhysical.build {
                    window(
                        i = DEFAULT_IMPL,
                        source = scan(
                            i = DEFAULT_IMPL,
                            expr = globalId("foo"),
                            asDecl = varDecl(0)
                        ),
                        windowSpecification = over(
                            windowPartitionList(
                                path(
                                    localId(0),
                                    listOf(pathExpr(lit(ionSymbol("a")), PartiqlPhysical.CaseSensitivity.CaseInsensitive()))
                                )
                            ),
                            windowSortSpecList(
                                sortSpec(
                                    path(
                                        localId(0),
                                        listOf(pathExpr(lit(ionSymbol("b")), PartiqlPhysical.CaseSensitivity.CaseInsensitive()))
                                    )
                                )
                            )
                        ),
                        windowExpressionList = listOf(
                            windowExpression(
                                varDecl(1),
                                "lag",
                                path(
                                    localId(0),
                                    listOf(
                                        pathExpr(lit(ionSymbol("b")), PartiqlPhysical.CaseSensitivity.CaseInsensitive())
                                    )
                                )
                            )
                        )
                    )
                }
            ),
        )
    }
}
