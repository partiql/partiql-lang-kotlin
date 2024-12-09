package org.partiql.planner.internal

import org.partiql.planner.internal.ir.Ref
import org.partiql.spi.function.Function

/**
 * Result of matching an unresolved function.
 */

internal sealed class FnMatch {

    /**
     * Successful match of a static function call.
     *
     * @property function
     * @property mapping
     */
    class Static(
        val function: Function.Instance,
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

            if (function != other.function) return false
            if (!mapping.contentEquals(other.mapping)) return false
            if (exact != other.exact) return false

            return true
        }

        override fun hashCode(): Int {
            var result = function.hashCode()
            result = 31 * result + mapping.contentHashCode()
            result = 31 * result + exact
            return result
        }
    }

    /**
     * This represents dynamic dispatch.
     *
     * @property candidates     Ordered list of potentially applicable functions to dispatch dynamically.
     */
    class Dynamic(val candidates: List<Function>) : FnMatch()
}
