package org.partiql.lang.planner

import com.amazon.ion.system.IonSystemBuilder
import org.partiql.lang.domains.PartiqlAlgebra
import org.partiql.lang.eval.BindingName
import org.partiql.lang.planner.transforms.toAstVarDecl
import org.partiql.lang.planner.transforms.toAlgebra
import org.partiql.lang.planner.transforms.toUnindexedAlgebra
import org.partiql.lang.syntax.SqlParser

/** A simple, experimental interface for a query planner. */
interface QueryPlanner {

    /**
     * Parses a query and transforms it all the way to a [PartiqlAlgebra.Statement].
     */
    fun plan(sql: String): PartiqlAlgebra.Statement
}

/**
 * Creates an implementation of an experimental query planner.
 *
 * @param globalVariableExists A function that the planner can call to verify the existence of a table or other global
 * variable.  For a given [BindingName], this function should return true if the `Bindings<StaticType>` used at
 * evaluation time would return non-null, or false if it would return null. In the future, this the type of this
 * argument will likely be changed to `Bindings<StaticType>` but we do not yet do so as a means to be clear about
 * the planner not being aware of static types.
 */
fun createQueryPlanner(
    globalVariableExists: (BindingName) -> Boolean
): QueryPlanner = QueryPlannerImpl(globalVariableExists)

private class QueryPlannerImpl(val tableExists: (BindingName) -> Boolean) : QueryPlanner {

    override fun plan(sql: String): PartiqlAlgebra.Statement {
        val ion = IonSystemBuilder.standard().build()
        val parser = SqlParser(ion)
        val ast = parser.parseAstStatement(sql)

        // Run the AST thru each transform until we arrive at the indexed algebra.
        return ast
            // Synthesizes unspecified AS aliases and assigns indexes to all variable declarations.
            .toAstVarDecl()
            // Converts to the relational algebra (but with variables unresolved).
            .toUnindexedAlgebra()
            // Resolves the unique index of every variable.
            .toAlgebra(tableExists)
    }
}
