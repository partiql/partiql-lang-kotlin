package org.partiql.lang.ast.passes

import com.amazon.ion.IonSystem
import org.partiql.lang.ast.ExprNode
import org.partiql.lang.ast.toAstStatement
import org.partiql.lang.ast.toExprNode
import org.partiql.lang.domains.PartiqlAst

/**
 *  This is a temporary bridge between [ExprNode] and the PIG-generated [PartiqlAst].
 *
 *  It allo
 *  */
class ExprNodeWrappingRewriter(val transform: PartiqlAst.VisitorTransform, val ion: IonSystem) : AstRewriter {
    override fun rewriteExprNode(node: ExprNode): ExprNode {
        val pigAst = node.toAstStatement()
        val result = transform.transformStatement(pigAst)
        val en = result.toExprNode(ion)
        return en
    }

}