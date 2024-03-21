package org.partiql.value

public data class BitVaryingType(
    val length: Int
) : PartiQLCoreTypeBase() {
    override val name: String = "BIT_VARYING"
    override fun toString(): String = "${this.name}(${this.length})"
}
