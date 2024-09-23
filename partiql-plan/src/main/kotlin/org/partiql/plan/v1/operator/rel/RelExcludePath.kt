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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RelExcludePathImpl) return false

        if (root != other.root) return false
        if (steps != other.steps) return false

        return true
    }

    override fun hashCode(): Int {
        var result = root.hashCode()
        result = 31 * result + steps.hashCode()
        return result
    }

    override fun toString(): String = "RelExcludePath(root=$root, steps=$steps)"
}
