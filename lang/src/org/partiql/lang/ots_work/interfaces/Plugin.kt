package org.partiql.lang.ots_work.interfaces

/**
 * Used to define a plugin
 */
interface Plugin {
    val scalarCast: ScalarCast

    val scalarIs: ScalarIs
}
