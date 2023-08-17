package org.partiql.planner

import org.junit.jupiter.api.Test

class HeaderTest {

    @Test
    // @Disabled
    fun print() {
        Header.Functions.operators().forEach {
            println("--- [${it.key}] ---------")
            println()
            it.value.forEach { fn -> println(fn) }
            println()
        }
    }
}
