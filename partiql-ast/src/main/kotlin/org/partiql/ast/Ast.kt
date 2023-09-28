package org.partiql.ast

import org.partiql.ast.builder.AstFactoryImpl
import org.partiql.ast.sql.SqlBlock
import org.partiql.ast.sql.SqlDialect
import org.partiql.ast.sql.SqlLayout
import org.partiql.ast.sql.sql

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
 * Pretty-print this [AstNode] as SQL text with the given [SqlLayout]
 */
@JvmOverloads
public fun AstNode.sql(
    layout: SqlLayout = SqlLayout.DEFAULT,
    dialect: SqlDialect = SqlDialect.PARTIQL,
): String = accept(dialect, SqlBlock.Nil).sql(layout)
