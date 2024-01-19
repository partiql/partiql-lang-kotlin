package org.partiql.spi.connector.sql

// I could not find a good way to disable the wildcard restriction under ktlint 0.42.1
// on their doc, it seems like we can suppress the wildcard import from 0.47.0 onward
// but upgrade ktlint will change a large number of files ...
// leaving those as is for now
import org.partiql.spi.connector.sql.internal.builtins.agg.Agg_ANY__BOOL__BOOL
import org.partiql.spi.connector.sql.internal.builtins.agg.Agg_AVG__DECIMAL_ARBITRARY__DECIMAL_ARBITRARY
import org.partiql.spi.connector.sql.internal.builtins.agg.Agg_AVG__FLOAT32__FLOAT32
import org.partiql.spi.connector.sql.internal.builtins.agg.Agg_AVG__FLOAT64__FLOAT64
import org.partiql.spi.connector.sql.internal.builtins.agg.Agg_AVG__INT16__INT16
import org.partiql.spi.connector.sql.internal.builtins.agg.Agg_AVG__INT32__INT32
import org.partiql.spi.connector.sql.internal.builtins.agg.Agg_AVG__INT64__INT64
import org.partiql.spi.connector.sql.internal.builtins.agg.Agg_AVG__INT8__INT8
import org.partiql.spi.connector.sql.internal.builtins.agg.Agg_AVG__INT__INT
import org.partiql.spi.connector.sql.internal.builtins.agg.Agg_COUNT_STAR____INT32
import org.partiql.spi.connector.sql.internal.builtins.agg.Agg_COUNT__ANY__INT32
import org.partiql.spi.connector.sql.internal.builtins.agg.Agg_EVERY__BOOL__BOOL
import org.partiql.spi.connector.sql.internal.builtins.agg.Agg_MAX__DECIMAL_ARBITRARY__DECIMAL_ARBITRARY
import org.partiql.spi.connector.sql.internal.builtins.agg.Agg_MAX__FLOAT32__FLOAT32
import org.partiql.spi.connector.sql.internal.builtins.agg.Agg_MAX__FLOAT64__FLOAT64
import org.partiql.spi.connector.sql.internal.builtins.agg.Agg_MAX__INT16__INT16
import org.partiql.spi.connector.sql.internal.builtins.agg.Agg_MAX__INT32__INT32
import org.partiql.spi.connector.sql.internal.builtins.agg.Agg_MAX__INT64__INT64
import org.partiql.spi.connector.sql.internal.builtins.agg.Agg_MAX__INT8__INT8
import org.partiql.spi.connector.sql.internal.builtins.agg.Agg_MAX__INT__INT
import org.partiql.spi.connector.sql.internal.builtins.agg.Agg_MIN__DECIMAL_ARBITRARY__DECIMAL_ARBITRARY
import org.partiql.spi.connector.sql.internal.builtins.agg.Agg_MIN__FLOAT32__FLOAT32
import org.partiql.spi.connector.sql.internal.builtins.agg.Agg_MIN__FLOAT64__FLOAT64
import org.partiql.spi.connector.sql.internal.builtins.agg.Agg_MIN__INT16__INT16
import org.partiql.spi.connector.sql.internal.builtins.agg.Agg_MIN__INT32__INT32
import org.partiql.spi.connector.sql.internal.builtins.agg.Agg_MIN__INT64__INT64
import org.partiql.spi.connector.sql.internal.builtins.agg.Agg_MIN__INT8__INT8
import org.partiql.spi.connector.sql.internal.builtins.agg.Agg_MIN__INT__INT
import org.partiql.spi.connector.sql.internal.builtins.agg.Agg_SOME__BOOL__BOOL
import org.partiql.spi.connector.sql.internal.builtins.agg.Agg_SUM__DECIMAL_ARBITRARY__DECIMAL_ARBITRARY
import org.partiql.spi.connector.sql.internal.builtins.agg.Agg_SUM__FLOAT32__FLOAT32
import org.partiql.spi.connector.sql.internal.builtins.agg.Agg_SUM__FLOAT64__FLOAT64
import org.partiql.spi.connector.sql.internal.builtins.agg.Agg_SUM__INT16__INT16
import org.partiql.spi.connector.sql.internal.builtins.agg.Agg_SUM__INT32__INT32
import org.partiql.spi.connector.sql.internal.builtins.agg.Agg_SUM__INT64__INT64
import org.partiql.spi.connector.sql.internal.builtins.agg.Agg_SUM__INT8__INT8
import org.partiql.spi.connector.sql.internal.builtins.agg.Agg_SUM__INT__INT
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_AND__BOOL_BOOL__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_AND__BOOL_MISSING__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_AND__MISSING_BOOL__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_AND__MISSING_MISSING__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_BETWEEN__CLOB_CLOB_CLOB__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_BETWEEN__DATE_DATE_DATE__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_BETWEEN__DECIMAL_ARBITRARY_DECIMAL_ARBITRARY_DECIMAL_ARBITRARY__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_BETWEEN__FLOAT32_FLOAT32_FLOAT32__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_BETWEEN__FLOAT64_FLOAT64_FLOAT64__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_BETWEEN__INT16_INT16_INT16__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_BETWEEN__INT32_INT32_INT32__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_BETWEEN__INT64_INT64_INT64__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_BETWEEN__INT8_INT8_INT8__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_BETWEEN__INT_INT_INT__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_BETWEEN__STRING_STRING_STRING__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_BETWEEN__SYMBOL_SYMBOL_SYMBOL__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_BETWEEN__TIMESTAMP_TIMESTAMP_TIMESTAMP__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_BETWEEN__TIME_TIME_TIME__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_BITWISE_AND__INT16_INT16__INT16
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_BITWISE_AND__INT32_INT32__INT32
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_BITWISE_AND__INT64_INT64__INT64
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_BITWISE_AND__INT8_INT8__INT8
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_BITWISE_AND__INT_INT__INT
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_CONCAT__CLOB_CLOB__CLOB
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_CONCAT__STRING_STRING__STRING
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_CONCAT__SYMBOL_SYMBOL__SYMBOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_CURRENT_DATE____DATE
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_CURRENT_USER____STRING
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_DATE_ADD_DAY__INT32_DATE__DATE
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_DATE_ADD_DAY__INT32_TIMESTAMP__TIMESTAMP
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_DATE_ADD_DAY__INT32_TIME__TIME
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_DATE_ADD_DAY__INT64_DATE__DATE
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_DATE_ADD_DAY__INT64_TIMESTAMP__TIMESTAMP
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_DATE_ADD_DAY__INT64_TIME__TIME
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_DATE_ADD_DAY__INT_DATE__DATE
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_DATE_ADD_DAY__INT_TIMESTAMP__TIMESTAMP
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_DATE_ADD_DAY__INT_TIME__TIME
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_DATE_ADD_HOUR__INT32_DATE__DATE
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_DATE_ADD_HOUR__INT32_TIMESTAMP__TIMESTAMP
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_DATE_ADD_HOUR__INT32_TIME__TIME
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_DATE_ADD_HOUR__INT64_DATE__DATE
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_DATE_ADD_HOUR__INT64_TIMESTAMP__TIMESTAMP
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_DATE_ADD_HOUR__INT64_TIME__TIME
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_DATE_ADD_HOUR__INT_DATE__DATE
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_DATE_ADD_HOUR__INT_TIMESTAMP__TIMESTAMP
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_DATE_ADD_HOUR__INT_TIME__TIME
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_DATE_ADD_MINUTE__INT32_DATE__DATE
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_DATE_ADD_MINUTE__INT32_TIMESTAMP__TIMESTAMP
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_DATE_ADD_MINUTE__INT32_TIME__TIME
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_DATE_ADD_MINUTE__INT64_DATE__DATE
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_DATE_ADD_MINUTE__INT64_TIMESTAMP__TIMESTAMP
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_DATE_ADD_MINUTE__INT64_TIME__TIME
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_DATE_ADD_MINUTE__INT_DATE__DATE
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_DATE_ADD_MINUTE__INT_TIMESTAMP__TIMESTAMP
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_DATE_ADD_MINUTE__INT_TIME__TIME
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_DATE_ADD_MONTH__INT32_DATE__DATE
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_DATE_ADD_MONTH__INT32_TIMESTAMP__TIMESTAMP
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_DATE_ADD_MONTH__INT32_TIME__TIME
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_DATE_ADD_MONTH__INT64_DATE__DATE
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_DATE_ADD_MONTH__INT64_TIMESTAMP__TIMESTAMP
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_DATE_ADD_MONTH__INT64_TIME__TIME
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_DATE_ADD_MONTH__INT_DATE__DATE
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_DATE_ADD_MONTH__INT_TIMESTAMP__TIMESTAMP
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_DATE_ADD_MONTH__INT_TIME__TIME
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_DATE_ADD_SECOND__INT32_DATE__DATE
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_DATE_ADD_SECOND__INT32_TIMESTAMP__TIMESTAMP
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_DATE_ADD_SECOND__INT32_TIME__TIME
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_DATE_ADD_SECOND__INT64_DATE__DATE
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_DATE_ADD_SECOND__INT64_TIMESTAMP__TIMESTAMP
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_DATE_ADD_SECOND__INT64_TIME__TIME
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_DATE_ADD_SECOND__INT_DATE__DATE
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_DATE_ADD_SECOND__INT_TIMESTAMP__TIMESTAMP
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_DATE_ADD_SECOND__INT_TIME__TIME
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_DATE_ADD_YEAR__INT32_DATE__DATE
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_DATE_ADD_YEAR__INT32_TIMESTAMP__TIMESTAMP
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_DATE_ADD_YEAR__INT32_TIME__TIME
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_DATE_ADD_YEAR__INT64_DATE__DATE
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_DATE_ADD_YEAR__INT64_TIMESTAMP__TIMESTAMP
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_DATE_ADD_YEAR__INT64_TIME__TIME
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_DATE_ADD_YEAR__INT_DATE__DATE
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_DATE_ADD_YEAR__INT_TIMESTAMP__TIMESTAMP
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_DATE_ADD_YEAR__INT_TIME__TIME
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_DATE_DIFF_DAY__DATE_DATE__INT64
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_DATE_DIFF_DAY__TIMESTAMP_TIMESTAMP__INT64
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_DATE_DIFF_DAY__TIME_TIME__INT64
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_DATE_DIFF_HOUR__DATE_DATE__INT64
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_DATE_DIFF_HOUR__TIMESTAMP_TIMESTAMP__INT64
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_DATE_DIFF_HOUR__TIME_TIME__INT64
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_DATE_DIFF_MINUTE__DATE_DATE__INT64
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_DATE_DIFF_MINUTE__TIMESTAMP_TIMESTAMP__INT64
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_DATE_DIFF_MINUTE__TIME_TIME__INT64
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_DATE_DIFF_MONTH__DATE_DATE__INT64
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_DATE_DIFF_MONTH__TIMESTAMP_TIMESTAMP__INT64
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_DATE_DIFF_MONTH__TIME_TIME__INT64
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_DATE_DIFF_SECOND__DATE_DATE__INT64
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_DATE_DIFF_SECOND__TIMESTAMP_TIMESTAMP__INT64
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_DATE_DIFF_SECOND__TIME_TIME__INT64
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_DATE_DIFF_YEAR__DATE_DATE__INT64
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_DATE_DIFF_YEAR__TIMESTAMP_TIMESTAMP__INT64
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_DATE_DIFF_YEAR__TIME_TIME__INT64
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_DIVIDE__DECIMAL_ARBITRARY_DECIMAL_ARBITRARY__DECIMAL_ARBITRARY
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_DIVIDE__FLOAT32_FLOAT32__FLOAT32
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_DIVIDE__FLOAT64_FLOAT64__FLOAT64
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_DIVIDE__INT16_INT16__INT16
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_DIVIDE__INT32_INT32__INT32
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_DIVIDE__INT64_INT64__INT64
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_DIVIDE__INT8_INT8__INT8
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_DIVIDE__INT_INT__INT
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_EQ__ANY_ANY__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_EQ__BAG_BAG__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_EQ__BINARY_BINARY__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_EQ__BLOB_BLOB__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_EQ__BOOL_BOOL__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_EQ__BYTE_BYTE__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_EQ__CHAR_CHAR__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_EQ__CLOB_CLOB__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_EQ__DATE_DATE__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_EQ__DECIMAL_ARBITRARY_DECIMAL_ARBITRARY__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_EQ__DECIMAL_DECIMAL__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_EQ__FLOAT32_FLOAT32__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_EQ__FLOAT64_FLOAT64__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_EQ__INT16_INT16__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_EQ__INT32_INT32__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_EQ__INT64_INT64__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_EQ__INT8_INT8__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_EQ__INTERVAL_INTERVAL__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_EQ__INT_INT__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_EQ__LIST_LIST__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_EQ__MISSING_MISSING__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_EQ__NULL_NULL__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_EQ__SEXP_SEXP__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_EQ__STRING_STRING__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_EQ__STRUCT_STRUCT__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_EQ__SYMBOL_SYMBOL__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_EQ__TIMESTAMP_TIMESTAMP__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_EQ__TIME_TIME__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_GTE__BOOL_BOOL__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_GTE__CLOB_CLOB__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_GTE__DATE_DATE__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_GTE__DECIMAL_ARBITRARY_DECIMAL_ARBITRARY__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_GTE__FLOAT32_FLOAT32__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_GTE__FLOAT64_FLOAT64__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_GTE__INT16_INT16__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_GTE__INT32_INT32__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_GTE__INT64_INT64__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_GTE__INT8_INT8__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_GTE__INT_INT__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_GTE__STRING_STRING__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_GTE__SYMBOL_SYMBOL__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_GTE__TIMESTAMP_TIMESTAMP__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_GTE__TIME_TIME__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_GT__BOOL_BOOL__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_GT__CLOB_CLOB__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_GT__DATE_DATE__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_GT__DECIMAL_ARBITRARY_DECIMAL_ARBITRARY__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_GT__FLOAT32_FLOAT32__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_GT__FLOAT64_FLOAT64__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_GT__INT16_INT16__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_GT__INT32_INT32__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_GT__INT64_INT64__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_GT__INT8_INT8__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_GT__INT_INT__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_GT__STRING_STRING__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_GT__SYMBOL_SYMBOL__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_GT__TIMESTAMP_TIMESTAMP__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_GT__TIME_TIME__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__ANY_BAG__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__ANY_LIST__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__ANY_SEXP__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__BAG_BAG__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__BAG_LIST__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__BAG_SEXP__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__BINARY_BAG__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__BINARY_LIST__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__BINARY_SEXP__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__BLOB_BAG__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__BLOB_LIST__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__BLOB_SEXP__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__BOOL_BAG__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__BOOL_LIST__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__BOOL_SEXP__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__BYTE_BAG__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__BYTE_LIST__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__BYTE_SEXP__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__CHAR_BAG__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__CHAR_LIST__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__CHAR_SEXP__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__CLOB_BAG__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__CLOB_LIST__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__CLOB_SEXP__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__DATE_BAG__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__DATE_LIST__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__DATE_SEXP__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__DECIMAL_ARBITRARY_BAG__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__DECIMAL_ARBITRARY_LIST__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__DECIMAL_ARBITRARY_SEXP__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__DECIMAL_BAG__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__DECIMAL_LIST__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__DECIMAL_SEXP__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__FLOAT32_BAG__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__FLOAT32_LIST__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__FLOAT32_SEXP__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__FLOAT64_BAG__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__FLOAT64_LIST__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__FLOAT64_SEXP__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__INT16_BAG__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__INT16_LIST__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__INT16_SEXP__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__INT32_BAG__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__INT32_LIST__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__INT32_SEXP__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__INT64_BAG__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__INT64_LIST__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__INT64_SEXP__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__INT8_BAG__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__INT8_LIST__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__INT8_SEXP__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__INTERVAL_BAG__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__INTERVAL_LIST__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__INTERVAL_SEXP__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__INT_BAG__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__INT_LIST__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__INT_SEXP__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__LIST_BAG__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__LIST_LIST__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__LIST_SEXP__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__MISSING_BAG__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__MISSING_LIST__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__MISSING_SEXP__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__NULL_BAG__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__NULL_LIST__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__NULL_SEXP__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__SEXP_BAG__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__SEXP_LIST__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__SEXP_SEXP__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__STRING_BAG__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__STRING_LIST__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__STRING_SEXP__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__STRUCT_BAG__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__STRUCT_LIST__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__STRUCT_SEXP__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__SYMBOL_BAG__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__SYMBOL_LIST__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__SYMBOL_SEXP__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__TIMESTAMP_BAG__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__TIMESTAMP_LIST__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__TIMESTAMP_SEXP__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__TIME_BAG__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__TIME_LIST__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IN_COLLECTION__TIME_SEXP__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IS_ANY__ANY__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IS_BAG__ANY__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IS_BINARY__ANY__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IS_BLOB__ANY__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IS_BOOL__ANY__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IS_BYTE__ANY__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IS_CHAR__ANY__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IS_CHAR__INT32_ANY__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IS_CLOB__ANY__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IS_DATE__ANY__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IS_DECIMAL_ARBITRARY__ANY__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IS_DECIMAL__ANY__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IS_DECIMAL__INT32_INT32_ANY__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IS_FLOAT32__ANY__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IS_FLOAT64__ANY__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IS_INT16__ANY__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IS_INT32__ANY__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IS_INT64__ANY__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IS_INT8__ANY__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IS_INTERVAL__ANY__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IS_INT__ANY__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IS_LIST__ANY__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IS_MISSING__ANY__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IS_NULL__ANY__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IS_SEXP__ANY__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IS_STRING__ANY__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IS_STRING__INT32_ANY__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IS_STRUCT__ANY__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IS_SYMBOL__ANY__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IS_TIMESTAMP__ANY__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IS_TIMESTAMP__BOOL_INT32_ANY__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IS_TIME__ANY__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_IS_TIME__BOOL_INT32_ANY__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_LIKE_ESCAPE__CLOB_CLOB_CLOB__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_LIKE_ESCAPE__STRING_STRING_STRING__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_LIKE_ESCAPE__SYMBOL_SYMBOL_SYMBOL__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_LIKE__CLOB_CLOB__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_LIKE__STRING_STRING__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_LIKE__SYMBOL_SYMBOL__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_LOWER__CLOB__CLOB
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_LOWER__STRING__STRING
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_LOWER__SYMBOL__SYMBOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_LTE__BOOL_BOOL__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_LTE__CLOB_CLOB__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_LTE__DATE_DATE__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_LTE__DECIMAL_ARBITRARY_DECIMAL_ARBITRARY__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_LTE__FLOAT32_FLOAT32__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_LTE__FLOAT64_FLOAT64__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_LTE__INT16_INT16__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_LTE__INT32_INT32__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_LTE__INT64_INT64__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_LTE__INT8_INT8__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_LTE__INT_INT__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_LTE__STRING_STRING__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_LTE__SYMBOL_SYMBOL__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_LTE__TIMESTAMP_TIMESTAMP__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_LTE__TIME_TIME__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_LT__BOOL_BOOL__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_LT__CLOB_CLOB__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_LT__DATE_DATE__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_LT__DECIMAL_ARBITRARY_DECIMAL_ARBITRARY__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_LT__FLOAT32_FLOAT32__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_LT__FLOAT64_FLOAT64__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_LT__INT16_INT16__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_LT__INT32_INT32__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_LT__INT64_INT64__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_LT__INT8_INT8__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_LT__INT_INT__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_LT__STRING_STRING__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_LT__SYMBOL_SYMBOL__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_LT__TIMESTAMP_TIMESTAMP__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_LT__TIME_TIME__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_MINUS__DECIMAL_ARBITRARY_DECIMAL_ARBITRARY__DECIMAL_ARBITRARY
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_MINUS__FLOAT32_FLOAT32__FLOAT32
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_MINUS__FLOAT64_FLOAT64__FLOAT64
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_MINUS__INT16_INT16__INT16
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_MINUS__INT32_INT32__INT32
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_MINUS__INT64_INT64__INT64
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_MINUS__INT8_INT8__INT8
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_MINUS__INT_INT__INT
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_MODULO__DECIMAL_ARBITRARY_DECIMAL_ARBITRARY__DECIMAL_ARBITRARY
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_MODULO__FLOAT32_FLOAT32__FLOAT32
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_MODULO__FLOAT64_FLOAT64__FLOAT64
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_MODULO__INT16_INT16__INT16
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_MODULO__INT32_INT32__INT32
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_MODULO__INT64_INT64__INT64
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_MODULO__INT8_INT8__INT8
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_MODULO__INT_INT__INT
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_NEG__DECIMAL_ARBITRARY__DECIMAL_ARBITRARY
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_NEG__FLOAT32__FLOAT32
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_NEG__FLOAT64__FLOAT64
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_NEG__INT16__INT16
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_NEG__INT32__INT32
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_NEG__INT64__INT64
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_NEG__INT8__INT8
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_NEG__INT__INT
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_NOT__BOOL__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_NOT__MISSING__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_OR__BOOL_BOOL__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_OR__BOOL_MISSING__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_OR__MISSING_BOOL__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_OR__MISSING_MISSING__BOOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_PLUS__DECIMAL_ARBITRARY_DECIMAL_ARBITRARY__DECIMAL_ARBITRARY
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_PLUS__FLOAT32_FLOAT32__FLOAT32
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_PLUS__FLOAT64_FLOAT64__FLOAT64
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_PLUS__INT16_INT16__INT16
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_PLUS__INT32_INT32__INT32
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_PLUS__INT64_INT64__INT64
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_PLUS__INT8_INT8__INT8
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_PLUS__INT_INT__INT
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_POSITION__CLOB_CLOB__INT64
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_POSITION__STRING_STRING__INT64
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_POSITION__SYMBOL_SYMBOL__INT64
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_POS__DECIMAL_ARBITRARY__DECIMAL_ARBITRARY
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_POS__FLOAT32__FLOAT32
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_POS__FLOAT64__FLOAT64
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_POS__INT16__INT16
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_POS__INT32__INT32
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_POS__INT64__INT64
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_POS__INT8__INT8
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_POS__INT__INT
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_SUBSTRING__CLOB_INT64_INT64__CLOB
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_SUBSTRING__CLOB_INT64__CLOB
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_SUBSTRING__STRING_INT64_INT64__STRING
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_SUBSTRING__STRING_INT64__STRING
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_SUBSTRING__SYMBOL_INT64_INT64__SYMBOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_SUBSTRING__SYMBOL_INT64__SYMBOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_TIMES__DECIMAL_ARBITRARY_DECIMAL_ARBITRARY__DECIMAL_ARBITRARY
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_TIMES__FLOAT32_FLOAT32__FLOAT32
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_TIMES__FLOAT64_FLOAT64__FLOAT64
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_TIMES__INT16_INT16__INT16
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_TIMES__INT32_INT32__INT32
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_TIMES__INT64_INT64__INT64
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_TIMES__INT8_INT8__INT8
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_TIMES__INT_INT__INT
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_TRIM_CHARS__CLOB_CLOB__CLOB
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_TRIM_CHARS__STRING_STRING__STRING
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_TRIM_CHARS__SYMBOL_SYMBOL__SYMBOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_TRIM_LEADING_CHARS__CLOB_CLOB__CLOB
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_TRIM_LEADING_CHARS__STRING_STRING__STRING
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_TRIM_LEADING_CHARS__SYMBOL_SYMBOL__SYMBOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_TRIM_LEADING__CLOB__CLOB
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_TRIM_LEADING__STRING__STRING
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_TRIM_LEADING__SYMBOL__SYMBOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_TRIM_TRAILING_CHARS__CLOB_CLOB__CLOB
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_TRIM_TRAILING_CHARS__STRING_STRING__STRING
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_TRIM_TRAILING_CHARS__SYMBOL_SYMBOL__SYMBOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_TRIM_TRAILING__CLOB__CLOB
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_TRIM_TRAILING__STRING__STRING
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_TRIM_TRAILING__SYMBOL__SYMBOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_TRIM__CLOB__CLOB
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_TRIM__STRING__STRING
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_TRIM__SYMBOL__SYMBOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_UPPER__CLOB__CLOB
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_UPPER__STRING__STRING
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_UPPER__SYMBOL__SYMBOL
import org.partiql.spi.connector.sql.internal.builtins.scalar.Fn_UTCNOW____TIMESTAMP
import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnExperimental
import org.partiql.value.PartiQLValueExperimental

