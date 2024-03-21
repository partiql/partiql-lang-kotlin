package org.partiql.value

public data class BitType(
    val length: Int
) : PartiQLCoreTypeBase() {
    override val name: String = "BIT"
    override fun toString(): String = "${this.name}(${this.length})"
}
