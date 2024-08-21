package org.partiql.ast.sql

import org.partiql.ast.AstNode

/**
 * Pretty-print this [AstNode] as SQL text with the given (or standard) [SqlLayout] and [SqlDialect].
 *
 * @see SqlLayout
 * @see SqlDialect
 */
@JvmOverloads
fun AstNode.sql(
    layout: SqlLayout = SqlLayout.STANDARD,
    dialect: SqlDialect = SqlDialect.STANDARD,
): String = dialect.transform(this).sql(layout)

/**
 * Write this [SqlBlock] tree as SQL text with the given [SqlLayout].
 */
fun SqlBlock.sql(layout: SqlLayout = SqlLayout.STANDARD) = layout.print(this)
