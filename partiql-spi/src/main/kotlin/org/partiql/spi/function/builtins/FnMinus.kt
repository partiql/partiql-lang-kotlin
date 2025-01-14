// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.errors.DataException
import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.internal.byteOverflows
import org.partiql.spi.internal.shortOverflows
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum

internal object FnMinus : DiadicArithmeticOperator("minus") {

    init {
        fillTable()
    }

    override fun getTinyIntInstance(tinyIntLhs: PType, tinyIntRhs: PType): Function.Instance {
        return basic(PType.tinyint()) { args ->
            val arg0 = args[0].byte
            val arg1 = args[1].byte
            val result = arg0 - arg1
            if (result.byteOverflows()) {
                throw DataException("Resulting value out of range for TINYINT: $arg0 - $arg1")
            } else {
                Datum.tinyint(result.toByte())
            }
        }
    }

    override fun getSmallIntInstance(smallIntLhs: PType, smallIntRhs: PType): Function.Instance {
        return basic(PType.smallint()) { args ->
            val arg0 = args[0].short
            val arg1 = args[1].short
            val result = arg0 - arg1
            if (result.shortOverflows()) {
                throw DataException("Resulting value out of range for SMALLINT: $arg0 - $arg1")
            } else {
                Datum.smallint(result.toShort())
            }
        }
    }

    override fun getIntegerInstance(integerLhs: PType, integerRhs: PType): Function.Instance {
        return basic(PType.integer()) { args ->
            val arg0 = args[0].int
            val arg1 = args[1].int
            try {
                val result = Math.subtractExact(arg0, arg1)
                Datum.integer(result)
            } catch (e: ArithmeticException) {
                throw DataException("Resulting value out of range for INT: $arg0 - $arg1")
            }
        }
    }

    override fun getBigIntInstance(bigIntLhs: PType, bigIntRhs: PType): Function.Instance {
        return basic(PType.bigint()) { args ->
            val arg0 = args[0].long
            val arg1 = args[1].long
            try {
                val result = Math.subtractExact(arg0, arg1)
                Datum.bigint(result)
            } catch (e: ArithmeticException) {
                throw DataException("Resulting value out of range for BIGINT: $arg0 - $arg1")
            }
        }
    }

    // TODO: Delete this
    override fun getNumericInstance(numericLhs: PType, numericRhs: PType): Function.Instance {
        return basic(DefaultNumeric.NUMERIC) { args ->
            val arg0 = args[0].bigDecimal
            val arg1 = args[1].bigDecimal
            Datum.numeric(arg0 - arg1)
        }
    }

    /**
     * Precision and scale calculation:
     * P = max(s1, s2) + max(p1 - s1, p2 - s2) + 1
     * S = max(s1, s2)
     */
    override fun getDecimalInstance(decimalLhs: PType, decimalRhs: PType): Function.Instance {
        val p = Math.min(38, Math.max(decimalLhs.scale, decimalRhs.scale) + Math.max(decimalLhs.precision - decimalLhs.scale, decimalRhs.precision - decimalRhs.scale) + 1)
        val s = Math.min(38, Math.max(decimalLhs.scale, decimalRhs.scale))
        return Function.instance(
            name = getName(),
            returns = PType.decimal(p, s),
            parameters = arrayOf(
                Parameter("lhs", decimalLhs),
                Parameter("rhs", decimalRhs),
            )
        ) { args ->
            val arg0 = args[0].bigDecimal
            val arg1 = args[1].bigDecimal
            Datum.decimal(arg0 - arg1, p, s)
        }
    }

    override fun getRealInstance(realLhs: PType, realRhs: PType): Function.Instance {
        return basic(PType.real()) { args ->
            val arg0 = args[0].float
            val arg1 = args[1].float
            Datum.real((arg0 - arg1))
        }
    }

    override fun getDoubleInstance(doubleLhs: PType, doubleRhs: PType): Function.Instance {
        return basic(PType.doublePrecision()) { args ->
            val arg0 = args[0].double
            val arg1 = args[1].double
            Datum.doublePrecision((arg0 - arg1))
        }
    }
}
