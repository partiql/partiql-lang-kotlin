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

/**
 * SQL UPPER function implementation.
 *
 * Implements the SQL <fold> function as defined in SQL2023 section 6.33 <string value function>.
 *
 * According to SQL specification:
 * - The declared type of the result is the declared type of the <character value expression>
 * - For CHAR, VARCHAR, and CLOB types, the length parameter is preserved from the input type
 *
 * PartiQL extensions:
 * - STRING type (PartiQL-specific unlimited length string) preserves its type
 *
 * Type preservation behavior:
 * - CHAR(n) → CHAR(n)
 * - VARCHAR(n) → VARCHAR(n)
 * - CLOB(n) → CLOB(n)
 * - STRING → STRING (PartiQL extension)
 *
 * KNOWN ISSUE (https://github.com/partiql/partiql-lang-kotlin/issues/1952): preserving `n` silently
 *   truncates, because case mapping is not length-preserving. Some codepoints uppercase to more than
 *   one character — 'ß' → "SS", 'ﬁ' → "FI" — so an n-character input can fold to more than n. The
 *   folded value is then boxed at the declared length and truncated: `upper(CAST('aßb' AS VARCHAR(3)))`
 *   yields 'ASS' instead of 'ASSB'. [FnLower] has the same defect. See the issue for scope and fix
 *   options; both should be fixed together.
 */
internal object FnUpper : FnOverload() {

    override fun getSignature(): RoutineOverloadSignature {
        return RoutineOverloadSignature("upper", listOf(PType.dynamic()))
    }

    override fun getInstance(args: Array<PType>): Fn? {
        // The argument must be a text type (or UNKNOWN, handled below); anything else does not match.
        val inputType = args[0]
        if (!inputType.isTextOrUnknown()) return null
        // An UNKNOWN argument (literal NULL) gets a resolution-only instance; the framework's isNullCall handles propagation.
        if (inputType.code() == PType.UNKNOWN) {
            return FnUtils.nullResolutionInstance("upper", PType.string(), args)
        }
        // <fold> preserves the input type and length exactly, so the result type is the input type.
        return Function.instance(
            name = "upper",
            returns = inputType,
            parameters = arrayOf(Parameter("value", inputType)),
        ) { params ->
            val value = params[0].textValue(inputType)
            val result = value.uppercase()
            inputType.datumOf(result)
        }
    }
}
