package org.partiql.spi.function

/* ktlint-disable no-wildcard-imports */
import org.partiql.spi.function.builtins.*

/**
 * This is where we will register all SQL builtins; consider raising a "library" interface.
 *
 * TODO some cleanup efforts
 *  - make a libs folder
 *  - mv fn -> functions
 *  - organize builtins by type
 *  - add type families
 */
internal object Builtins {

    fun getFunctions(name: String): Collection<Function> = functions[name] ?: emptyList()

    fun getAggregations(name: String): Collection<Aggregation> = aggregations[name] ?: emptyList()

    private val functions = listOf(
        Fn_ABS__INT8__INT8,
        Fn_ABS__INT16__INT16,
        Fn_ABS__INT32__INT32,
        Fn_ABS__INT64__INT64,
        Fn_ABS__NUMERIC__NUMERIC,
        Fn_ABS__FLOAT32__FLOAT32,
        Fn_ABS__FLOAT64__FLOAT64,
        Fn_ABS__DECIMAL_ARBITRARY__DECIMAL_ARBITRARY,
        Fn_AND__BOOL_BOOL__BOOL,
        Fn_BETWEEN__INT8_INT8_INT8__BOOL,
        Fn_BETWEEN__INT16_INT16_INT16__BOOL,
        Fn_BETWEEN__INT32_INT32_INT32__BOOL,
        Fn_BETWEEN__INT64_INT64_INT64__BOOL,
        Fn_BETWEEN__INT_INT_INT__BOOL,
        Fn_BETWEEN__FLOAT32_FLOAT32_FLOAT32__BOOL,
        Fn_BETWEEN__FLOAT64_FLOAT64_FLOAT64__BOOL,
        Fn_BETWEEN__DECIMAL_ARBITRARY_DECIMAL_ARBITRARY_DECIMAL_ARBITRARY__BOOL,
        Fn_BETWEEN__STRING_STRING_STRING__BOOL,
        Fn_BETWEEN__CLOB_CLOB_CLOB__BOOL,

        Fn_BETWEEN__DATE_DATE_DATE__BOOL,
        Fn_BETWEEN__TIME_TIME_TIME__BOOL,
        Fn_BETWEEN__TIMESTAMP_TIMESTAMP_TIMESTAMP__BOOL,
        Fn_BIT_LENGTH__STRING__INT32,
        Fn_BIT_LENGTH__CLOB__INT32,
        FnBitwiseAnd,
        Fn_CARDINALITY__BAG__INT32,
        Fn_CARDINALITY__LIST__INT32,

        Fn_CARDINALITY__STRUCT__INT32,
        Fn_CHAR_LENGTH__STRING__INT,
        Fn_CHAR_LENGTH__CLOB__INT,

        Fn_COLL_AGG__BAG__ANY.ANY_ALL,
        Fn_COLL_AGG__BAG__ANY.AVG_ALL,
        Fn_COLL_AGG__BAG__ANY.COUNT_ALL,
        Fn_COLL_AGG__BAG__ANY.EVERY_ALL,
        Fn_COLL_AGG__BAG__ANY.MAX_ALL,
        Fn_COLL_AGG__BAG__ANY.MIN_ALL,
        Fn_COLL_AGG__BAG__ANY.SOME_ALL,
        Fn_COLL_AGG__BAG__ANY.SUM_ALL,
        Fn_COLL_AGG__BAG__ANY.ANY_DISTINCT,
        Fn_COLL_AGG__BAG__ANY.AVG_DISTINCT,
        Fn_COLL_AGG__BAG__ANY.COUNT_DISTINCT,
        Fn_COLL_AGG__BAG__ANY.EVERY_DISTINCT,
        Fn_COLL_AGG__BAG__ANY.MAX_DISTINCT,
        Fn_COLL_AGG__BAG__ANY.MIN_DISTINCT,
        Fn_COLL_AGG__BAG__ANY.SOME_DISTINCT,
        Fn_COLL_AGG__BAG__ANY.SUM_DISTINCT,
        Fn_CONCAT__CHAR_CHAR__CHAR,
        Fn_CONCAT__VARCHAR_VARCHAR__VARCHAR,
        Fn_CONCAT__STRING_STRING__STRING,
        Fn_CONCAT__CLOB_CLOB__CLOB,

        Fn_CURRENT_DATE____DATE,
        Fn_CURRENT_USER____STRING,
        Fn_DATE_ADD_DAY__INT32_DATE__DATE,
        Fn_DATE_ADD_DAY__INT32_TIMESTAMP__TIMESTAMP,
        Fn_DATE_ADD_DAY__INT64_DATE__DATE,
        Fn_DATE_ADD_DAY__INT64_TIMESTAMP__TIMESTAMP,
        Fn_DATE_ADD_DAY__INT_DATE__DATE,
        Fn_DATE_ADD_DAY__INT_TIMESTAMP__TIMESTAMP,
        Fn_DATE_ADD_HOUR__INT32_TIME__TIME,
        Fn_DATE_ADD_HOUR__INT32_TIMESTAMP__TIMESTAMP,
        Fn_DATE_ADD_HOUR__INT64_TIME__TIME,
        Fn_DATE_ADD_HOUR__INT64_TIMESTAMP__TIMESTAMP,
        Fn_DATE_ADD_HOUR__INT_TIME__TIME,
        Fn_DATE_ADD_HOUR__INT_TIMESTAMP__TIMESTAMP,
        Fn_DATE_ADD_MINUTE__INT32_TIME__TIME,
        Fn_DATE_ADD_MINUTE__INT32_TIMESTAMP__TIMESTAMP,
        Fn_DATE_ADD_MINUTE__INT64_TIME__TIME,
        Fn_DATE_ADD_MINUTE__INT64_TIMESTAMP__TIMESTAMP,
        Fn_DATE_ADD_MINUTE__INT_TIME__TIME,
        Fn_DATE_ADD_MINUTE__INT_TIMESTAMP__TIMESTAMP,
        Fn_DATE_ADD_MONTH__INT32_DATE__DATE,
        Fn_DATE_ADD_MONTH__INT32_TIMESTAMP__TIMESTAMP,
        Fn_DATE_ADD_MONTH__INT64_DATE__DATE,
        Fn_DATE_ADD_MONTH__INT64_TIMESTAMP__TIMESTAMP,
        Fn_DATE_ADD_MONTH__INT_DATE__DATE,
        Fn_DATE_ADD_MONTH__INT_TIMESTAMP__TIMESTAMP,
        Fn_DATE_ADD_SECOND__INT32_TIME__TIME,
        Fn_DATE_ADD_SECOND__INT32_TIMESTAMP__TIMESTAMP,
        Fn_DATE_ADD_SECOND__INT64_TIME__TIME,
        Fn_DATE_ADD_SECOND__INT64_TIMESTAMP__TIMESTAMP,
        Fn_DATE_ADD_SECOND__INT_TIME__TIME,
        Fn_DATE_ADD_SECOND__INT_TIMESTAMP__TIMESTAMP,
        Fn_DATE_ADD_YEAR__INT32_DATE__DATE,
        Fn_DATE_ADD_YEAR__INT32_TIMESTAMP__TIMESTAMP,
        Fn_DATE_ADD_YEAR__INT64_DATE__DATE,
        Fn_DATE_ADD_YEAR__INT64_TIMESTAMP__TIMESTAMP,
        Fn_DATE_ADD_YEAR__INT_DATE__DATE,
        Fn_DATE_ADD_YEAR__INT_TIMESTAMP__TIMESTAMP,
        Fn_DATE_DIFF_DAY__DATE_DATE__INT64,
        Fn_DATE_DIFF_DAY__TIMESTAMP_TIMESTAMP__INT64,
        Fn_DATE_DIFF_HOUR__TIME_TIME__INT64,
        Fn_DATE_DIFF_HOUR__TIMESTAMP_TIMESTAMP__INT64,
        Fn_DATE_DIFF_MINUTE__TIME_TIME__INT64,
        Fn_DATE_DIFF_MINUTE__TIMESTAMP_TIMESTAMP__INT64,
        Fn_DATE_DIFF_MONTH__DATE_DATE__INT64,
        Fn_DATE_DIFF_MONTH__TIMESTAMP_TIMESTAMP__INT64,
        Fn_DATE_DIFF_SECOND__TIME_TIME__INT64,
        Fn_DATE_DIFF_SECOND__TIMESTAMP_TIMESTAMP__INT64,
        Fn_DATE_DIFF_YEAR__DATE_DATE__INT64,
        Fn_DATE_DIFF_YEAR__TIMESTAMP_TIMESTAMP__INT64,
        FnDivide,
        FnEq,
        Fn_EXTRACT_DAY__DATE__INT32,
        Fn_EXTRACT_DAY__TIMESTAMP__INT32,
        Fn_EXTRACT_HOUR__TIME__INT32,
        Fn_EXTRACT_HOUR__TIMESTAMP__INT32,
        Fn_EXTRACT_MINUTE__TIME__INT32,
        Fn_EXTRACT_MINUTE__TIMESTAMP__INT32,
        Fn_EXTRACT_MONTH__DATE__INT32,
        Fn_EXTRACT_MONTH__TIMESTAMP__INT32,
        Fn_EXTRACT_SECOND__TIME__DECIMAL_ARBITRARY,
        Fn_EXTRACT_SECOND__TIMESTAMP__DECIMAL_ARBITRARY,
        Fn_EXTRACT_TIMEZONE_HOUR__TIME__INT32,
        Fn_EXTRACT_TIMEZONE_HOUR__TIMESTAMP__INT32,
        Fn_EXTRACT_TIMEZONE_MINUTE__TIME__INT32,
        Fn_EXTRACT_TIMEZONE_MINUTE__TIMESTAMP__INT32,
        Fn_EXTRACT_YEAR__DATE__INT32,
        Fn_EXTRACT_YEAR__TIMESTAMP__INT32,
        FnGt,
        FnGte,
        FnInCollection,

        Fn_IS_TRUE__ANY__BOOL,
        Fn_IS_FALSE__ANY__BOOL,
        Fn_IS_UNKNOWN__ANY__BOOL,

        Fn_IS_ANY__ANY__BOOL,
        Fn_IS_BAG__ANY__BOOL,
        Fn_IS_BINARY__ANY__BOOL,
        Fn_IS_BLOB__ANY__BOOL,
        Fn_IS_BOOL__ANY__BOOL,
        Fn_IS_BYTE__ANY__BOOL,
        Fn_IS_CHAR__INT32_ANY__BOOL,
        Fn_IS_CHAR__ANY__BOOL,
        Fn_IS_CLOB__ANY__BOOL,
        Fn_IS_DATE__ANY__BOOL,
        Fn_IS_DECIMAL__INT32_INT32_ANY__BOOL,
        Fn_IS_DECIMAL__ANY__BOOL,
        Fn_IS_FLOAT32__ANY__BOOL,
        Fn_IS_FLOAT64__ANY__BOOL,
        Fn_IS_INT__ANY__BOOL,
        Fn_IS_INT16__ANY__BOOL,
        Fn_IS_INT32__ANY__BOOL,
        Fn_IS_INT64__ANY__BOOL,
        Fn_IS_INT8__ANY__BOOL,
        Fn_IS_INTERVAL__ANY__BOOL,
        Fn_IS_LIST__ANY__BOOL,
        Fn_IS_MISSING__ANY__BOOL,
        Fn_IS_NULL__ANY__BOOL,

        Fn_IS_STRING__INT32_ANY__BOOL,
        Fn_IS_STRING__ANY__BOOL,
        Fn_IS_STRUCT__ANY__BOOL,

        Fn_IS_TIME__BOOL_INT32_ANY__BOOL,
        Fn_IS_TIME__ANY__BOOL,
        Fn_IS_TIMESTAMP__BOOL_INT32_ANY__BOOL,
        Fn_IS_TIMESTAMP__ANY__BOOL,
        Fn_LIKE__STRING_STRING__BOOL,
        Fn_LIKE__CLOB_CLOB__BOOL,

        Fn_LIKE_ESCAPE__STRING_STRING_STRING__BOOL,
        Fn_LIKE_ESCAPE__CLOB_CLOB_CLOB__BOOL,

        Fn_LOWER__STRING__STRING,
        Fn_LOWER__CLOB__CLOB,

        FnLt,
        FnLte,
        FnMinus,
        FnModulo,
        Fn_NEG__INT8__INT8,
        Fn_NEG__INT16__INT16,
        Fn_NEG__INT32__INT32,
        Fn_NEG__INT64__INT64,
        Fn_NEG__INT__INT,
        Fn_NEG__FLOAT32__FLOAT32,
        Fn_NEG__FLOAT64__FLOAT64,
        Fn_NEG__DECIMAL_ARBITRARY__DECIMAL_ARBITRARY,
        Fn_NOT__BOOL__BOOL,
        Fn_OR__BOOL_BOOL__BOOL,
        Fn_OCTET_LENGTH__STRING__INT32,
        Fn_OCTET_LENGTH__CLOB__INT32,
        FnPlus,
        Fn_POS__INT8__INT8,
        Fn_POS__INT16__INT16,
        Fn_POS__INT32__INT32,
        Fn_POS__INT64__INT64,
        Fn_POS__INT__INT,
        Fn_POS__FLOAT32__FLOAT32,
        Fn_POS__FLOAT64__FLOAT64,
        Fn_POS__DECIMAL_ARBITRARY__DECIMAL_ARBITRARY,
        Fn_POSITION__STRING_STRING__INT64,
        Fn_POSITION__CLOB_CLOB__INT64,

        Fn_SUBSTRING__STRING_INT32__STRING,
        Fn_SUBSTRING__STRING_INT32_INT32__STRING,
        Fn_SUBSTRING__CLOB_INT64__CLOB,
        Fn_SUBSTRING__CLOB_INT64_INT64__CLOB,

        FnTimes,
        Fn_TRIM__STRING__STRING,
        Fn_TRIM__CLOB__CLOB,

        Fn_TRIM_CHARS__STRING_STRING__STRING,
        Fn_TRIM_CHARS__CLOB_CLOB__CLOB,

        Fn_TRIM_LEADING__STRING__STRING,
        Fn_TRIM_LEADING__CLOB__CLOB,

        Fn_TRIM_LEADING_CHARS__STRING_STRING__STRING,
        Fn_TRIM_LEADING_CHARS__CLOB_CLOB__CLOB,

        Fn_TRIM_TRAILING__STRING__STRING,
        Fn_TRIM_TRAILING__CLOB__CLOB,

        Fn_TRIM_TRAILING_CHARS__STRING_STRING__STRING,
        Fn_TRIM_TRAILING_CHARS__CLOB_CLOB__CLOB,

        Fn_UPPER__STRING__STRING,
        Fn_UPPER__CLOB__CLOB,

        Fn_UTCNOW____TIMESTAMP,
        //
        // NON SQL FUNCTIONS
        //
        Fn_EXISTS__BAG__BOOL,
        Fn_EXISTS__LIST__BOOL,

        Fn_EXISTS__STRUCT__BOOL,
        Fn_SIZE__BAG__INT32,
        Fn_SIZE__LIST__INT32,

        Fn_SIZE__STRUCT__INT32
    ).groupBy { it.getName() }

