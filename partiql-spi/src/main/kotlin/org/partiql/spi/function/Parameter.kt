package org.partiql.spi.function

import org.partiql.spi.internal.SqlTypeFamily
import org.partiql.spi.internal.SqlTypes
import org.partiql.types.PType
import org.partiql.types.PType.Kind.DYNAMIC

/**
 * [Parameter] is a formal argument's definition.
 */
public class Parameter private constructor(
    private var name: String,
    private var type: SqlTypeFamily,
    private var dynamic: Boolean = false,
    private var variadic: Boolean = false,
) {

    /**
     *                 DEVELOPER NOTES
     * ------------------------------------------------
     * Representation of a "family" is internalized
     * to the library, and is not (and should not) be
     * exposed to the library consumers.
     *
     * Consumers may only instantiate parameters by
     * using the static factory methods or the simple
     * constructor. This enables refinement of the
     * parameter representation without impacting the
     * public API.
     *
     * This is not an interface)because it is NOT meant
     * to be extended, as that may interfere with
     * function resolution and query semantics.
     *
     * Note that there are several places to enhance
     * this, and some things may be redundant. Again,
     * this is an internal API and any refinements
     * and optimizations are certainly welcomed.
     * ------------------------------------------------
     */

    /**
     * @constructor
     * Create a non-variadic parameter with the exact type.
     *
     * @param name  Parameter name used for error reporting, debugging, and documentation.
     * @param type  Parameter type used for function resolution.
     */
    public constructor(name: String, type: PType) : this(name, SqlTypeFamily(type)) {
        dynamic = type.kind == DYNAMIC
        variadic = false
    }

    /**
     * Get the parameter name; used for debugging and error reporting.
     */
    public fun getName(): String = name

    /**
     * Get the parameter preferred type.
     */
    public fun getType(): PType = type.preferred

    /**
     * Get match is used for function resolution; it indicates an exact match, coercion, or no match.
     *
     * Rules
     *  1. If arg matches the parameter exactly, return arg.
     *  2. If arg can be coerced to the parameter, return the coercion type.
     *  3. If arg does NOT match the parameter, return null.
     */
    public fun getMatch(arg: PType): PType? {
        if (dynamic || arg in type) {
            return arg // exact match
        }
        if (arg.kind == DYNAMIC || SqlTypes.isAssignable(arg, type.preferred)) {
            return type.preferred
        }
        return null
    }

    public companion object {

        /**
         * Create a dynamic [Parameter].
         */
        @JvmStatic
        public fun dynamic(name: String): Parameter = Parameter(name, PType.dynamic())

        /**
         * Create a character string [Parameter].
         */
        @JvmStatic
        public fun text(name: String): Parameter = Parameter(name, SqlTypeFamily.TEXT, false)

        /**
         * Create a number [Parameter].
         */
        @JvmStatic
        public fun number(name: String): Parameter = Parameter(name, SqlTypeFamily.NUMBER, false)

        /**
         * Create a collection [Parameter].
         */
        @JvmStatic
        public fun collection(name: String): Parameter = Parameter(name, SqlTypeFamily.COLLECTION, false)
    }
}
