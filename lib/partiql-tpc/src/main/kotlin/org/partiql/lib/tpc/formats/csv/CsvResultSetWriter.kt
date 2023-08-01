package org.partiql.lib.tpc.formats.csv

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import org.partiql.lib.tpc.ResultSet
import org.partiql.lib.tpc.formats.ResultSetWriter
import org.partiql.lib.tpc.mappers
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.file.Path

class CsvResultSetWriter(
    private val output: Path,
) : ResultSetWriter {

    override fun open() {}

    override fun write(records: ResultSet) {
        // Initialize output appendable
        val tableName = records.table.name.lowercase()
        val path = output.resolve(tableName).toString() + ".csv"
        val file = FileOutputStream(path)
        val out = OutputStreamWriter(file)
        val printer = CSVPrinter(out, CSVFormat.DEFAULT)
        // Builder mapper closures
        val mappers = records.table.mappers()
        // Process each record
        try {

            for (rows in records.results) {
                if (rows == null) {
                    // skip silently; make a note??
                    continue
                }
                for (row in rows) {
                    if (row == null) {
                        // skip silently; make a note??
                        continue
                    }
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
        out.close()
    }

    override fun close() {}
}
