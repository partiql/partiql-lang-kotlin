package org.partiql.planner.internal.utils

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
    const val OP_NOT: String = SYSTEM_PREFIX_INTERNAL + "not"
    const val OP_AND: String = SYSTEM_PREFIX_INTERNAL + "and"
    const val OP_OR: String = SYSTEM_PREFIX_INTERNAL + "or"
    const val OP_POSITION: String = SYSTEM_PREFIX_INTERNAL + "position"
    const val OP_TRIM: String = SYSTEM_PREFIX_INTERNAL + "trim"
    const val OP_TRIM_CHARS: String = SYSTEM_PREFIX_INTERNAL + "trim_chars"
    const val OP_TRIM_LEADING: String = SYSTEM_PREFIX_INTERNAL + "trim_leading"
    const val OP_TRIM_LEADING_CHARS: String = SYSTEM_PREFIX_INTERNAL + "trim_leading_chars"
    const val OP_TRIM_TRAILING: String = SYSTEM_PREFIX_INTERNAL + "trim_trailing"
    const val OP_TRIM_TRAILING_CHARS: String = SYSTEM_PREFIX_INTERNAL + "trim_trailing_chars"
    const val OP_PLUS: String = SYSTEM_PREFIX_INTERNAL + "plus"
    const val OP_MINUS: String = SYSTEM_PREFIX_INTERNAL + "minus"
    const val OP_DIVIDE: String = SYSTEM_PREFIX_INTERNAL + "divide"
    const val OP_TIMES: String = SYSTEM_PREFIX_INTERNAL + "times"
    const val OP_MODULO: String = SYSTEM_PREFIX_INTERNAL + "modulo"
    const val OP_BITWISE_AND: String = SYSTEM_PREFIX_INTERNAL + "bitwise_and"
    const val OP_CONCAT: String = SYSTEM_PREFIX_INTERNAL + "concat"
    const val OP_NEG: String = SYSTEM_PREFIX_INTERNAL + "neg"
    const val OP_POS: String = SYSTEM_PREFIX_INTERNAL + "pos"
    const val OP_EQ: String = SYSTEM_PREFIX_INTERNAL + "eq"
    const val OP_GTE: String = SYSTEM_PREFIX_INTERNAL + "gte"
    const val OP_LTE: String = SYSTEM_PREFIX_INTERNAL + "lte"
    const val OP_GT: String = SYSTEM_PREFIX_INTERNAL + "gt"
    const val OP_LT: String = SYSTEM_PREFIX_INTERNAL + "lt"
    const val OP_IS_TRUE: String = SYSTEM_PREFIX_INTERNAL + "is_true"
    const val OP_IS_FALSE: String = SYSTEM_PREFIX_INTERNAL + "is_false"
    const val OP_IS_UNKNOWN: String = SYSTEM_PREFIX_INTERNAL + "is_unknown"
    const val OP_LIKE: String = SYSTEM_PREFIX_INTERNAL + "like"
    const val OP_LIKE_ESCAPE: String = SYSTEM_PREFIX_INTERNAL + "like_escape"
    const val OP_BETWEEN: String = SYSTEM_PREFIX_INTERNAL + "between"
    const val OP_IN_COLLECTION: String = SYSTEM_PREFIX_INTERNAL + "in_collection"
    const val OP_IS_NULL: String = SYSTEM_PREFIX_INTERNAL + "is_null"
    const val OP_IS_MISSING: String = SYSTEM_PREFIX_INTERNAL + "is_missing"
    const val OP_IS_CHAR: String = SYSTEM_PREFIX_INTERNAL + "is_char"
    const val OP_IS_VARCHAR: String = SYSTEM_PREFIX_INTERNAL + "is_varchar"
    const val OP_IS_STRING: String = SYSTEM_PREFIX_INTERNAL + "is_string"
    const val OP_IS_CLOB: String = SYSTEM_PREFIX_INTERNAL + "is_clob"
    const val OP_IS_BLOB: String = SYSTEM_PREFIX_INTERNAL + "is_blob"
    const val OP_IS_SYMBOL: String = SYSTEM_PREFIX_INTERNAL + "is_symbol"
    const val OP_IS_BOOL: String = SYSTEM_PREFIX_INTERNAL + "is_bool"
    const val OP_IS_BIT: String = SYSTEM_PREFIX_INTERNAL + "is_bit"
    const val OP_IS_BIT_VARYING: String = SYSTEM_PREFIX_INTERNAL + "is_bit_varying"
    const val OP_IS_NUMERIC: String = SYSTEM_PREFIX_INTERNAL + "is_numeric"
    const val OP_IS_INT: String = SYSTEM_PREFIX_INTERNAL + "is_int"
    const val OP_IS_INT8: String = SYSTEM_PREFIX_INTERNAL + "is_int8"
    const val OP_IS_INT16: String = SYSTEM_PREFIX_INTERNAL + "is_int16"
    const val OP_IS_INT32: String = SYSTEM_PREFIX_INTERNAL + "is_int32"
    const val OP_IS_INT64: String = SYSTEM_PREFIX_INTERNAL + "is_int64"
    const val OP_IS_FLOAT32: String = SYSTEM_PREFIX_INTERNAL + "is_float32"
    const val OP_IS_FLOAT64: String = SYSTEM_PREFIX_INTERNAL + "is_float64"
    const val OP_IS_REAL: String = SYSTEM_PREFIX_INTERNAL + "is_real"
    const val OP_IS_DECIMAL: String = SYSTEM_PREFIX_INTERNAL + "is_decimal"
    const val OP_IS_DATE: String = SYSTEM_PREFIX_INTERNAL + "is_date"
    const val OP_IS_TIME: String = SYSTEM_PREFIX_INTERNAL + "is_time"
    const val OP_IS_TIMEZ: String = SYSTEM_PREFIX_INTERNAL + "is_timeWithTz"
    const val OP_IS_TIMESTAMP: String = SYSTEM_PREFIX_INTERNAL + "is_timestamp"
    const val OP_IS_TIMESTAMPZ: String = SYSTEM_PREFIX_INTERNAL + "is_timestampWithTz"
    const val OP_IS_INTERVAL: String = SYSTEM_PREFIX_INTERNAL + "is_interval"
    const val OP_IS_LIST: String = SYSTEM_PREFIX_INTERNAL + "is_list"
    const val OP_IS_BAG: String = SYSTEM_PREFIX_INTERNAL + "is_bag"
    const val OP_IS_SEXP: String = SYSTEM_PREFIX_INTERNAL + "is_sexp"
    const val OP_IS_STRUCT: String = SYSTEM_PREFIX_INTERNAL + "is_struct"
    const val OP_IS_CUSTOM: String = SYSTEM_PREFIX_INTERNAL + "is_custom"

    fun opExtract(field: DatetimeField): String = "${SYSTEM_PREFIX_INTERNAL}extract_${field.name().lowercase()}"

    fun hidden(name: String): String {
        return SYSTEM_PREFIX_INTERNAL + name
    }
}
