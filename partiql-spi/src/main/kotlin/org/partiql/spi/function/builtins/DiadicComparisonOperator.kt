package org.partiql.spi.function.builtins

import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.internal.SqlTypeFamily
import org.partiql.spi.value.Datum
import org.partiql.types.PType

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
        val hasDecimal = lhs.kind == PType.Kind.DECIMAL || rhs.kind == PType.Kind.DECIMAL
        val allNumbers = (SqlTypeFamily.NUMBER.contains(lhs) && SqlTypeFamily.NUMBER.contains(rhs))
        if (hasDecimal && allNumbers) {
            return getAnyInstance(lhs, rhs)
        }
        return super.getInstance(args)
    }

    private fun getAnyInstance(lhs: PType, rhs: PType): Function.Instance {
        return basic(PType.bool(), lhs, rhs) { args ->
            val l = args[0].getNumber()
            val r = args[1].getNumber()
            Datum.bool(getComparison(l, r))
        }
    }

    abstract fun getComparison(lhs: Number, rhs: Number): Boolean

    private fun Datum.getNumber(): Number {
        return when (this.type.kind) {
            PType.Kind.TINYINT -> this.byte
            PType.Kind.INTEGER -> this.int
            PType.Kind.SMALLINT -> this.short
            PType.Kind.BIGINT -> this.long
            PType.Kind.REAL -> this.float
            PType.Kind.DOUBLE -> this.double
            PType.Kind.DECIMAL -> this.bigDecimal
            PType.Kind.NUMERIC -> this.bigInteger
            else -> error("Unexpected type: ${this.type}")
        }
    }
}
