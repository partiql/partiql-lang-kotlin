package org.partiql.value

/**
 * SQL:1999's INTERVAL type
 */
public data class IntervalType(
    // TODO: Does this need a `fields` property?
    val precision: Int
) : PartiQLCoreTypeBase() {
    override val name: String = "INTERVAL"
    override fun toString(): String = "${this.name}(${this.precision})"

    public companion object {
        @JvmStatic
        public val MAX_PRECISION: Int = 10 // TODO: Actually do
    }
}
