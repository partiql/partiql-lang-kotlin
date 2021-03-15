package org.partiql.lang.syntax

import junitparams.Parameters
import org.junit.Test
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.domains.id

class SqlParserDateTimeTests : SqlParserTestBase() {

    data class DateTimeTestCase(val source: String, val block: PartiqlAst.Builder.() -> PartiqlAst.PartiqlAstNode)

    @Test
    @Parameters
    fun dateLiteralTests(tc: DateTimeTestCase) = assertExpression(tc.source, tc.block)

    fun parametersForDateLiteralTests() = listOf(
        DateTimeTestCase("DATE '2012-02-29'") {
            date(2012, 2, 29)
        },
        DateTimeTestCase("DATE'1992-11-30'") {
            date(1992, 11, 30)
        },
        DateTimeTestCase("SELECT DATE '2021-03-10' FROM foo") {
            select(
                project = projectList(projectExpr(date(2021, 3, 10))),
                from = scan(id("foo"))
            )
        }
    )
}