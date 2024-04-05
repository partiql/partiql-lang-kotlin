package org.partiql.value

public object Int8Type : PartiQLCoreTypeBase() {
    override val name: String = "INT8"
    public const val PRECISION: Byte = 3
    public const val MAX_VALUE: Byte = Byte.MAX_VALUE // 127
    public const val MIN_VALUE: Byte = Byte.MIN_VALUE // -128
}
