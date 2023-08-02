package org.partiql.lib.tpc.formats.parquet

import io.trino.tpcds.Results
import io.trino.tpcds.Table
import io.trino.tpcds.column.ColumnType
import org.apache.avro.Schema
import org.apache.avro.generic.GenericData
import org.apache.hadoop.conf.Configuration
import org.apache.parquet.avro.AvroParquetWriter
import org.apache.parquet.hadoop.ParquetWriter
import org.apache.parquet.hadoop.util.HadoopOutputFile
import org.partiql.lib.tpc.Mapper
import org.partiql.lib.tpc.formats.ResultSetWriter
import org.partiql.lib.tpc.mappers
import java.nio.file.Path
import java.time.LocalDate
import java.time.LocalTime

/**
 * Write the TPC result data into Parquet using the (very helpful) Avro toolchain.
 *
 * !! HACK !! NOTE: Time -> Nanoseconds in Day
 * !! HACK !! NOTE: Date -> Epoch Day
 */
class ParquetResultSetWriter(
    private val output: Path,
) : ResultSetWriter {

    private lateinit var schema: Schema
    private lateinit var writer: ParquetWriter<GenericData.Record>
    private lateinit var mappers: Array<Mapper<*>>

    override fun open(table: Table) {
        val tableName = table.name.lowercase()
        val path = output.resolve(tableName).toString() + ".parquet"
        // HDFS
        val hConf = Configuration(false)
        val hPath = org.apache.hadoop.fs.Path(path)
        val hFile = HadoopOutputFile.fromPath(hPath, hConf)
        // Avro
        schema = getSchema(table)
        writer = AvroParquetWriter.builder<GenericData.Record>(hFile)
            .withSchema(schema)
            .build()
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
                    // write
                    val record = GenericData.Record(schema)
                    row.forEachIndexed { i, s ->
                        val v = when (s) {
                            null -> null
                            else -> when (val v = mappers[i](s)) {
                                is LocalDate -> v.toEpochDay()
                                is LocalTime -> v.toNanoOfDay()
                                else -> v
                            }
                        }
                        record.put(i, v)
                    }
                    writer.write(record)
                }
            }
        } catch (ex: NullPointerException) {
            // NPE at io.trino.tpcds.Results$ResultsIterator.<init>(Results.java:88)
            // As this is for rapid prototyping, I'll catch and do nothing
        }
    }

    override fun close() {
        writer.close()
    }

    /**
     * Create a Parquet schema from this [Table] column descriptions.
     */
    private fun getSchema(table: Table): Schema {
        val name = table.name.lowercase()
        val doc = null
        val namespace = "org.partiql.lib.tpc"
        val isError = false
        val fields = table.columns.map {
            val field = it.name.lowercase()
            val type = when (it.type.base) {
                ColumnType.Base.INTEGER -> Schema.Type.INT
                ColumnType.Base.IDENTIFIER -> Schema.Type.STRING
                ColumnType.Base.VARCHAR -> Schema.Type.STRING
                ColumnType.Base.CHAR -> Schema.Type.STRING
                ColumnType.Base.DECIMAL -> Schema.Type.DOUBLE
                // again, hand-waving on these classics
                ColumnType.Base.DATE -> Schema.Type.LONG
                ColumnType.Base.TIME -> Schema.Type.LONG
                else -> error("unreachable")
            }
            // all fields are nullable
            Schema.Field(field, Schema.createUnion(Schema.create(type), Schema.create(Schema.Type.NULL)))
        }
        return Schema.createRecord(name, doc, namespace, isError, fields)
    }
}
