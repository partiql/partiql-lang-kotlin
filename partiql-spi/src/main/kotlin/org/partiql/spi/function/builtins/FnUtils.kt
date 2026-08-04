package org.partiql.spi.function.builtins

import org.partiql.spi.function.Fn
import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.internal.SqlTypeFamily
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum

internal object FnUtils {
    const val MAXLENGTH = Int.MAX_VALUE

    /**
     * True when this type is a member of [SqlTypeFamily.TEXT] (CHAR/VARCHAR/STRING/CLOB) or is
     * UNKNOWN (a literal NULL/MISSING, whose type is only known at resolution time).
     *
     * Overload resolution passes UNKNOWN through `getInstance`, so text-only functions must accept
     * it here and then return a [nullResolutionInstance] rather than rejecting the call.
     */
    fun PType.isTextOrUnknown(): Boolean = this in SqlTypeFamily.TEXT || this.code() == PType.UNKNOWN

    /**
     * Reads this text [Datum] of the given [type] as a [String]. CLOB is byte-backed (decoded as
     * UTF-8); the character types (CHAR/VARCHAR/STRING) are read directly.
     */
    fun Datum.textValue(type: PType): String = when (type.code()) {
        PType.CLOB -> this.bytes.toString(Charsets.UTF_8)
        else -> this.string
    }

    /**
     * Wraps [value] into a [Datum] of exactly this text type, carrying this type's declared length —
     * the inverse of [textValue]. The receiver must be a text type (CHAR/VARCHAR/CLOB/STRING); any
     * other type falls through to STRING, so callers must guard with [isTextOrUnknown] first.
     *
     * Unlike [stringFnResult], this applies no widening: a CHAR type yields a padded CHAR datum. Use
     * it when the result type is computed up front and must be reproduced verbatim (e.g. the coerced
     * result type of `||`, or a function that preserves its input type such as UPPER/LOWER); use
     * [stringFnResult] for the SQL <string value function> convention.
     */
    fun PType.datumOf(value: String): Datum = when (this.code()) {
        PType.CHAR -> Datum.character(value, this.length)
        PType.VARCHAR -> Datum.varchar(value, this.length)
        PType.CLOB -> Datum.clob(value.toByteArray(), this.length)
        else -> Datum.string(value)
    }

    /**
     * The result type a SQL <string value function> (SQL2023 section 6.33) produces over this text
     * type, per the spec's result-type rules:
     * - CHAR(n)/VARCHAR(n) -> VARCHAR, because these functions may change the length, and a
     *   fixed-length result would have to pad
     * - CLOB(n)            -> CLOB
     * - STRING             -> STRING (PartiQL extension)
     *
     * [length] is the result's maximum length, and callers pick it by what they can prove about the
     * result at plan time:
     * 1. The result can only be *shorter* than the input — pass the input's length (the bounded text
     *    types expose it, STRING carries none). TRIM and its variants, SUBSTRING, and SPLIT elements
     *    are all in this group: the input length is a correct upper bound, so CHAR(n)/VARCHAR(n) ->
     *    VARCHAR(n) and CLOB(n) -> CLOB(n).
     * 2. The result's length is *computable* from the argument types — pass the computed value. `||`
     *    is the only such case (L1 + L2, exact), and it builds its type directly rather than going
     *    through here.
     * 3. The result's length *cannot be determined* at plan time — omit [length]. REPLACE is the case:
     *    growth is `occurrences * (to.length - from.length)`, all runtime values. The result is then
     *    unbounded and takes [MAXLENGTH].
     *
     * Never substitute a smaller arbitrary bound for case 3: [stringFnResult] truncates a value that
     * exceeds the length it is given, so an invented bound silently cuts correct output.
     *
     * Note that [MAXLENGTH] participates in `||`'s length arithmetic; a variable-length concat clamps
     * the sum back to [MAXLENGTH] rather than overflowing — see [addLengthsClamped].
     *
     * Pairs with [stringFnResult], which boxes a value at this type. Keep the two in step: a function
     * must declare `returns` from this and build its [Datum] from that, with the same [length].
     */
    fun PType.stringFnReturnType(length: Int? = null): PType = when (this.code()) {
        PType.CHAR, PType.VARCHAR -> PType.varchar(length ?: MAXLENGTH)
        PType.CLOB -> PType.clob(length ?: MAXLENGTH)
        else -> PType.string()
    }

    /**
     * Wraps a computed [value] into a [Datum] typed as [stringFnReturnType] for this type — the value
     * counterpart of that function, following the same SQL <string value function> convention (so a
     * CHAR input yields a VARCHAR datum, not a padded CHAR). See [stringFnReturnType] for the meaning
     * of [length], which must match the one passed there.
     */
    fun PType.stringFnResult(value: String, length: Int? = null): Datum = when (this.code()) {
        PType.CHAR, PType.VARCHAR -> Datum.varchar(value, length ?: MAXLENGTH)
        PType.CLOB -> Datum.clob(value.toByteArray(), length ?: MAXLENGTH)
        else -> Datum.string(value)
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
     * Safely adds two lengths and returns the result, throwing if the sum exceeds [MAXLENGTH].
     *
     * Use this for a *fixed-length* result (CHAR), where the declared length is the exact length of
     * every value: there is nothing to clamp to, since a shorter length would describe a different
     * value. Variable-length results should use [addLengthsClamped] instead.
     */
    fun addLengths(length1: Int, length2: Int): Int {
        checkLengthOverflow(length1, length2)
        return length1 + length2
    }

    /**
     * Adds two lengths, clamping the result to [MAXLENGTH] rather than overflowing.
     *
     * Use this for a *variable-length* result (VARCHAR/CLOB), where the declared length is only an
     * upper bound: clamping an over-long bound down to [MAXLENGTH] loses nothing, because no value can
     * exceed [MAXLENGTH] characters anyway. This mirrors SQL2023 section 6.32 <string value
     * expression>, whose result length is `min(L1 + L2, maximum length)` for the variable-length and
     * large-object cases but an error for the fixed-length one.
     *
     * Clamping (rather than raising) is what lets an unbounded length participate in concatenation at
     * all: [stringFnReturnType] assigns [MAXLENGTH] when a function's result length is not computable
     * (REPLACE), and an unbounded CLOB defaults to [MAXLENGTH], so `replace(...) || x` and
     * `CLOB || CLOB` would otherwise be errors for a sum that is merely imprecise, not invalid.
     *
     * The addition is done in [Long] so the sum itself cannot overflow before being clamped.
     */
    fun addLengthsClamped(length1: Int, length2: Int): Int =
        minOf(length1.toLong() + length2.toLong(), MAXLENGTH.toLong()).toInt()

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
            PType.STRING to 5,
            PType.CLOB to 4,
            PType.VARCHAR to 3,
            PType.CHAR to 2,
            PType.UNKNOWN to 1
        )
        val coer1 = coercibility[type1] ?: error("Unknown type: $type1")
        val coer2 = coercibility[type2] ?: error("Unknown type: $type2")
        return if (coer1 >= coer2) type1 else type2
    }
}
