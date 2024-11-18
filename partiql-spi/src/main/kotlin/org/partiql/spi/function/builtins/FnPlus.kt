// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.value.Datum
import org.partiql.types.PType

internal object FnPlus : ArithmeticDiadicOperator() {
    override fun getName(): String {
        return "plus"
    }

    override fun getTinyIntInstance(tinyIntLhs: PType, tinyIntRhs: PType): Function.Instance {
        return basic(PType.tinyint()) { args ->
            @Suppress("DEPRECATION") val arg0 = args[0].byte
            @Suppress("DEPRECATION") val arg1 = args[1].byte
            Datum.tinyint((arg0 + arg1).toByte())
        }
    }

    override fun getSmallIntInstance(smallIntLhs: PType, smallIntRhs: PType): Function.Instance {
        return basic(PType.smallint()) { args ->
            val arg0 = args[0].short
            val arg1 = args[1].short
            Datum.smallint((arg0 + arg1).toShort())
        }
    }

    override fun getIntegerInstance(integerLhs: PType, integerRhs: PType): Function.Instance {
        return basic(PType.integer()) { args ->
            val arg0 = args[0].int
            val arg1 = args[1].int
            Datum.integer(arg0 + arg1)
        }
    }

    override fun getBigIntInstance(bigIntLhs: PType, bigIntRhs: PType): Function.Instance {
        return basic(PType.bigint()) { args ->
            val arg0 = args[0].long
            val arg1 = args[1].long
            Datum.bigint(arg0 + arg1)
        }
    }

    // TODO: Probably remove this if we don't expose NUMERIC
    override fun getNumericInstance(numericLhs: PType, numericRhs: PType): Function.Instance {
        return basic(PType.numeric()) { args ->
            val arg0 = args[0].bigInteger
            val arg1 = args[1].bigInteger
            Datum.numeric(arg0 + arg1)
        }
    }

    /**
     * Precision and scale calculation:
     * P = max(s1, s2) + max(p1 - s1, p2 - s2) + 1
     * S = max(s1, s2)
     */
    override fun getDecimalInstance(v1: PType, v2: PType): Function.Instance {
        val p = Math.min(38, Math.max(v1.scale, v2.scale) + Math.max(v1.precision - v1.scale, v2.precision - v2.scale) + 1)
        val s = Math.min(38, Math.max(v1.scale, v2.scale))
        return Function.instance(
            name = "plus",
            returns = PType.decimal(p, s),
            parameters = arrayOf(
                Parameter("lhs", v1),
                Parameter("rhs", v2),
            )
        ) { args ->
            val arg0 = args[0].bigDecimal
            val arg1 = args[1].bigDecimal
            Datum.decimal(arg0 + arg1, p, s)
        }
    }

    override fun getRealInstance(realLhs: PType, realRhs: PType): Function.Instance {
        return basic(PType.real()) { args ->
            val arg0 = args[0].float
            val arg1 = args[1].float
            Datum.real(arg0 + arg1)
        }
    }

    override fun getDoubleInstance(doubleLhs: PType, doubleRhs: PType): Function.Instance {
        return basic(PType.doublePrecision()) { args ->
            val arg0 = args[0].double
            val arg1 = args[1].double
            Datum.doublePrecision(arg0 + arg1)
        }
    }
}
