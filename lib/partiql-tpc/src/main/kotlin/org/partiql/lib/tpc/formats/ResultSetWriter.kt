package org.partiql.lib.tpc.formats

import io.trino.tpcds.Results
import io.trino.tpcds.Table
import org.partiql.lib.tpc.Format
import org.partiql.lib.tpc.formats.csv.CsvResultSetWriter
import org.partiql.lib.tpc.formats.ion.IonResultSetWriter
import org.partiql.lib.tpc.formats.parquet.ParquetResultSetWriter
import java.nio.file.Path

/**
 * Simple interface for writing [Results].
 */
interface ResultSetWriter : AutoCloseable {

    /**
     * Open any resources for writing results
     *
     */
    fun open(table: Table)

    /**
     * Write a result set
     */
    fun write(results: Results)

    /**
     * Close any resources
     */
    override fun close()
}

object ResultSetWriterFactory {

    fun create(table: Table, format: Format, output: Path) = when (format) {
        Format.ION -> IonResultSetWriter(output)
        Format.CSV -> CsvResultSetWriter(output)
        Format.PARQUET -> ParquetResultSetWriter(output)
    }
}
