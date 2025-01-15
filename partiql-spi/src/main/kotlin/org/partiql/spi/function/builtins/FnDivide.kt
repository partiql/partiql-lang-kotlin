// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.errors.DataException
import org.partiql.spi.function.Function
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum

internal object FnDivide : DiadicArithmeticOperator("divide") {

    init {
        fillTable()
    }

    override fun getTinyIntInstance(tinyIntLhs: PType, tinyIntRhs: PType): Function.Instance {
        return basic(PType.tinyint()) { args ->
            val arg0 = args[0].byte
            val arg1 = args[1].byte
            if (arg1 == 0.toByte()) {
                throw DataException("Division by zero for TINYINT: $arg0 / $arg1")
            } else if (arg0 == Byte.MIN_VALUE && arg1.toInt() == -1) {
                throw DataException("Resulting value out of range for TINYINT: $arg0 / $arg1")
            }
            Datum.tinyint((arg0 / arg1).toByte())
        }
    }

    override fun getSmallIntInstance(smallIntLhs: PType, smallIntRhs: PType): Function.Instance {
        return basic(PType.smallint()) { args ->
            val arg0 = args[0].short
            val arg1 = args[1].short
            if (arg1 == 0.toShort()) {
                throw DataException("Division by zero for SMALLINT: $arg0 / $arg1")
            } else if (arg0 == Short.MIN_VALUE && arg1.toInt() == -1) {
                throw DataException("Resulting value out of range for SMALLINT: $arg0 / $arg1")
            }
            Datum.smallint((arg0 / arg1).toShort())
        }
    }

    override fun getIntegerInstance(integerLhs: PType, integerRhs: PType): Function.Instance {
        return basic(PType.integer()) { args ->
            val arg0 = args[0].int
            val arg1 = args[1].int
            if (arg1 == 0) {
                throw DataException("Division by zero for INT: $arg0 / $arg1")
            } else if (arg0 == Int.MIN_VALUE && arg1 == -1) {
                throw DataException("Resulting value out of range for INT: $arg0 / $arg1")
            }
            Datum.integer(arg0 / arg1)
        }
    }

    override fun getBigIntInstance(bigIntLhs: PType, bigIntRhs: PType): Function.Instance {
        return basic(PType.bigint()) { args ->
            val arg0 = args[0].long
            val arg1 = args[1].long
            if (arg1 == 0L) {
                throw DataException("Division by zero for BIGINT: $arg0 / $arg1")
            } else if (arg0 == Long.MIN_VALUE && arg1 == -1L) {
                throw DataException("Resulting value out of range for BIGINT: $arg0 / $arg1")
            }
            Datum.bigint(arg0 / arg1)
        }
    }

    override fun getNumericInstance(numericLhs: PType, numericRhs: PType): Function.Instance {
        return basic(DefaultNumeric.NUMERIC) { args ->
            val arg0 = args[0].bigDecimal
            val arg1 = args[1].bigDecimal
            Datum.numeric(arg0 / arg1)
        }
    }

    // SQL:Server:
    // p = p1 - s1 + s2 + max(6, s1 + p2 + 1)
    // s = max(6, s1 + p2 + 1)
    override fun getDecimalInstance(decimalLhs: PType, decimalRhs: PType): Function.Instance {
        val p = decimalLhs.precision - decimalLhs.scale + decimalRhs.scale + Math.max(6, decimalLhs.scale + decimalRhs.precision + 1)
        val s = Math.max(6, decimalLhs.scale + decimalRhs.precision + 1)
        return basic(PType.decimal(p, s), decimalLhs, decimalRhs) { args ->
            val arg0 = args[0].bigDecimal
            val arg1 = args[1].bigDecimal
            Datum.decimal(arg0 / arg1, p, s)
        }
    }

    override fun getRealInstance(realLhs: PType, realRhs: PType): Function.Instance {
        return basic(PType.real()) { args ->
            val arg0 = args[0].float
            val arg1 = args[1].float
            Datum.real(arg0 / arg1)
        }
    }

    override fun getDoubleInstance(doubleLhs: PType, doubleRhs: PType): Function.Instance {
        return basic(PType.doublePrecision()) { args ->
            val arg0 = args[0].double
            val arg1 = args[1].double
            Datum.doublePrecision(arg0 / arg1)
        }
    }
}
