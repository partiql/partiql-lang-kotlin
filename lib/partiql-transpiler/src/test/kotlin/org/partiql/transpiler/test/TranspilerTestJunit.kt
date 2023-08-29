package org.partiql.transpiler.test

import org.junit.jupiter.api.Test

/**
 * TODO this should actually transform a suite into a TestProvider
 */
class TranspilerTestJunit {

    @Test
    fun test() {
        val runner = TranspilerTestRunner()
        runner.run()
    }
}
