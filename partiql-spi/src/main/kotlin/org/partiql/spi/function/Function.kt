package org.partiql.spi.function

import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum

/**
 * Factory methods for standard function implementations.
 */
internal object Function {

    /**
     * @param name
     * @param parameters
     * @param returns
     * @param isNullCall
     * @param isMissingCall
     * @param invoke
     * @return
     */
    @JvmStatic
    fun instance(
        name: String,
        parameters: Array<Parameter>,
        returns: PType,
        isNullCall: Boolean = true,
        isMissingCall: Boolean = true,
        invoke: (Array<Datum>) -> Datum,
    ): Fn {
        return Fn.Builder(name)
            .returns(returns)
            .addParameters(parameters.toList())
            .isNullCall(isNullCall)
            .isMissingCall(isMissingCall)
            .body(invoke)
            .build()
    }

    /**
     * @param name
     * @param parameters
     * @param returns
     * @param isNullCall
     * @param isMissingCall
     * @param invoke
     * @return
     */
    @JvmStatic
    fun overload(
        name: String,
        parameters: Array<Parameter>,
        returns: PType,
        isNullCall: Boolean = true,
        isMissingCall: Boolean = true,
        invoke: (Array<Datum>) -> Datum,
    ): FnOverload = FnOverload.Builder(name)
        .returns(returns)
        .addParameters(parameters.toList())
        .isNullCall(isNullCall)
        .isMissingCall(isMissingCall)
        .body(invoke)
        .build()
}
