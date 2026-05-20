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

// TODO: Extend with PartiQL-specific nodes for full AST-to-Doc formatting (bags, arrays, CASE, paths).

/**
 * Pretty-printing document algebra based on Wadler's "A Prettier Printer" (1997).
 *
 * Documents describe multiple possible layouts; the renderer picks the best fit
 * for a target line width. Example:
 *
 * ```
 * val doc = group(text("SELECT") cat nest(4, Line cat text("a, b, c")))
 * render(doc, width = 40)  // → "SELECT a, b, c"         (fits on one line)
 * render(doc, width = 10)  // → "SELECT\n    a, b, c"    (broken)
 * ```
 *
 * See: http://homepages.inf.ed.ac.uk/wadler/papers/prettier/prettier.pdf
 */
internal sealed interface Doc {
    data object Nil : Doc
    /** Literal text (never broken across lines). */
    data class Text(val s: String) : Doc
    data class Keyword(val s: String) : Doc
    /** Newline when broken, space when flat. */
    data object Line : Doc
    /** Always a newline (cannot be flattened). */
    data object HardLine : Doc
    data class Concat(val a: Doc, val b: Doc) : Doc
    /** Indent [doc] by [n] spaces. */
    data class Nest(val n: Int, val doc: Doc) : Doc
    /** Flat layout if it fits, otherwise broken layout. */
    data class Union(val flat: Doc, val broken: Doc) : Doc
}

// Constructors & Combinators

internal fun text(s: String): Doc = if (s.isEmpty()) Doc.Nil else Doc.Text(s)
internal fun keyword(s: String): Doc = Doc.Keyword(s)
internal fun nest(n: Int, doc: Doc): Doc = Doc.Nest(n, doc)

internal infix fun Doc.cat(other: Doc): Doc = when {
    this is Doc.Nil -> other
    other is Doc.Nil -> this
    else -> Doc.Concat(this, other)
}

/** Try flat layout; fall back to broken if it doesn't fit within the line width. */
internal fun group(doc: Doc): Doc {
    val f = flatten(doc) ?: return doc
    return Doc.Union(f, doc)
}

/** Returns the single-line form, or null if the doc contains [Doc.HardLine]. */
private fun flatten(doc: Doc): Doc? {
    return when (doc) {
        is Doc.Nil -> Doc.Nil
        is Doc.Text -> doc
        is Doc.Keyword -> doc
        is Doc.Line -> Doc.Text(" ")
        is Doc.HardLine -> null
        is Doc.Concat -> {
            val a = flatten(doc.a) ?: return null
            val b = flatten(doc.b) ?: return null
            Doc.Concat(a, b)
        }
        is Doc.Nest -> {
            val inner = flatten(doc.doc) ?: return null
            Doc.Nest(doc.n, inner)
        }
        is Doc.Union -> flatten(doc.flat)
    }
}

// Renderer

/** Render a [Doc] to a string, choosing the best layout for the given [width]. */
internal fun render(doc: Doc, width: Int = 80): String {
    val sb = StringBuilder()
    best(sb, width, 0, listOf(Triple(0, Mode.BREAK, doc)))
    return sb.toString().trimEnd()
}

private enum class Mode { FLAT, BREAK }

private fun best(sb: StringBuilder, width: Int, col: Int, docs: List<Triple<Int, Mode, Doc>>) {
    var k = col
    val stack = ArrayDeque(docs)
    while (stack.isNotEmpty()) {
        val (i, mode, doc) = stack.removeFirst()
        when (doc) {
            is Doc.Nil -> {}
            is Doc.Text -> { sb.append(doc.s); k += doc.s.length }
            is Doc.Keyword -> { sb.append(doc.s); k += doc.s.length }
            is Doc.HardLine -> { sb.append('\n'); repeat(i) { sb.append(' ') }; k = i }
            is Doc.Line -> when (mode) {
                Mode.FLAT -> { sb.append(' '); k += 1 }
                Mode.BREAK -> { sb.append('\n'); repeat(i) { sb.append(' ') }; k = i }
            }
            is Doc.Concat -> {
                stack.addFirst(Triple(i, mode, doc.b))
                stack.addFirst(Triple(i, mode, doc.a))
            }
            is Doc.Nest -> {
                stack.addFirst(Triple(i + doc.n, mode, doc.doc))
            }
            is Doc.Union -> {
                if (fits(width - k, listOf(Triple(i, Mode.FLAT, doc.flat)))) {
                    stack.addFirst(Triple(i, Mode.FLAT, doc.flat))
                } else {
                    stack.addFirst(Triple(i, Mode.BREAK, doc.broken))
                }
            }
        }
    }
}

private fun fits(remaining: Int, docs: List<Triple<Int, Mode, Doc>>): Boolean {
    var w = remaining
    val stack = ArrayDeque(docs)
    while (stack.isNotEmpty()) {
        if (w < 0) return false
        val (i, mode, doc) = stack.removeFirst()
        when (doc) {
            is Doc.Nil -> {}
            is Doc.Text -> w -= doc.s.length
            is Doc.Keyword -> w -= doc.s.length
            is Doc.Line -> when (mode) {
                Mode.FLAT -> w -= 1
                Mode.BREAK -> return true
            }
            is Doc.HardLine -> return true
            is Doc.Concat -> {
                stack.addFirst(Triple(i, mode, doc.b))
                stack.addFirst(Triple(i, mode, doc.a))
            }
            is Doc.Nest -> stack.addFirst(Triple(i + doc.n, mode, doc.doc))
            is Doc.Union -> stack.addFirst(Triple(i, Mode.FLAT, doc.flat))
        }
    }
    return w >= 0
}
