package org.partiql.lang.eval.like

import kotlin.streams.toList

internal sealed class PatternPart {
    object AnyOneChar : PatternPart()
    object AnyZeroOrMoreChars : PatternPart()
    @Suppress("ArrayInDataClass")
    data class ExactChars(val codepoints: IntArray) : PatternPart()
}

private val ANY_CHARS = '%'.toInt()
private val ANY_ONE_CHAR = '_'.toInt()

// TODO: merge multiple consecutive % together?
// TODO: does the % in '%_' actually mean anything?
internal fun parsePattern(pattern: String, escapeChar: Int?): List<PatternPart> {
    val codepoints = pattern.codePoints().toList().listIterator()
    val parts = ArrayList<PatternPart>()
    while(codepoints.hasNext()) {
        val c = codepoints.next()
        parts.add(when(c) {
            ANY_ONE_CHAR -> PatternPart.AnyOneChar
            ANY_CHARS -> PatternPart.AnyZeroOrMoreChars
            else -> {

                codepoints.previous()
                // Build pattern for matching the exact string
                val buffer = ArrayList<Int>()
                // stop building if we encounter end of input
                do {
                    val cc = codepoints.next()
                    // stop building and back up one if we encounter `%` or `_` characters not precdeed by
                    // the escape character
                    if(escapeChar != null && cc == escapeChar) {
                        buffer.add(codepoints.next())
                    } else {
                        if (cc == ANY_ONE_CHAR || cc == ANY_CHARS) {
                            codepoints.previous()
                            break
                        }
                        buffer.add(cc)
                    }

                } while(codepoints.hasNext())

                PatternPart.ExactChars(buffer.toIntArray())
            }
        })
    }

    return parts
}

private fun <T> List<T>.isLast(idx: Int) = this.size - 1 == idx
private fun IntArray.isLast(idx: Int) = this.size - 1 == idx

internal fun executePattern(parts: List<PatternPart>, str: String): Boolean {
    return executePattern(
        CheckpointIteratorImpl(parts), CheckointCodepointIterator(str))
}

private fun executePattern(partsItr: CheckpointIterator<PatternPart>, charsItr: CheckointCodepointIterator): Boolean {
    while (partsItr.hasNext()) {
        if(!executeOnePart(partsItr, charsItr))
            return false
    }
    return !charsItr.hasNext()
}

private fun executeOnePart(partsItr: CheckpointIterator<PatternPart>, charsItr: CheckointCodepointIterator): Boolean {
    when (val currentPart = partsItr.next()) {
        is PatternPart.AnyOneChar -> {
            if(!charsItr.hasNext())
                return false

            charsItr.next()
            return true
        }
        is PatternPart.ExactChars -> {
            currentPart.codepoints.forEach {
                if (!charsItr.hasNext() || charsItr.next() != it) {
                    return false
                }
            }
            return true
        }
        PatternPart.AnyZeroOrMoreChars -> {
            // No need to check the rest of the string if this is the last pattern part
            if (!partsItr.hasNext()) {
                charsItr.skipToEnd()  // consume rest of string otherwise we will consider this a non-match.
                return true
            }

            while (true) {
                partsItr.checkpoint()
                charsItr.checkpoint()

                val nextPatternMatches = executePattern(partsItr, charsItr)
                partsItr.restore()
                charsItr.restore()

                if (nextPatternMatches) {
                    // TODO:  we can pop the index stack instead of restoring it here to avoid having to
                    // re-run the patternpart during the next call to executeOnePart
                    return true
                }

                charsItr.next()
                if (!charsItr.hasNext()) {
                    return false
                }
            }
        }
    }
}

