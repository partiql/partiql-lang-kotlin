package org.partiql.lang.eval.like

import kotlin.streams.toList

internal sealed class PatternPart {
    object AnyOneChar : PatternPart()
    object ZeroOrMoreOfAnyChar : PatternPart()
    data class ExactChars(val codepoints: IntArray) : PatternPart() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ExactChars) return false

            if (!codepoints.contentEquals(other.codepoints)) return false

            return true
        }

        override fun hashCode(): Int {
            return codepoints.contentHashCode()
        }
    }
}

private const val ZERO_OR_MORE_OF_ANY_CHAR = '%'.toInt()
private const val ANY_ONE_CHAR = '_'.toInt()

internal fun parsePattern(pattern: String, escapeChar: Int?): List<PatternPart> {
    val codepointList = pattern.codePoints().toList()
    val codepointsItr = codepointList.listIterator()
    val parts = ArrayList<PatternPart>()
    while(codepointsItr.hasNext()) {
        val c = codepointsItr.next()
        parts.add(when(c) {
            ANY_ONE_CHAR -> PatternPart.AnyOneChar
            ZERO_OR_MORE_OF_ANY_CHAR -> {
                // consider consecutive `%` to be the same as one `%`
                while(codepointsItr.hasNext() && codepointList[codepointsItr.nextIndex()] == ZERO_OR_MORE_OF_ANY_CHAR) {
                    codepointsItr.next()
                }

                PatternPart.ZeroOrMoreOfAnyChar
            }
            else -> {
                codepointsItr.previous()
                // Build pattern for matching the exact string
                val buffer = ArrayList<Int>()
                // stop building if we encounter end of input
                do {
                    val cc = codepointsItr.next()
                    // If [escapeChar] is encountered, just add the next codepoint to the buffer.
                    if(escapeChar != null && cc == escapeChar) {
                        buffer.add(codepointsItr.next())
                    } else {
                        // stop building and back up one if we encounter `%` or `_` characters
                        if (cc == ANY_ONE_CHAR || cc == ZERO_OR_MORE_OF_ANY_CHAR) {
                            codepointsItr.previous()
                            break
                        }
                        buffer.add(cc)
                    }

                } while(codepointsItr.hasNext())

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
            if(!charsItr.hasNext()) {
                return false
            }

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
        PatternPart.ZeroOrMoreOfAnyChar -> {
            // No need to check the rest of the string if this is the last pattern part
            if (!partsItr.hasNext()) {
                charsItr.skipToEnd()  // consume rest of string otherwise we will consider this a non-match.
                return true
            }

            while (true) {
                // Mark checkpoints on our iterators so that we can store the current position
                // of them later if the remaining pattern parts don't match. We will keep
                // doing this and and advancing the current character position until the
                // remaining pattern parts match. If we reach the end of the string, then there is no match.
                partsItr.saveCheckpoint()
                charsItr.saveCheckpoint()

                if (executePattern(partsItr, charsItr)) {
                    // Discard the checkpoint saved above.  We don't technically need to do this
                    // but it prevents the *next* pattern part from executing again without need.
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

