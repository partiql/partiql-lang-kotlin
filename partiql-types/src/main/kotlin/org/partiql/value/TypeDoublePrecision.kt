package org.partiql.value

/**
 * Approximate Numeric Type
 *
 * Aliases include: DOUBLE PRECISION
 * TODO: What is SQL:1999's `FLOAT`?
 */
public object TypeDoublePrecision : PartiQLCoreTypeBase() {
    override val name: String = "DOUBLE_PRECISION"
}
