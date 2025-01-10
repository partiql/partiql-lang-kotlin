package org.partiql.planner.internal.util

import org.partiql.ast.DatetimeField

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

    // The following are public functions, able to be directly invoked via PartiQL text.
    const val FN_SUBSTRING: String = "substring"
    const val FN_CHAR_LENGTH: String = "char_length"

    // The following are hidden operators, unable to be invoked via PartiQL text.
    val OP_NOT: String = hide("not")
    val OP_AND: String = hide("and")
    val OP_OR: String = hide("or")
    val OP_POSITION: String = hide("position")
    val OP_TRIM: String = hide("trim")
    val OP_TRIM_CHARS: String = hide("trim_chars")
    val OP_TRIM_LEADING: String = hide("trim_leading")
    val OP_TRIM_LEADING_CHARS: String = hide("trim_leading_chars")
    val OP_TRIM_TRAILING: String = hide("trim_trailing")
    val OP_TRIM_TRAILING_CHARS: String = hide("trim_trailing_chars")
    val OP_PLUS: String = hide("plus")
    val OP_MINUS: String = hide("minus")
    val OP_DIVIDE: String = hide("divide")
    val OP_TIMES: String = hide("times")
    val OP_MODULO: String = hide("modulo")
    val OP_BITWISE_AND: String = hide("bitwise_and")
    val OP_CONCAT: String = hide("concat")
    val OP_NEG: String = hide("neg")
    val OP_POS: String = hide("pos")
    val OP_EQ: String = hide("eq")
    val OP_GTE: String = hide("gte")
    val OP_LTE: String = hide("lte")
    val OP_GT: String = hide("gt")
    val OP_LT: String = hide("lt")
    val OP_IS_TRUE: String = hide("is_true")
    val OP_IS_FALSE: String = hide("is_false")
    val OP_IS_UNKNOWN: String = hide("is_unknown")
    val OP_LIKE: String = hide("like")
    val OP_LIKE_ESCAPE: String = hide("like_escape")
    val OP_BETWEEN: String = hide("between")
    val OP_IN_COLLECTION: String = hide("in_collection")
    val OP_IS_NULL: String = hide("is_null")
    val OP_IS_MISSING: String = hide("is_missing")
    val OP_IS_CHAR: String = hide("is_char")
    val OP_IS_VARCHAR: String = hide("is_varchar")
    val OP_IS_STRING: String = hide("is_string")
    val OP_IS_CLOB: String = hide("is_clob")
    val OP_IS_BLOB: String = hide("is_blob")
    val OP_IS_SYMBOL: String = hide("is_symbol")
    val OP_IS_BOOL: String = hide("is_bool")
    val OP_IS_BIT: String = hide("is_bit")
    val OP_IS_BIT_VARYING: String = hide("is_bit_varying")
    val OP_IS_NUMERIC: String = hide("is_numeric")
    val OP_IS_INT: String = hide("is_int")
    val OP_IS_INT8: String = hide("is_int8")
    val OP_IS_INT16: String = hide("is_int16")
    val OP_IS_INT32: String = hide("is_int32")
    val OP_IS_INT64: String = hide("is_int64")
    val OP_IS_FLOAT32: String = hide("is_float32")
    val OP_IS_FLOAT64: String = hide("is_float64")
    val OP_IS_REAL: String = hide("is_real")
    val OP_IS_DECIMAL: String = hide("is_decimal")
    val OP_IS_DATE: String = hide("is_date")
    val OP_IS_TIME: String = hide("is_time")
    val OP_IS_TIMEZ: String = hide("is_timeWithTz")
    val OP_IS_TIMESTAMP: String = hide("is_timestamp")
    val OP_IS_TIMESTAMPZ: String = hide("is_timestampWithTz")
    val OP_IS_INTERVAL: String = hide("is_interval")
    val OP_IS_LIST: String = hide("is_list")
    val OP_IS_BAG: String = hide("is_bag")
    val OP_IS_SEXP: String = hide("is_sexp")
    val OP_IS_STRUCT: String = hide("is_struct")
    val OP_IS_CUSTOM: String = hide("is_custom")

    /**
     * Gets the corresponding operator name for the binary operator ([op]).
     * TODO eventually move hard-coded operator resolution into SPI
     */
    fun getBinaryOp(op: String): String? {
        return when (op) {
            "<" -> OP_LT
            ">" -> OP_GT
            "<=" -> OP_LTE
            ">=" -> OP_GTE
            "=" -> OP_EQ
            "||" -> OP_CONCAT
            "+" -> OP_PLUS
            "-" -> OP_MINUS
            "*" -> OP_TIMES
            "/" -> OP_DIVIDE
            "%" -> OP_MODULO
            "&" -> OP_BITWISE_AND
            else -> null
        }
    }

    /**
     * Gets the corresponding operator name for the binary operator ([op]).
     * TODO eventually move hard-coded operator resolution into SPI
     */
    fun getUnaryOp(op: String): String? {
        return when (op) {
            "-" -> OP_NEG
            "+" -> OP_POS
            else -> null
        }
    }

    /**
     * Returns a hidden function name for the EXTRACT expression, given the [DatetimeField].
     */
    fun opExtract(field: DatetimeField): String {
        return hide("extract_${field.name().lowercase()}")
    }

    /**
     * Hides a function name by prefixing it with [SYSTEM_PREFIX_INTERNAL].
     */
    fun hide(name: String): String {
        return SYSTEM_PREFIX_INTERNAL + name
    }
}
