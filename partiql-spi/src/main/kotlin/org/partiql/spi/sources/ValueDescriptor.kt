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

package org.partiql.spi.sources

import org.partiql.types.StaticType

/**
 * Represents the descriptor for a PartiQL Value that can be referenced in queries.
 */
public sealed class ValueDescriptor {

    /**
     * Represents a Table Descriptor in SQL terminology.
     */
    public data class TableDescriptor(
        public val name: String,
        public val attributes: List<ColumnMetadata>
    ) : ValueDescriptor()

    /**
     * Represents a [StaticType]
     */
    public data class TypeDescriptor(
        public val type: StaticType
    ) : ValueDescriptor()
}
