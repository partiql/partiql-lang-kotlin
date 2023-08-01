package org.partiql.lib.tpc.formats

import org.partiql.lib.tpc.Format
import org.partiql.lib.tpc.ResultSet
import org.partiql.lib.tpc.formats.csv.CsvResultSetWriter
import java.nio.file.Path

/**
 * Simple interface for writing a [ResultSet].
 */
interface ResultSetWriter : AutoCloseable {

    /**
     * Open any resources for writing results
     *
     */
    fun open()

    /**
     * Write a result set
     */
    fun write(records: ResultSet)

    /**
     * Close any resources
     */
    override fun close()
}

object ResultSetWriterFactory {

    fun create(format: Format, output: Path) = when (format) {
        Format.ION -> TODO("Ion not implemented")
        Format.CSV -> CsvResultSetWriter(output)
        Format.PARQUET -> TODO("Parquet not implemented")
    }
}
