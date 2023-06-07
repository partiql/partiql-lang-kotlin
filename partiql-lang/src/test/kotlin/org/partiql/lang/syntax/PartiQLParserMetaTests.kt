package org.partiql.lang.syntax

import org.junit.jupiter.api.Test
import org.partiql.lang.ast.IsListParenthesizedMeta
import org.partiql.lang.domains.PartiqlAst

internal class PartiQLParserMetaTests : PartiQLParserTestBase() {

    override val targets: Array<ParserTarget> = arrayOf(ParserTarget.DEFAULT, ParserTarget.EXPERIMENTAL)

    @Test
    fun listParenthesized(): Unit = forEachTarget {
        val query = "(0, 1, 2)"
        val ast = parser.parseAstStatement(query) as PartiqlAst.Statement.Query
        val list = ast.expr as PartiqlAst.Expr.List

        assert(list.metas.containsKey(IsListParenthesizedMeta.tag))
    }

    @Test
    fun listParenthesizedNot(): Unit = forEachTarget {
        val query = "[0, 1, 2]"
        val ast = parser.parseAstStatement(query) as PartiqlAst.Statement.Query
        val list = ast.expr as PartiqlAst.Expr.List

        assert(list.metas.containsKey(IsListParenthesizedMeta.tag).not())
    }

    @Test
    fun inListParenthesized(): Unit = forEachTarget {
        val query = "0 IN (0, 1, 2)"
        val ast = parser.parseAstStatement(query) as PartiqlAst.Statement.Query
        val inCollection = ast.expr as PartiqlAst.Expr.InCollection
        val list = inCollection.operands[1] as PartiqlAst.Expr.List

        assert(list.metas.containsKey(IsListParenthesizedMeta.tag))
    }

    @Test
    fun inListParenthesizedNot(): Unit = forEachTarget {
        val query = "0 IN [0, 1, 2]"
        val ast = parser.parseAstStatement(query) as PartiqlAst.Statement.Query
        val inCollection = ast.expr as PartiqlAst.Expr.InCollection
        val list = inCollection.operands[1] as PartiqlAst.Expr.List

        assert(list.metas.containsKey(IsListParenthesizedMeta.tag).not())
    }

    @Test
    fun inListParenthesizedSingleElement(): Unit = forEachTarget {
        val query = "0 IN (0)"
        val ast = parser.parseAstStatement(query) as PartiqlAst.Statement.Query
        val inCollection = ast.expr as PartiqlAst.Expr.InCollection
        val list = inCollection.operands[1] as PartiqlAst.Expr.List

        assert(list.metas.containsKey(IsListParenthesizedMeta.tag))
    }
}
