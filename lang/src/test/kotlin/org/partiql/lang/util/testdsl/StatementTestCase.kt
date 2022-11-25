package org.partiql.lang.util.testdsl

import org.junit.jupiter.api.fail
import org.partiql.lang.domains.PartiqlAst

/**
 * Defines a test case that has only an [PartiqlAst.Statement] as an input and a [name].
 */
data class StatementTestCase(val name: String, val expr: PartiqlAst.Statement) {
    override fun toString(): String = "$name - $expr"

    fun assertEquals(actual: PartiqlAst.Statement) {
        if (expr != actual) {
            println("Failing test case \"${name}\"")
            println("expected: $expr")
            println("actual  : $actual")
            fail("Unexpected PartiQLAst Statement for test: '$name', see console")
        }
    }
}
