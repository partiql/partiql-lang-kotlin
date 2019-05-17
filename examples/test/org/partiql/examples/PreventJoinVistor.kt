
package org.partiql.examples

import org.junit.*
import org.junit.Assert.*
import com.amazon.ion.system.*
import org.partiql.lang.ast.*
import org.partiql.lang.ast.passes.*
import org.partiql.lang.syntax.*


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
class CustomSemanticCheckExampleTest {
    private val ion = IonSystemBuilder.standard().build()
    private val parser = SqlParser(ion)
    private val astWalker = AstWalker(PreventJoinVisitor())

    private fun checkForJoins(sql: String) {
        val ast = parser.parseExprNode(sql)
        astWalker.walk(ast)
    }

    /** Ensures no exception is thrown when the query doesn't contain a JOIN. */
    @Test
    fun testWithoutJoin() {
        checkForJoins("SELECT foo FROM bar")
    }

    /** Ensures an exception is thrown when the query does contain a JOIN. */
    @Test
    fun testWithJoin() {
        try {
            checkForJoins("SELECT foo FROM bar CROSS JOIN bat")
            fail("Exception was not thrown")
        }
        catch(e: InvalidAstException) {
            //Do nothing -- this is the expected outcome
        }
    }
}

