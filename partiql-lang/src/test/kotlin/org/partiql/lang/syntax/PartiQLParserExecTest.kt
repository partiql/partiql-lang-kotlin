package org.partiql.lang.syntax

import com.amazon.ion.Decimal
import com.amazon.ionelement.api.ionDecimal
import com.amazon.ionelement.api.ionInt
import com.amazon.ionelement.api.ionString
import org.junit.Test
import org.partiql.lang.domains.id

/**
 * Tests parsing of EXEC using just the PIG parser.
 */
class PartiQLParserExecTest : PartiQLParserTestBase() {
    override val targets: Array<ParserTarget> = arrayOf(ParserTarget.DEFAULT)

    // ****************************************
    // EXEC clause parsing
    // ****************************************
    @Test
    fun execNoArgs() = assertExpression(
        "EXEC foo"
    ) {
        exec("foo", emptyList())
    }

    @Test
    fun execOneStringArg() = assertExpression(
        "EXEC foo 'bar'"
    ) {
        exec("foo", listOf(lit(ionString("bar"))))
    }

    @Test
    fun execOneIntArg() = assertExpression(
        "EXEC foo 1"
    ) {
        exec("foo", listOf(lit(ionInt(1))))
    }

    @Test
    fun execMultipleArg() = assertExpression(
        "EXEC foo 'bar0', `1d0`, 2, [3]"
    ) {
        exec(
            "foo",
            listOf(lit(ionString("bar0")), lit(ionDecimal(Decimal.valueOf(1))), lit(ionInt(2)), list(lit(ionInt(3))))
        )
    }

    @Test
    fun execWithMissing() = assertExpression(
        "EXEC foo MISSING"
    ) {
        exec("foo", listOf(missing()))
    }

    @Test
    fun execWithBag() = assertExpression(
        "EXEC foo <<1>>"
    ) {
        exec("foo", listOf(bag(lit(ionInt(1)))))
    }

    @Test
    fun execWithSelectQuery() = assertExpression(
        "EXEC foo SELECT baz FROM bar"
    ) {
        exec(
            "foo",
            listOf(
                select(
                    project = projectList(projectExpr(id("baz"))),
                    from = scan(id("bar"))
                )
            )
        )
    }
}
