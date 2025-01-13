// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Function
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum

internal object FnTimes : DiadicArithmeticOperator("times") {

    init {
        fillTable()
    }

    override fun getTinyIntInstance(tinyIntLhs: PType, tinyIntRhs: PType): Function.Instance {
        return basic(PType.tinyint()) { args ->
            @Suppress("DEPRECATION") val arg0 = args[0].byte
            @Suppress("DEPRECATION") val arg1 = args[1].byte
            Datum.tinyint((arg0 * arg1).toByte())
        }
    }

    override fun getSmallIntInstance(smallIntLhs: PType, smallIntRhs: PType): Function.Instance {
        return basic(PType.smallint()) { args ->
            val arg0 = args[0].short
            val arg1 = args[1].short
            Datum.smallint((arg0 * arg1).toShort())
        }
    }

    override fun getIntegerInstance(integerLhs: PType, integerRhs: PType): Function.Instance {
        return basic(PType.integer()) { args ->
            val arg0 = args[0].int
            val arg1 = args[1].int
            Datum.integer(arg0 * arg1)
        }
    }

    override fun getBigIntInstance(bigIntLhs: PType, bigIntRhs: PType): Function.Instance {
        return basic(PType.bigint()) { args ->
            val arg0 = args[0].long
            val arg1 = args[1].long
            Datum.bigint(arg0 * arg1)
        }
    }

    override fun getNumericInstance(numericLhs: PType, numericRhs: PType): Function.Instance {
        return basic(DefaultNumeric.NUMERIC) { args ->
            val arg0 = args[0].bigDecimal
            val arg1 = args[1].bigDecimal
            Datum.numeric(arg0 * arg1)
        }
    }

    // SQL Server:
    // p = p1 + p2 + 1
    // s = s1 + s2
    override fun getDecimalInstance(decimalLhs: PType, decimalRhs: PType): Function.Instance {
        val p = decimalLhs.precision + decimalRhs.precision + 1
        val s = decimalLhs.scale + decimalRhs.scale
        return basic(PType.decimal(p, s), decimalLhs, decimalRhs) { args ->
            val arg0 = args[0].bigDecimal
            val arg1 = args[1].bigDecimal
            Datum.decimal(arg0 * arg1, p, s)
        }
    }

    override fun getRealInstance(realLhs: PType, realRhs: PType): Function.Instance {
        return basic(PType.real()) { args ->
            val arg0 = args[0].float
            val arg1 = args[1].float
            Datum.real(arg0 * arg1)
        }
    }

    override fun getDoubleInstance(doubleLhs: PType, doubleRhs: PType): Function.Instance {
        return basic(PType.doublePrecision()) { args ->
            val arg0 = args[0].double
            val arg1 = args[1].double
            Datum.doublePrecision(arg0 * arg1)
        }
    }
}
