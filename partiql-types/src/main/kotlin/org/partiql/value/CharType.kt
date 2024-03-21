package org.partiql.value

/**
 * SQL:1999's CHARACTER type
 */
public data class CharType(
    val length: Int
) : PartiQLCoreTypeBase() {
    override val name: String = "CHAR"
    override fun toString(): String = "${this.name}(${this.length})"
    public companion object {
        public const val MAX_LENGTH: Int = 250 // TODO
    }
}
