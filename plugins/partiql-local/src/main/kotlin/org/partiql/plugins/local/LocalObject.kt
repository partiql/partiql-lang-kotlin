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

package org.partiql.plugins.local

import org.partiql.spi.connector.ConnectorObject
import org.partiql.types.StaticType

/**
 * Associate a resolved path with a [StaticType]
 *
 * @property path
 * @property type
 */
internal class LocalObject(
    val path: List<String>,
    private val type: StaticType,
) : ConnectorObject {
    override fun getType(): StaticType = type
}
