package org.partiql.lang.planner.transforms.optimizations

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.planner.litTrue
import org.partiql.lang.util.ArgumentsProviderBase


class RemoveUselessAndsPassTests {
    @ParameterizedTest
    @ArgumentsSource(Arguments::class)
    fun runTestCase(tc: PhysicalPlanPassExprTestCase) = tc.runTest(createRemoveUselessAndsPass())

    class Arguments() : ArgumentsProviderBase() {
        override fun getParameters() = listOf(
            PhysicalPlanPassExprTestCase(
                PartiqlPhysical.build { and(litTrue(), litTrue()) },
                PartiqlPhysical.build { litTrue() }
            ),
            // replaces and expression with single non-(lit true) operand, with that operand
            PhysicalPlanPassExprTestCase(
                PartiqlPhysical.build { and(litTrue(), localId(42)) },
                PartiqlPhysical.build { localId(42) }
            ),
            PhysicalPlanPassExprTestCase(
                PartiqlPhysical.build { and(localId(42), litTrue()) },
                PartiqlPhysical.build { localId(42) }
            ),

            // Removes (lit true) from and expressions with two or more non-(lit true) operands
            // with three opreands
            PhysicalPlanPassExprTestCase(
                PartiqlPhysical.build { and(litTrue(), localId(42), localId(43)) },
                PartiqlPhysical.build { and(localId(42), localId(43)) }
            ),
            PhysicalPlanPassExprTestCase(
                PartiqlPhysical.build { and(localId(42), litTrue(), localId(43)) },
                PartiqlPhysical.build { and(localId(42), localId(43)) }
            ),
            PhysicalPlanPassExprTestCase(
                PartiqlPhysical.build { and(localId(42), localId(43), litTrue()) },
                PartiqlPhysical.build { and(localId(42), localId(43)) }
            ),

            // with four operands
            PhysicalPlanPassExprTestCase(
                PartiqlPhysical.build { and(litTrue(), localId(42), localId(43), localId(44)) },
                PartiqlPhysical.build { and(localId(42), localId(43), localId(44)) }
            ),
            PhysicalPlanPassExprTestCase(
                PartiqlPhysical.build { and(localId(42), litTrue(), localId(43), localId(44)) },
                PartiqlPhysical.build { and(localId(42), localId(43), localId(44)) }
            ),
            PhysicalPlanPassExprTestCase(
                PartiqlPhysical.build { and(localId(42), localId(43), litTrue(), localId(44)) },
                PartiqlPhysical.build { and(localId(42), localId(43), localId(44)) }
            ),
            PhysicalPlanPassExprTestCase(
                PartiqlPhysical.build { and(localId(42), localId(43), localId(44), litTrue()) },
                PartiqlPhysical.build { and(localId(42), localId(43), localId(44)) }
            ),

            // leaves ands with no (lit true) untouched.
            PhysicalPlanPassExprTestCase(
                PartiqlPhysical.build { and(localId(42), localId(43)) },
                PartiqlPhysical.build { and(localId(42), localId(43)) }
            ),
        )
    }
}
