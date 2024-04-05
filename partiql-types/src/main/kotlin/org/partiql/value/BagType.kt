package org.partiql.value

/**
 * PartiQL's BAG type
 */
public object BagType : PartiQLCoreTypeBase() {
    override val name: String = "BAG"
    override fun toString(): String = this.name
}
