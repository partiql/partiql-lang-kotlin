package org.partiql.lang.eval.visitors

import org.partiql.lang.domains.PartiqlAst

/**
 * A simple [PartiqlAst.VisitorTransform] transform that provides a pipeline of [PartiqlAst.VisitorTransform]s.
 *
 * @param rewriters
 */
class PipelinedTransformer(vararg rewriters: PartiqlAst.VisitorTransform) : PartiqlAst.VisitorTransform() {
    private val rewriterList =  rewriters.toList()

    override fun transformStatement(node: PartiqlAst.Statement): PartiqlAst.Statement =
        rewriterList.fold(node) { currentNode, xformer -> xformer.transformStatement(currentNode) }

}