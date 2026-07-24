package org.partiql.spi.function.builtins

import org.partiql.spi.function.Fn
import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum

internal object FnUtils {
    const val MAXLENGTH = Int.MAX_VALUE

    /**
     * Reads this text [Datum] of the given [type] as a [String]. CLOB is byte-backed (decoded as
     * UTF-8); the character types (CHAR/VARCHAR/STRING) are read directly.
     */
    fun Datum.textValue(type: PType): String = when (type.code()) {
        PType.CLOB -> this.bytes.toString(Charsets.UTF_8)
        else -> this.string
    }

    /**
     * The result type for a SQL <string value function> over this text type. CHAR/VARCHAR map to
     * VARCHAR, CLOB stays CLOB, and everything else is STRING.
     *
     * Pass [length] to carry this type's declared length into the result — this is used by
     * length-preserving functions such as TRIM (CHAR(n)/VARCHAR(n) -> VARCHAR(n), CLOB(n) -> CLOB(n)).
     * Omit it for functions whose result length may differ from the input (e.g. REPLACE, SPLIT
     * elements, SUBSTRING), which produce unbounded VARCHAR/CLOB.
     */
    fun PType.stringFnReturn(length: Int? = null): PType = when (this.code()) {
        PType.CHAR, PType.VARCHAR -> if (length != null) PType.varchar(length) else PType.varchar()
        PType.CLOB -> if (length != null) PType.clob(length) else PType.clob()
        else -> PType.string()
    }

    /**
     * Wraps a [result] string into a [Datum] whose type matches [stringFnReturn] for this type.
     * See [stringFnReturn] for the meaning of [length].
     */
    fun PType.stringFnDatum(result: String, length: Int? = null): Datum = when (this.code()) {
        PType.CHAR, PType.VARCHAR -> if (length != null) Datum.varchar(result, length) else Datum.varchar(result)
        PType.CLOB -> if (length != null) Datum.clob(result.toByteArray(), length) else Datum.clob(result.toByteArray())
        else -> Datum.string(result)
    }

    /**
     * Returns an [Fn] instance whose only purpose is to let overload resolution succeed when an
     * argument is UNKNOWN (a literal NULL/MISSING).
     *
     * `FnResolver.match` calls `FnOverload.getInstance` with the full argument array *before* it
     * skips UNKNOWN args, so a function that returns null for a NULL argument would fail to resolve.
     * This instance exists purely so resolution matches; the framework then short-circuits on the
     * default `isNullCall` / `isMissingCall` (see `ExprCall`) and returns NULL/MISSING itself. The
     * invoke body is therefore never reached — it throws to make that invariant explicit rather than
     * fabricate a value.
     */
    fun nullResolutionInstance(name: String, returns: PType, args: Array<PType>): Fn {
        val parameters = args.mapIndexed { i, type -> Parameter("arg$i", type) }.toTypedArray()
        return Function.instance(
            name = name,
            returns = returns,
            parameters = parameters,
        ) {
            error("unreachable: null/missing propagation should be handled by isNullCall/isMissingCall")
        }
    }

    /**
     * Checks if adding two integers would cause overflow.
     * Uses the property: arg1 >= 0 && arg2 >= 0 && arg1 + arg2 < 0 => overflow occurred
     */
    fun checkLengthOverflow(length1: Int, length2: Int) {
        if (length1 >= 0 && length2 >= 0 && length1 + length2 < 0) {
            throw IllegalArgumentException("String length overflow: $length1 + $length2 exceeds maximum allowed length ($MAXLENGTH)")
        }
    }

    /**
     * Safely adds two lengths and returns the result, throwing if overflow occurs.
     */
    fun addLengths(length1: Int, length2: Int): Int {
        checkLengthOverflow(length1, length2)
        return length1 + length2
    }

    /**
     * Gets the length of a type, handling STRING types that don't have length constraints.
     */
    fun getTypeLength(type: PType): Int {
        return when (type.code()) {
            PType.STRING -> error("STRING type does not have length constraints")
            else -> type.length
        }
    }

    /**
     * Returns the type with higher coercibility for string type coercion.
     * Coercibility order: STRING > CLOB > VARCHAR > CHAR
     */
    fun getHigherCoercibilityType(type1: Int, type2: Int): Int {
        val coercibility = mapOf(
            PType.STRING to 4,
            PType.CLOB to 3,
            PType.VARCHAR to 2,
            PType.CHAR to 1
        )
        val coer1 = coercibility[type1] ?: error("Unknown type: $type1")
        val coer2 = coercibility[type2] ?: error("Unknown type: $type2")
        return if (coer1 >= coer2) type1 else type2
    }
}
