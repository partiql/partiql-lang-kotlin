package org.partiql.examples

import com.amazon.ion.system.*
import org.partiql.examples.util.Example
import org.partiql.lang.ast.*
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.syntax.*
import java.io.PrintStream

/** The exception thrown by when a JOIN clause was detected. */
private class InvalidAstException(message: String) : RuntimeException(message)

/**
 * Customers wishing to embed PartiQL into their application might wish to restrict the use of certain language
 * features or provide custom semantic checking.  One way of accomplishing that is by inspecting the AST, this 
 * example shows how to prevent the use of any kind of JOIN clause.
 */

class PreventJoinVisitorExample(out: PrintStream) : Example(out) {
    private val ion = IonSystemBuilder.standard().build()
    private val parser = SqlParser(ion)

    private fun hasJoins(sql: String): Boolean = try {
        // TODO use PIG AST to iterate the AST
        val ast = parser.parseExprNode(sql)

        if(ast.any { it is FromSourceJoin }) {
            throw InvalidAstException("JOINs are prevented")
        }

        false
    } catch (e: InvalidAstException) {
        true
    }

    /** Ensures no exception is thrown when the query doesn't contain a JOIN. */
    override fun run() {
        val queryWithoutJoi = "SELECT foo FROM bar"
        print("PartiQL query without JOINs:", queryWithoutJoi)
        print("Has joins:", hasJoins(queryWithoutJoi).toString())

        val queryWithJoin = "SELECT foo FROM bar CROSS JOIN bat"
        print("PartiQL query with JOINs:", queryWithJoin)
        print("Has joins:", hasJoins(queryWithJoin).toString())
    }
}

