package org.partiql.ast

import org.partiql.ast.builder.AstFactoryImpl

/**
 * Singleton instance of the default factory; also accessible via `AstFactory.DEFAULT`.
 */
object Ast : AstBaseFactory()

/**
 * AstBaseFactory can be used to create a factory which extends from the factory provided by AstFactory.DEFAULT.
 */
public abstract class AstBaseFactory : AstFactoryImpl() {
    // internal default overrides here
}

/**
 * Wraps a rewriter with a default entry point.
 */
public interface AstPass {

    public fun apply(statement: Statement): Statement
}
