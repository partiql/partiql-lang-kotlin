package org.partiql.lang.planner

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.partiql.lang.domains.PartiqlAlgebra

/**
 * This is a very simple test class to cover the basic features of the planner.
 *
 * The transforms made by the planner are tested thoroughly on an individual basis.
 * See the tests located in the [org.partiql.lang.planner.transforms] package.
 */
class QueryPlannerTests {

    @Test
    fun simpleEndToEnd() {
        val queryPlanner = createQueryPlanner { it.isEquivalentTo("foo") }

        val expectedAlgebra = PartiqlAlgebra.build {
            query(global("foo", caseInsensitive()))
        }

        assertEquals(expectedAlgebra, queryPlanner.plan("foo"))
    }
}
