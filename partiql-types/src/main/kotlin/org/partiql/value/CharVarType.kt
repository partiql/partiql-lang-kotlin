package org.partiql.value

/**
 * SQL:1999's CHARACTER VARYING(n) type
 * Aliases are VARCHAR(n), STRING(n), and SYMBOL(n)
 */
public data class CharVarType(
    val length: Int
) : PartiQLCoreTypeBase() {
    override val name: String = "VARCHAR" // TODO: For now
    override fun toString(): String = "${this.name}(${this.length})"

    public companion object {
        public const val MAX_LENGTH: Int = 250 // TODO
    }
}
