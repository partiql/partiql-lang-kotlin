/*
 * Copyright Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

package org.partiql.cli.format

import org.partiql.ast.AstNode
import org.partiql.ast.sql.SqlDialect
import org.partiql.ast.sql.SqlLayout
import org.partiql.ast.sql.sql

// TODO: Replace with a full AST-to-Doc visitor for per-expression layout control.
//  See CockroachDB's sqlfmt: https://github.com/cockroachdb/cockroach/tree/master/pkg/util/pretty

/**
 * Pretty-prints a PartiQL [AstNode] using Wadler's document algebra.
 *
 * Serializes the AST to normalized SQL via [SqlDialect.STANDARD], then splits at
 * clause boundaries into a [Doc] tree for width-aware rendering.
 *
 * ```
 * // Short query (fits in 80 chars) → single line:
 * "SELECT a, b FROM t WHERE x > 1"
 *
 * // Long query → clause per line:
 * "SELECT a, b, c, d, e\nFROM some_long_table_name\nWHERE x > 1 AND y < 100"
 *
 * // Very long clause body → keyword + indented body:
 * "SELECT\n    a, b, c, d, e, f, g, h\nFROM t"
 * ```
 */
internal fun AstNode.pretty(width: Int = 80): String {
    val sql = this.sql(layout = SqlLayout.ONELINE, dialect = SqlDialect.STANDARD)
    val doc = sqlToDoc(sql)
    return render(doc, width)
}

/** Converts normalized SQL into a [Doc] tree with [group] nodes at clause boundaries. */
private fun sqlToDoc(sql: String): Doc {
    val clauses = splitClauses(sql)
    if (clauses.size <= 1) return text(sql)

    val docs = clauses.map { (kw, body) ->
        if (kw.isEmpty()) {
            text(body.trim())
        } else if (body.isEmpty()) {
            keyword(kw)
        } else {
            group(keyword(kw) cat nest(4, Doc.Line cat text(body.trim())))
        }
    }
    return group(docs.reduce { acc, doc -> acc cat Doc.Line cat doc })
}

/** Splits SQL into (keyword, body) pairs at top-level clause boundaries (paren/quote-aware). */
private fun splitClauses(sql: String): List<Pair<String, String>> {
    // Keywords that start a new clause at depth 0
    val clauseKeywords = listOf(
        // DQL
        "SELECT ", "FROM ", "WHERE ", "GROUP BY ", "HAVING ",
        "ORDER BY ", "LIMIT ", "OFFSET ", "WINDOW ", "LET ", "EXCLUDE ",
        // Set operations (OUTER variants first, then ALL before bare)
        "OUTER UNION ALL ", "OUTER UNION ", "OUTER INTERSECT ALL ", "OUTER INTERSECT ",
        "OUTER EXCEPT ALL ", "OUTER EXCEPT ",
        "UNION ALL ", "UNION ", "INTERSECT ALL ", "INTERSECT ", "EXCEPT ALL ", "EXCEPT ",
        // Joins (longer matches first)
        "INNER JOIN ", "LEFT OUTER JOIN ", "RIGHT OUTER JOIN ", "FULL OUTER JOIN ",
        "LEFT CROSS JOIN ", "LEFT JOIN ", "RIGHT JOIN ", "CROSS JOIN ", "FULL JOIN ", "JOIN ",
        // WITH
        "WITH RECURSIVE ", "WITH ",
    )

    data class ClauseMatch(val pos: Int, val keyword: String)

    val matches = mutableListOf<ClauseMatch>()
    var i = 0
    var depth = 0

    while (i < sql.length) {
        val c = sql[i]
        // Track paren depth
        if (c == '(') { depth++; i++; continue }
        if (c == ')') { depth--; i++; continue }
        // Skip string literals and quoted identifiers
        if (c == '\'' || c == '"') {
            i = skipQuoted(sql, i, c)
            continue
        }
        // Only match at depth 0
        if (depth == 0) {
            // Check if we're at a word boundary (start of string or preceded by space)
            val atBoundary = i == 0 || sql[i - 1] == ' '
            if (atBoundary) {
                val matched = clauseKeywords.firstOrNull { kw ->
                    sql.regionMatches(i, kw, 0, kw.length, ignoreCase = false)
                }
                if (matched != null) {
                    matches.add(ClauseMatch(i, matched.trimEnd()))
                    i += matched.length
                    continue
                }
            }
        }
        i++
    }

    // Build clause pairs
    if (matches.isEmpty()) return listOf("" to sql)

    val result = mutableListOf<Pair<String, String>>()
    // Text before first keyword (if any)
    if (matches.first().pos > 0) {
        result.add("" to sql.substring(0, matches.first().pos))
    }
    for (idx in matches.indices) {
        val start = matches[idx].pos + matches[idx].keyword.length + 1 // +1 for trailing space
        val end = if (idx + 1 < matches.size) matches[idx + 1].pos else sql.length
        val body = sql.substring(start.coerceAtMost(sql.length), end).trim()
        result.add(matches[idx].keyword to body)
    }
    return result
}

/** Advance past a quoted span (single or double), handling escaped quotes ('' or ""). */
private fun skipQuoted(sql: String, start: Int, quote: Char): Int {
    var i = start + 1
    while (i < sql.length) {
        if (sql[i] == quote) {
            i++
            if (i < sql.length && sql[i] == quote) { i++; continue }
            break
        }
        i++
    }
    return i
}
