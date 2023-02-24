package org.partiql.cli.plugin.localdb

import org.partiql.spi.connector.ConnectorTableHandle
import org.partiql.spi.sources.ColumnMetadata
import org.partiql.spi.sources.TableSchema
import org.partiql.spi.types.DecimalType
import org.partiql.spi.types.IntType
import org.partiql.spi.types.StringType

internal class LocalConnectorTableHandle(
    private val schema: String
) : ConnectorTableHandle {
    private val jsonSchema = LocalSchema.fromJson(schema)
    private val name = jsonSchema.name
    private val columns = jsonSchema.attributes.map { attr ->
        val type = when (attr.type) {
            "INT" -> IntType()
            "STRING" -> StringType()
            "DOUBLE" -> {
                when (attr.typeParams.size) {
                    2 -> {
                        val precision = attr.typeParams[0].toInt()
                        val scale = attr.typeParams[1].toInt()
                        DecimalType(DecimalType.PrecisionScaleConstraint.Constrained(precision, scale))
                    }
                    0 -> DecimalType()
                    else -> error("Unsupported number of decimal parameters")
                }
            }
            else -> TODO("JsonDB ONLY SUPPORTS INT TYPE")
        }
        ColumnMetadata(
            name = attr.name,
            type = type,
            comment = null,
            emptyMap()
        )
    }
    public fun getSchemaMetadata(): TableSchema {
        return TableSchema(name, emptyList(), TableSchema.SchemaOrdering.UNORDERED, TableSchema.AttributeOrdering.ORDERED, columns)
    }
}
