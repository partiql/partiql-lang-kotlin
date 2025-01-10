// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Function
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum
import kotlin.experimental.and

internal object FnBitwiseAnd : DiadicArithmeticOperator("bitwise_and") {

    init {
        fillTable()
    }

    override fun getTinyIntInstance(tinyIntLhs: PType, tinyIntRhs: PType): Function.Instance {
        return basic(PType.tinyint()) { args ->
            @Suppress("DEPRECATION") val arg0 = args[0].byte
            @Suppress("DEPRECATION") val arg1 = args[1].byte
            Datum.tinyint(arg0 and arg1)
        }
    }

    override fun getSmallIntInstance(smallIntLhs: PType, smallIntRhs: PType): Function.Instance {
        return basic(PType.smallint()) { args ->
            val arg0 = args[0].short
            val arg1 = args[1].short
            Datum.smallint(arg0 and arg1)
        }
    }

    override fun getIntegerInstance(integerLhs: PType, integerRhs: PType): Function.Instance {
        return basic(PType.integer()) { args ->
            val arg0 = args[0].int
            val arg1 = args[1].int
            Datum.integer(arg0 and arg1)
        }
    }

    override fun getBigIntInstance(bigIntLhs: PType, bigIntRhs: PType): Function.Instance {
        return basic(PType.bigint()) { args ->
            val arg0 = args[0].long
            val arg1 = args[1].long
            Datum.bigint(arg0 and arg1)
        }
    }

    override fun getNumericInstance(numericLhs: PType, numericRhs: PType): Function.Instance {
        return getBigIntInstance(numericLhs, numericRhs)
    }

    override fun getDecimalInstance(decimalLhs: PType, decimalRhs: PType): Function.Instance {
        return getBigIntInstance(decimalLhs, decimalRhs)
    }

    override fun getRealInstance(realLhs: PType, realRhs: PType): Function.Instance {
        return getBigIntInstance(realLhs, realRhs)
    }

    override fun getDoubleInstance(doubleLhs: PType, doubleRhs: PType): Function.Instance {
        return getBigIntInstance(doubleLhs, doubleRhs)
    }
}
