package org.partiql.parser.internal

import org.partiql.ast.v1.AstNode
import org.partiql.ast.v1.AstVisitor
import org.partiql.ast.v1.Statement

internal class PFileV1(
    internal val statements: List<Statement>,
) : AstNode() {
    override fun children(): MutableCollection<AstNode> {
        return statements.toMutableList()
    }

    override fun <R : Any?, C : Any?> accept(visitor: AstVisitor<R, C>, ctx: C): R {
        error("This is an internal ast node and cannot be passed to a visitor.")
    }
}
