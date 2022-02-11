
package org.partiql.planner.transforms

internal fun errAstNotNormalized(message: String): Nothing =
    error("$message - have the basic visitor transforms been executed first?")

