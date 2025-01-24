// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Fn
import org.partiql.spi.function.builtins.internal.PErrors
import org.partiql.spi.internal.isZero
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum
import java.math.RoundingMode

internal object FnDivide : DiadicArithmeticOperator("divide") {

    init {
        fillTable()
    }

    override fun getTinyIntInstance(tinyIntLhs: PType, tinyIntRhs: PType): Fn {
        return basic(PType.tinyint()) { args ->
            val arg0 = args[0].byte
            val arg1 = args[1].byte
            if (arg1 == 0.toByte()) {
                throw PErrors.divisionByZeroException(arg0, PType.tinyint())
            } else if (arg0 == Byte.MIN_VALUE && arg1.toInt() == -1) {
                throw PErrors.numericValueOutOfRangeException("$arg0 / $arg1", PType.tinyint())
            }
            Datum.tinyint((arg0 / arg1).toByte())
        }
    }

    override fun getSmallIntInstance(smallIntLhs: PType, smallIntRhs: PType): Fn {
        return basic(PType.smallint()) { args ->
            val arg0 = args[0].short
            val arg1 = args[1].short
            if (arg1 == 0.toShort()) {
                throw PErrors.divisionByZeroException(arg0, PType.smallint())
            } else if (arg0 == Short.MIN_VALUE && arg1.toInt() == -1) {
                throw PErrors.numericValueOutOfRangeException("$arg0 / $arg1", PType.smallint())
            }
            Datum.smallint((arg0 / arg1).toShort())
        }
    }

    override fun getIntegerInstance(integerLhs: PType, integerRhs: PType): Fn {
        return basic(PType.integer()) { args ->
            val arg0 = args[0].int
            val arg1 = args[1].int
            if (arg1 == 0) {
                throw PErrors.divisionByZeroException(arg0, PType.integer())
            } else if (arg0 == Int.MIN_VALUE && arg1 == -1) {
                throw PErrors.numericValueOutOfRangeException("$arg0 / $arg1", PType.integer())
            }
            Datum.integer(arg0 / arg1)
        }
    }

    override fun getBigIntInstance(bigIntLhs: PType, bigIntRhs: PType): Fn {
        return basic(PType.bigint()) { args ->
            val arg0 = args[0].long
            val arg1 = args[1].long
            if (arg1 == 0L) {
                throw PErrors.divisionByZeroException(arg0, PType.bigint())
            } else if (arg0 == Long.MIN_VALUE && arg1 == -1L) {
                throw PErrors.numericValueOutOfRangeException("$arg0 / $arg1", PType.bigint())
            }
            Datum.bigint(arg0 / arg1)
        }
    }

    override fun getNumericInstance(numericLhs: PType, numericRhs: PType): Fn {
        val (p, s) = dividePrecisionScale(numericLhs, numericRhs)
        return basic(PType.numeric(p, s), numericLhs, numericRhs) { args ->
            val arg0 = args[0].bigDecimal
            val arg1 = args[1].bigDecimal
            if (arg1.isZero()) {
                throw PErrors.divisionByZeroException(arg0, PType.numeric(p, s))
            }
            val result = arg0.divide(arg1, s, RoundingMode.HALF_UP)
            Datum.numeric(result, p, s)
        }
    }

    override fun getDecimalInstance(decimalLhs: PType, decimalRhs: PType): Fn {
        val (p, s) = dividePrecisionScale(decimalLhs, decimalRhs)
        return basic(PType.decimal(p, s), decimalLhs, decimalRhs) { args ->
            val arg0 = args[0].bigDecimal
            val arg1 = args[1].bigDecimal
            if (arg1.isZero()) {
                throw PErrors.divisionByZeroException(arg0, PType.decimal(p, s))
            }
            val result = arg0.divide(arg1, s, RoundingMode.HALF_UP)
            Datum.decimal(result, p, s)
        }
    }

    /**
     * SQL Server: p = p1 - s1 + s2 + max(6, s1 + p2 + 1)
     * SQL Server: s = max(6, s1 + p2 + 1)
     */
    private fun dividePrecisionScale(lhs: PType, rhs: PType): Pair<Int, Int> {
        val (p1, s1) = lhs.precision to lhs.scale
        val (p2, s2) = rhs.precision to rhs.scale
        val p = p1 - s1 + s2 + Math.max(6, s1 + p2 + 1)
        val s = Math.max(6, s1 + p2 + 1)
        val returnedP = p.coerceAtMost(38)
        val returnedS = s.coerceAtMost(p)
        return returnedP to returnedS
    }

    override fun getRealInstance(realLhs: PType, realRhs: PType): Fn {
        return basic(PType.real()) { args ->
            val arg0 = args[0].float
            val arg1 = args[1].float
            if (arg1.isZero()) {
                throw PErrors.divisionByZeroException(arg0, PType.real())
            }
            Datum.real(arg0 / arg1)
        }
    }

    override fun getDoubleInstance(doubleLhs: PType, doubleRhs: PType): Fn {
        return basic(PType.doublePrecision()) { args ->
            val arg0 = args[0].double
            val arg1 = args[1].double
            if (arg1.isZero()) {
                throw PErrors.divisionByZeroException(arg0, PType.doublePrecision())
            }
            Datum.doublePrecision(arg0 / arg1)
        }
    }
}
