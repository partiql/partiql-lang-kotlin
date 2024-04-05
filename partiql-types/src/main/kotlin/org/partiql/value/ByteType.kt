package org.partiql.value

/**
 * TODO: Should this be allowed? It's not in SQL:1999
 */
public object ByteType : PartiQLCoreTypeBase() {
    override val name: String = "BYTE"
}
