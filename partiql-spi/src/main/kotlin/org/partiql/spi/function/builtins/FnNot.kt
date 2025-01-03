// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.function.utils.FunctionUtils
import org.partiql.spi.value.Datum
import org.partiql.types.PType

/**
 * This is the boolean NOT predicate. Its name is obfuscated via the use of [FunctionUtils.SYSTEM_PREFIX_INTERNAL].
 */
internal val Fn_NOT__BOOL__BOOL = object : Function {

    private val name = FunctionUtils.SYSTEM_PREFIX_INTERNAL + "not"

    private var parameters = arrayOf(Parameter("value", PType.dynamic()))

    private var returns = PType.bool()

    private var instance = object : Function.Instance(
        name,
        parameters = arrayOf(PType.dynamic()),
        returns = PType.bool(),
        isNullCall = true,
        isMissingCall = false
    ) {
        override fun invoke(args: Array<Datum>): Datum {
            val arg = args[0]
            if (arg.isMissing) {
                return Datum.nullValue(PType.bool())
            }
            val value = arg.boolean
            return Datum.bool(value.not())
        }
    }

    override fun getName(): String = name

    override fun getParameters(): Array<Parameter> = parameters

    override fun getReturnType(args: Array<PType>): PType = returns

    override fun getInstance(args: Array<PType>) = instance
}
