@file:OptIn(ConnectorFunctionExperimental::class)

package org.partiql.spi.connector.base

/* ktlint-disable no-wildcard-imports */
import org.partiql.spi.connector.ConnectorFunction
import org.partiql.spi.connector.ConnectorFunctionExperimental
import org.partiql.spi.internal.fn.agg.*
import org.partiql.spi.internal.fn.scalar.*

/**
 * TODO
 */
public open class BaseRoutines {

    public open val functions: List<ConnectorFunction.Scalar> = emptyList()

    public open val aggregations: List<ConnectorFunction.Aggregation> = emptyList()

    private val functions =

    private companion object {

        /**
         *
         */
        @JvmStatic
        private val functions: List<ConnectorFunction.Scalar> = listOf(
            Fn_UPPER__STRING__STRING,
            Fn_UPPER__SYMBOL__SYMBOL,
            Fn_UPPER__CLOB__CLOB,
            Fn_LOWER__STRING__STRING,
            Fn_LOWER__SYMBOL__SYMBOL,
            Fn_LOWER__CLOB__CLOB,
            Fn_POSITION__STRING_STRING__INT64,
            Fn_POSITION__SYMBOL_SYMBOL__INT64,
            Fn_POSITION__CLOB_CLOB__INT64,
            Fn_SUBSTRING__STRING_INT64__STRING,
            Fn_SUBSTRING__STRING_INT64_INT64__STRING,
            Fn_SUBSTRING__SYMBOL_INT64__SYMBOL,
            Fn_SUBSTRING__SYMBOL_INT64_INT64__SYMBOL,
            Fn_SUBSTRING__CLOB_INT64__CLOB,
            Fn_SUBSTRING__CLOB_INT64_INT64__CLOB,
            Fn_TRIM__STRING__STRING,
            Fn_TRIM__SYMBOL__SYMBOL,
            Fn_TRIM__CLOB__CLOB,
            Fn_UTCNOW____TIMESTAMP
        )

        @JvmStatic
        private val aggregations: List<ConnectorFunction.Aggregation> = listOf(
            Agg_EVERY__BOOL__BOOL,
            Agg_ANY__BOOL__BOOL,
            Agg_SOME__BOOL__BOOL,
            Agg_COUNT__ANY__INT32,
            Agg_COUNT_STAR____INT32,
            Agg_MIN__INT8__INT8,
            Agg_MIN__INT16__INT16,
            Agg_MIN__INT32__INT32,
            Agg_MIN__INT64__INT64,
            Agg_MIN__INT__INT,
            Agg_MIN__DECIMAL_ARBITRARY__DECIMAL_ARBITRARY,
            Agg_MIN__FLOAT32__FLOAT32,
            Agg_MIN__FLOAT64__FLOAT64,
            Agg_MAX__INT8__INT8,
            Agg_MAX__INT16__INT16,
            Agg_MAX__INT32__INT32,
            Agg_MAX__INT64__INT64,
            Agg_MAX__INT__INT,
            Agg_MAX__DECIMAL_ARBITRARY__DECIMAL_ARBITRARY,
            Agg_MAX__FLOAT32__FLOAT32,
            Agg_MAX__FLOAT64__FLOAT64,
            Agg_SUM__INT8__INT8,
            Agg_SUM__INT16__INT16,
            Agg_SUM__INT32__INT32,
            Agg_SUM__INT64__INT64,
            Agg_SUM__INT__INT,
            Agg_SUM__DECIMAL_ARBITRARY__DECIMAL_ARBITRARY,
            Agg_SUM__FLOAT32__FLOAT32,
            Agg_SUM__FLOAT64__FLOAT64,
            Agg_AVG__INT8__INT8,
            Agg_AVG__INT16__INT16,
            Agg_AVG__INT32__INT32,
            Agg_AVG__INT64__INT64,
            Agg_AVG__INT__INT,
            Agg_AVG__DECIMAL_ARBITRARY__DECIMAL_ARBITRARY,
            Agg_AVG__FLOAT32__FLOAT32,
            Agg_AVG__FLOAT64__FLOAT64
        )
    }
}
