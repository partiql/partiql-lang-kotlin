package org.partiql.spi.function

import org.partiql.spi.types.PType

/**
 * Represents a SQL aggregation function, such as MAX, MIN, SUM, etc.
 */
internal object Aggregation {

    /**
     * @param name
     * @param parameters
     * @param returns
     * @param accumulator
     * @return
     */
    @JvmStatic
    fun overload(
        name: String,
        parameters: Array<Parameter>,
        returns: PType,
        accumulator: () -> Accumulator,
    ): AggOverload {
        return AggOverload.Builder(name)
            .returns(returns)
            .addParameters(*parameters.map { it.getType() }.toTypedArray())
            .body(accumulator)
            .build()
    }
}
