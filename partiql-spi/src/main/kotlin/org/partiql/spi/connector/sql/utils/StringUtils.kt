package org.partiql.spi.connector.sql.utils

internal object StringUtils {

    // String.codePoints() is from Java 9+
    private fun String.toIntArray() = this.codePoints().toArray()

    // Default codepoints to remove
    private val SPACE = intArrayOf(" ".codePointAt(0))

    /** Provides a lazy sequence over the code points in the given string. */
    internal fun String.codePointSequence(): Sequence<Int> {
        val text = this
        return Sequence {
            var pos = 0
            object : Iterator<Int> {
                override fun hasNext(): Boolean = pos < text.length
                override fun next(): Int {
                    val cp = text.codePointAt(pos)
                    pos += Character.charCount(cp)
                    return cp
                }
            }
        }
    }

    /**
     * Removes the given string (" " by default) from both ends of this
     */
    internal fun String.codepointTrim(toRemove: String? = null): String {
        val codepoints = this.toIntArray()
        val codepointsToRemove = toRemove?.toIntArray() ?: SPACE
        return codepoints.trim(codepointsToRemove)
    }

    /**
     * Removes the given string (" " by default) from the leading end of this
     */
    internal fun String.codepointTrimLeading(toRemove: String? = null): String {
        val codepoints = this.toIntArray()
        val codepointsToRemove = toRemove?.toIntArray() ?: SPACE
        return codepoints.trimLeading(codepointsToRemove)
    }

    /**
     * Removes the given string (" " by default) from the trailing end of this
     */
    internal fun String.codepointTrimTrailing(toRemove: String? = null): String {
        val codepoints = this.toIntArray()
        val codepointsToRemove = toRemove?.toIntArray() ?: SPACE
        return codepoints.trimTrailing(codepointsToRemove)
    }

    /**
     * Returns the first 1-indexed position of probe in this; else 0
     */
    internal fun String.codepointPosition(probe: String): Int {
        if (probe.length > this.length) return 0
        val codepoints = this.toIntArray()
        val codepointsToFind = probe.toIntArray()
        return codepoints.positionOf(codepointsToFind)
    }

    /**
     * Replaces this with overlay from 1-indexed position `startPosition` for up to `length` codepoints
     */
    internal fun String.codepointOverlay(overlay: String, position: Int, length: Int? = null): String {
        if (this.isEmpty()) return this
        val codepoints = this.toIntArray()
        val codepointsToOverlay = overlay.toIntArray()
        return codepoints.overlay(codepointsToOverlay, position, length)
    }

    /**
     * Substring defined by SQL-92 page 135.
     *
     * @param start
     * @param end
     * @return
     */
    internal fun String.codepointSubstring(start: Int, end: Int? = null): String {
        val codePointCount = this.codePointCount(0, this.length)
        if (start > codePointCount) {
            return ""
        }

        // startPosition starts at 1
        // calculate this before adjusting start position to account for negative startPosition
        val endPosition = when (end) {
            null -> codePointCount
            else -> Integer.min(codePointCount, start + end - 1)
        }

        // Clamp start indexes to values that make sense for java substring
        val adjustedStartPosition = Integer.max(0, start - 1)

        if (endPosition < adjustedStartPosition) {
            return ""
        }

        val byteIndexStart = this.offsetByCodePoints(0, adjustedStartPosition)
        val byteIndexEnd = this.offsetByCodePoints(0, endPosition)

        return this.substring(byteIndexStart, byteIndexEnd)
    }

    internal fun IntArray.trim(toRemove: IntArray? = null): String {
        val codepointsToRemove = toRemove ?: SPACE
        val leadingOffset = trimLeadingOffset(this, codepointsToRemove)
        val trailingOffset = trimTrailingOffset(this, codepointsToRemove)
        val length = 0.coerceAtLeast(this.size - trailingOffset - leadingOffset)
        return String(this, leadingOffset, length)
    }

    internal fun IntArray.trimLeading(toRemove: IntArray? = null): String {
        val codepointsToRemove = toRemove ?: SPACE
        val offset = trimLeadingOffset(this, codepointsToRemove)
        return String(this, offset, this.size - offset)
    }

    internal fun IntArray.trimTrailing(toRemove: IntArray? = null): String {
        val codepointsToRemove = toRemove ?: SPACE
        val offset = trimTrailingOffset(this, codepointsToRemove)
        return String(this, 0, this.size - offset)
    }

    internal fun IntArray.trimLeadingOffset(codepoints: IntArray, toRemove: IntArray): Int {
        var offset = 0
        while (offset < this.size && toRemove.contains(codepoints[offset])) offset += 1
        return offset
    }

    internal fun IntArray.trimTrailingOffset(codepoints: IntArray, toRemove: IntArray): Int {
        var offset = 0
        while (offset < this.size && toRemove.contains(codepoints[this.size - offset - 1])) offset += 1
        return offset
    }

    internal fun IntArray.positionOf(probe: IntArray): Int {
        val extent = this.size - probe.size
        if (extent < 0) return 0
        var start = 0
        window@ while (start <= extent) {
            // check current window for equality
            for (i in probe.indices) {
                if (probe[i] != this[start + i]) {
                    start += 1
                    continue@window
                }
            }
            // nothing was not equal — everything was equal
            return start + 1
        }
        return 0
    }

    internal fun IntArray.overlay(overlay: IntArray, position: Int, length: Int? = null): String {
        val len = (length ?: overlay.size)
        val prefixLen = (position - 1).coerceAtMost(this.size)
        val suffixLen = (this.size - (len + prefixLen)).coerceAtLeast(0)
        val buffer = IntArray(prefixLen + overlay.size + suffixLen)
        var i = 0
        // Fill prefix
        for (j in 0 until prefixLen) {
            buffer[i++] = this[j]
        }
        // Fill overlay
        for (j in overlay.indices) {
            buffer[i++] = overlay[j]
        }
        // Fill suffix
        for (j in 0 until suffixLen) {
            buffer[i++] = this[prefixLen + len + j]
        }
        return String(buffer, 0, buffer.size)
    }
}
