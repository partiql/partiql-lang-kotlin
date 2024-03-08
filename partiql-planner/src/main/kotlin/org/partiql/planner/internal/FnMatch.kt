package org.partiql.planner.internal

import org.partiql.planner.internal.ir.Ref
import org.partiql.spi.fn.FnExperimental
import org.partiql.spi.fn.FnSignature
import org.partiql.value.PartiQLType
import org.partiql.value.PartiQLValueExperimental

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

        override fun equals(other: Any?): Boolean {
            if (other !is Static) return false
            return signature.equals(other.signature)
        }

        override fun hashCode(): Int = signature.hashCode()
    }

    /**
     * This represents dynamic dispatch.
     *
     * @property candidates     Ordered list of potentially applicable functions to dispatch dynamically.
     * @property exhaustive     True if all argument permutations (branches) are matched.
     */
    data class Dynamic(
        val candidates: List<Candidate>,
        val exhaustive: Boolean,
    ) : FnMatch() {

        /**
         * Represents a candidate of dynamic dispatch.
         *
         * @property fn             Function to invoke.
         * @property parameters     Represents the input type(s) to match. (ex: INT32)
         */
        data class Candidate @OptIn(PartiQLValueExperimental::class) constructor(
            val fn: Static,
            val parameters: List<PartiQLType>
        )
    }
}
