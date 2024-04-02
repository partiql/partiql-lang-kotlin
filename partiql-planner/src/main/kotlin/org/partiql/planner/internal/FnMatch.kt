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
    data class Static(
        val signature: FnSignature,
        val mapping: Array<Ref.Cast?>,
    ) : FnMatch() {

        /**
         * The number of exact matches. Useful when ranking function matches.
         */
        val exact: Int = mapping.count { it != null }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Static

            if (signature != other.signature) return false
            if (!mapping.contentEquals(other.mapping)) return false
            if (exact != other.exact) return false

            return true
        }

        override fun hashCode(): Int {
            var result = signature.hashCode()
            result = 31 * result + mapping.contentHashCode()
            result = 31 * result + exact
            return result
        }
    }

    /**
     * This represents dynamic dispatch.
     *
     * @property candidates     Ordered list of potentially applicable functions to dispatch dynamically.
     * @property exhaustive     True if all argument permutations (branches) are matched.
     */
    data class Dynamic(
        val candidates: List<Static>,
        val exhaustive: Boolean,
    ) : FnMatch()
}
