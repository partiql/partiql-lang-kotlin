package org.partiql.lang.syntax

import org.junit.jupiter.api.Test
import org.partiql.lang.ast.IsListParenthesizedMeta
import org.partiql.lang.domains.PartiqlAst

internal class PartiQLParserMetaTests : PartiQLParserTestBase() {

    @Test
    fun listParenthesized() {
        val query = "(0, 1, 2)"
        val ast = parse(query) as PartiqlAst.Statement.Query
        val list = ast.expr as PartiqlAst.Expr.List

        assert(list.metas.containsKey(IsListParenthesizedMeta.TAG))
    }

    @Test
    fun listParenthesizedNot() {
        val query = "[0, 1, 2]"
        val ast = parse(query) as PartiqlAst.Statement.Query
        val list = ast.expr as PartiqlAst.Expr.List

        assert(list.metas.containsKey(IsListParenthesizedMeta.TAG).not())
    }

    @Test
    fun inListParenthesized() {
        val query = "0 IN (0, 1, 2)"
        val ast = parse(query) as PartiqlAst.Statement.Query
        val inCollection = ast.expr as PartiqlAst.Expr.InCollection
        val list = inCollection.operands[1] as PartiqlAst.Expr.List

        assert(list.metas.containsKey(IsListParenthesizedMeta.TAG))
    }

    @Test
    fun inListParenthesizedNot() {
        val query = "0 IN [0, 1, 2]"
        val ast = parse(query) as PartiqlAst.Statement.Query
        val inCollection = ast.expr as PartiqlAst.Expr.InCollection
        val list = inCollection.operands[1] as PartiqlAst.Expr.List

        assert(list.metas.containsKey(IsListParenthesizedMeta.TAG).not())
    }

    @Test
    fun inListParenthesizedSingleElement() {
        val query = "0 IN (0)"
        val ast = parse(query) as PartiqlAst.Statement.Query
        val inCollection = ast.expr as PartiqlAst.Expr.InCollection
        val list = inCollection.operands[1] as PartiqlAst.Expr.List

        assert(list.metas.containsKey(IsListParenthesizedMeta.TAG))
    }
}
