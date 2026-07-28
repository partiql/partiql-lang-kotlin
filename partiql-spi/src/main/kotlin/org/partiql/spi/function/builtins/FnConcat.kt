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
 * Length overflow handling:
 * - If L1 + L2 exceeds maximum allowed length, an exception is raised at compile time
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
        // STRING is unbounded; the bounded text types carry length L1 + L2, which may overflow.
        val returns = when (resultType) {
            PType.STRING -> PType.string()
            PType.CHAR -> PType.character(totalLength(lhsType, rhsType))
            PType.VARCHAR -> PType.varchar(totalLength(lhsType, rhsType))
            PType.CLOB -> PType.clob(totalLength(lhsType, rhsType))
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

    private fun totalLength(lhsType: PType, rhsType: PType): Int =
        FnUtils.addLengths(FnUtils.getTypeLength(lhsType), FnUtils.getTypeLength(rhsType))
}
