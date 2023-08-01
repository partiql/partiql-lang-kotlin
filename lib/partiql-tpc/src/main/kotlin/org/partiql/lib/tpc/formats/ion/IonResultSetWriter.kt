package org.partiql.lib.tpc.formats.ion

import com.amazon.ion.IonType
import com.amazon.ion.IonWriter
import com.amazon.ion.Timestamp
import com.amazon.ion.system.IonTextWriterBuilder
import io.trino.tpcds.Table
import io.trino.tpcds.column.ColumnType
import org.partiql.lib.tpc.ResultSet
import org.partiql.lib.tpc.formats.ResultSetWriter
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.file.Path
import java.time.LocalDate
import java.time.LocalTime

/**
 * Basic function which answers "How do I write this string to the IonWriter?"
 */
internal typealias IonMapper = (IonWriter, String) -> Unit

/**
 * This is an early, highly-simplified implementation of a ResultSetWriter which outputs Ion to a file. I believe we
 * can greatly improve by having a streaming writer as well as having the binary Ion option
 *
 * I may tackle those now depending on how things are going.
 *
 * Lots of potential options here on formatting of output, but I'm sticking with the simplest for now of just using
 * a document of structs.
 */
class IonResultSetWriter(private val output: Path) : ResultSetWriter {

    val ionTextWriter = IonTextWriterBuilder.standard()

    override fun open() {}

    override fun write(records: ResultSet) {
        // Initialize output appendable
        val tableName = records.table.name.lowercase()
        val path = output.resolve(tableName).toString() + ".ion"
        val file = FileOutputStream(path)
        val out = OutputStreamWriter(file)
        val writer = IonTextWriterBuilder.standard().build(file)
        // Generate field mappers
        val mappers: Array<Pair<String, IonMapper>> = records.table.fieldMappers()
        // Process each record
        try {
            for (rows in records.results) {
                // skip silently; make a note??
                if (rows == null) continue
                for (row in rows) {
                    // skip silently; make a note??
                    if (row == null) continue
                    // BEGIN {
                    writer.stepIn(IonType.STRUCT)
                    mappers.forEachIndexed { i, (field, mapper) ->
                        val s = row[i]
                        writer.setFieldName(field)
                        when (s) {
                            null -> writer.writeNull()
                            else -> mapper(writer, s)
                        }
                    }
                    // END }
                    writer.stepOut()
                }
            }
        } catch (ex: NullPointerException) {
            // NPE at io.trino.tpcds.Results$ResultsIterator.<init>(Results.java:88)
            // As this is for rapid prototyping, I'll catch and do nothing
        }
        writer.close()
    }

    override fun close() {}

    /**
     * Field name to IonMapper
     */
    private fun Table.fieldMappers(): Array<Pair<String, IonMapper>> = columns.map {
        val col = it.name.lowercase()
        val mapper = when (it.type.base) {
            ColumnType.Base.INTEGER -> ::writeInt
            ColumnType.Base.DECIMAL -> ::writeDecimal
            ColumnType.Base.IDENTIFIER -> ::writeString
            ColumnType.Base.VARCHAR -> ::writeString
            ColumnType.Base.CHAR -> ::writeString
            ColumnType.Base.TIME -> ::writeTime
            ColumnType.Base.DATE -> ::writeDate
            null -> error("unreachable")
        }
        col to mapper
    }.toTypedArray()

    private fun writeInt(writer: IonWriter, s: String) = writer.writeInt(s.toLong(10))

    private fun writeDecimal(writer: IonWriter, s: String) = writer.writeFloat(s.toDouble())

    private fun writeString(writer: IonWriter, s: String) = writer.writeString(s)

    // Ignoring timezones; see PartiQLValueIonWriter
    private fun writeTime(writer: IonWriter, s: String) {
        val time = LocalTime.parse(s)
        writer.stepIn(IonType.STRUCT)
        writer.setFieldName("hour")
        writer.writeInt(time.hour.toLong())
        writer.setFieldName("minute")
        writer.writeInt(time.minute.toLong())
        writer.setFieldName("second")
        writer.writeInt(time.second.toLong())
        writer.stepOut()
    }

    private fun writeDate(writer: IonWriter, s: String) {
        val date = LocalDate.parse(s)
        val timestamp = Timestamp.forDay(date.year, date.monthValue, date.dayOfMonth)
        writer.writeTimestamp(timestamp)
    }
}
