package org.partiql.examples

import org.junit.Assert.*
import com.amazon.ion.system.*
import org.partiql.examples.util.Example
import org.partiql.lang.ast.*
import org.partiql.lang.ast.passes.*
import org.partiql.lang.syntax.*
import java.io.PrintStream


/**
 * Customers wishing to embed PartiQL into their application might wish to restrict the use of certain language
 * features or provide custom semantic checking.  One way of accomplishing that is by using an implementation of
 * [AstVisitor] such as this one, which prevents the use of any kind of JOIN clause.
 *
 * Visitors are used to perform inspections of AST nodes -- see [AstRewriter] and [AstRewriterBase] for rewrites.
 */
private class PreventJoinVisitor : AstVisitor {
    override fun visitFromSource(fromSource: FromSource) {
        if (fromSource is FromSourceJoin) {
            throw InvalidAstException("JOINs are prevented")
        }
    }
}

/** The exception thrown by [PreventJoinVisitor] when a JOIN clause was detected. */
private class InvalidAstException(message: String) : RuntimeException(message)


/** A couple of tests for [PreventJoinVisitor]. */
class PreventJoinVisitorExample(out: PrintStream) : Example(out) {
    private val ion = IonSystemBuilder.standard().build()
    private val parser = SqlParser(ion)
    private val astWalker = AstWalker(PreventJoinVisitor())

    private fun hasJoins(sql: String): Boolean = try {
        val ast = parser.parseExprNode(sql)
        astWalker.walk(ast)
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

