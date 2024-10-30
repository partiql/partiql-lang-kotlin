package org.partiql.parser.internal

import org.partiql.ast.AstNode
import org.partiql.ast.Statement
import org.partiql.ast.visitor.AstVisitor

internal class PFile(
    internal val statements: List<Statement>,
) : AstNode() {

    override val children: List<AstNode> = this.statements

    override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R {
        error("This is an internal ast node and cannot be passed to a visitor.")
    }
}
