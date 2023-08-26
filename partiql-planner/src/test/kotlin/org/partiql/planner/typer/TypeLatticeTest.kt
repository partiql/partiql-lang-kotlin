package org.partiql.planner.typer

import org.junit.jupiter.api.Test

class TypeLatticeTest {

    @Test
    fun latticeAsciidocDump() {
        // this test only exists for dumping the type lattice as Asciidoc
        println(TypeLattice.partiql())
    }
}
