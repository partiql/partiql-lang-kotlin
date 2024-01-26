package org.partiql.planner.internal

import org.partiql.planner.internal.ir.Identifier
import org.partiql.planner.internal.ir.Rex
import org.partiql.spi.fn.FnExperimental
import org.partiql.spi.fn.FnSignature

/**
 * Result of attempting to match an unresolved function.
 */
@OptIn(FnExperimental::class)
internal sealed class FnMatch<T : FnSignature> {

    /**
     * 7.1 Inputs with wrong types
     *      It follows that all functions return MISSING when one of their inputs is MISSING
     *
     * @property signature
     * @property mapping
     * @property isMissable TRUE when anyone of the arguments _could_ be MISSING. We *always* propagate MISSING.
     */
    public data class Ok<T : FnSignature>(
        public val signature: T,
        public val mapping: Boolean,
        public val isMissable: Boolean,
    ) : FnMatch<T>()

    /**
     * This represents dynamic dispatch.
     *
     * @property candidates an ordered list of potentially applicable functions to dispatch dynamically.
     * @property isMissable TRUE when the argument permutations may not definitively invoke one of the candidates. You
     * can think of [isMissable] as being the same as "not exhaustive". For example, if we have ABS(INT | STRING), then
     * this function call [isMissable] because there isn't an `ABS(STRING)` function signature AKA we haven't exhausted
     * all the arguments. On the other hand, take an "exhaustive" scenario: ABS(INT | DEC). In this case, [isMissable]
     * is false because we have functions for each potential argument AKA we have exhausted the arguments.
     */
    public data class Dynamic<T : FnSignature>(
        public val candidates: List<Ok<T>>,
        public val isMissable: Boolean,
    ) : FnMatch<T>()

    public data class Error<T : FnSignature>(
        public val identifier: Identifier,
        public val args: List<Rex>,
        public val candidates: List<FnSignature>,
    ) : FnMatch<T>()
}
