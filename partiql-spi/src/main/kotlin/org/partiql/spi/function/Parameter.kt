package org.partiql.spi.function

import org.partiql.types.PType

/**
 * [Parameter] is a formal argument's definition.
 *
 * @constructor
 * Default constructor.
 *
 * @param name  Parameter name used for error reporting, debugging, and documentation.
 * @param type  Parameter type used for function resolution.
 */
public class Parameter(name: String, type: PType) {

    private var _name: String = name
    private var _type: PType = type

    public fun getName(): String = _name
    public fun getType(): PType = _type
}
