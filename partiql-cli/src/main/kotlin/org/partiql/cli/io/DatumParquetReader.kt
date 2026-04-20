package org.partiql.cli.io

import org.apache.parquet.example.data.simple.SimpleGroup
import org.apache.parquet.example.data.simple.convert.GroupRecordConverter
import org.apache.parquet.hadoop.ParquetFileReader
import org.apache.parquet.io.ColumnIOFactory
import org.apache.parquet.io.LocalInputFile
import org.apache.parquet.schema.LogicalTypeAnnotation
import org.apache.parquet.schema.MessageType
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName
import org.partiql.spi.value.Datum
import org.partiql.spi.value.Field
import java.io.File
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Reads Parquet files into a [Datum] bag of structs.
 * Data is streamed Row Group by Row Group.
 */
internal object DatumParquetReader {

    fun read(file: File): Datum {
        val inputFile = LocalInputFile(file.toPath())
        val schema = ParquetFileReader.open(inputFile).use { it.footer.fileMetaData.schema }
        return Datum.bag(
            Iterable {
                iterator {
                    ParquetFileReader.open(inputFile).use { reader ->
                        var store = reader.readNextRowGroup()
                        while (store != null) {
                            val columnIO = ColumnIOFactory().getColumnIO(schema)
                            val recordReader = columnIO.getRecordReader(
                                store,
                                GroupRecordConverter(schema)
                            )
                            for (i in 0 until store.rowCount) {
                                yield(groupToDatum(recordReader.read() as SimpleGroup, schema))
                            }
                            store = reader.readNextRowGroup()
                        }
                    }
                }
            }
        )
    }

    private fun groupToDatum(group: SimpleGroup, schema: MessageType): Datum {
        val fields = (0 until schema.fieldCount).map { i ->
            val value = if (group.getFieldRepetitionCount(i) == 0) {
                Datum.nullValue()
            } else {
                convertValue(group, i, schema.getType(i))
            }
            Field.of(schema.getFieldName(i), value)
        }
        return Datum.struct(fields)
    }

    private fun convertValue(
        group: SimpleGroup,
        i: Int,
        type: org.apache.parquet.schema.Type,
    ): Datum {
        if (!type.isPrimitive) {
            return groupToDatum(
                group.getGroup(i, 0) as SimpleGroup,
                type.asGroupType() as MessageType
            )
        }
        val pt = type.asPrimitiveType()
        val logical = pt.logicalTypeAnnotation
        return when (pt.primitiveTypeName) {
            PrimitiveTypeName.BOOLEAN -> Datum.bool(group.getBoolean(i, 0))
            PrimitiveTypeName.INT32 ->
                if (logical is LogicalTypeAnnotation.DateLogicalTypeAnnotation) {
                    Datum.date(java.time.LocalDate.ofEpochDay(group.getInteger(i, 0).toLong()))
                } else {
                    Datum.integer(group.getInteger(i, 0))
                }
            PrimitiveTypeName.INT64 -> Datum.bigint(group.getLong(i, 0))
            PrimitiveTypeName.FLOAT -> Datum.doublePrecision(group.getFloat(i, 0).toDouble())
            PrimitiveTypeName.DOUBLE -> Datum.doublePrecision(group.getDouble(i, 0))
            PrimitiveTypeName.BINARY ->
                if (logical is LogicalTypeAnnotation.DecimalLogicalTypeAnnotation) {
                    val bytes = group.getBinary(i, 0).bytes
                    Datum.decimal(BigDecimal(BigInteger(bytes), logical.scale), logical.precision, logical.scale)
                } else {
                    Datum.string(group.getString(i, 0))
                }
            PrimitiveTypeName.FIXED_LEN_BYTE_ARRAY ->
                if (logical is LogicalTypeAnnotation.DecimalLogicalTypeAnnotation) {
                    val bytes = group.getBinary(i, 0).bytes
                    Datum.decimal(BigDecimal(BigInteger(bytes), logical.scale), logical.precision, logical.scale)
                } else {
                    Datum.string(group.getValueToString(i, 0))
                }
            else -> Datum.string(group.getValueToString(i, 0))
        }
    }
}
