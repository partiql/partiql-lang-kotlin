package org.partiql.plan

import org.partiql.plan.rex.RexVar

/**
 * Logical representation of an EXCLUDE path.
 *
 * TODO does not need to be an interface.
 */
public interface ExcludePath {

    public fun getRoot(): RexVar

    public fun getSteps(): Collection<ExcludeStep>

    public companion object {

        @JvmStatic
        public fun of(root: RexVar, steps: Collection<ExcludeStep>): ExcludePath = ExcludePathImpl(root, steps)
    }
}

/**
 * Internal standard implementation of [ExcludePath].
 *
 * @property root
 * @property steps
 */
internal class ExcludePathImpl(
    private var root: RexVar,
    private var steps: Collection<ExcludeStep>,
) : ExcludePath {
    override fun getRoot(): RexVar = root
    override fun getSteps(): Collection<ExcludeStep> = steps

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ExcludePathImpl) return false

        if (root != other.root) return false
        if (steps != other.steps) return false

        return true
    }

    override fun hashCode(): Int {
        var result = root.hashCode()
        result = 31 * result + steps.hashCode()
        return result
    }

    override fun toString(): String = "ExcludePath(root=$root, steps=$steps)"
}
