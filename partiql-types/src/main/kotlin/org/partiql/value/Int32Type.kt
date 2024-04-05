package org.partiql.value

public object Int32Type : PartiQLCoreTypeBase() {
    override val name: String = "INT32"
    public const val PRECISION: Byte = 10
    public const val MAX_VALUE: Int = Int.MAX_VALUE // (2^31) - 1 = 2,147,483,647
    public const val MIN_VALUE: Int = Int.MIN_VALUE // -(2^31) = -2,147,483,648
}
