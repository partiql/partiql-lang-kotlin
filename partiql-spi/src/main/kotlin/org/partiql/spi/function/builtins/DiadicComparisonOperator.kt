package org.partiql.spi.function.builtins

import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.internal.SqlTypeFamily
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum

/**
 * This carries along with it a static table containing a mapping between the input types and the implementation.
 */
internal abstract class DiadicComparisonOperator(name: String) : DiadicOperator(
    name,
    Parameter.dynamic("lhs"),
    Parameter.dynamic("rhs")
) {

    override fun getDecimalInstance(decimalLhs: PType, decimalRhs: PType): Function.Instance? {
        return null
    }

    override fun getReturnType(args: Array<PType>): PType {
        return getInstance(args)!!.returns
    }

    override fun getInstance(args: Array<PType>): Function.Instance? {
        val lhs = args[0]
        val rhs = args[1]
        val hasDecimal = lhs.code() == PType.DECIMAL || rhs.code() == PType.DECIMAL
        val allNumbers = (SqlTypeFamily.NUMBER.contains(lhs) && SqlTypeFamily.NUMBER.contains(rhs))
        if (hasDecimal && allNumbers) {
            return getNumberInstance(lhs, rhs)
        }
        return super.getInstance(args)
    }

    private fun getNumberInstance(lhs: PType, rhs: PType): Function.Instance {
        return basic(PType.bool(), lhs, rhs) { args ->
            val l = args[0].getNumber()
            val r = args[1].getNumber()
            Datum.bool(getNumberComparison(l, r))
        }
    }

    abstract fun getNumberComparison(lhs: Number, rhs: Number): Boolean

    private fun Datum.getNumber(): Number {
        return when (this.type.code()) {
            PType.TINYINT -> this.byte
            PType.INTEGER -> this.int
            PType.SMALLINT -> this.short
            PType.BIGINT -> this.long
            PType.REAL -> this.float
            PType.DOUBLE -> this.double
            PType.DECIMAL -> this.bigDecimal
            PType.NUMERIC -> this.bigDecimal
            else -> error("Unexpected type: ${this.type}")
        }
    }
}
