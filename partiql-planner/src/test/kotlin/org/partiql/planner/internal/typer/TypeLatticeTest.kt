package org.partiql.planner.internal.typer

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.partiql.planner.internal.casts.CastTable

class TypeLatticeTest {

    @Test
    @Disabled
    fun latticeAsciidocDump() {
        // this test only exists for dumping the type lattice as Asciidoc
        println(CastTable.partiql)
    }
}
