package org.partiql.ast.sql

import org.partiql.ast.AstNode

/**
 * Pretty-print this [AstNode] as SQL text with the given [SqlLayout]
 */
@JvmOverloads
public fun AstNode.sql(
    layout: SqlLayout = SqlLayout.DEFAULT,
    dialect: SqlDialect = SqlDialect.PARTIQL,
): String = accept(dialect, SqlBlock.Nil).sql(layout)

// a <> b  <-> a concat b

internal infix fun SqlBlock.concat(rhs: SqlBlock): SqlBlock = link(this, rhs)

internal infix fun SqlBlock.concat(text: String): SqlBlock = link(this, text(text))

internal infix operator fun SqlBlock.plus(rhs: SqlBlock): SqlBlock = link(this, rhs)

internal infix operator fun SqlBlock.plus(text: String): SqlBlock = link(this, text(text))

// Shorthand

internal val NIL = SqlBlock.Nil

internal val NL = SqlBlock.NL

internal fun text(text: String) = SqlBlock.Text(text)

internal fun link(lhs: SqlBlock, rhs: SqlBlock) = SqlBlock.Link(lhs, rhs)

internal fun nest(block: () -> SqlBlock) = SqlBlock.Nest(block())

internal fun list(start: String?, end: String?, delimiter: String? = ",", items: () -> List<SqlBlock>): SqlBlock {
    var h: SqlBlock = NIL
    h = if (start != null) h + start else h
    h += nest {
        val kids = items()
        var list: SqlBlock = NIL
        kids.foldIndexed(list) { i, a, item ->
            list += item
            list = if (delimiter != null && (i + 1) < kids.size) a + delimiter else a
            list
        }
    }
    h = if (end != null) h + end else h
    return h
}
