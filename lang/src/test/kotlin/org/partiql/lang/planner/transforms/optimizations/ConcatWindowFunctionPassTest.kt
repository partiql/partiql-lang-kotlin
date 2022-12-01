package org.partiql.lang.planner.transforms.optimizations

import com.amazon.ionelement.api.ionSymbol
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.annotation.PartiQLExperimental
import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.planner.transforms.DEFAULT_IMPL
import org.partiql.lang.util.ArgumentsProviderBase

@PartiQLExperimental
class ConcatWindowFunctionPassTest {
    @ParameterizedTest
    @ArgumentsSource(Arguments::class)
    fun runTestCase(tc: PhysicalPlanPassBexprTestCase) = tc.runTest(createConcatWindowFunctionPass())

    class Arguments() : ArgumentsProviderBase() {
        override fun getParameters() = listOf(
            // Single window function, an exact copy is returned
            PhysicalPlanPassBexprTestCase(
                inputBexpr = PartiqlPhysical.build {
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
                },
                expectedOutputBexpr = PartiqlPhysical.build {
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

            // multiple window functions that doesn't share the same window
            // an exact copy is returned
            PhysicalPlanPassBexprTestCase(
                inputBexpr = PartiqlPhysical.build {
                    window(
                        i = DEFAULT_IMPL,
                        source = window(
                            i = DEFAULT_IMPL,
                            source = scan(
                                i = DEFAULT_IMPL,
                                expr = globalId("foo"),
                                asDecl = varDecl(0)
                            ),
                            windowSpecification = over(),
                            windowExpressionList = listOf(
                                windowExpression(
                                    varDecl(2),
                                    "lead",
                                    path(
                                        localId(0),
                                        listOf(
                                            pathExpr(lit(ionSymbol("b")), PartiqlPhysical.CaseSensitivity.CaseInsensitive())
                                        )
                                    )
                                )
                            )
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
                },
                expectedOutputBexpr = PartiqlPhysical.build {
                    window(
                        i = DEFAULT_IMPL,
                        source = window(
                            i = DEFAULT_IMPL,
                            source = scan(
                                i = DEFAULT_IMPL,
                                expr = globalId("foo"),
                                asDecl = varDecl(0)
                            ),
                            windowSpecification = over(),
                            windowExpressionList = listOf(
                                windowExpression(
                                    varDecl(2),
                                    "lead",
                                    path(
                                        localId(0),
                                        listOf(
                                            pathExpr(lit(ionSymbol("b")), PartiqlPhysical.CaseSensitivity.CaseInsensitive())
                                        )
                                    )
                                )
                            )
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
                },
            ),

            PhysicalPlanPassBexprTestCase(
                inputBexpr = PartiqlPhysical.build {
                    window(
                        i = DEFAULT_IMPL,
                        source = window(
                            i = DEFAULT_IMPL,
                            source = scan(
                                i = DEFAULT_IMPL,
                                expr = globalId("foo"),
                                asDecl = varDecl(0)
                            ),
                            windowSpecification = over(),
                            windowExpressionList = listOf(
                                windowExpression(
                                    varDecl(2),
                                    "lead",
                                    path(
                                        localId(0),
                                        listOf(
                                            pathExpr(lit(ionSymbol("b")), PartiqlPhysical.CaseSensitivity.CaseInsensitive())
                                        )
                                    )
                                )
                            )
                        ),
                        windowSpecification = over(),
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
                },
                expectedOutputBexpr = PartiqlPhysical.build {
                    window(
                        i = DEFAULT_IMPL,
                        source = scan(
                            i = DEFAULT_IMPL,
                            expr = globalId("foo"),
                            asDecl = varDecl(0)
                        ),
                        windowSpecification = over(),
                        windowExpressionList = listOf(
                            windowExpression(
                                varDecl(2),
                                "lead",
                                path(
                                    localId(0),
                                    listOf(
                                        pathExpr(lit(ionSymbol("b")), PartiqlPhysical.CaseSensitivity.CaseInsensitive())
                                    )
                                )
                            ),
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
                },
            ),
            PhysicalPlanPassBexprTestCase(
                inputBexpr = PartiqlPhysical.build {
                    window(
                        i = DEFAULT_IMPL,
                        source = window(
                            i = DEFAULT_IMPL,
                            source = window(
                                i = DEFAULT_IMPL,
                                source = scan(
                                    i = DEFAULT_IMPL,
                                    expr = globalId("foo"),
                                    asDecl = varDecl(0)
                                ),
                                windowSpecification = over(),
                                windowExpressionList = listOf(
                                    windowExpression(
                                        varDecl(3),
                                        "lead",
                                        path(
                                            localId(0),
                                            listOf(
                                                pathExpr(lit(ionSymbol("g")), PartiqlPhysical.CaseSensitivity.CaseInsensitive())
                                            )
                                        )
                                    )
                                )
                            ),
                            windowSpecification = over(),
                            windowExpressionList = listOf(
                                windowExpression(
                                    varDecl(2),
                                    "lead",
                                    path(
                                        localId(0),
                                        listOf(
                                            pathExpr(lit(ionSymbol("b")), PartiqlPhysical.CaseSensitivity.CaseInsensitive())
                                        )
                                    )
                                )
                            )
                        ),
                        windowSpecification = over(),
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
                },
                expectedOutputBexpr = PartiqlPhysical.build {
                    window(
                        i = DEFAULT_IMPL,
                        source = scan(
                            i = DEFAULT_IMPL,
                            expr = globalId("foo"),
                            asDecl = varDecl(0)
                        ),
                        windowSpecification = over(),
                        windowExpressionList = listOf(
                            windowExpression(
                                varDecl(3),
                                "lead",
                                path(
                                    localId(0),
                                    listOf(
                                        pathExpr(lit(ionSymbol("g")), PartiqlPhysical.CaseSensitivity.CaseInsensitive())
                                    )
                                )
                            ),
                            windowExpression(
                                varDecl(2),
                                "lead",
                                path(
                                    localId(0),
                                    listOf(
                                        pathExpr(lit(ionSymbol("b")), PartiqlPhysical.CaseSensitivity.CaseInsensitive())
                                    )
                                )
                            ),
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
                },
            ),
        )
    }
}
