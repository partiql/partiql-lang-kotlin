// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Function
import org.partiql.spi.internal.compareTo
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum

internal object FnLte : DiadicComparisonOperator("lte") {

    init {
        fillTable()
    }

    override fun getNumberComparison(lhs: Number, rhs: Number): Boolean {
        return lhs <= rhs
    }

    override fun getTinyIntInstance(tinyIntLhs: PType, tinyIntRhs: PType): Function.Instance {
        return basic(PType.bool(), PType.tinyint()) { args ->
            val lhs = args[0].byte
            val rhs = args[1].byte
            Datum.bool(lhs <= rhs)
        }
    }

    override fun getSmallIntInstance(smallIntLhs: PType, smallIntRhs: PType): Function.Instance {
        return basic(PType.bool(), PType.smallint()) { args ->
            val lhs = args[0].short
            val rhs = args[1].short
            Datum.bool(lhs <= rhs)
        }
    }

    override fun getIntegerInstance(integerLhs: PType, integerRhs: PType): Function.Instance {
        return basic(PType.bool(), PType.integer()) { args ->
            val lhs = args[0].int
            val rhs = args[1].int
            Datum.bool(lhs <= rhs)
        }
    }

    override fun getBigIntInstance(bigIntLhs: PType, bigIntRhs: PType): Function.Instance {
        return basic(PType.bool(), PType.bigint()) { args ->
            val lhs = args[0].long
            val rhs = args[1].long
            Datum.bool(lhs <= rhs)
        }
    }

    // TODO: Update
    override fun getNumericInstance(numericLhs: PType, numericRhs: PType): Function.Instance {
        return basic(PType.bool(), DefaultNumeric.NUMERIC) { args ->
            val lhs = args[0].bigDecimal
            val rhs = args[1].bigDecimal
            Datum.bool(lhs <= rhs)
        }
    }

    override fun getRealInstance(realLhs: PType, realRhs: PType): Function.Instance {
        return basic(PType.bool(), PType.real()) { args ->
            val lhs = args[0].float
            val rhs = args[1].float
            Datum.bool(lhs <= rhs)
        }
    }

    override fun getDoubleInstance(doubleLhs: PType, doubleRhs: PType): Function.Instance {
        return basic(PType.bool(), PType.doublePrecision()) { args ->
            val lhs = args[0].double
            val rhs = args[1].double
            Datum.bool(lhs <= rhs)
        }
    }

    override fun getStringInstance(stringLhs: PType, stringRhs: PType): Function.Instance {
        return basic(PType.bool(), PType.string()) { args ->
            val lhs = args[0].string
            val rhs = args[1].string
            Datum.bool(lhs <= rhs)
        }
    }

    override fun getTimestampInstance(timestampLhs: PType, timestampRhs: PType): Function.Instance {
        return basic(PType.bool(), timestampLhs, timestampRhs) { args ->
            val lhs = args[0].localDateTime
            val rhs = args[1].localDateTime
            Datum.bool(lhs <= rhs)
        }
    }

    override fun getDateInstance(dateLhs: PType, dateRhs: PType): Function.Instance {
        return basic(PType.bool(), PType.date()) { args ->
            val lhs = args[0].localDate
            val rhs = args[1].localDate
            Datum.bool(lhs <= rhs)
        }
    }

    override fun getTimeInstance(timeLhs: PType, timeRhs: PType): Function.Instance {
        return basic(PType.bool(), timeLhs, timeRhs) { args ->
            val lhs = args[0].localTime
            val rhs = args[1].localTime
            Datum.bool(lhs <= rhs)
        }
    }

    override fun getBooleanInstance(booleanLhs: PType, booleanRhs: PType): Function.Instance {
        return basic(PType.bool()) { args ->
            val lhs = args[0].boolean
            val rhs = args[1].boolean
            Datum.bool(lhs <= rhs)
        }
    }

    override fun getCharInstance(charLhs: PType, charRhs: PType): Function.Instance {
        return basic(PType.bool(), charLhs, charRhs) { args ->
            val lhs = args[0].string
            val rhs = args[1].string
            Datum.bool(lhs <= rhs)
        }
    }

    override fun getVarcharInstance(varcharLhs: PType, varcharRhs: PType): Function.Instance {
        return basic(PType.bool(), varcharLhs, varcharRhs) { args ->
            val lhs = args[0].string
            val rhs = args[1].string
            Datum.bool(lhs <= rhs)
        }
    }

    override fun getClobInstance(clobLhs: PType, clobRhs: PType): Function.Instance {
        return basic(PType.bool(), clobLhs, clobRhs) { args ->
            val lhs = args[0].bytes.toString()
            val rhs = args[1].bytes.toString()
            Datum.bool(lhs <= rhs)
        }
    }
}
