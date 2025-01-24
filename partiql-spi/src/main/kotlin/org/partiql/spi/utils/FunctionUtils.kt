package org.partiql.spi.utils

import org.partiql.spi.function.FnOverload
import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.function.builtins.internal.PErrors
import org.partiql.spi.types.PType
import org.partiql.spi.utils.NumberUtils.isNumber
import org.partiql.spi.value.Datum

/**
 * Utility methods for [Function]s and aggregations.
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

    internal fun checkIsBooleanType(funcName: String, value: Datum) {
        if (value.type.code() == PType.VARIANT) {
            return checkIsBooleanType(funcName, value.lower())
        }
        if (value.type.code() != PType.BOOL) {
            throw PErrors.unexpectedTypeException(value.type, listOf(PType.bool()))
        }
    }

    internal fun Datum.booleanValue(): Boolean = when (this.type.code()) {
        PType.VARIANT -> this.lower().booleanValue()
        PType.BOOL -> this.boolean
        else -> error("Cannot convert PartiQLValue ($this) to boolean.")
    }

    /**
     * This is specifically for SUM/AVG
     */
    internal fun nullToTargetType(type: PType): Datum = Datum.nullValue(type)

    internal fun comparisonAccumulator(comparator: Comparator<Datum>): (Datum?, Datum) -> Datum =
        { left, right ->
            when {
                left == null || comparator.compare(left, right) > 0 -> right
                else -> left
            }
        }

    internal fun checkIsNumberType(funcName: String, value: Datum) {
        if (value.type.code() == PType.VARIANT) {
            return checkIsNumberType(funcName, value.lower())
        }
        if (!value.type.isNumber()) {
            throw PErrors.unexpectedTypeException(value.type, listOf(PType.tinyint(), PType.smallint(), PType.integer(), PType.bigint(), PType.decimal(), PType.numeric(), PType.real(), PType.doublePrecision()))
        }
    }
}
