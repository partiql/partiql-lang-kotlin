package org.partiql.transpiler.test

import org.junit.jupiter.api.Test

class TranspilerTestJunit {

    @Test
    fun test() {
        val runner = TranspilerTestRunner()
        runner.run()
    }
}
