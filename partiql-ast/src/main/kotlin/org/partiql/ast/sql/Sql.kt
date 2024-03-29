package org.partiql.ast.sql

import org.partiql.ast.AstNode
import org.partiql.ast.sql.internal.InternalSqlDialect
import org.partiql.ast.sql.internal.InternalSqlLayout

/**
 * No argument uses optimized internal. Leaving older ones for backwards-compatibility.
 */
public fun AstNode.sql(): String {
    val head = InternalSqlDialect.PARTIQL.apply(this)
    return InternalSqlLayout.format(head)
}

/**
 * Pretty-print this [AstNode] as SQL text with the given [SqlLayout]
 */
@Deprecated("To be removed in the next major version")
public fun AstNode.sql(
    layout: SqlLayout = SqlLayout.DEFAULT,
): String = SqlDialect.PARTIQL.apply(this).sql(layout)

/**
 * Pretty-print this [AstNode] as SQL text with the given [SqlDialect]
 */
@Deprecated("To be removed in the next major version")
public fun AstNode.sql(
    dialect: SqlDialect = SqlDialect.PARTIQL,
): String = dialect.apply(this).sql(SqlLayout.DEFAULT)

/**
 * Pretty-print this [AstNode] as SQL text with the given [SqlLayout] and [SqlDialect]
 */
@Deprecated("To be removed in the next major version")
public fun AstNode.sql(
    layout: SqlLayout,
    dialect: SqlDialect,
): String = dialect.apply(this).sql(layout)

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
