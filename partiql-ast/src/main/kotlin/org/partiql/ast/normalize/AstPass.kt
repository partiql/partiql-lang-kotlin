package org.partiql.ast.normalize

import org.partiql.ast.Statement

/**
 * Wraps a rewriter with a default entry point.
 */
public interface AstPass {

    public fun apply(statement: Statement): Statement
}
