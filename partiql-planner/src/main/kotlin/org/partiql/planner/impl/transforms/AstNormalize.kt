package org.partiql.planner.impl.transforms

import org.partiql.ast.Statement

/**
 * TODO implement AST normalization passes.
 */
internal object AstNormalize {

    @JvmStatic
    fun apply(statement: Statement): Statement = statement
}
