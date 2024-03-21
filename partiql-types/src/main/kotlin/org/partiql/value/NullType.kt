package org.partiql.value

/**
 * Ion's NULL.NULL type
 */
public object NullType : PartiQLCoreTypeBase() {
    override val name: String = "NULL"
}
