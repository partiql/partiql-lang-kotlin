package org.partiql.value

/**
 * PartiQL's Array type
 *
 * Aliases include LIST
 */
public object ArrayType : PartiQLCoreTypeBase() {
    override val name: String = "ARRAY"
    override fun toString(): String = this.name
}
