package org.partiql.cli.io

import org.apache.parquet.hadoop.ParquetFileReader
import org.apache.parquet.io.LocalInputFile
import org.apache.parquet.schema.LogicalTypeAnnotation
import org.apache.parquet.schema.MessageType
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName
import org.partiql.spi.catalog.Name
import org.partiql.spi.types.PType
import org.partiql.spi.types.PTypeField
import java.io.File

/**
 * A [LazyTable] backed by a Parquet file. Reads only the footer at construction time
 * to extract schema. Data reading is delegated to [DatumParquetReader].
 */
internal class ParquetTable(
    name: Name,
    file: File,
) : LazyTable(name, file, { DatumParquetReader.read(it) }) {

    private val parquetSchema: MessageType by lazy {
        ParquetFileReader.open(LocalInputFile(file.toPath())).use { it.footer.fileMetaData.schema }
    }

    override fun getSchema(): PType {
        val fields = parquetSchema.fields.map { PTypeField.of(it.name, parquetTypeToPType(it)) }
        return PType.bag(PType.row(fields))
    }

    private fun parquetTypeToPType(type: org.apache.parquet.schema.Type): PType {
        if (!type.isPrimitive) return PType.struct()
        val pt = type.asPrimitiveType()
        val logical = pt.logicalTypeAnnotation
        return when (pt.primitiveTypeName) {
            PrimitiveTypeName.BOOLEAN -> PType.bool()
            PrimitiveTypeName.INT32 ->
                if (logical is LogicalTypeAnnotation.DateLogicalTypeAnnotation) {
                    PType.date()
                } else {
                    PType.integer()
                }
            PrimitiveTypeName.INT64 -> PType.bigint()
            PrimitiveTypeName.FLOAT, PrimitiveTypeName.DOUBLE -> PType.doublePrecision()
            PrimitiveTypeName.BINARY, PrimitiveTypeName.FIXED_LEN_BYTE_ARRAY ->
                if (logical is LogicalTypeAnnotation.DecimalLogicalTypeAnnotation) {
                    PType.decimal(logical.precision, logical.scale)
                } else {
                    PType.string()
                }
            else -> PType.dynamic()
        }
    }
}
