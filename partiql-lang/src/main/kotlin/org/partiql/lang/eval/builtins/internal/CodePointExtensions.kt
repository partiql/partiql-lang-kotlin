package org.partiql.lang.eval.builtins.internal

// String.codePoints() is from Java 9+
@Suppress("Since15")
private fun String.toIntArray() = this.codePoints().toArray()

// Default codepoints to remove
private val SPACE = intArrayOf(" ".codePointAt(0))

/**
 * Removes the given string (" " by default) from both ends of sourceString
 */
internal fun codepointTrim(sourceString: String, toRemove: String? = null): String {
    val codepoints = sourceString.toIntArray()
    val codepointsToRemove = toRemove?.toIntArray() ?: SPACE
    return codepoints.trim(codepointsToRemove)
}

/**
 * Removes the given string (" " by default) from the leading end of sourceString
 */
internal fun codepointLeadingTrim(sourceString: String, toRemove: String? = null): String {
    val codepoints = sourceString.toIntArray()
    val codepointsToRemove = toRemove?.toIntArray() ?: SPACE
    return codepoints.leadingTrim(codepointsToRemove)
}

/**
 * Removes the given string (" " by default) from the trailing end of sourceString
 */
internal fun codepointTrailingTrim(sourceString: String, toRemove: String? = null): String {
    val codepoints = sourceString.toIntArray()
    val codepointsToRemove = toRemove?.toIntArray() ?: SPACE
    return codepoints.trailingTrim(codepointsToRemove)
}

/**
 * Returns the first 1-indexed position of probe in sourceString; else 0
 */
internal fun codepointPosition(sourceString: String, probe: String): Int {
    if (probe.length > sourceString.length) return 0
    val codepoints = sourceString.toIntArray()
    val codepointsToFind = probe.toIntArray()
    return codepoints.positionOf(codepointsToFind)
}

/**
 * Replaces sourceString with overlay from 1-indexed position `startPosition` for up to `length` codepoints
 */
internal fun codepointOverlay(sourceString: String, overlay: String, position: Int, length: Int? = null): String {
    if (sourceString.isEmpty()) return sourceString
    val codepoints = sourceString.toIntArray()
    val codepointsToOverlay = overlay.toIntArray()
    return codepoints.overlay(codepointsToOverlay, position, length)
}

internal fun IntArray.trim(toRemove: IntArray? = null): String {
    val codepointsToRemove = toRemove ?: SPACE
    val leadingOffset = leadingTrimOffset(this, codepointsToRemove)
    val trailingOffset = trailingTrimOffSet(this, codepointsToRemove)
    val length = Math.max(0, this.size - trailingOffset - leadingOffset)
    return String(this, leadingOffset, length)
}

internal fun IntArray.leadingTrim(toRemove: IntArray? = null): String {
    val codepointsToRemove = toRemove ?: SPACE
    val offset = leadingTrimOffset(this, codepointsToRemove)
    return String(this, offset, this.size - offset)
}

internal fun IntArray.trailingTrim(toRemove: IntArray? = null): String {
    val codepointsToRemove = toRemove ?: SPACE
    val offset = trailingTrimOffSet(this, codepointsToRemove)
    return String(this, 0, this.size - offset)
}

internal fun IntArray.leadingTrimOffset(codepoints: IntArray, toRemove: IntArray): Int {
    var offset = 0
    while (offset < this.size && toRemove.contains(codepoints[offset])) offset += 1
    return offset
}

internal fun IntArray.trailingTrimOffSet(codepoints: IntArray, toRemove: IntArray): Int {
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
