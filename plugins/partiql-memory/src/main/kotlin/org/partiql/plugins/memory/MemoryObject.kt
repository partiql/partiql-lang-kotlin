/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.partiql.plugins.memory

import org.partiql.shape.PShape
import org.partiql.spi.connector.ConnectorObject
import org.partiql.types.StaticType
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental

@OptIn(PartiQLValueExperimental::class)
public class MemoryObject(
    private val shape: PShape,
    private val value: PartiQLValue? = null,
) : ConnectorObject {

    public constructor(type: StaticType, value: PartiQLValue? = null) : this(PShape.fromStaticType(type), value)

    public fun getValue(): PartiQLValue? = value

    override fun getType(): PShape = shape
}
