// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.value.Datum
import org.partiql.types.PType

/**
 * Function (operator) for the `IS MISSING` special form.
 */
internal val Fn_IS_MISSING__ANY__BOOL = object : Function {

    private var name = "is_missing"

    private var parameters = arrayOf(Parameter("value", PType.dynamic()))

    private var returns = PType.bool()

    /**
     * IS MISSING implementation.
     */
    private var instance = object : Function.Instance(
        name,
        parameters = arrayOf(PType.dynamic()),
        returns = PType.bool(),
        isNullCall = false,
        isMissingCall = false,
    ) {
        override fun invoke(args: Array<Datum>): Datum {
            return Datum.bool(args[0].isMissing)
        }
    }

    override fun getName(): String = name

    override fun getParameters(): Array<Parameter> = parameters

    override fun getReturnType(args: Array<PType>): PType = returns

    override fun getInstance(args: Array<PType>) = instance
}
