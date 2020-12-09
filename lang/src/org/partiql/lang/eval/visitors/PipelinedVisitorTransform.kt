package org.partiql.lang.eval.visitors

import org.partiql.lang.domains.PartiqlAst

/**
 * A simple visitor transformer that provides a pipeline of transformers to be executed in sequential order.
 *
 * @param transformers visitor transforms to be executed
 */
class PipelinedVisitorTransform(vararg transformers: PartiqlAst.VisitorTransform) : PartiqlAst.VisitorTransform() {
    private val transformerList =  transformers.toList()

    override fun transformStatement(node: PartiqlAst.Statement): PartiqlAst.Statement =
        transformerList.fold(node) {
            intermediateNode, transformer ->
            transformer.transformStatement(intermediateNode)
        }
}