/**
 * This is where we will register all SQL builtins. For now, we wrap the generated header to keep the diff small.
 */
@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object SqlBuiltins {

    @JvmStatic
    private val scalars: List<Fn> = listOf(
        Fn_POS__INT8__INT8,
        Fn_POS__INT16__INT16,
        Fn_POS__INT32__INT32,
        Fn_POS__INT64__INT64,
        Fn_POS__INT__INT,
        Fn_POS__DECIMAL_ARBITRARY__DECIMAL_ARBITRARY,
        Fn_POS__FLOAT32__FLOAT32,
        Fn_POS__FLOAT64__FLOAT64,
        Fn_NEG__INT8__INT8,
        Fn_NEG__INT16__INT16,
        Fn_NEG__INT32__INT32,
        Fn_NEG__INT64__INT64,
        Fn_NEG__INT__INT,
        Fn_NEG__DECIMAL_ARBITRARY__DECIMAL_ARBITRARY,
        Fn_NEG__FLOAT32__FLOAT32,
        Fn_NEG__FLOAT64__FLOAT64,
        Fn_PLUS__INT8_INT8__INT8,
        Fn_PLUS__INT16_INT16__INT16,
        Fn_PLUS__INT32_INT32__INT32,
        Fn_PLUS__INT64_INT64__INT64,
        Fn_PLUS__INT_INT__INT,
        Fn_PLUS__DECIMAL_ARBITRARY_DECIMAL_ARBITRARY__DECIMAL_ARBITRARY,
        Fn_PLUS__FLOAT32_FLOAT32__FLOAT32,
        Fn_PLUS__FLOAT64_FLOAT64__FLOAT64,
        Fn_MINUS__INT8_INT8__INT8,
        Fn_MINUS__INT16_INT16__INT16,
        Fn_MINUS__INT32_INT32__INT32,
        Fn_MINUS__INT64_INT64__INT64,
        Fn_MINUS__INT_INT__INT,
        Fn_MINUS__DECIMAL_ARBITRARY_DECIMAL_ARBITRARY__DECIMAL_ARBITRARY,
        Fn_MINUS__FLOAT32_FLOAT32__FLOAT32,
        Fn_MINUS__FLOAT64_FLOAT64__FLOAT64,
        Fn_TIMES__INT8_INT8__INT8,
        Fn_TIMES__INT16_INT16__INT16,
        Fn_TIMES__INT32_INT32__INT32,
        Fn_TIMES__INT64_INT64__INT64,
        Fn_TIMES__INT_INT__INT,
        Fn_TIMES__DECIMAL_ARBITRARY_DECIMAL_ARBITRARY__DECIMAL_ARBITRARY,
        Fn_TIMES__FLOAT32_FLOAT32__FLOAT32,
        Fn_TIMES__FLOAT64_FLOAT64__FLOAT64,
        Fn_DIVIDE__INT8_INT8__INT8,
        Fn_DIVIDE__INT16_INT16__INT16,
        Fn_DIVIDE__INT32_INT32__INT32,
        Fn_DIVIDE__INT64_INT64__INT64,
        Fn_DIVIDE__INT_INT__INT,
        Fn_DIVIDE__DECIMAL_ARBITRARY_DECIMAL_ARBITRARY__DECIMAL_ARBITRARY,
        Fn_DIVIDE__FLOAT32_FLOAT32__FLOAT32,
        Fn_DIVIDE__FLOAT64_FLOAT64__FLOAT64,
        Fn_MODULO__INT8_INT8__INT8,
        Fn_MODULO__INT16_INT16__INT16,
        Fn_MODULO__INT32_INT32__INT32,
        Fn_MODULO__INT64_INT64__INT64,
        Fn_MODULO__INT_INT__INT,
        Fn_MODULO__DECIMAL_ARBITRARY_DECIMAL_ARBITRARY__DECIMAL_ARBITRARY,
        Fn_MODULO__FLOAT32_FLOAT32__FLOAT32,
        Fn_MODULO__FLOAT64_FLOAT64__FLOAT64,
        Fn_CONCAT__STRING_STRING__STRING,
        Fn_CONCAT__SYMBOL_SYMBOL__SYMBOL,
        Fn_CONCAT__CLOB_CLOB__CLOB,
        Fn_BITWISE_AND__INT8_INT8__INT8,
        Fn_BITWISE_AND__INT16_INT16__INT16,
        Fn_BITWISE_AND__INT32_INT32__INT32,
        Fn_BITWISE_AND__INT64_INT64__INT64,
        Fn_BITWISE_AND__INT_INT__INT
    ) + listOf(
        Fn_UPPER__STRING__STRING,
        Fn_UPPER__SYMBOL__SYMBOL,
        Fn_UPPER__CLOB__CLOB,
        Fn_LOWER__STRING__STRING,
        Fn_LOWER__SYMBOL__SYMBOL,
        Fn_LOWER__CLOB__CLOB,
        Fn_POSITION__STRING_STRING__INT64,
        Fn_POSITION__SYMBOL_SYMBOL__INT64,
        Fn_POSITION__CLOB_CLOB__INT64,
        Fn_POSITION__STRING_STRING__INT64,
        Fn_POSITION__SYMBOL_SYMBOL__INT64,
        Fn_POSITION__CLOB_CLOB__INT64,
        Fn_SUBSTRING__STRING_INT64__STRING,
        Fn_SUBSTRING__STRING_INT64_INT64__STRING,
        Fn_SUBSTRING__SYMBOL_INT64__SYMBOL,
        Fn_SUBSTRING__SYMBOL_INT64_INT64__SYMBOL,
        Fn_SUBSTRING__CLOB_INT64__CLOB,
        Fn_SUBSTRING__CLOB_INT64_INT64__CLOB,
        Fn_SUBSTRING__STRING_INT64__STRING,
        Fn_SUBSTRING__STRING_INT64_INT64__STRING,
        Fn_SUBSTRING__SYMBOL_INT64__SYMBOL,
        Fn_SUBSTRING__SYMBOL_INT64_INT64__SYMBOL,
        Fn_SUBSTRING__CLOB_INT64__CLOB,
        Fn_SUBSTRING__CLOB_INT64_INT64__CLOB,
        Fn_TRIM__STRING__STRING,
        Fn_TRIM__SYMBOL__SYMBOL,
        Fn_TRIM__CLOB__CLOB,
        Fn_UTCNOW____TIMESTAMP,
        Fn_NOT__BOOL__BOOL,
        Fn_NOT__MISSING__BOOL,
        Fn_AND__BOOL_BOOL__BOOL,
        Fn_AND__MISSING_BOOL__BOOL,
        Fn_AND__BOOL_MISSING__BOOL,
        Fn_AND__MISSING_MISSING__BOOL,
        Fn_OR__BOOL_BOOL__BOOL,
        Fn_OR__MISSING_BOOL__BOOL,
        Fn_OR__BOOL_MISSING__BOOL,
        Fn_OR__MISSING_MISSING__BOOL,
        Fn_LT__INT8_INT8__BOOL,
        Fn_LT__INT16_INT16__BOOL,
        Fn_LT__INT32_INT32__BOOL,
        Fn_LT__INT64_INT64__BOOL,
        Fn_LT__INT_INT__BOOL,
        Fn_LT__DECIMAL_ARBITRARY_DECIMAL_ARBITRARY__BOOL,
        Fn_LT__FLOAT32_FLOAT32__BOOL,
        Fn_LT__FLOAT64_FLOAT64__BOOL,
        Fn_LT__STRING_STRING__BOOL,
        Fn_LT__SYMBOL_SYMBOL__BOOL,
        Fn_LT__CLOB_CLOB__BOOL,
        Fn_LT__DATE_DATE__BOOL,
        Fn_LT__TIME_TIME__BOOL,
        Fn_LT__TIMESTAMP_TIMESTAMP__BOOL,
        Fn_LT__BOOL_BOOL__BOOL,
        Fn_LTE__INT8_INT8__BOOL,
        Fn_LTE__INT16_INT16__BOOL,
        Fn_LTE__INT32_INT32__BOOL,
        Fn_LTE__INT64_INT64__BOOL,
        Fn_LTE__INT_INT__BOOL,
        Fn_LTE__DECIMAL_ARBITRARY_DECIMAL_ARBITRARY__BOOL,
        Fn_LTE__FLOAT32_FLOAT32__BOOL,
        Fn_LTE__FLOAT64_FLOAT64__BOOL,
        Fn_LTE__STRING_STRING__BOOL,
        Fn_LTE__SYMBOL_SYMBOL__BOOL,
        Fn_LTE__CLOB_CLOB__BOOL,
        Fn_LTE__DATE_DATE__BOOL,
        Fn_LTE__TIME_TIME__BOOL,
        Fn_LTE__TIMESTAMP_TIMESTAMP__BOOL,
        Fn_LTE__BOOL_BOOL__BOOL,
        Fn_GT__INT8_INT8__BOOL,
        Fn_GT__INT16_INT16__BOOL,
        Fn_GT__INT32_INT32__BOOL,
        Fn_GT__INT64_INT64__BOOL,
        Fn_GT__INT_INT__BOOL,
        Fn_GT__DECIMAL_ARBITRARY_DECIMAL_ARBITRARY__BOOL,
        Fn_GT__FLOAT32_FLOAT32__BOOL,
        Fn_GT__FLOAT64_FLOAT64__BOOL,
        Fn_GT__STRING_STRING__BOOL,
        Fn_GT__SYMBOL_SYMBOL__BOOL,
        Fn_GT__CLOB_CLOB__BOOL,
        Fn_GT__DATE_DATE__BOOL,
        Fn_GT__TIME_TIME__BOOL,
        Fn_GT__TIMESTAMP_TIMESTAMP__BOOL,
        Fn_GT__BOOL_BOOL__BOOL,
        Fn_GTE__INT8_INT8__BOOL,
        Fn_GTE__INT16_INT16__BOOL,
        Fn_GTE__INT32_INT32__BOOL,
        Fn_GTE__INT64_INT64__BOOL,
        Fn_GTE__INT_INT__BOOL,
        Fn_GTE__DECIMAL_ARBITRARY_DECIMAL_ARBITRARY__BOOL,
        Fn_GTE__FLOAT32_FLOAT32__BOOL,
        Fn_GTE__FLOAT64_FLOAT64__BOOL,
        Fn_GTE__STRING_STRING__BOOL,
        Fn_GTE__SYMBOL_SYMBOL__BOOL,
        Fn_GTE__CLOB_CLOB__BOOL,
        Fn_GTE__DATE_DATE__BOOL,
        Fn_GTE__TIME_TIME__BOOL,
        Fn_GTE__TIMESTAMP_TIMESTAMP__BOOL,
        Fn_GTE__BOOL_BOOL__BOOL,
        Fn_EQ__ANY_ANY__BOOL,
        Fn_EQ__BOOL_BOOL__BOOL,
        Fn_EQ__INT8_INT8__BOOL,
        Fn_EQ__INT16_INT16__BOOL,
        Fn_EQ__INT32_INT32__BOOL,
        Fn_EQ__INT64_INT64__BOOL,
        Fn_EQ__INT_INT__BOOL,
        Fn_EQ__DECIMAL_DECIMAL__BOOL,
        Fn_EQ__DECIMAL_ARBITRARY_DECIMAL_ARBITRARY__BOOL,
        Fn_EQ__FLOAT32_FLOAT32__BOOL,
        Fn_EQ__FLOAT64_FLOAT64__BOOL,
        Fn_EQ__CHAR_CHAR__BOOL,
        Fn_EQ__STRING_STRING__BOOL,
        Fn_EQ__SYMBOL_SYMBOL__BOOL,
        Fn_EQ__BINARY_BINARY__BOOL,
        Fn_EQ__BYTE_BYTE__BOOL,
        Fn_EQ__BLOB_BLOB__BOOL,
        Fn_EQ__CLOB_CLOB__BOOL,
        Fn_EQ__DATE_DATE__BOOL,
        Fn_EQ__TIME_TIME__BOOL,
        Fn_EQ__TIMESTAMP_TIMESTAMP__BOOL,
        Fn_EQ__INTERVAL_INTERVAL__BOOL,
        Fn_EQ__BAG_BAG__BOOL,
        Fn_EQ__LIST_LIST__BOOL,
        Fn_EQ__SEXP_SEXP__BOOL,
        Fn_EQ__STRUCT_STRUCT__BOOL,
        Fn_EQ__NULL_NULL__BOOL,
        Fn_EQ__MISSING_MISSING__BOOL,
        Fn_BETWEEN__INT8_INT8_INT8__BOOL,
        Fn_BETWEEN__INT16_INT16_INT16__BOOL,
        Fn_BETWEEN__INT32_INT32_INT32__BOOL,
        Fn_BETWEEN__INT64_INT64_INT64__BOOL,
        Fn_BETWEEN__INT_INT_INT__BOOL,
        Fn_BETWEEN__DECIMAL_ARBITRARY_DECIMAL_ARBITRARY_DECIMAL_ARBITRARY__BOOL,
        Fn_BETWEEN__FLOAT32_FLOAT32_FLOAT32__BOOL,
        Fn_BETWEEN__FLOAT64_FLOAT64_FLOAT64__BOOL,
        Fn_BETWEEN__STRING_STRING_STRING__BOOL,
        Fn_BETWEEN__SYMBOL_SYMBOL_SYMBOL__BOOL,
        Fn_BETWEEN__CLOB_CLOB_CLOB__BOOL,
        Fn_BETWEEN__DATE_DATE_DATE__BOOL,
        Fn_BETWEEN__TIME_TIME_TIME__BOOL,
        Fn_BETWEEN__TIMESTAMP_TIMESTAMP_TIMESTAMP__BOOL,
        Fn_IN_COLLECTION__ANY_BAG__BOOL,
        Fn_IN_COLLECTION__ANY_LIST__BOOL,
        Fn_IN_COLLECTION__ANY_SEXP__BOOL,
        Fn_IN_COLLECTION__BOOL_BAG__BOOL,
        Fn_IN_COLLECTION__BOOL_LIST__BOOL,
        Fn_IN_COLLECTION__BOOL_SEXP__BOOL,
        Fn_IN_COLLECTION__INT8_BAG__BOOL,
        Fn_IN_COLLECTION__INT8_LIST__BOOL,
        Fn_IN_COLLECTION__INT8_SEXP__BOOL,
        Fn_IN_COLLECTION__INT16_BAG__BOOL,
        Fn_IN_COLLECTION__INT16_LIST__BOOL,
        Fn_IN_COLLECTION__INT16_SEXP__BOOL,
        Fn_IN_COLLECTION__INT32_BAG__BOOL,
        Fn_IN_COLLECTION__INT32_LIST__BOOL,
        Fn_IN_COLLECTION__INT32_SEXP__BOOL,
        Fn_IN_COLLECTION__INT64_BAG__BOOL,
        Fn_IN_COLLECTION__INT64_LIST__BOOL,
        Fn_IN_COLLECTION__INT64_SEXP__BOOL,
        Fn_IN_COLLECTION__INT_BAG__BOOL,
        Fn_IN_COLLECTION__INT_LIST__BOOL,
        Fn_IN_COLLECTION__INT_SEXP__BOOL,
        Fn_IN_COLLECTION__DECIMAL_BAG__BOOL,
        Fn_IN_COLLECTION__DECIMAL_LIST__BOOL,
        Fn_IN_COLLECTION__DECIMAL_SEXP__BOOL,
        Fn_IN_COLLECTION__DECIMAL_ARBITRARY_BAG__BOOL,
        Fn_IN_COLLECTION__DECIMAL_ARBITRARY_LIST__BOOL,
        Fn_IN_COLLECTION__DECIMAL_ARBITRARY_SEXP__BOOL,
        Fn_IN_COLLECTION__FLOAT32_BAG__BOOL,
        Fn_IN_COLLECTION__FLOAT32_LIST__BOOL,
        Fn_IN_COLLECTION__FLOAT32_SEXP__BOOL,
        Fn_IN_COLLECTION__FLOAT64_BAG__BOOL,
        Fn_IN_COLLECTION__FLOAT64_LIST__BOOL,
        Fn_IN_COLLECTION__FLOAT64_SEXP__BOOL,
        Fn_IN_COLLECTION__CHAR_BAG__BOOL,
        Fn_IN_COLLECTION__CHAR_LIST__BOOL,
        Fn_IN_COLLECTION__CHAR_SEXP__BOOL,
        Fn_IN_COLLECTION__STRING_BAG__BOOL,
        Fn_IN_COLLECTION__STRING_LIST__BOOL,
        Fn_IN_COLLECTION__STRING_SEXP__BOOL,
        Fn_IN_COLLECTION__SYMBOL_BAG__BOOL,
        Fn_IN_COLLECTION__SYMBOL_LIST__BOOL,
        Fn_IN_COLLECTION__SYMBOL_SEXP__BOOL,
        Fn_IN_COLLECTION__BINARY_BAG__BOOL,
        Fn_IN_COLLECTION__BINARY_LIST__BOOL,
        Fn_IN_COLLECTION__BINARY_SEXP__BOOL,
        Fn_IN_COLLECTION__BYTE_BAG__BOOL,
        Fn_IN_COLLECTION__BYTE_LIST__BOOL,
        Fn_IN_COLLECTION__BYTE_SEXP__BOOL,
        Fn_IN_COLLECTION__BLOB_BAG__BOOL,
        Fn_IN_COLLECTION__BLOB_LIST__BOOL,
        Fn_IN_COLLECTION__BLOB_SEXP__BOOL,
        Fn_IN_COLLECTION__CLOB_BAG__BOOL,
        Fn_IN_COLLECTION__CLOB_LIST__BOOL,
        Fn_IN_COLLECTION__CLOB_SEXP__BOOL,
        Fn_IN_COLLECTION__DATE_BAG__BOOL,
        Fn_IN_COLLECTION__DATE_LIST__BOOL,
        Fn_IN_COLLECTION__DATE_SEXP__BOOL,
        Fn_IN_COLLECTION__TIME_BAG__BOOL,
        Fn_IN_COLLECTION__TIME_LIST__BOOL,
        Fn_IN_COLLECTION__TIME_SEXP__BOOL,
        Fn_IN_COLLECTION__TIMESTAMP_BAG__BOOL,
        Fn_IN_COLLECTION__TIMESTAMP_LIST__BOOL,
        Fn_IN_COLLECTION__TIMESTAMP_SEXP__BOOL,
        Fn_IN_COLLECTION__INTERVAL_BAG__BOOL,
        Fn_IN_COLLECTION__INTERVAL_LIST__BOOL,
        Fn_IN_COLLECTION__INTERVAL_SEXP__BOOL,
        Fn_IN_COLLECTION__BAG_BAG__BOOL,
        Fn_IN_COLLECTION__BAG_LIST__BOOL,
        Fn_IN_COLLECTION__BAG_SEXP__BOOL,
        Fn_IN_COLLECTION__LIST_BAG__BOOL,
        Fn_IN_COLLECTION__LIST_LIST__BOOL,
        Fn_IN_COLLECTION__LIST_SEXP__BOOL,
        Fn_IN_COLLECTION__SEXP_BAG__BOOL,
        Fn_IN_COLLECTION__SEXP_LIST__BOOL,
        Fn_IN_COLLECTION__SEXP_SEXP__BOOL,
        Fn_IN_COLLECTION__STRUCT_BAG__BOOL,
        Fn_IN_COLLECTION__STRUCT_LIST__BOOL,
        Fn_IN_COLLECTION__STRUCT_SEXP__BOOL,
        Fn_IN_COLLECTION__NULL_BAG__BOOL,
        Fn_IN_COLLECTION__NULL_LIST__BOOL,
        Fn_IN_COLLECTION__NULL_SEXP__BOOL,
        Fn_IN_COLLECTION__MISSING_BAG__BOOL,
        Fn_IN_COLLECTION__MISSING_LIST__BOOL,
        Fn_IN_COLLECTION__MISSING_SEXP__BOOL,
        Fn_LIKE__STRING_STRING__BOOL,
        Fn_LIKE__SYMBOL_SYMBOL__BOOL,
        Fn_LIKE__CLOB_CLOB__BOOL,
        Fn_LIKE_ESCAPE__STRING_STRING_STRING__BOOL,
        Fn_LIKE_ESCAPE__SYMBOL_SYMBOL_SYMBOL__BOOL,
        Fn_LIKE_ESCAPE__CLOB_CLOB_CLOB__BOOL,
        Fn_IS_NULL__ANY__BOOL,
        Fn_IS_MISSING__ANY__BOOL,
        Fn_IS_ANY__ANY__BOOL,
        Fn_IS_BOOL__ANY__BOOL,
        Fn_IS_INT8__ANY__BOOL,
        Fn_IS_INT16__ANY__BOOL,
        Fn_IS_INT32__ANY__BOOL,
        Fn_IS_INT64__ANY__BOOL,
        Fn_IS_INT__ANY__BOOL,
        Fn_IS_DECIMAL__ANY__BOOL,
        Fn_IS_DECIMAL__INT32_INT32_ANY__BOOL,
        Fn_IS_DECIMAL_ARBITRARY__ANY__BOOL,
        Fn_IS_FLOAT32__ANY__BOOL,
        Fn_IS_FLOAT64__ANY__BOOL,
        Fn_IS_CHAR__ANY__BOOL,
        Fn_IS_CHAR__INT32_ANY__BOOL,
        Fn_IS_STRING__ANY__BOOL,
        Fn_IS_STRING__INT32_ANY__BOOL,
        Fn_IS_SYMBOL__ANY__BOOL,
        Fn_IS_BINARY__ANY__BOOL,
        Fn_IS_BYTE__ANY__BOOL,
        Fn_IS_BLOB__ANY__BOOL,
        Fn_IS_CLOB__ANY__BOOL,
        Fn_IS_DATE__ANY__BOOL,
        Fn_IS_TIME__ANY__BOOL,
        Fn_IS_TIME__BOOL_INT32_ANY__BOOL,
        Fn_IS_TIMESTAMP__ANY__BOOL,
        Fn_IS_TIMESTAMP__BOOL_INT32_ANY__BOOL,
        Fn_IS_INTERVAL__ANY__BOOL,
        Fn_IS_BAG__ANY__BOOL,
        Fn_IS_LIST__ANY__BOOL,
        Fn_IS_SEXP__ANY__BOOL,
        Fn_IS_STRUCT__ANY__BOOL,
        Fn_POS__INT8__INT8,
        Fn_POS__INT16__INT16,
        Fn_POS__INT32__INT32,
        Fn_POS__INT64__INT64,
        Fn_POS__INT__INT,
        Fn_POS__DECIMAL_ARBITRARY__DECIMAL_ARBITRARY,
        Fn_POS__FLOAT32__FLOAT32,
        Fn_POS__FLOAT64__FLOAT64,
        Fn_NEG__INT8__INT8,
        Fn_NEG__INT16__INT16,
        Fn_NEG__INT32__INT32,
        Fn_NEG__INT64__INT64,
        Fn_NEG__INT__INT,
        Fn_NEG__DECIMAL_ARBITRARY__DECIMAL_ARBITRARY,
        Fn_NEG__FLOAT32__FLOAT32,
        Fn_NEG__FLOAT64__FLOAT64,
        Fn_PLUS__INT8_INT8__INT8,
        Fn_PLUS__INT16_INT16__INT16,
        Fn_PLUS__INT32_INT32__INT32,
        Fn_PLUS__INT64_INT64__INT64,
        Fn_PLUS__INT_INT__INT,
        Fn_PLUS__DECIMAL_ARBITRARY_DECIMAL_ARBITRARY__DECIMAL_ARBITRARY,
        Fn_PLUS__FLOAT32_FLOAT32__FLOAT32,
        Fn_PLUS__FLOAT64_FLOAT64__FLOAT64,
        Fn_MINUS__INT8_INT8__INT8,
        Fn_MINUS__INT16_INT16__INT16,
        Fn_MINUS__INT32_INT32__INT32,
        Fn_MINUS__INT64_INT64__INT64,
        Fn_MINUS__INT_INT__INT,
        Fn_MINUS__DECIMAL_ARBITRARY_DECIMAL_ARBITRARY__DECIMAL_ARBITRARY,
        Fn_MINUS__FLOAT32_FLOAT32__FLOAT32,
        Fn_MINUS__FLOAT64_FLOAT64__FLOAT64,
        Fn_TIMES__INT8_INT8__INT8,
        Fn_TIMES__INT16_INT16__INT16,
        Fn_TIMES__INT32_INT32__INT32,
        Fn_TIMES__INT64_INT64__INT64,
        Fn_TIMES__INT_INT__INT,
        Fn_TIMES__DECIMAL_ARBITRARY_DECIMAL_ARBITRARY__DECIMAL_ARBITRARY,
        Fn_TIMES__FLOAT32_FLOAT32__FLOAT32,
        Fn_TIMES__FLOAT64_FLOAT64__FLOAT64,
        Fn_DIVIDE__INT8_INT8__INT8,
        Fn_DIVIDE__INT16_INT16__INT16,
        Fn_DIVIDE__INT32_INT32__INT32,
        Fn_DIVIDE__INT64_INT64__INT64,
        Fn_DIVIDE__INT_INT__INT,
        Fn_DIVIDE__DECIMAL_ARBITRARY_DECIMAL_ARBITRARY__DECIMAL_ARBITRARY,
        Fn_DIVIDE__FLOAT32_FLOAT32__FLOAT32,
        Fn_DIVIDE__FLOAT64_FLOAT64__FLOAT64,
        Fn_MODULO__INT8_INT8__INT8,
        Fn_MODULO__INT16_INT16__INT16,
        Fn_MODULO__INT32_INT32__INT32,
        Fn_MODULO__INT64_INT64__INT64,
        Fn_MODULO__INT_INT__INT,
        Fn_MODULO__DECIMAL_ARBITRARY_DECIMAL_ARBITRARY__DECIMAL_ARBITRARY,
        Fn_MODULO__FLOAT32_FLOAT32__FLOAT32,
        Fn_MODULO__FLOAT64_FLOAT64__FLOAT64,
        Fn_CONCAT__STRING_STRING__STRING,
        Fn_CONCAT__SYMBOL_SYMBOL__SYMBOL,
        Fn_CONCAT__CLOB_CLOB__CLOB,
        Fn_BITWISE_AND__INT8_INT8__INT8,
        Fn_BITWISE_AND__INT16_INT16__INT16,
        Fn_BITWISE_AND__INT32_INT32__INT32,
        Fn_BITWISE_AND__INT64_INT64__INT64,
        Fn_BITWISE_AND__INT_INT__INT,
        Fn_TRIM_CHARS__STRING_STRING__STRING,
        Fn_TRIM_CHARS__SYMBOL_SYMBOL__SYMBOL,
        Fn_TRIM_CHARS__CLOB_CLOB__CLOB,
        Fn_TRIM_LEADING__STRING__STRING,
        Fn_TRIM_LEADING__SYMBOL__SYMBOL,
        Fn_TRIM_LEADING__CLOB__CLOB,
        Fn_TRIM_LEADING_CHARS__STRING_STRING__STRING,
        Fn_TRIM_LEADING_CHARS__SYMBOL_SYMBOL__SYMBOL,
        Fn_TRIM_LEADING_CHARS__CLOB_CLOB__CLOB,
        Fn_TRIM_TRAILING__STRING__STRING,
        Fn_TRIM_TRAILING__SYMBOL__SYMBOL,
        Fn_TRIM_TRAILING__CLOB__CLOB,
        Fn_TRIM_TRAILING_CHARS__STRING_STRING__STRING,
        Fn_TRIM_TRAILING_CHARS__SYMBOL_SYMBOL__SYMBOL,
        Fn_TRIM_TRAILING_CHARS__CLOB_CLOB__CLOB,
        Fn_DATE_ADD_YEAR__INT32_DATE__DATE,
        Fn_DATE_ADD_YEAR__INT64_DATE__DATE,
        Fn_DATE_ADD_YEAR__INT_DATE__DATE,
        Fn_DATE_ADD_YEAR__INT32_TIME__TIME,
        Fn_DATE_ADD_YEAR__INT64_TIME__TIME,
        Fn_DATE_ADD_YEAR__INT_TIME__TIME,
        Fn_DATE_ADD_YEAR__INT32_TIMESTAMP__TIMESTAMP,
        Fn_DATE_ADD_YEAR__INT64_TIMESTAMP__TIMESTAMP,
        Fn_DATE_ADD_YEAR__INT_TIMESTAMP__TIMESTAMP,
        Fn_DATE_ADD_MONTH__INT32_DATE__DATE,
        Fn_DATE_ADD_MONTH__INT64_DATE__DATE,
        Fn_DATE_ADD_MONTH__INT_DATE__DATE,
        Fn_DATE_ADD_MONTH__INT32_TIME__TIME,
        Fn_DATE_ADD_MONTH__INT64_TIME__TIME,
        Fn_DATE_ADD_MONTH__INT_TIME__TIME,
        Fn_DATE_ADD_MONTH__INT32_TIMESTAMP__TIMESTAMP,
        Fn_DATE_ADD_MONTH__INT64_TIMESTAMP__TIMESTAMP,
        Fn_DATE_ADD_MONTH__INT_TIMESTAMP__TIMESTAMP,
        Fn_DATE_ADD_DAY__INT32_DATE__DATE,
        Fn_DATE_ADD_DAY__INT64_DATE__DATE,
        Fn_DATE_ADD_DAY__INT_DATE__DATE,
        Fn_DATE_ADD_DAY__INT32_TIME__TIME,
        Fn_DATE_ADD_DAY__INT64_TIME__TIME,
        Fn_DATE_ADD_DAY__INT_TIME__TIME,
        Fn_DATE_ADD_DAY__INT32_TIMESTAMP__TIMESTAMP,
        Fn_DATE_ADD_DAY__INT64_TIMESTAMP__TIMESTAMP,
        Fn_DATE_ADD_DAY__INT_TIMESTAMP__TIMESTAMP,
        Fn_DATE_ADD_HOUR__INT32_DATE__DATE,
        Fn_DATE_ADD_HOUR__INT64_DATE__DATE,
        Fn_DATE_ADD_HOUR__INT_DATE__DATE,
        Fn_DATE_ADD_HOUR__INT32_TIME__TIME,
        Fn_DATE_ADD_HOUR__INT64_TIME__TIME,
        Fn_DATE_ADD_HOUR__INT_TIME__TIME,
        Fn_DATE_ADD_HOUR__INT32_TIMESTAMP__TIMESTAMP,
        Fn_DATE_ADD_HOUR__INT64_TIMESTAMP__TIMESTAMP,
        Fn_DATE_ADD_HOUR__INT_TIMESTAMP__TIMESTAMP,
        Fn_DATE_ADD_MINUTE__INT32_DATE__DATE,
        Fn_DATE_ADD_MINUTE__INT64_DATE__DATE,
        Fn_DATE_ADD_MINUTE__INT_DATE__DATE,
        Fn_DATE_ADD_MINUTE__INT32_TIME__TIME,
        Fn_DATE_ADD_MINUTE__INT64_TIME__TIME,
        Fn_DATE_ADD_MINUTE__INT_TIME__TIME,
        Fn_DATE_ADD_MINUTE__INT32_TIMESTAMP__TIMESTAMP,
        Fn_DATE_ADD_MINUTE__INT64_TIMESTAMP__TIMESTAMP,
        Fn_DATE_ADD_MINUTE__INT_TIMESTAMP__TIMESTAMP,
        Fn_DATE_ADD_SECOND__INT32_DATE__DATE,
        Fn_DATE_ADD_SECOND__INT64_DATE__DATE,
        Fn_DATE_ADD_SECOND__INT_DATE__DATE,
        Fn_DATE_ADD_SECOND__INT32_TIME__TIME,
        Fn_DATE_ADD_SECOND__INT64_TIME__TIME,
        Fn_DATE_ADD_SECOND__INT_TIME__TIME,
        Fn_DATE_ADD_SECOND__INT32_TIMESTAMP__TIMESTAMP,
        Fn_DATE_ADD_SECOND__INT64_TIMESTAMP__TIMESTAMP,
        Fn_DATE_ADD_SECOND__INT_TIMESTAMP__TIMESTAMP,
        Fn_DATE_DIFF_YEAR__DATE_DATE__INT64,
        Fn_DATE_DIFF_YEAR__TIME_TIME__INT64,
        Fn_DATE_DIFF_YEAR__TIMESTAMP_TIMESTAMP__INT64,
        Fn_DATE_DIFF_MONTH__DATE_DATE__INT64,
        Fn_DATE_DIFF_MONTH__TIME_TIME__INT64,
        Fn_DATE_DIFF_MONTH__TIMESTAMP_TIMESTAMP__INT64,
        Fn_DATE_DIFF_DAY__DATE_DATE__INT64,
        Fn_DATE_DIFF_DAY__TIME_TIME__INT64,
        Fn_DATE_DIFF_DAY__TIMESTAMP_TIMESTAMP__INT64,
        Fn_DATE_DIFF_HOUR__DATE_DATE__INT64,
        Fn_DATE_DIFF_HOUR__TIME_TIME__INT64,
        Fn_DATE_DIFF_HOUR__TIMESTAMP_TIMESTAMP__INT64,
        Fn_DATE_DIFF_MINUTE__DATE_DATE__INT64,
        Fn_DATE_DIFF_MINUTE__TIME_TIME__INT64,
        Fn_DATE_DIFF_MINUTE__TIMESTAMP_TIMESTAMP__INT64,
        Fn_DATE_DIFF_SECOND__DATE_DATE__INT64,
        Fn_DATE_DIFF_SECOND__TIME_TIME__INT64,
        Fn_DATE_DIFF_SECOND__TIMESTAMP_TIMESTAMP__INT64,
        Fn_CURRENT_USER____STRING,
        Fn_CURRENT_DATE____DATE
    )

    @JvmStatic
    private val aggregations: List<Fn> = listOf(Agg_EVERY__BOOL__BOOL, Agg_ANY__BOOL__BOOL, Agg_SOME__BOOL__BOOL, Agg_COUNT__ANY__INT32, Agg_COUNT_STAR____INT32, Agg_MIN__INT8__INT8, Agg_MIN__INT16__INT16, Agg_MIN__INT32__INT32, Agg_MIN__INT64__INT64, Agg_MIN__INT__INT, Agg_MIN__DECIMAL_ARBITRARY__DECIMAL_ARBITRARY, Agg_MIN__FLOAT32__FLOAT32, Agg_MIN__FLOAT64__FLOAT64, Agg_MAX__INT8__INT8, Agg_MAX__INT16__INT16, Agg_MAX__INT32__INT32, Agg_MAX__INT64__INT64, Agg_MAX__INT__INT, Agg_MAX__DECIMAL_ARBITRARY__DECIMAL_ARBITRARY, Agg_MAX__FLOAT32__FLOAT32, Agg_MAX__FLOAT64__FLOAT64, Agg_SUM__INT8__INT8, Agg_SUM__INT16__INT16, Agg_SUM__INT32__INT32, Agg_SUM__INT64__INT64, Agg_SUM__INT__INT, Agg_SUM__DECIMAL_ARBITRARY__DECIMAL_ARBITRARY, Agg_SUM__FLOAT32__FLOAT32, Agg_SUM__FLOAT64__FLOAT64, Agg_AVG__INT8__INT8, Agg_AVG__INT16__INT16, Agg_AVG__INT32__INT32, Agg_AVG__INT64__INT64, Agg_AVG__INT__INT, Agg_AVG__DECIMAL_ARBITRARY__DECIMAL_ARBITRARY, Agg_AVG__FLOAT32__FLOAT32, Agg_AVG__FLOAT64__FLOAT64)

    @JvmStatic
    val builtins: List<Fn> = scalars + aggregations
}
