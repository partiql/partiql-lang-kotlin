package org.partiql.lang.planner.transforms.optimizations

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.planner.litInt
import org.partiql.lang.planner.litTrue
import org.partiql.lang.planner.transforms.DEFAULT_IMPL
import org.partiql.lang.util.ArgumentsProviderBase

class RemoveUselessFiltersPassTests {
    @ParameterizedTest
    @ArgumentsSource(Arguments::class)
    fun runTestCase(tc: PhysicalPlanPassBexprTestCase) = tc.runTest(createRemoveUselessFiltersPass())

    class Arguments() : ArgumentsProviderBase() {
        override fun getParameters() = listOf(
            // Useless filters are removed
            PhysicalPlanPassBexprTestCase(
                PartiqlPhysical.build {
                    filter(
                        DEFAULT_IMPL,
                        litTrue(), // this filter is useless since this predicate always returns true
                        scan(DEFAULT_IMPL, globalId("foo"), varDecl(0))
                    )
                },
                PartiqlPhysical.build {
                    scan(DEFAULT_IMPL, globalId("foo"), varDecl(0))
                }
            ),
            // Useful filters are NOT removed
            PhysicalPlanPassBexprTestCase(
                PartiqlPhysical.build {
                    filter(
                        DEFAULT_IMPL,
                        eq(localId(0), litInt(42)), // foo = 42
                        scan(DEFAULT_IMPL, globalId("foo"), varDecl(0))
                    )
                },
                PartiqlPhysical.build {
                    filter(
                        DEFAULT_IMPL,
                        eq(localId(0), litInt(42)),
                        scan(DEFAULT_IMPL, globalId("foo"), varDecl(0))
                    )
                }
            )
        )
    }
}
