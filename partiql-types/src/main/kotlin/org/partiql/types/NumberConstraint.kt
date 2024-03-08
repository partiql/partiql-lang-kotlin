package org.partiql.types

public sealed class NumberConstraint {

    /** Returns true of [num] matches the constraint. */
    public abstract fun matches(num: Int): Boolean

    public abstract val value: Int

    public data class Equals(override val value: Int) : NumberConstraint() {
        override fun matches(num: Int): Boolean = value == num
    }

    public data class UpTo(override val value: Int) : NumberConstraint() {
        override fun matches(num: Int): Boolean = value >= num
    }
}