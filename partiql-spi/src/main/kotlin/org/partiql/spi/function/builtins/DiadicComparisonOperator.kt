package org.partiql.spi.function.builtins

import org.partiql.spi.function.Fn
import org.partiql.spi.internal.SqlTypeFamily
import org.partiql.spi.types.PType
import org.partiql.spi.utils.getNumber
import org.partiql.spi.value.Datum

/**
 * This carries along with it a static table containing a mapping between the input types and the implementation.
 */
internal abstract class DiadicComparisonOperator(name: String) : DiadicOperator(
    name,
    PType.dynamic(),
    PType.dynamic()
) {

    override fun getDecimalInstance(decimalLhs: PType, decimalRhs: PType): Fn? {
        return null
    }

    override fun getInstance(args: Array<PType>): Fn? {
        val lhs = args[0]
        val rhs = args[1]
        val hasDecimal = lhs.code() == PType.DECIMAL || rhs.code() == PType.DECIMAL
        val allNumbers = (SqlTypeFamily.NUMBER.contains(lhs) && SqlTypeFamily.NUMBER.contains(rhs))
        if (hasDecimal && allNumbers) {
            return getNumberInstance(lhs, rhs)
        }
        return super.getInstance(args)
    }

    private fun getNumberInstance(lhs: PType, rhs: PType): Fn {
        return basic(PType.bool(), lhs, rhs) { args ->
            val l = args[0].getNumber()
            val r = args[1].getNumber()
            Datum.bool(getNumberComparison(l, r))
        }
    }

    abstract fun getNumberComparison(lhs: Number, rhs: Number): Boolean
}
