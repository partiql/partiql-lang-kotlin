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
                    // If [escapeChar] is encountered, just add the next codepoint to the buffer.]
                    if(escapeChar != null && cc == escapeChar) {
                        buffer.add(codepoints.next())
                    } else {
                        // stop building and back up one if we encounter `%` or `_` characters
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

internal fun executePattern(parts: List<PatternPart>, str: String): Boolean {
    return executePattern(
        CheckpointIteratorImpl(parts), CodepointCheckpointIterator(str))
}

private fun executePattern(partsItr: CheckpointIterator<PatternPart>, charsItr: CodepointCheckpointIterator): Boolean {
    while (partsItr.hasNext()) {
        if(!executeOnePart(partsItr, charsItr))
            return false
    }
    return !charsItr.hasNext()
}

private fun executeOnePart(partsItr: CheckpointIterator<PatternPart>, charsItr: CodepointCheckpointIterator): Boolean {
    when (val currentPart = partsItr.next()) {
        is PatternPart.AnyOneChar -> {
            if(!charsItr.hasNext())
                return false

            charsItr.next()
            return true
        }
        is PatternPart.ExactChars -> {
            // Consume characters as long
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
                // Mark checkpoints on our iterators that so we can store the current position
                // of them later if the next pattern part doesn't match. We will keep doing this
                // until the next pattern part matches.
                partsItr.saveCheckpoint()
                charsItr.saveCheckpoint()

                if (executePattern(partsItr, charsItr)) {
                    // Discard the checkpoint saved above.  We don't technically need to do this
                    // but it prevents the *next* pattern part from executing needlessly.
                    partsItr.discardCheckpoint()
                    charsItr.discardCheckpoint()
                    return true
                } else {
                    // The next pattern did not match, restore the iterator positions for the next iteration
                    partsItr.restoreCheckpoint()
                    charsItr.restoreCheckpoint()
                }

                if (!charsItr.hasNext()) {
                    return false
                }

                charsItr.next()
            }
        }
    }
}

