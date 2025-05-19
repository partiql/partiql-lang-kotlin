package org.partiql.planner.internal.typer

import org.junit.jupiter.api.Assertions.assertEquals
import org.partiql.parser.PartiQLParser
import org.partiql.plan.Action.Query
import org.partiql.plan.rex.RexBag
import org.partiql.plan.rex.RexType
import org.partiql.planner.PartiQLPlanner
import org.partiql.spi.catalog.Session
import org.partiql.spi.types.PType
import kotlin.test.Test

class InternalToPublicPlanTest {
    val parser = PartiQLParser.standard()
    val planner = PartiQLPlanner.standard()

    @Test
    fun testCollectionArgs() {
        val parserResult = parser.parse("<<1>>")
        val plannerResult = planner.plan(parserResult.statements.first(), Session.empty())
        val rex = ((plannerResult.plan.action) as Query).rex

        assertEquals(rex.type, RexType.of(PType.bag(PType.integer())))

        rex as RexBag
        val firstElement = rex.values.first()
        assertEquals(firstElement.type, RexType.of(PType.integer()))
    }
}
