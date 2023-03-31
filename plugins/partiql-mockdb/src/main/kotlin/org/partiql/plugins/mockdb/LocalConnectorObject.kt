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

package org.partiql.plugins.mockdb

import org.partiql.spi.connector.ConnectorObject
import org.partiql.spi.sources.ColumnMetadata
import org.partiql.spi.sources.ValueDescriptor
import org.partiql.spi.sources.ValueDescriptor.TableDescriptor
import org.partiql.spi.sources.ValueDescriptor.TypeDescriptor
import org.partiql.types.DecimalType
import org.partiql.types.IntType
import org.partiql.types.NumberConstraint
import org.partiql.types.StringType
import org.partiql.types.StructType

internal class LocalConnectorObject(
    private val schema: String
) : ConnectorObject {

    private val jsonSchema = LocalSchema.fromJson(schema)
    private val descriptor = jsonSchema.getDescriptor()

    public fun getDescriptor(): ValueDescriptor = descriptor

    init {
        println(descriptor)
    }

    //
    //
    // HELPERS
    //
    //

    private fun LocalSchema.getDescriptor(): ValueDescriptor = when (this) {
        is LocalSchema.TableSchema -> this.getDesc()
        is LocalSchema.ValueSchema -> this.getValueDesc()
    }

    private fun LocalSchema.ValueSchema.getValueDesc(): TypeDescriptor = when (this) {
        is LocalSchema.ValueSchema.StructSchema -> this.getDesc()
        is LocalSchema.ValueSchema.ScalarSchema -> this.getDesc()
    }

    private fun LocalSchema.getName() = when (this) {
        is LocalSchema.TableSchema -> this.name
        is LocalSchema.ValueSchema.ScalarSchema -> this.name
        is LocalSchema.ValueSchema.StructSchema -> this.name
    }

    private fun LocalSchema.ValueSchema.ScalarSchema.getDesc(): TypeDescriptor = when (this.type) {
        LocalObjectType.INT -> {
            val constraint = when (val size = this.attributes.getOrNull(0)) {
                null -> IntType.IntRangeConstraint.UNCONSTRAINED
                else -> when (size) {
                    2 -> IntType.IntRangeConstraint.SHORT
                    4 -> IntType.IntRangeConstraint.INT4
                    8 -> IntType.IntRangeConstraint.LONG
                    else -> error("Unsupported integer size")
                }
            }
            TypeDescriptor(IntType(constraint))
        }
        LocalObjectType.DECIMAL -> {
            val constraint = when (val prec = this.attributes.getOrNull(0)) {
                null -> DecimalType.PrecisionScaleConstraint.Unconstrained
                else -> {
                    val scale = this.attributes.getOrNull(1) ?: 0
                    DecimalType.PrecisionScaleConstraint.Constrained(precision = prec, scale)
                }
            }
            TypeDescriptor(
                DecimalType(constraint)
            )
        }
        LocalObjectType.STRING -> {
            val constraint = when (val maxLength = this.attributes.getOrNull(0)) {
                null -> StringType.StringLengthConstraint.Unconstrained
                else -> StringType.StringLengthConstraint.Constrained(
                    NumberConstraint.UpTo(maxLength)
                )
            }
            TypeDescriptor(StringType(constraint))
        }
    }

    private fun LocalSchema.ValueSchema.StructSchema.getDesc() = TypeDescriptor(
        StructType(
            fields = this.attributes.associate {
                it.getName() to it.getValueDesc().type
            },
            contentClosed = true
        )
    )

    private fun LocalSchema.TableSchema.getDesc(): TableDescriptor {
        return TableDescriptor(
            this.name,
            this.attributes.map {
                ColumnMetadata(it.getName(), it.getValueDesc().type)
            }
        )
    }
}
