package org.partiql.lib.tpc

import io.trino.tpcds.Results
import io.trino.tpcds.Table
import io.trino.tpcds.column.ColumnType
import java.time.LocalDate
import java.time.LocalTime

/**
 * Output formats
 */
enum class Format {
    ION, CSV, PARQUET,
}

/**
 * Benchmark type to generate for
 */
enum class Benchmark(val display: String) {
    TPCDS("TPC-DS"),
    TPCH("TPC-H"),
}

/**
 * Associate a Table with
 *
 * @property table
 * @property results
 */
class ResultSet(
    val table: Table,
    val results: Results,
)

/**
 * Generated data is string-ly typed
 */
typealias Mapper<T> = (String) -> T

/**
 * Produce an array of mappers from the table schema. Following Trino's consumption.
 *
 * https://github.com/trinodb/trino/blob/master/plugin/trino-tpcds/src/main/java/io/trino/plugin/tpcds/TpcdsRecordSet.java#L133-L145
 */
fun Table.mappers(): Array<Mapper<*>> = columns.map {
    when (it.type.base) {
        ColumnType.Base.INTEGER -> ::toInt
        ColumnType.Base.DECIMAL -> ::toDecimal
        ColumnType.Base.IDENTIFIER -> ::identity
        ColumnType.Base.VARCHAR -> ::identity
        ColumnType.Base.CHAR -> ::identity
        ColumnType.Base.TIME -> ::toTime
        ColumnType.Base.DATE -> ::toDate
        null -> error("unreachable")
    }
}.toTypedArray()

private fun toInt(s: String): Int = s.toInt(10)

private fun toDecimal(s: String): Double = s.toDouble() // should it be BigDecimal?

private fun identity(s: String): String = s

private fun toTime(s: String): LocalTime = LocalTime.parse(s)

private fun toDate(s: String): LocalDate = LocalDate.parse(s)
