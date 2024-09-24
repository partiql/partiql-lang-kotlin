package org.partiql.spi.function

import org.partiql.types.PType

/**
 * [Parameter] is a formal argument's definition.
 */
public class Parameter private constructor(
    private var name: String,
    private var type: PType,
    private var variadic: Boolean,
) {

    /**
     * @constructor
     * Default public constructor forms a regular (non-variadic
     *
     * @param name  Parameter name used for error reporting, debugging, and documentation.
     * @param type  Parameter type used for function resolution.
     */
    public constructor(name: String, type: PType) : this(name, type, false)

    public fun getName(): String = name

    public fun getType(): PType = type
}
