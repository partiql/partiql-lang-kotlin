package org.partiql.value

/**
 * This is SQL:1999's BINARY LARGE OBJECT and Ion's BLOB type
 *
 * Aliases included BLOB
 */
public data class BlobType(
    val length: Int
) : PartiQLCoreTypeBase() {

    override val name: String = "BLOB"
    public companion object {
        @JvmStatic
        public val MAXIMUM_LENGTH: Int = 10 // TODO: Define MAXIMUM. Here is Oracle's: 2_147_483_647
    }

    override fun toString(): String = "${this.name}(${this.length})"
}
