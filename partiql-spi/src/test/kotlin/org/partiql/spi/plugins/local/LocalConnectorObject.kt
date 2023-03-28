/*
 * Copyright Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 *  A copy of the License is located at:
 *
 *       http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

package org.partiql.spi.plugins.local

import org.partiql.spi.connector.ConnectorObject
import org.partiql.spi.sources.ColumnMetadata
import org.partiql.spi.sources.ValueDescriptor
import org.partiql.spi.sources.ValueDescriptor.TableDescriptor
import org.partiql.spi.sources.ValueDescriptor.TypeDescriptor
import org.partiql.types.DecimalType
import org.partiql.types.IntType
import org.partiql.types.StringType
import org.partiql.types.StructType

internal class LocalConnectorObject(
    private val schema: String
) : ConnectorObject {
    private val jsonSchema = LocalSchema.fromJson(schema)
    private val name = jsonSchema.name
    private val type = jsonSchema.type
    private val columns = jsonSchema.attributes.map { attr ->
        val type = when (attr.type.toUpperCase()) {
            "INT" -> IntType()
            "STRING" -> StringType()
            "DECIMAL" -> {
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
            "STRUCT" -> {
                val fields = attr.typeParams.associate {
                    val (name, type) = it.split("::")
                    name to parseSimpleType(type)
                }
                StructType(
                    fields,
                    contentClosed = true
                )
            }
            else -> error("Unrecognized type: ${attr.type}")
        }
        ColumnMetadata(
            name = attr.name,
            type = type
        )
    }

    private fun parseSimpleType(type: String) = when (type.toUpperCase()) {
        "INT" -> IntType()
        "DECIMAL" -> DecimalType()
        "STRING" -> StringType()
        "STRUCT" -> StructType()
        else -> error("Unable to parse type: $type")
    }

    public fun getDescriptor(): ValueDescriptor {
        return when (type) {
            LocalObjectType.INT -> TypeDescriptor(IntType())
            LocalObjectType.TABLE -> TableDescriptor(name, columns)
            LocalObjectType.STRUCT -> {
                val fields = columns.associate {
                    it.name to it.type
                }
                TypeDescriptor(type = StructType(fields, contentClosed = true))
            }
        }
    }
}
