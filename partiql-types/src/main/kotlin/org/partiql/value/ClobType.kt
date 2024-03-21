package org.partiql.value

/**
 * SQL:1999's CHARACTER LARGE OBJECT(n) type and Ion's CLOB type
 * Aliases are CLOB(n)
 */
public data class ClobType(
    val length: Int
) : PartiQLCoreTypeBase() {
    override val name: String = "CLOB"
    override fun toString(): String = "${this.name}(${this.length})"

    public companion object {
        public const val MAX_LENGTH: Int = 250 // TODO
    }
}
