package org.partiql.lang.util

internal fun String.countMatchingSubstrings(substr: String): Int {
    var index = 0
    var count = 0

    while (true) {
        index = indexOf(substr, index)
        index += if (index != -1) {
            count++
            substr.length
        } else {
            return count
        }
    }
}
