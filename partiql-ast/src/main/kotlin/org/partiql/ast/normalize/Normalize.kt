package org.partiql.ast.normalize

import org.partiql.ast.Statement

/**
 * AST normalization
 */
public fun Statement.normalize(): Statement {
    // could be a fold, but this is nice for setting breakpoints
    var ast = this
    ast = NormalizeSelectList.apply(ast)
    ast = NormalizeFromSource.apply(ast)
    ast = NormalizeSelectStar.apply(ast)
    return ast
}
