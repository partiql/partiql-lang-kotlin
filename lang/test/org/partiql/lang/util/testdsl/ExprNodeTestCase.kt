package org.partiql.lang.util.testdsl

import org.junit.jupiter.api.fail
import org.partiql.lang.ast.ExprNode

/**
 * Defines a test case that has only an [ExprNode] as an input and a [name].
 */
data class ExprNodeTestCase(val name: String, val expr: ExprNode) {
    override fun toString(): String = "$name - $expr"

    fun assertEquals(actual: ExprNode) {
        if(expr != actual) {
            println("Failing test case \"${name}\"")
            println("expected: ${expr}")
            println("actual  : $actual")
            fail("Unexpected ExprNode AST for test: '$name', see console")
        }
    }
}