    @JvmStatic
    private val aggregations: Map<String, List<Aggregation>> = listOf(
        Agg_ANY__BOOL__BOOL,
        Agg_AVG__INT8__INT8,
        Agg_AVG__INT16__INT16,
        Agg_AVG__INT32__INT32,
        Agg_AVG__INT64__INT64,
        Agg_AVG__NUMERIC__NUMERIC,
        Agg_AVG__DECIMAL_ARBITRARY__DECIMAL_ARBITRARY,
        Agg_AVG__FLOAT32__FLOAT32,
        Agg_AVG__FLOAT64__FLOAT64,
        Agg_AVG__ANY__ANY,
        Agg_COUNT__ANY__INT64,
        Agg_EVERY__BOOL__BOOL,
        Agg_EVERY__ANY__BOOL,
        Agg_MAX__INT8__INT8,
        Agg_MAX__INT16__INT16,
        Agg_MAX__INT32__INT32,
        Agg_MAX__INT64__INT64,
        Agg_MAX__NUMERIC__NUMERIC,
        Agg_MAX__DECIMAL_ARBITRARY__DECIMAL_ARBITRARY,
        Agg_MAX__FLOAT32__FLOAT32,
        Agg_MAX__FLOAT64__FLOAT64,
        Agg_MAX__ANY__ANY,
        Agg_MIN__INT8__INT8,
        Agg_MIN__INT16__INT16,
        Agg_MIN__INT32__INT32,
        Agg_MIN__INT64__INT64,
        Agg_MIN__INT__INT,
        Agg_MIN__DECIMAL_ARBITRARY__DECIMAL_ARBITRARY,
        Agg_MIN__FLOAT32__FLOAT32,
        Agg_MIN__FLOAT64__FLOAT64,
        Agg_MIN__ANY__ANY,
        Agg_SOME__BOOL__BOOL,
        Agg_SOME__ANY__BOOL,
        Agg_SUM__INT8__INT8,
        Agg_SUM__INT16__INT16,
        Agg_SUM__INT32__INT32,
        Agg_SUM__INT64__INT64,
        Agg_SUM__NUMERIC__NUMERIC,
        Agg_SUM__DECIMAL_ARBITRARY__DECIMAL_ARBITRARY,
        Agg_SUM__FLOAT32__FLOAT32,
        Agg_SUM__FLOAT64__FLOAT64,
        Agg_SUM__ANY__ANY,
        Agg_GROUP_AS__ANY__ANY
    ).groupBy { it.getName() }
}
