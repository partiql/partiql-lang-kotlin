package org.partiql.planner.typer

import org.junit.jupiter.api.Test

class TypeLatticeTest {

    @Test
    fun latticeDotDump() {
        println(TypeLattice.partiql())
    }
}
