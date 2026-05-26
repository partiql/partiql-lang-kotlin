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

/**
 * Pretty-printing document algebra based on Wadler's "A Prettier Printer" (1997).
 *
 * Documents describe multiple possible layouts; the renderer picks the best fit
 * for a target line width.
 *
 * See: http://homepages.inf.ed.ac.uk/wadler/papers/prettier/prettier.pdf
 */
internal sealed interface Doc {
    data object Nil : Doc
    data class Text(val s: String) : Doc
    /** Newline when broken, space when flat. */
    data object Line : Doc
    /** Newline when broken, empty string when flat. Used inside brackets. */
    data object SoftBreak : Doc
    /** Always a newline (cannot be flattened). */
    data object HardLine : Doc
    data class Concat(val a: Doc, val b: Doc) : Doc
    data class Nest(val n: Int, val doc: Doc) : Doc
    /** Flat layout if it fits, otherwise broken layout. */
    data class Union(val flat: Doc, val broken: Doc) : Doc
}

// Constructors & Combinators

internal fun text(s: String): Doc = if (s.isEmpty()) Doc.Nil else Doc.Text(s)
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

/** Join a list of docs with a separator between each pair. */
internal fun join(docs: List<Doc>, sep: Doc): Doc {
    if (docs.isEmpty()) return Doc.Nil
    return docs.reduce { acc, doc -> acc cat sep cat doc }
}

/** Join with comma + Line between items (break after comma). */
internal fun commaJoin(docs: List<Doc>): Doc = join(docs, text(",") cat Doc.Line)

/** Bracket: open + SoftBreak + nested content + SoftBreak + close. */
internal fun bracket(open: String, doc: Doc, close: String, indent: Int = 4): Doc =
    group(text(open) cat nest(indent, Doc.SoftBreak cat doc) cat Doc.SoftBreak cat text(close))

/** Returns the single-line form, or null if the doc contains [Doc.HardLine]. */
private fun flatten(doc: Doc): Doc? {
    return when (doc) {
        is Doc.Nil -> Doc.Nil
        is Doc.Text -> doc
        is Doc.Line -> Doc.Text(" ")
        is Doc.SoftBreak -> Doc.Nil
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
            is Doc.HardLine -> { sb.append('\n'); repeat(i) { sb.append(' ') }; k = i }
            is Doc.SoftBreak -> when (mode) {
                Mode.FLAT -> {} // empty string when flat
                Mode.BREAK -> { sb.append('\n'); repeat(i) { sb.append(' ') }; k = i }
            }
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
            is Doc.SoftBreak -> when (mode) {
                Mode.FLAT -> {} // contributes 0 width
                Mode.BREAK -> return true
            }
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
