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
import org.partiql.types.BagType
import org.partiql.types.DecimalType
import org.partiql.types.IntType
import org.partiql.types.NumberConstraint
import org.partiql.types.StaticType
import org.partiql.types.StringType
import org.partiql.types.StructType
import org.partiql.types.TupleConstraint

/**
 * This mock implementation of [ConnectorObject] is used to parse the [schema] into a [StaticType]. Currently,
 * this implementation allows for Tables, Structs, Ints, Decimals, and Booleans. When [LocalConnectorMetadata] requests
 * for the object's [StaticType], it returns the parsed descriptor.
 */
internal class LocalConnectorObject(
    private val schema: String
) : ConnectorObject {

    private val jsonSchema = LocalSchema.fromJson(schema)
    private val descriptor = jsonSchema.getDescriptor()

    public fun getDescriptor(): StaticType = descriptor

    //
    //
    // HELPERS
    //
    //

    private fun LocalSchema.getDescriptor(): StaticType = when (this) {
        is LocalSchema.TableSchema -> this.getDesc()
        is LocalSchema.ValueSchema -> this.getValueDesc()
    }

    private fun LocalSchema.ValueSchema.getValueDesc(): StaticType = when (this) {
        is LocalSchema.ValueSchema.StructSchema -> this.getDesc()
        is LocalSchema.ValueSchema.ScalarSchema -> this.getDesc()
    }

    private fun LocalSchema.getName() = when (this) {
        is LocalSchema.TableSchema -> this.name
        is LocalSchema.ValueSchema.ScalarSchema -> this.name
        is LocalSchema.ValueSchema.StructSchema -> this.name
    }

    private fun LocalSchema.ValueSchema.ScalarSchema.getDesc(): StaticType = when (this.type) {
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
            IntType(constraint)
        }
        LocalObjectType.DECIMAL -> {
            val constraint = when (val prec = this.attributes.getOrNull(0)) {
                null -> DecimalType.PrecisionScaleConstraint.Unconstrained
                else -> {
                    val scale = this.attributes.getOrNull(1) ?: 0
                    DecimalType.PrecisionScaleConstraint.Constrained(precision = prec, scale)
                }
            }
            DecimalType(constraint)
        }
        LocalObjectType.STRING -> {
            val constraint = when (val maxLength = this.attributes.getOrNull(0)) {
                null -> StringType.StringLengthConstraint.Unconstrained
                else -> StringType.StringLengthConstraint.Constrained(
                    NumberConstraint.UpTo(maxLength)
                )
            }
            StringType(constraint)
        }
    }

    private fun LocalSchema.ValueSchema.StructSchema.getDesc() =
        StructType(
            fields = this.attributes.map {
                StructType.Field(it.getName(), it.getValueDesc())
            },
            contentClosed = true,
            constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true)),
        )

    private fun LocalSchema.TableSchema.getDesc(): StaticType {
        return BagType(
            StructType(
                fields = this.attributes.map {
                    StructType.Field(it.getName(), it.getValueDesc())
                },
                contentClosed = true
            )
        )
    }
}
