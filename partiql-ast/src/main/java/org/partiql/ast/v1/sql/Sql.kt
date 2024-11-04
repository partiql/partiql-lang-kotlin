package org.partiql.ast.v1.sql

import org.partiql.ast.v1.AstNode

/**
 * Pretty-print this [AstNode] as SQL text with the given (or standard) [SqlLayout] and [SqlDialect].
 *
 * @see SqlLayout
 * @see SqlDialect
 */
@JvmOverloads
public fun AstNode.sql(
    layout: SqlLayout = SqlLayout.STANDARD,
    dialect: SqlDialect = SqlDialect.STANDARD,
): String = dialect.transform(this).sql(layout)

/**
 * Write this [SqlBlock] tree as SQL text with the given [SqlLayout].
 */
public fun SqlBlock.sql(layout: SqlLayout = SqlLayout.STANDARD): String = layout.print(this)
