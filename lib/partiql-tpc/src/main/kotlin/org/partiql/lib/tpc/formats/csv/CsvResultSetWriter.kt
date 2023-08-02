package org.partiql.lib.tpc.formats.csv

import io.trino.tpcds.Results
import io.trino.tpcds.Table
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import org.partiql.lib.tpc.Mapper
import org.partiql.lib.tpc.formats.ResultSetWriter
import org.partiql.lib.tpc.mappers
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.file.Path

class CsvResultSetWriter(
    private val output: Path,
) : ResultSetWriter {

    private lateinit var printer: CSVPrinter
    private lateinit var mappers: Array<Mapper<*>>

    override fun open(table: Table) {
        // Initialize output appendable
        val tableName = table.name.lowercase()
        val path = output.resolve(tableName).toString() + ".csv"
        val file = FileOutputStream(path)
        val out = OutputStreamWriter(file)
        // Builder mapper closures
        printer = CSVPrinter(out, CSVFormat.DEFAULT)
        mappers = table.mappers()
    }

    override fun write(results: Results) {
        // Process each record
        try {
            for (rows in results) {
                // skip silently; make a note??
                if (rows == null) continue
                for (row in rows) {
                    // skip silently; make a note??
                    if (row == null) continue
                    // map values
                    val r = row.mapIndexed { i, s ->
                        when (s) {
                            null -> "NULL"
                            else -> mappers[i](s)
                        }
                    }
                    printer.printRecord(r)
                }
            }
        } catch (ex: NullPointerException) {
            // NPE at io.trino.tpcds.Results$ResultsIterator.<init>(Results.java:88)
            // As this is for rapid prototyping, I'll catch and do nothing
        }
    }

    override fun close() {
        printer.close()
    }
}
