// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.value.Datum
import org.partiql.types.PType

/**
 * Function (operator) for the `IS NULL` special form.
 */
internal val Fn_IS_NULL__ANY__BOOL = object : Function {

    private var name = "is_null"

    private var parameters = arrayOf(Parameter("value", PType.dynamic()))

    private var returns = PType.bool()

    /**
     * IS NULL implementation.
     */
    private var instance = object : Function.Instance(
        name,
        parameters = arrayOf(PType.dynamic()),
        returns = PType.bool(),
        isNullCall = false,
        isMissingCall = false,
    ) {
        override fun invoke(args: Array<Datum>): Datum {
            if (args[0].isMissing) {
                return Datum.bool(true)
            }
            return Datum.bool(args[0].isNull)
        }
    }

    override fun getName(): String = name

    override fun getParameters(): Array<Parameter> = parameters

    override fun getReturnType(args: Array<PType>): PType = returns

    override fun getInstance(args: Array<PType>) = instance
}
