package org.partiql.value

/**
 * Aliases include DECIMAL(p, s)
 *
 * NUMERIC cannot have a [precision]
 *
 * @property precision if NULL, represents an arbitrary (infinite) precision.
 * @property scale if NULL, represents an arbitrary scale up to [precision].
 */
public data class NumericType(
    val precision: Int,
    val scale: Int
) : PartiQLCoreTypeBase() {
    override val name: String = "NUMERIC"

    override fun toString(): String = "${this.name}(${this.precision}, ${this.scale})"

    public companion object {
        public const val MAX_PRECISION: Int = 38 // TODO
        public const val MIN_PRECISION: Int = 1 // TODO
        public const val MIN_SCALE: Int = 0 // TODO
        public const val MAX_SCALE: Int = 38 // TODO

        public val UNCONSTRAINED: TypeNumericUnbounded = TypeNumericUnbounded
    }
}
