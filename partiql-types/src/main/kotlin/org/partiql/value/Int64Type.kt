package org.partiql.value

public object Int64Type : PartiQLCoreTypeBase() {
    override val name: String = "INT64"
    public const val PRECISION: Byte = 19
    public const val MAX_VALUE: Long = Long.MAX_VALUE // (2^63) - 1 = 9,223,372,036,854,775,807
    public const val MIN_VALUE: Long = Long.MIN_VALUE // -(2^63) = -9,223,372,036,854,775,808
}
