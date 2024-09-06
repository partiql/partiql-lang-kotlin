package org.partiql.plan.v1.operator.rel

import org.partiql.plan.v1.operator.rex.RexVar

/**
 * Logical representation of an EXCLUDE path.
 */
public interface RelExcludePath {

    public fun getRoot(): RexVar

    public fun getSteps(): Collection<RelExcludeStep>

    public companion object {

        @JvmStatic
        public fun of(root: RexVar, steps: Collection<RelExcludeStep>): RelExcludePath = RelExcludePathImpl(root, steps)
    }
}

/**
 * Internal standard implementation of [RelExcludePath].
 *
 * @property root
 * @property steps
 */
internal class RelExcludePathImpl(
    private var root: RexVar,
    private var steps: Collection<RelExcludeStep>,
) : RelExcludePath {
    override fun getRoot(): RexVar = root
    override fun getSteps(): Collection<RelExcludeStep> = steps
}
