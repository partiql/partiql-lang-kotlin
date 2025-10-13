package org.partiql.planner.internal.typer.functions

import org.partiql.parser.PartiQLParser
import org.partiql.plan.Action
import org.partiql.planner.PartiQLPlanner
import org.partiql.planner.internal.TestCatalog
import org.partiql.spi.catalog.Session
import org.partiql.spi.types.PType

object FnTestUtils {
    /**
     * Helper function to parse, plan, and extract the result type from a PartiQL query.
     */
    fun getQueryResultType(query: String): PType {
        val session = Session.builder()
            .catalog("default")
            .catalogs(
                TestCatalog.builder()
                    .name("default")
                    .build()
            )
            .build()
        val parseResult = PartiQLParser.standard().parse(query)
        val ast = parseResult.statements[0]
        val planner = PartiQLPlanner.builder().build()
        val result = planner.plan(ast, session)
        val queryAction = result.plan.action as Action.Query
        return queryAction.rex.type.pType
    }
}
