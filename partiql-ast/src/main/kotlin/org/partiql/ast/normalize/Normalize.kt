package org.partiql.ast.normalize

import org.partiql.ast.Statement

/**
 * AST normalization
 */
public fun Statement.normalize(): Statement {
    // could be a fold, but this is nice for setting breakpoints
    var ast = this
    ast = NormalizeFromSource.apply(ast)
    ast = NormalizeSelect.apply(ast)
    return ast
}
