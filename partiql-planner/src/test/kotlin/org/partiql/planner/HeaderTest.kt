package org.partiql.planner

import org.junit.jupiter.api.Test
import org.partiql.types.TypingMode

class HeaderTest {

    @Test
    // @Disabled
    fun print() {
        println(Header.partiql(TypingMode.PERMISSIVE))
    }
}
