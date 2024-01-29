package org.partiql.planner.internal

import org.partiql.planner.internal.ir.Ref
import org.partiql.spi.fn.FnExperimental
import org.partiql.spi.fn.FnSignature

/**
 * Result of matching an unresolved function.
 */
@OptIn(FnExperimental::class)
internal sealed class FnMatch {

    /**
     * Successful match of a static function call.
     *
     * @property signature
     * @property mapping
     */
    class Static(
        val signature: FnSignature,
        val mapping: Array<Ref.Cast?>,
    ) : FnMatch() {

        /**
         * The number of exact matches. Useful when ranking function matches.
         */
        val exact: Int = mapping.count { it != null }
    }

    /**
     * This represents dynamic dispatch.
     *
     * @property candidates an ordered list of potentially applicable functions to dispatch dynamically.
     */
    data class Dynamic(val candidates: List<Static>) : FnMatch()
}
