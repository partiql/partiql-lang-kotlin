package org.partiql.ast

import org.partiql.ast.builder.AstFactory
import org.partiql.ast.builder.AstFactoryImpl
import org.partiql.ast.sql.SqlBlock
import org.partiql.ast.sql.SqlDialect
import org.partiql.ast.sql.SqlLayout
import org.partiql.ast.sql.SqlWriter

/**
 * Singleton instance of the default factory; also accessible via `AstFactory.DEFAULT`.
 */
object Ast : AstBaseFactory() {

    public inline fun <T : AstNode> create(block: AstFactory.() -> T) = this.block()
}

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

/**
 * Pretty-print this [AstNode] as SQL text with the given [SqlLayout]
 *
 * TODO clean up interfaces (probably remove SqlWriter) for formatting / single-line format.
 */
@JvmOverloads
public fun AstNode.sql(layout: SqlLayout = SqlLayout.DEFAULT): String = when {
    (layout == SqlLayout.ONELINE) -> SqlWriter.write(accept(SqlDialect.PARTIQL, SqlBlock.Nil))
    else -> SqlWriter.format(accept(SqlDialect.PARTIQL, SqlBlock.Nil))
}
