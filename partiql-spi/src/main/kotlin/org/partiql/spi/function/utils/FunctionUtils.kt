package org.partiql.spi.function.utils

import org.partiql.spi.function.FnOverload
import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum

/**
 * Utility methods for [Function]s.
 */
internal object FunctionUtils {

    /**
     * The internal system prefix is '\uFDEF', one of unicode's 'internal-use' non-characters. This allows us to "hide"
     * certain functions from being directly invocable via PartiQL text.
     * See:
     * - http://www.unicode.org/faq/private_use.html#nonchar1
     * - http://www.unicode.org/versions/Unicode5.2.0/ch16.pdf#G19635
     * - http://www.unicode.org/versions/corrigendum9.html
     */
    private const val SYSTEM_PREFIX_INTERNAL: String = "\uFDEF"

    /**
     * Returns the [name] prefixed by [SYSTEM_PREFIX_INTERNAL]. This makes it difficult for PartiQL query authors to
     * accidentally invoke the "hidden" built-in functions such as: +, -, trim_leading, and more.
     * Note: Some built-in functions are intentionally not hidden (and should not use this method). These are
     * defined by SQL:1999, such as ABS, LOWER, CHAR_LENGTH, the aggregation functions, and more scalar functions. For
     * the full list of scalar functions, see Section 20.70 of SQL:1999.
     */
    fun hide(name: String): String {
        return "$SYSTEM_PREFIX_INTERNAL$name"
    }

    /**
     * Returns an implementation of a [Function] that has the [name] hidden via the [SYSTEM_PREFIX_INTERNAL].
     */
    fun hidden(
        name: String,
        parameters: Array<Parameter>,
        returns: PType,
        isNullCall: Boolean = true,
        isMissingCall: Boolean = true,
        invoke: (Array<Datum>) -> Datum,
    ): FnOverload {
        val hiddenName = hide(name)
        return FnOverload.Builder(hiddenName)
            .addParameters(*parameters.map { it.getType() }.toTypedArray())
            .returns(returns)
            .isNullCall(isNullCall)
            .isMissingCall(isMissingCall)
            .body(invoke)
            .build()
    }
}
