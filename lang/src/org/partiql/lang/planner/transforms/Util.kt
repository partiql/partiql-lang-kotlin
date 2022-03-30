
package org.partiql.lang.planner.transforms

internal fun errAstNotNormalized(message: String): Nothing =
    error("$message - have the basic visitor transforms been executed first?")
