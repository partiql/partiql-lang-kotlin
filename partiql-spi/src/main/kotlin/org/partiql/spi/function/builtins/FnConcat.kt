package org.partiql.spi.function.builtins

import org.partiql.spi.function.Fn
import org.partiql.spi.function.FnOverload
import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.function.RoutineOverloadSignature
import org.partiql.spi.function.builtins.FnUtils.datumOf
import org.partiql.spi.function.builtins.FnUtils.isTextOrUnknown
import org.partiql.spi.function.builtins.FnUtils.textValue
import org.partiql.spi.types.PType
import org.partiql.spi.utils.FunctionUtils

/**
 * SQL concatenation function implementation.
 *
 * Implements the SQL <concatenation> as defined in SQL2023 section 6.32 <string value expression>.
 *
 * According to SQL specification, result type is determined by coercibility:
 * - If either argument is CLOB: result is CLOB with length = min(L1 + L2, max_clob_length)
 * - If either argument is VARCHAR: result is VARCHAR with length = min(L1 + L2, max_varchar_length)
 * - If both arguments are CHAR: result is CHAR with length = min(L1 + L2, max_char_length)
 *
 * PartiQL extensions:
 * - STRING type (PartiQL-specific unlimited length string) has the highest coercibility
 *
 * Coercibility order: STRING > CLOB > VARCHAR > CHAR
 * - STRING || any → STRING (PartiQL extension)
 * - CLOB(L1) || CHAR(L2)/VARCHAR(L2) → CLOB(L1 + L2)
 * - VARCHAR(L1) || CHAR(L2) → VARCHAR(L1 + L2)
 * - CHAR(L1) || CHAR(L2) → CHAR(L1 + L2)
 *
 * Length overflow handling, following the spec's split between fixed- and variable-length results:
 * - CHAR (fixed-length): if L1 + L2 exceeds the maximum length, an exception is raised at compile
 *   time. A CHAR(n) value is exactly n characters, so a clamped bound would describe a different
 *   value; there is nothing to fall back to.
 * - VARCHAR/CLOB (variable-length): the length is min(L1 + L2, [FnUtils.MAXLENGTH]). The declared
 *   length is only an upper bound, so clamping an over-long one loses nothing — no value can exceed
 *   MAXLENGTH characters regardless.
 *
 * Clamping the variable-length cases is also what allows an *unbounded* length to be concatenated at
 * all: [FnUtils.stringFnReturnType] assigns MAXLENGTH when a function's result length is not
 * computable at plan time (REPLACE), and an unbounded `CLOB` defaults to MAXLENGTH. Raising there
 * would reject `replace(...) || x` and `CLOB || CLOB` over a bound that is imprecise, not invalid.
 */
internal object FnConcat : FnOverload() {

    override fun getSignature(): RoutineOverloadSignature {
        return RoutineOverloadSignature(FunctionUtils.hide("concat"), listOf(PType.dynamic(), PType.dynamic()))
    }

    override fun getInstance(args: Array<PType>): Fn? {
        // Every argument must be a text type (or UNKNOWN, handled below); anything else does not match.
        if (args.any { !it.isTextOrUnknown() }) return null
        val lhsType = args[0]
        val rhsType = args[1]
        // If any argument is UNKNOWN (literal NULL), return a resolution-only instance;
        if (args.any { it.code() == PType.UNKNOWN }) {
            return FnUtils.nullResolutionInstance(FunctionUtils.hide("concat"), lhsType, args)
        }
        // If string types are different, use coercibility: STRING > CLOB > VARCHAR > CHAR
        val resultType = if (lhsType.code() != rhsType.code()) {
            FnUtils.getHigherCoercibilityType(lhsType.code(), rhsType.code())
        } else {
            lhsType.code()
        }
        // STRING is unbounded; the bounded text types carry length L1 + L2. CHAR is fixed-length, so an
        // over-long sum is an error; VARCHAR/CLOB are upper bounds, so an over-long sum clamps.
        val returns = when (resultType) {
            PType.STRING -> PType.string()
            PType.CHAR -> PType.character(totalLength(lhsType, rhsType, clamp = false))
            PType.VARCHAR -> PType.varchar(totalLength(lhsType, rhsType, clamp = true))
            PType.CLOB -> PType.clob(totalLength(lhsType, rhsType, clamp = true))
            else -> return null
        }
        return Function.instance(
            name = FunctionUtils.hide("concat"),
            returns = returns,
            parameters = arrayOf(Parameter("lhs", lhsType), Parameter("rhs", rhsType)),
        ) { params ->
            // Either operand may be a byte-backed CLOB, so read each according to its own type.
            val lhs = params[0].textValue(lhsType)
            val rhs = params[1].textValue(rhsType)
            returns.datumOf(lhs + rhs)
        }
    }

    /**
     * The summed operand length L1 + L2 that the result type carries.
     *
     * [clamp] selects between the spec's two cases. A variable-length result (VARCHAR/CLOB) declares
     * only an upper bound, so an over-long sum clamps to [FnUtils.MAXLENGTH] and loses nothing. A
     * fixed-length result (CHAR) is exactly n characters, so no shorter bound describes the same value
     * and an over-long sum raises instead.
     */
    private fun totalLength(lhsType: PType, rhsType: PType, clamp: Boolean): Int {
        val lhsLength = FnUtils.getTypeLength(lhsType)
        val rhsLength = FnUtils.getTypeLength(rhsType)
        return when {
            clamp -> FnUtils.addLengthsClamped(lhsLength, rhsLength)
            else -> FnUtils.addLengths(lhsLength, rhsLength)
        }
    }
}
