package org.partiql.value

/**
 * PartiQL's Tuple type
 *
 * Aliases include STRUCT TODO: Are we sure?
 */
public object TupleType : PartiQLCoreTypeBase() {
    override val name: String = "TUPLE"
    override fun toString(): String = this.name
}
