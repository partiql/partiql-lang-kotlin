package org.partiql.value

public object Int16Type : PartiQLCoreTypeBase() {
    override val name: String = "INT16"
    public const val PRECISION: Byte = 5
    public const val MAX_VALUE: Short = Short.MAX_VALUE // (2^15) - 1 = 32,767
    public const val MIN_VALUE: Short = Short.MIN_VALUE // -(2^15) = -32,768
}
