
package org.partiql.lang.planner.transforms

/**
 * This is the version number of the logical and physical plans supported by this version of PartiQL.
 *
 * It would be nice to embed this in the PIG domain somehow, but this isn't supported, so we have to include it
 * here for now.  https://github.com/partiql/partiql-ir-generator/issues/121
 */
const val PLAN_VERSION_NUMBER = 1

internal fun errAstNotNormalized(message: String): Nothing =
    error("$message - have the basic visitor transforms been executed first?")
