package org.partiql.lang.thread

import org.partiql.lang.ast.ExprNode
import org.partiql.lang.eval.ExprValue

class EndlessExprNodeList(
    override val size: Int,
    private val exprNode: ExprNode
) : AbstractList<ExprNode>() {

    override fun get(index: Int): ExprNode = exprNode
}

