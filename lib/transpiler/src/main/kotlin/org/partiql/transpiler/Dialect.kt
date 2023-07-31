package org.partiql.transpiler

import org.partiql.ast.AstNode
import org.partiql.ast.visitor.AstBaseVisitor
import org.partiql.transpiler.block.Block

/**
 * A dialect writes an AST to a Block tree via a fold.
 *
 * TODO remove visitor; leaving as implementation detail.
 */
abstract class Dialect : AstBaseVisitor<Block, Block>() {

    // No functionality, just a nicer name
    fun write(node: AstNode, onProblem: ProblemCallback) = node.accept(this, Block.Nil)
}
