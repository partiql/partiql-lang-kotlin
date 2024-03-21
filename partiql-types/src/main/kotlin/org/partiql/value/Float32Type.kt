package org.partiql.value

/**
 * Approximate Numeric Type
 *
 * Aliases include: REAL
 */
public object Float32Type : PartiQLCoreTypeBase() {
    override val name: String = "FLOAT32"
}
