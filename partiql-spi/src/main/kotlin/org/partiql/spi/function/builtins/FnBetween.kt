package org.partiql.spi.function.builtins

import org.partiql.spi.function.Fn
import org.partiql.spi.function.FnOverload
import org.partiql.spi.function.Function.instance
import org.partiql.spi.function.Parameter
import org.partiql.spi.function.RoutineOverloadSignature
import org.partiql.spi.function.builtins.TypePrecedence.TYPE_PRECEDENCE
import org.partiql.spi.internal.SqlTypeFamily
import org.partiql.spi.types.PType
import org.partiql.spi.utils.FunctionUtils
import org.partiql.spi.utils.NumberUtils.compareTo
import org.partiql.spi.utils.getNumber
import org.partiql.spi.value.Datum

internal object FnBetween : FnOverload() {
    val name = FunctionUtils.hide("between")
    override fun getSignature(): RoutineOverloadSignature {
        return RoutineOverloadSignature(name, listOf(PType.dynamic(), PType.dynamic(), PType.dynamic()))
    }

    override fun getInstance(args: Array<out PType>): Fn? {
        val arg0 = args[0]
        val arg1 = args[1]
        val arg2 = args[2]
        // Overload based on the type family
        if (args.all { SqlTypeFamily.NUMBER.contains(it) }) {
            return getNumberInstance(arg0, arg1, arg2)
        }
        if (args.all { SqlTypeFamily.TEXT.contains(it) }) {
            return getStringInstance(arg0, arg1, arg2)
        }

        if (args.all { it.code() == PType.CLOB }) {
            return getClobInstance(arg0, arg1, arg2)
        }

        // DATE_TIMESTAMP family: DATE, TIMESTAMP, TIMESTAMPZ
        // Find the highest-precedence type and use it for all parameters to enable autocast.
        if (args.all { SqlTypeFamily.DATE_TIMESTAMP.contains(it) }) {
            val target = args.maxByOrNull { TYPE_PRECEDENCE[it.code()]!! }!!
            return when (target.code()) {
                PType.DATE -> getDateInstance(target, target, target)
                PType.TIMESTAMP -> getTimestampInstance(target, target, target)
                PType.TIMESTAMPZ -> getTimestampzInstance(target, target, target)
                else -> null
            }
        }

        if (args.all { SqlTypeFamily.TIME.contains(it) }) {
            val target = args.maxByOrNull { TYPE_PRECEDENCE[it.code()]!! }!!
            return when (target.code()) {
                PType.TIME -> getTimeInstance(target, target, target)
                PType.TIMEZ -> getTimezInstance(target, target, target)
                else -> null
            }
        }

        return null
    }

    private fun getNumberInstance(arg0: PType, arg1: PType, arg2: PType): Fn {
        return instance(
            name = name,
            returns = PType.bool(),
            parameters = arrayOf(
                Parameter("value", arg0),
                Parameter("lower", arg1),
                Parameter("upper", arg2)
            ),
            invoke = { args ->
                val v = args[0].getNumber()
                val l = args[1].getNumber()
                val r = args[2].getNumber()
                Datum.bool(l <= v && v <= r)
            }
        )
    }

    private fun getStringInstance(arg0: PType, arg1: PType, arg2: PType): Fn {
        return instance(
            name = name,
            returns = PType.bool(),
            parameters = arrayOf(
                Parameter("value", arg0),
                Parameter("lower", arg1),
                Parameter("upper", arg2)
            ),
            invoke = { args ->
                val v = args[0].string
                val l = args[1].string
                val r = args[2].string
                Datum.bool(v in l..r)
            }
        )
    }

    private fun getClobInstance(arg0: PType, arg1: PType, arg2: PType): Fn {
        return instance(
            name = name,
            returns = PType.bool(),
            parameters = arrayOf(
                Parameter("value", arg0),
                Parameter("lower", arg1),
                Parameter("upper", arg2)
            ),
            invoke = { args ->
                val v = args[0].bytes.toString(Charsets.UTF_8)
                val l = args[1].bytes.toString(Charsets.UTF_8)
                val r = args[2].bytes.toString(Charsets.UTF_8)
                Datum.bool(v in l..r)
            }
        )
    }

    private fun getDateInstance(arg0: PType, arg1: PType, arg2: PType): Fn {
        return instance(
            name = name,
            returns = PType.bool(),
            parameters = arrayOf(
                Parameter("value", arg0),
                Parameter("lower", arg1),
                Parameter("upper", arg2)
            ),
            invoke = { args ->
                val v = args[0].localDate
                val l = args[1].localDate
                val r = args[2].localDate
                Datum.bool(v in l..r)
            }
        )
    }

    private fun getTimeInstance(arg0: PType, arg1: PType, arg2: PType): Fn {
        return instance(
            name = name,
            returns = PType.bool(),
            parameters = arrayOf(
                Parameter("value", arg0),
                Parameter("lower", arg1),
                Parameter("upper", arg2)
            ),
            invoke = { args ->
                val v = args[0].localTime
                val l = args[1].localTime
                val r = args[2].localTime
                Datum.bool(v in l..r)
            }
        )
    }

    private fun getTimestampInstance(arg0: PType, arg1: PType, arg2: PType): Fn {
        return instance(
            name = name,
            returns = PType.bool(),
            parameters = arrayOf(
                Parameter("value", arg0),
                Parameter("lower", arg1),
                Parameter("upper", arg2)
            ),
            invoke = { args ->
                val v = args[0].localDateTime
                val l = args[1].localDateTime
                val r = args[2].localDateTime
                Datum.bool(v in l..r)
            }
        )
    }

    private fun getTimezInstance(arg0: PType, arg1: PType, arg2: PType): Fn {
        return instance(
            name = name,
            returns = PType.bool(),
            parameters = arrayOf(
                Parameter("value", arg0),
                Parameter("lower", arg1),
                Parameter("upper", arg2)
            ),
            invoke = { args ->
                val v = args[0].offsetTime
                val l = args[1].offsetTime
                val r = args[2].offsetTime
                Datum.bool(v >= l && v <= r)
            }
        )
    }

    private fun getTimestampzInstance(arg0: PType, arg1: PType, arg2: PType): Fn {
        return instance(
            name = name,
            returns = PType.bool(),
            parameters = arrayOf(
                Parameter("value", arg0),
                Parameter("lower", arg1),
                Parameter("upper", arg2)
            ),
            invoke = { args ->
                val v = args[0].offsetDateTime
                val l = args[1].offsetDateTime
                val r = args[2].offsetDateTime
                Datum.bool(v >= l && v <= r)
            }
        )
    }
}
