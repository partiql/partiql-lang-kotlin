package org.partiql.value

/**
 * SQL:1999's TIMESTAMP WITHOUT TIME ZONE type
 * TODO: Does this differ from Ion?
 */
public data class TimestampType(
    val precision: Int
) : PartiQLCoreTypeBase() {
    override val name: String = "TIMESTAMP"
    override fun toString(): String = "${this.name}(${this.precision})"

    public companion object {
        @JvmStatic
        public val MAX_PRECISION: Int = 10 // TODO: Actually do
    }
}
